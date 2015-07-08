package modelo;

import interfaz.InterfazDownloader;

import java.io.File;
import java.sql.SQLException;
import java.util.Observable;

import clases.Archivo;
import estructuras.implementations.vectores.Vector;

/** Clase principal encargada de organizar las series en sus respectivas
 * carpetas.
 *
 * @author Ricardo Pérez Díez */
public class Subtitler extends Observable
{
	// -----------------------------------------------------------------
	// Constantes
	// -----------------------------------------------------------------

	/** Carpeta a donde se moverán los archivos de las series */
	public static final File		RUTA_DESTINO	= new File("//PEREZDIEZSERVER/CinePerez");

	// Estados ---------------------------------------------------------

	/** Pendiente */
	public static final String		PENDIENTE		= "Pendiente";

	/** Moviendo */
	private static final String		MOVIENDO		= "Actualmente Moviendo";

	/** Complete */
	private static final String		COMPLETE		= "Complete";

	/** No se moverá */
	public static final String		NO_MOVER		= "No se moverá";

	/** Está siendo usado */
	private static final String		USED			= "Está siendo usado";

	// -----------------------------------------------------------------
	// Atributos
	// -----------------------------------------------------------------

	/** Series a organizar */
	private final Vector<Serie>		series;

	/** Videos en calidad normal */
	private final Vector<Archivo>	videosMP4;

	/** Videos en calidad HD */
	private final Vector<Archivo>	videosMKV;

	/** Subtítulos */
	private final Vector<Archivo>	subs;

	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------

	/** Método Constructor. */
	public Subtitler()
	{
		series = new Vector<>();
		videosMP4 = new Vector<>();
		videosMKV = new Vector<>();
		subs = new Vector<>();
		recon(true);
	}

	// -----------------------------------------------------------------
	// Métodos
	// -----------------------------------------------------------------

	/** @return the series */
	public Vector<Serie> getSeries()
	{
		return series;
	}

