package modelo;

import interfaz.InterfazDownloader;

import java.awt.Desktop;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.sql.SQLException;
import java.util.Observable;

import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import clases.tiempo.Fecha;
import clases.tiempo.FechaH;
import clases.tiempo.Hora;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

import estructuras.implementations.vectores.Vector;
import excepciones.InvalidTimeException;

/** Clase principal encargada de buscar y descargar torrents y subtítulos.
 *
 * @author Ricardo Pérez Díez */
public class Downloader extends Observable
{
	// -----------------------------------------------------------------
	// Atributos
	// -----------------------------------------------------------------
	
	/** Navegador para descargar links de subtítulos */
	private WebClient		wc;
	
	/** Ejecutar en modo de pruebas (no descarga ni actualiza BD) */
	private final boolean	test	= false;
	
	/** Hora para autoverificar por series nuevas */
	private FechaH			checkHour;
	
	/** Hora de la última verificación de proximidad */
	private FechaH			lastCheckHour;
	
	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------
	
	/** Método Constructor.
	 *
	 * @throws Exception Problemas iniciando la base de datos */
	public Downloader() throws Exception
	{
		Preferencias.inic();
		
		DataBase.start();
		
		final Thread r = new Thread()
		{
			@ Override
			public void run()
			{
				wc = new WebClient(BrowserVersion.CHROME);
			}
		};
		
		r.start();

		checkHour = new FechaH(new Fecha(), new Hora(3, 0, 0, Hora.PM));
		// autoCheck();
	}
	
	// -----------------------------------------------------------------
	// Métodos
	// -----------------------------------------------------------------
	
	/** Automatizamente verifica las series que salieron al aire hace 2 días. */
	public void autoCheck()
	{
		final Thread r = new Thread()
		{
			@ Override
			public void run()
			{
				while (true)
					try
					{
						File file;
						if (!test) file = new File("/Autodescargas.xlsx");
						else file = new File("Z:/AutodescargasP.xlsx");

						while ((lastCheckHour = new FechaH()).compareTo(checkHour) < 0)
							Thread.sleep(3600000);
						
						try (FileInputStream input = new FileInputStream(file))
						{
							XSSFWorkbook wb = new XSSFWorkbook(input);
							XSSFSheet descargas = wb.getSheetAt(0);
							XSSFSheet verificaciones = wb.getSheetAt(1);
							
							final Vector<Serie> series = getSeries();
							
							Fecha verificar = checkHour.toAyer().toAyer();
							
							for (int i = 0; i < series.getSize(); i++)
							{
								final Serie serie = series.get(i);
								
								if (serie.getDia().equals(verificar.getDiaLargo()))
								{
									Fecha f = new Fecha();
									
									XSSFRow row = verificaciones.createRow(verificaciones.getLastRowNum() + 1);
									
									row.createCell(0).setCellValue(serie.getNombre());
									row.createCell(1).setCellValue("Verificación");
									row.createCell(3).setCellValue(f.getAño());
									row.createCell(4).setCellValue(f.getMes());
									row.createCell(5).setCellValue(f.getDia());
									row.createCell(6).setCellValue(f.getSemana());
									
									try
									{
										getLinksTorrentTPB(serie, descargas);
										downloadLinksSubtitles(serie, serie.getEpisodioActual().getTemporada(), descargas);
										
										if (serie.lastDifferent() && !test) DataBase.updateLastEpisode(serie);
									}
									catch (Exception e)
									{
										row.createCell(0).setCellValue(serie.getNombre());
										row.createCell(1).setCellValue("Error");
										row.createCell(2).setCellValue(e.getMessage());
										row.createCell(3).setCellValue(f.getAño());
										row.createCell(4).setCellValue(f.getMes());
										row.createCell(5).setCellValue(f.getDia());
										row.createCell(6).setCellValue(f.getSemana());
									}
								}
							}
							
							try (FileOutputStream excel = new FileOutputStream(file))
							{
								descargas.autoSizeColumn(0);
								descargas.autoSizeColumn(3);
								wb.setActiveSheet(0);
								wb.write(excel);
							}
						}	
						setChanged();
						notifyObservers(InterfazDownloader.UPDATE);
					}
					catch (final Exception e)
					{
						InterfazDownloader.deployError(e);
					}
					finally
					{
						try
						{
							checkHour = new FechaH(new Fecha().toMañana(), new Hora(3, 0, 0, Hora.PM));
						}
						catch (InvalidTimeException e)
						{}
					}
			}
		};
		
		r.start();
	}
	
