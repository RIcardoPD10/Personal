package modelo;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import estructuras.implementations.vectores.Vector;

/** Clase que representa una serie.
 * 
 * @author Ricardo Pérez Díez */
public class Serie implements Serializable
{
	// -----------------------------------------------------------------
	// Constantes
	// -----------------------------------------------------------------
	
	/** Constante de Serialización */
	private static final long      serialVersionUID = 1L;
	
	/** Campos de una serie */
	public static final String[]   CAMPOS           = new String[] { "Nombre", "Último Descargado", "Día Aire", "Id EZTV", "Id subtitulos.es" };
	
	// -----------------------------------------------------------------
	// Atributos
	// -----------------------------------------------------------------
	
	/** Nombre de la serie */
	private final String           nombre;
	
	/** Si es serie o documental */
	private final boolean          isSerie;
	
	/** Último episodio descargado */
	private final Episodio         actual;
	
	/** Último episodio encontrado */
	private Episodio               encontrado;
	
	/** Día en que sale al aire */
	private String                 dia;
	
	/** Id de la serie en eztv */
	private final int              eztv;
	
	/** Id de la serie en subtitulos.es */
	private final int              subs;
	
	/** Episodios encontrados */
	private final Vector<Episodio> episodios;
	
	/** Progreso actual de la serie */
	private String                 progreso;
	
	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------
	
	/** Método Constructor.
	 * 
	 * @param nombre Nombre de la serie
	 * @param isSerie Si es una serie o un documental
	 * @param temporada Temporada del último episodio descargado
	 * @param episodio Último episodio descargado
	 * @param eztv URL de la serie en eztv
	 * @param subs URL de la serie en subtitulos.es */
	public Serie(final String nombre, final boolean isSerie, final int temporada, final int episodio, final int eztv, final int subs)
	{
		this.nombre = nombre;
		this.isSerie = isSerie;
		actual = new Episodio(temporada, episodio);
		this.eztv = eztv;
		this.subs = subs;
		
		episodios = new Vector<>();
	}
	
	/** Método Constructor.
	 * 
	 * @param nombre Nombre de la serie
	 * @param isSerie Si es una serie o un documental
	 * @param temporada Temporada del último episodio descargado
	 * @param episodio Último episodio descargado
	 * @param dia Dia en que sale al aire
	 * @param eztv URL de la serie en eztv
	 * @param subs URL de la serie en subtitulos.es */
	public Serie(final String nombre, final boolean isSerie, final int temporada, final int episodio, final String dia, final int eztv, final int subs)
	{
		this.nombre = nombre;
		this.isSerie = isSerie;
		actual = new Episodio(temporada, episodio);
		this.dia = dia;
		this.eztv = eztv;
		this.subs = subs;
		
		episodios = new Vector<>();
	}
	
	/** Método Constructor. Uso únicamente para calcular las webs de EZTV.
	 * 
	 * @param eztv URL de la serie en eztv */
	private Serie(final int eztv)
	{
		this.nombre = null;
		this.isSerie = false;
		actual = null;
		this.dia = null;
		this.eztv = eztv;
		this.subs = 0;
		
		episodios = null;
	}

	// -----------------------------------------------------------------
	// Métodos
	// -----------------------------------------------------------------
	
	/** Determina si el episodio dado es más nuevo que el actual.
	 * 
	 * @param temporada Temporada
	 * @param episodio Episodio
	 * @return Si el episodio dado por parámetro es mas nuevo que el actual */
	public boolean isNewer(int temporada, int episodio)
	{
		if (temporada > actual.getTemporada()) return true;
		if (actual.getTemporada() == temporada && actual.getEpisodio() < episodio) return true;
		return false;
	}

	/** Agrega un episodio.
	 * 
	 * @param episodio Episodio a agregar */
	public void addEpisodio(final Episodio episodio)
	{
		episodios.add(episodio);
		if (encontrado == null || episodio.getTemporada() > encontrado.getTemporada()
		        || (episodio.getTemporada() == encontrado.getTemporada() && episodio.getEpisodio() > encontrado.getEpisodio()))
			encontrado = episodio;
	}
	
	@Override
	public boolean equals(final Object obj)
	{
		if (obj instanceof Serie)
			return nombre.equals(((Serie) obj).getNombre());
		return false;
	}
	