	/** Mueve los archivos a su carpeta correspondiente.
	 *
	 * @param archivos Vector con los archivos */
	private void move(final Vector<Archivo> archivos)
	{
		for (int i = 0; i < archivos.getSize(); i++)
		{
			final Archivo a = archivos.get(i);
			if (a.canBeModified())
			{
				final String nombre = a.getNombre();

				int index = nombre.indexOf(" S0");
				if (index == -1) index = nombre.indexOf(" S1");

				if (index != -1)
				{
					Serie serie = null;
					try
					{
						serie = DataBase.getSerie(nombre.substring(0, index));
					}
					catch (final SQLException e)
					{
						InterfazDownloader.deployError(e);
					}
					String s;

					if (serie == null || serie.isSerie()) s = "Series";
					else s = "Documentales";

					String ruta = RUTA_DESTINO.getAbsolutePath() + File.separator + s + File.separator;
					ruta += nombre.substring(0, index) + File.separator + nombre.substring(0, index + 4);

					Version v = new Version(null, false, null);
					if (serie != null)
					{
						serie = series.get(serie);
						final int temporada = Integer.parseInt(a.getName().substring(index + 2, index + 4));
						final int episodio = Integer.parseInt(a.getName().substring(index + 5, index + 7));
						final Vector<Version> versiones = serie.getEpisodio(new Episodio(temporada, episodio)).getVersiones();

						for (int j = 0; j < versiones.getSize(); j++)
							if (a.getExtension(false).equals(versiones.get(j).getLink())) v = versiones.get(j);

						if (v.getProgreso().equals(PENDIENTE)) v.setProgreso(MOVIENDO);

						notificar(serie);
					}

					if (v.getProgreso().equals(MOVIENDO))
					{
						new File(ruta).mkdirs();

						if (a.getExtension(false).equals("srt")) a.renameTo(new File(ruta + File.separator, a.getNombre() + ".spa.srt"));
						else a.renameTo(new File(ruta + File.separator, a.getName()));

						if (serie != null)
						{
							v.setProgreso(COMPLETE);
							notificar(serie);
						}
					}
				}
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

	/** Renombra y mueve los archivos a sus carpetas correspondientes. */
	public void ordenar()
	{
		rename(videosMP4);
		rename(videosMKV);
		if (Preferencias.serverFound)
		{
			recon(false);
			move(videosMP4);
			move(videosMKV);
			move(subs);
		}
	}

	/** Reconoce un archivo.
	 *
	 * @param a Archivo a reconocer */
	private void recognize(Archivo a)
	{
		Archivo or = a;
		int i = a.getName().indexOf(" S0");
		if (i == -1) i = a.getName().indexOf(" S1");
		if (i == -1) i = a.getName().indexOf(".S0");
		if (i == -1) i = a.getName().indexOf(".S1");
		if (i == -1)
		{
			a = a.getParentFile();
			i = a.getName().indexOf(" S0");
			if (i == -1) i = a.getName().indexOf(" S1");
			if (i == -1) i = a.getName().indexOf(".S0");
			if (i == -1) i = a.getName().indexOf(".S1");
		}

		if (i != -1)
		{
			final String nomS = nameException(a.getName().substring(0, i).replace('.', ' '));
			final int temporada = Integer.parseInt(a.getName().substring(i + 2, i + 4));
			final int episodio = Integer.parseInt(a.getName().substring(i + 5, i + 7));
			Serie s = series.get(new Serie(nomS, false, 0, 0, null, 0, 0));

			if (a.getExtension(false).equals("srt"))
			{
				if (s == null) try
				{
					s = DataBase.getSerie(nomS);
					if (s != null) series.add(s);
				}
				catch (final SQLException e)
				{
					InterfazDownloader.deployError(e);
				}

				if (s != null)
				{
					final String nomE = a.getNombre().substring(i + 10);
					Episodio e = new Episodio(nomE, temporada, episodio);
					final Version v = new Version("Subtítulos", false, "srt");

					final Episodio eF = s.getEpisodio(e);
					if (eF != null) e = eF;

					v.setProgreso(PENDIENTE);
					e.addVersion(v);
					s.addEpisodio(e);
				}
			}
			else if (s != null)
			{
				Episodio e = s.getEpisodio(new Episodio(temporada, episodio));
				if (e != null)
				{
					final String nomV = a.getNombre().substring(i + 8).replace('.', ' ');
					
					final Version v = new Version(nomV, or.getExtension(false).equals("mkv"), or.getExtension(false));
					if (!or.canBeModified())
					{
						v.setProgreso(USED);
						e.setUsed();
					}
					else v.setProgreso(PENDIENTE);

					e.addVersion(v);
				}
			}
		}
	}

	/** Analiza la carpeta de descargas y agrega cada tipo de archivo a su vector
	 * correspondiente.
	 *
	 * @param recognize Reconocer los archivos {@link #recognize(Archivo)} */
	public void recon(final boolean recognize)
	{
		videosMP4.removeAll();
		videosMKV.removeAll();
		subs.removeAll();

		final File[] files = new File(Preferencias.ruta).listFiles();

		for (final File file: files)
			if (!file.isDirectory())
			{
				final Archivo a = new Archivo(file);
				if (isSerie(a)) if (a.getName().endsWith(".srt")) subs.add(a);
				else if (a.getName().endsWith(".mp4") || a.getName().endsWith(".avi")) videosMP4.add(a);
				else if (a.getName().endsWith(".mkv")) videosMKV.add(a);
			}
			else
			{
				final File[] filesIn = file.listFiles();
				for (final File fileIn: filesIn)
					if (!fileIn.isDirectory())
					{
						final Archivo a = new Archivo(fileIn);
						if (isSerie(a)) if (a.getName().endsWith(".mp4") || a.getName().endsWith(".avi")) videosMP4.add(a);
						else if (a.getName().endsWith(".mkv")) videosMKV.add(a);
					}
			}
		if (recognize)
		{
			for (int i = 0; i < subs.getSize(); i++)
				recognize(subs.get(i));
			for (int i = 0; i < videosMP4.getSize(); i++)
				recognize(videosMP4.get(i));
			for (int i = 0; i < videosMKV.getSize(); i++)
				recognize(videosMKV.get(i));

			for (int i = 0; i < series.getSize(); i++)
			{
				final Serie s = series.get(i);
				for (Episodio e: s.getEpisodios())
				{
					if (e.isUsed() || !e.getProgreso().equals(""))
					{
						for (Version v: e.getVersiones())
							if (v.getProgreso().equals(Subtitler.PENDIENTE)) v.setProgreso(Subtitler.NO_MOVER);
					}
				}
			}
		}
	}

	/** Renombra los videos al mismo nombre que los subtítulos
	 *
	 * @param videos Vector con archivos de videos */
	private void rename(final Vector<Archivo> videos)
	{
		for (int i = 0; i < videos.getSize(); i++)
			if (videos.get(i).canBeModified())
			{
				String nombre = videos.get(i).getName();
				if (!nombre.contains(" "))
				{
					int index = nombre.indexOf(".S0");
					if (index == -1) index = nombre.indexOf(".S1");

					nombre = nombre.substring(0, index + 7).replace('.', ' ');
					nombre = nameException(nombre);

					for (int j = 0; j < subs.getSize(); j++)
						if (subs.get(j).getName().toUpperCase().startsWith(nombre.toUpperCase()))
						{
							final String sub = subs.get(j).getNombre();

							videos.get(i).renameTo(new File(Preferencias.ruta, sub + videos.get(i).getExtension(true)));
							break;
						}
				}
			}
	}

	/** Si el archivo corresponde a alguna serie.
	 *
	 * @param a Archivo a analizar
	 * @return Si corresponde a alguna serie */
	private static boolean isSerie(Archivo a)
	{
		try
		{
			int i = a.getName().indexOf(" S0");
			if (i == -1) i = a.getName().indexOf(" S1");
			if (i == -1) i = a.getName().indexOf(".S0");
			if (i == -1) i = a.getName().indexOf(".S1");
			if (i == -1)
			{
				a = a.getParentFile();
				i = a.getName().indexOf(" S0");
				if (i == -1) i = a.getName().indexOf(" S1");
				if (i == -1) i = a.getName().indexOf(".S0");
				if (i == -1) i = a.getName().indexOf(".S1");
			}

			if (i != -1)
			{
				final String nomS = nameException(a.getName().substring(0, i).replace('.', ' '));
				Integer.parseInt(a.getName().substring(i + 2, i + 4));
				Integer.parseInt(a.getName().substring(i + 5, i + 7));
				final Serie s = DataBase.getSerie(nameException(nomS));

				if (s != null) return true;
			}
		}
		catch (final Exception e)
		{
			return false;
		}
		return false;
	}

	/** Cambia el nombre por el que está en la base de datos.
	 *
	 * @param nombre Nombre a cambiar
	 * @return Nombre cambiado */
	private static String nameException(final String nombre)
	{
		if (nombre.startsWith("Scandal US")) return nombre.replaceFirst("Scandal US", "Scandal").trim();
		else if (nombre.startsWith("Da Vincis")) return nombre.replaceFirst("Da Vincis", "Da Vinci's");
		else if (nombre.startsWith("Cosmos A Space Time Odyssey")) return nombre.replaceFirst("Cosmos A Space Time", "Cosmos - A Space-Time");
		else if (nombre.startsWith("House of Cards 2013")) return nombre.replaceFirst("House of Cards 2013", "House of Cards");
		else if (nombre.startsWith("House Of Cards 2013")) return nombre.replaceFirst("House Of Cards 2013", "House of Cards");
		else if (nombre.startsWith("House of Cards 2014")) return nombre.replaceFirst("House of Cards 2014", "House of Cards");

		return nombre;
	}

}