	/** Verifica por nuevos episodios de todas las series.
	 *
	 * @throws Exception Problemas con la base de datos o con los canales de
	 *             comunicación */
	public void checkAll() throws Exception
	{
		final Vector<Serie> series = DataBase.getSeries(true);
		
		for (int i = 0; i < series.getSize(); i++)
			checkSerie(series.get(i));
	}
	
	/** Verifica si hay nuevos episodios y los descarga con sus subtítulos.
	 *
	 * @param serie Serie a verificar
	 * @throws Exception Problemas con la base de datos o con los canales de
	 *             comunicación */
	public void checkSerie(final Serie serie) throws Exception
	{
		serie.setProgreso("Checking");
		notificar(serie);
		downloadLinksSubtitles(serie, serie.getEpisodioActual().getTemporada(), null);
		
		if (serie.lastDifferent())
		{
			getLinksTorrentTPB(serie, null);
			notificar(serie);
			if (!test) DataBase.updateLastEpisode(serie);
			
			if (!Preferencias.isServer && !Preferencias.dbLocal && Preferencias.receptorFound) sendToServer(InterfazDownloader.UPDATE);
		}
		
		serie.setProgreso("Done");
		notificar(serie);
	}
	
	/** Busca los subtítulos de una serie y los descarga.
	 *
	 * @param serie Serie a descargar
	 * @param temp Si es >0 descarga esa temporada. Si no, descarga la última
	 *            temporada descargada
	 *            actual
	 * @param sheet Hoja de excel para poner los resultados
	 * @throws Exception Problemas descargando */
	public void downloadLinksSubtitles(final Serie serie, final int temp, final XSSFSheet sheet) throws Exception
	{
		final HtmlPage page = wc.getPage(serie.getSubsLink(temp));
		
		if (!page.asText().isEmpty())
		{
			final String[] pag = page.asText().split("\n");
			
			for (int i = 0; i < pag.length; i++)
				pag[i] = pag[i].trim();
			
			int i = 0;
			
			page.tabToNextElement();
			
			while (i < pag.length)
			{
				String nomEpisodio;
				if (pag[i].indexOf("-") + 2 < pag[i].length()) nomEpisodio = pag[i].substring(pag[i].indexOf("-") + 2, pag[i].length());
				else nomEpisodio = "No Title";
				nomEpisodio = nomEpisodio.replace('?', '.').trim();
				nomEpisodio = nomEpisodio.replace(':', '-');
				if (nomEpisodio.endsWith(".")) nomEpisodio = nomEpisodio.substring(0, nomEpisodio.length() - 2);
				
				final int index = pag[i].indexOf("-");
				final int tem = Integer.parseInt(pag[i].substring(index - 6, index - 4).trim());
				final int epi = Integer.parseInt(pag[i].substring(index - 3, index - 1));
				final int temA = serie.getEpisodioActual().getTemporada();
				final int epiA = serie.getEpisodioActual().getEpisodio();
				
				if ((tem == temA && epi > epiA) || tem > temA)
				{
					while (!(pag[i].trim().startsWith("Versión") && !pag[i].contains("Web")))
					{
						if (pag[i].endsWith("descargar")) page.tabToNextElement();
						i++;
					}
					
					boolean españolEspañaEncontrado = false;
					int españolEspañaTabs = 0;
					boolean españolLatamEncontrado = false;
					int españolLatamTabs = 0;
					
					i++;
					
					while (pag[i].length() != 0)
					{
						if (pag[i].startsWith("Español (España)") && pag[i].endsWith("descargar") && !españolEspañaEncontrado)
						{
							españolEspañaTabs++;
							españolEspañaEncontrado = true;
						}
						else if (pag[i].startsWith("Español (Latinoamérica)") && pag[i].endsWith("descargar"))
						{
							españolLatamTabs++;
							españolLatamEncontrado = true;
							break;
						}
						
						if (pag[i].endsWith("descargar"))
						{
							if (!españolEspañaEncontrado) españolEspañaTabs++;
							if (!españolLatamEncontrado) españolLatamTabs++;
						}
						
						i++;
					}
					
					if (españolLatamEncontrado) for (int tabs = 0; tabs < españolLatamTabs; tabs++)
						page.tabToNextElement();
					else if (españolEspañaEncontrado) for (int tabs = 0; tabs < españolEspañaTabs; tabs++)
						page.tabToNextElement();
					
					if (españolEspañaEncontrado || españolLatamEncontrado)
					{
						final Episodio episodio = new Episodio(nomEpisodio, temp, epi);
						
						if (!test) try (final InputStream is = page.getFocusedElement().click().getWebResponse().getContentAsStream())
						{
							try (final ReadableByteChannel rbc = Channels.newChannel(is))
							{
								final String nombre = Preferencias.ruta + serie.getNombre() + " " + episodio.toString() + ".srt";
								
								try (final FileOutputStream fos = new FileOutputStream(nombre))
								{
									fos.getChannel().transferFrom(rbc, 0, 1 << 24);
								}
							}
						}
						
						if (sheet != null)
						{
							Fecha f = new Fecha();
							
							XSSFRow row = sheet.createRow(sheet.getLastRowNum() + 1);
							
							row.createCell(0).setCellValue(serie.getNombre());
							row.createCell(1).setCellValue(tem);
							row.createCell(2).setCellValue(epi);
							row.createCell(3).setCellValue(episodio.getNombre());
							row.createCell(4).setCellValue("Subtítulo");
							row.createCell(5).setCellValue(f.getAño());
							row.createCell(6).setCellValue(f.getMes());
							row.createCell(7).setCellValue(f.getDia());
							row.createCell(8).setCellValue(f.getSemana());
						}
						
						episodio.setProgreso("Subtítulos descargados");
						serie.addEpisodio(episodio);
						notificar(serie);
					}
				}
				else page.tabToNextElement();
				
				while (pag[i].length() != 0)
					i++;
				
				i++;
				
				while (page.getFocusedElement().asText().equals("descargar"))
					page.tabToNextElement();
			}
			wc.closeAllWindows();
			downloadLinksSubtitles(serie, temp + 1, sheet);
		}
	}
	