	/** @return the dia */
	public String getDia()
	{
		return dia;
	}
	
	/** Busca el objeto del episodio.
	 * 
	 * @param episodio Episodio buscado
	 * @return Episodio */
	public Episodio getEpisodio(final Episodio episodio)
	{
		return episodios.get(episodio);
	}
	
	/** @return the episodio */
	public Episodio getEpisodioActual()
	{
		return actual;
	}
	
	/** @return the episodioEncontrada */
	public Episodio getEpisodioEncontrado()
	{
		return encontrado;
	}
	
	/** @return the episodios */
	public Vector<Episodio> getEpisodios()
	{
		return episodios;
	}
	
	/** @return Id eztv */
	public int getEztv()
	{
		return eztv;
	}
	
	/** @return Página eztv */
	public String getEztvS()
	{
		// return "http://185.19.104.70/shows/" + eztv + "/";vieja
		// return "http://162.159.243.249/shows/" + eztv + "/";
		// String start = "https://eztv-proxy.net/"; //Proxy
		// String start = "https://eztv.it/";
		String start = "https://eztv.ch/";
		
		return start + "shows/" + eztv + "/";
	}
	
	/** @return URL eztv */
	public URL getEztvU()
	{
		try
		{
			return new URL(getEztvS());
		}
		catch (final MalformedURLException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/** @return URL TPB */
	public URL getTPBU()
	{
		try
		{
			return new URL("https://thepiratebay.la/search/" + getNombre().replaceAll(" ", "%20") + "/0/3/200");
		}
		catch (final MalformedURLException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	/** @return the nombre */
	public String getNombre()
	{
		return nombre;
	}
	
	/** @return the progreso */
	public String getProgreso()
	{
		return progreso;
	}
	
	/** Obtiene un arreglo con toda la información de la serie.
	 * 
	 * @return Arreglo con toda la información de la serie */
	public Object[] getSerie()
	{
		return new Object[] { nombre, actual.getReferencia(), dia, new Integer(eztv), new Integer(subs) };
	}
	
	/** @return ID subtitulos.es */
	public int getSubs()
	{
		return subs;
	}
	
	/** @param temp Si es >0 descarga esa temporada. Si no, descarga la última
	 *            temporada descargada
	 *            actual
	 * @return Página subtitulos.es */
	public String getSubsLink(final int temp)
	{
		return "http://www.subtitulos.es/ajax_loadShow.php?show=" + subs + "&season=" + (temp > 0 ? temp : actual.getTemporada());
	}
	
	/** @return the isSerie */
	public boolean isSerie()
	{
		return isSerie;
	}
	
	/** Verifica si la serie tiene episodios nuevos
	 * 
	 * @return La serie tiene episodios nuevos */
	public boolean lastDifferent()
	{
		return encontrado != null && !actual.equals(encontrado);
	}
	
	/** Remueve el episodio por ser el segundo de un episodio doble.
	 * 
	 * @param temporada Temporada del episodio
	 * @param episodio Número del episodio */
	public void removeDoble(final int temporada, final int episodio)
	{
		episodios.delete(new Episodio(temporada, episodio));
	}
	
	/** @param dia the dia to set */
	public void setDia(final String dia)
	{
		this.dia = dia;
	}
	
	/** @param progreso the progreso to set */
	public void setProgreso(final String progreso)
	{
		this.progreso = progreso;
	}
	
	@Override
	public String toString()
	{
		return nombre + (actual != null ? " " + actual.getReferencia() : "") + (encontrado != null ? " - " + encontrado.getReferencia() : "");
	}
	
	/** @param eztv Id de Eztv
	 * @return URI de la página */
	public static URI getEztvUri(int eztv)
	{
		Serie s = new Serie(eztv);
		
		try
		{
			return new URI(s.getEztvS());
		}
		catch (URISyntaxException e)
		{
			e.printStackTrace();
		}
		return null;
	}
	
	/** @param subs Id de subtitulos.es
	 * @return URI de la página */
	public static URI getSubsUri(int subs)
	{
		try
		{
			return new URI("http://www.subtitulos.es/show/" + subs);
		}
		catch (URISyntaxException e)
		{
			e.printStackTrace();
		}
		return null;
	}

}
