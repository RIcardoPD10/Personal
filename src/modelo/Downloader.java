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

/** Clase principal encargada de buscar y descargar torrents y subt�tulos.
 *
 * @author Ricardo P�rez D�ez */
public class Downloader extends Observable
{
	// -----------------------------------------------------------------
	// Atributos
	// -----------------------------------------------------------------
	
	/** Navegador para descargar links de subt�tulos */
	private WebClient		wc;
	
	/** Ejecutar en modo de pruebas (no descarga ni actualiza BD) */
	private final boolean	test	= false;
	
	/** Hora para autoverificar por series nuevas */
	private FechaH			checkHour;
	
	/** Hora de la �ltima verificaci�n de proximidad */
	private FechaH			lastCheckHour;
	
	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------
	
	/** M�todo Constructor.
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
	// M�todos
	// -----------------------------------------------------------------
	
	/** Automatizamente verifica las series que salieron al aire hace 2 d�as. */
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
									row.createCell(1).setCellValue("Verificaci�n");
									row.createCell(3).setCellValue(f.getA�o());
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
										row.createCell(3).setCellValue(f.getA�o());
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
							checkHour = new FechaH(new Fecha().toMa�ana(), new Hora(3, 0, 0, Hora.PM));
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
	 *             comunicaci�n */
	public void checkAll() throws Exception
	{
		final Vector<Serie> series = DataBase.getSeries(true);
		
		for (int i = 0; i < series.getSize(); i++)
			checkSerie(series.get(i));
	}
	
	/** Verifica si hay nuevos episodios y los descarga con sus subt�tulos.
	 *
	 * @param serie Serie a verificar
	 * @throws Exception Problemas con la base de datos o con los canales de
	 *             comunicaci�n */
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
	
	/** Busca los subt�tulos de una serie y los descarga.
	 *
	 * @param serie Serie a descargar
	 * @param temp Si es >0 descarga esa temporada. Si no, descarga la �ltima
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
					while (!(pag[i].trim().startsWith("Versi�n") && !pag[i].contains("Web")))
					{
						if (pag[i].endsWith("descargar")) page.tabToNextElement();
						i++;
					}
					
					boolean espa�olEspa�aEncontrado = false;
					int espa�olEspa�aTabs = 0;
					boolean espa�olLatamEncontrado = false;
					int espa�olLatamTabs = 0;
					
					i++;
					
					while (pag[i].length() != 0)
					{
						if (pag[i].startsWith("Espa�ol (Espa�a)") && pag[i].endsWith("descargar") && !espa�olEspa�aEncontrado)
						{
							espa�olEspa�aTabs++;
							espa�olEspa�aEncontrado = true;
						}
						else if (pag[i].startsWith("Espa�ol (Latinoam�rica)") && pag[i].endsWith("descargar"))
						{
							espa�olLatamTabs++;
							espa�olLatamEncontrado = true;
							break;
						}
						
						if (pag[i].endsWith("descargar"))
						{
							if (!espa�olEspa�aEncontrado) espa�olEspa�aTabs++;
							if (!espa�olLatamEncontrado) espa�olLatamTabs++;
						}
						
						i++;
					}
					
					if (espa�olLatamEncontrado) for (int tabs = 0; tabs < espa�olLatamTabs; tabs++)
						page.tabToNextElement();
					else if (espa�olEspa�aEncontrado) for (int tabs = 0; tabs < espa�olEspa�aTabs; tabs++)
						page.tabToNextElement();
					
					if (espa�olEspa�aEncontrado || espa�olLatamEncontrado)
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
							row.createCell(4).setCellValue("Subt�tulo");
							row.createCell(5).setCellValue(f.getA�o());
							row.createCell(6).setCellValue(f.getMes());
							row.createCell(7).setCellValue(f.getDia());
							row.createCell(8).setCellValue(f.getSemana());
						}
						
						episodio.setProgreso("Subt�tulos descargados");
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
	
	/** @return Hora de verificaci�n autom�tica */
	public FechaH getCheckHour()
	{
		return checkHour;
	}
	
	/** @return Hora de la �ltima verificaci�n de proximidad */
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
								row.createCell(5).setCellValue(f.getA�o());
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
								row.createCell(5).setCellValue(f.getA�o());
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
	
	/** Cambia la hora de la pr�xima verificaci�n autom�tica.
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
	 * @throws Exception Datos inv�lidos */
	public static void addSerie(final Serie serie) throws SQLException, Exception
	{
		if (serie.getNombre().equals("")) throw new Exception("Nombre inv�lido.");
		else if (serie.getEpisodioActual().getTemporada() < 0) throw new Exception("Temporada inv�lida.");
		else if (serie.getEpisodioActual().getEpisodio() < 0) throw new Exception("Episodio inv�lido.");
		else if (serie.getEztv() == 0) throw new Exception("Id de eztv inv�lido.");
		else if (serie.getSubs() == 0) throw new Exception("Id de subtitulos.es inv�lido.");
		
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
	
	/** Obtiene el d�a que la serie sale al aire.
	 *
	 * @param serie Serie a buscar
	 * @return D�a que sale al aire
	 * @throws Exception Problemas consultando la p�gina */
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
	
	/** Env�a un mensaje para que sea tratado por el servidor.
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