	/** @return Hora de verificación automática */
	public FechaH getCheckHour()
	{
		return checkHour;
	}
	
	/** @return Hora de la última verificación de proximidad */
	public FechaH getLastCheckHour()
	{
		return lastCheckHour;
	}
	
	/** Busca nuevos episodios en EZTV de una serie y los descarga.
	 *
	 * @param serie Serie a descargar
	 * @param sheet Hoja de excel para poner los resultados
	 * @throws Exception Problemas enviando los links o descargando */
	public void getLinksTorrentEZTV(final Serie serie, final XSSFSheet sheet) throws Exception
	{
		final HttpURLConnection connection = (HttpURLConnection) serie.getEztvU().openConnection();
		connection.addRequestProperty("User-Agent", "Mozilla/5.0");
		try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream())))
		{
			String str;
			boolean old = false;
			
			while ((str = in.readLine()) != null && !old)
				if (str.contains("magnet")) try
				{
					String link = str.replaceAll("%20", ".");
					
					final int ini = link.indexOf("magnet:");
					link = link.substring(ini);
					
					final int fin = link.indexOf("\"");
					link = link.substring(0, fin);
					
					int i = link.indexOf(".S0");
					if (i == -1) i = link.indexOf(".S1");
					
					if (i != -1)
					{
						final boolean doble = !link.substring(i + 7, i + 8).equals(".");
						
						final String nombre = link.substring(i + (doble ? link.substring(i + 1).indexOf(".") + 2 : 8), i + link.substring(i).indexOf("&"));
						final int temporada = Integer.parseInt(link.substring(i + 2, i + 4));
						final int episodio = Integer.parseInt(link.substring(i + 5, i + 7));
						final boolean isHd = link.contains(".720p.");
						
						if (serie.isNewer(temporada, episodio))
						{
							if (sheet != null) serie.addEpisodio(new Episodio(nombre, temporada, episodio));
							final Episodio e = serie.getEpisodio(new Episodio(temporada, episodio));
							final Version ver = new Version(nombre, isHd, link);
							e.addVersion(ver);
							if (doble)
							{
								e.setDoble(true);
								serie.removeDoble(temporada, episodio + 1);
							}
							
							if (!test) downloadLinkTorrent(ver);
							
							if (sheet != null)
							{
								Fecha f = new Fecha();
								
								XSSFRow row = sheet.createRow(sheet.getLastRowNum() + 1);
								
								row.createCell(0).setCellValue(serie.getNombre());
								row.createCell(1).setCellValue(e.getTemporada());
								row.createCell(2).setCellValue(e.getEpisodio());
								row.createCell(3).setCellValue(ver.getNombre());
								row.createCell(4).setCellValue("Torrent");
								row.createCell(5).setCellValue(f.getAño());
								row.createCell(6).setCellValue(f.getMes());
								row.createCell(7).setCellValue(f.getDia());
								row.createCell(8).setCellValue(f.getSemana());
							}
						}
						else old = true;
					}
				}
				catch (final Exception e)
				{
					System.out.println(serie + " ---" + str);
				}
		}
	}
	
	/** Busca nuevos episodios en The Pirate Bay de una serie y los descarga.
	 *
	 * @param serie Serie a descargar
	 * @param sheet Hoja de excel para poner los resultados
	 * @throws Exception Problemas enviando los links o descargando */
	public void getLinksTorrentTPB(final Serie serie, final XSSFSheet sheet) throws Exception
	{
		final HttpURLConnection connection = (HttpURLConnection) serie.getTPBU().openConnection();
		connection.addRequestProperty("User-Agent", "Mozilla/5.0");
		try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream())))
		{
			String str;
			boolean old = false;
			
			while ((str = in.readLine()) != null && !old)
				if (str.contains("magnet:")) try
				{
					String link = str.replace('+', '.');

					final int ini = link.indexOf("magnet:");
					link = link.substring(ini);

					final int fin = link.indexOf("\"");
					link = link.substring(0, fin);

					int i = link.indexOf(".S0");
					if (i == -1) i = link.indexOf(".S1");

					if (i != -1)
					{
						final boolean doble = !link.substring(i + 7, i + 8).equals(".");

						final String nombre = link.substring(i + (doble ? link.substring(i + 1).indexOf(".") + 2 : 8), i + link.substring(i).indexOf("&"));
						final int temporada = Integer.parseInt(link.substring(i + 2, i + 4));
						final int episodio = Integer.parseInt(link.substring(i + 5, i + 7));
						final boolean isHd = link.contains(".720p.");

						if (serie.isNewer(temporada, episodio))
						{
							if (sheet != null) serie.addEpisodio(new Episodio(nombre, temporada, episodio));
							final Episodio e = serie.getEpisodio(new Episodio(temporada, episodio));
							final Version ver = new Version(nombre, isHd, link);
							e.addVersion(ver);
							if (doble)
							{
								e.setDoble(true);
								serie.removeDoble(temporada, episodio + 1);
							}

							if (!test) downloadLinkTorrent(ver);

							if (sheet != null)
							{
								Fecha f = new Fecha();
								
								XSSFRow row = sheet.createRow(sheet.getLastRowNum() + 1);

								row.createCell(0).setCellValue(serie.getNombre());
								row.createCell(1).setCellValue(e.getTemporada());
								row.createCell(2).setCellValue(e.getEpisodio());
								row.createCell(3).setCellValue(ver.getNombre());
								row.createCell(4).setCellValue("Torrent");
								row.createCell(5).setCellValue(f.getAño());
								row.createCell(6).setCellValue(f.getMes());
								row.createCell(7).setCellValue(f.getDia());
								row.createCell(8).setCellValue(f.getSemana());
							}
						}
						else old = true;
					}
			}
			catch (final Exception e)
			{
				System.out.println(serie + " ---" + str);
			}
		}
	}
	
	/** Notifica cambios en una serie.
	 *
	 * @param serie Serie a notificar */
	private void notificar(final Serie serie)
	{
		setChanged();
		notifyObservers(serie);
	}
	
	/** Cambia la hora de la próxima verificación automática.
	 *
	 * @param hora Nueva hora */
	public void setNextCheckHour(final Hora hora)
	{
		checkHour = new FechaH(new Fecha(), hora);
	}
	
	/** Agrega una serie a la base de datos.
	 *
	 * @param serie Seria a agregar
	 * @throws SQLException Problemas con la base de datos
	 * @throws Exception Datos inválidos */
	public static void addSerie(final Serie serie) throws SQLException, Exception
	{
		if (serie.getNombre().equals("")) throw new Exception("Nombre inválido.");
		else if (serie.getEpisodioActual().getTemporada() < 0) throw new Exception("Temporada inválida.");
		else if (serie.getEpisodioActual().getEpisodio() < 0) throw new Exception("Episodio inválido.");
		else if (serie.getEztv() == 0) throw new Exception("Id de eztv inválido.");
		else if (serie.getSubs() == 0) throw new Exception("Id de subtitulos.es inválido.");
		
		final String dia = getAirDay(serie);
		if (dia != null) serie.setDia(dia);
		
		DataBase.addSerie(serie);
	}
	
	/** Descarga el link magnet.
	 *
	 * @param version Version con el link a descargar
	 * @throws Exception Problemas descargando */
	protected static void downloadLinkTorrent(final Version version) throws Exception
	{
		if (Preferencias.sendTorrent())
		{
			sendToServer(version.getLink());
			version.setProgreso("Link enviado para descarga");
		}
		else
		{
			Desktop.getDesktop().browse(new URI(version.getLink()));
			version.setProgreso("Descargando Link");
		}
	}
	
	/** Obtiene el día que la serie sale al aire.
	 *
	 * @param serie Serie a buscar
	 * @return Día que sale al aire
	 * @throws Exception Problemas consultando la página */
	private static String getAirDay(final Serie serie) throws Exception
	{
		final HttpURLConnection connection = (HttpURLConnection) serie.getEztvU().openConnection();
		connection.addRequestProperty("User-Agent", "Mozilla/5.0");
		try (@ SuppressWarnings("resource")
		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream())))
		{
			String str;
			
			while ((str = in.readLine()) != null)
				if (str.contains("Airs: "))
				{
					final String dia = str.substring(str.indexOf("<b>") + 3, str.indexOf("</b>"));
					
					return Fecha.translateDay(dia);
				}
		}
		
		return null;
	}
	
	/** @return Series no terminadas
	 * @throws SQLException Problemas consultando la base de datos */
	public static Vector<Serie> getSeries() throws SQLException
	{
		return DataBase.getSeries(true);
	}
	
	/** Consulta las series de la base de datos.
	 *
	 * @param noTerminadas Solo series no terminadas
	 * @return Las series de la base de datos
	 * @throws SQLException Problemas con la base de datos */
	public static Object[][] getSeries(final boolean noTerminadas) throws SQLException
	{
		final Vector<Serie> series = DataBase.getSeries(noTerminadas);
		final Object[][] seriesM = new Object[series.getSize()][Serie.CAMPOS.length];
		
		for (int i = 0; i < series.getSize(); i++)
		{
			final Object[] serie = series.get(i).getSerie();
			seriesM[i] = serie;
		}
		
		return seriesM;
	}
	
	/** Envía un mensaje para que sea tratado por el servidor.
	 *
	 * @param msg Mensaje a ser tratado
	 * @throws Exception Problemas con los sockets */
	public static void sendToServer(final String msg) throws Exception
	{
		try (Socket socket = new Socket(Receptor.IP, Receptor.PORT))
		{
			try (final PrintWriter out = new PrintWriter(socket.getOutputStream()))
			{
				out.println(msg);
			}
		}
	}
	
}
