package modelo;

import estructuras.implementations.vectores.Vector;

/** Clase que representa un eposidio de una serie.
 *
 * @author Ricardo P�rez D�ez */
public class Episodio
{
	// -----------------------------------------------------------------
	// Constantes
	// -----------------------------------------------------------------

	// -----------------------------------------------------------------
	// Atributos
	// -----------------------------------------------------------------

	/** Nombre del episodio */
	private final String			nombre;

	/** Temporada del episodio */
	private final int				temporada;

	/** N�mero del episodio */
	private final int				episodio;

	/** Dos episodios en uno */
	private boolean					doble;

	/** Versiones */
	private final Vector<Version>	versiones;

	/** Progreso actual de la serie */
	private String					progreso;

	/** Si alguno de sus versiones est� siendo usada */
	private boolean					used;
	
	// -----------------------------------------------------------------
	// Constructores
	// -----------------------------------------------------------------

	/** M�todo Constructor. Crea un episodio.
	 *
	 * @param temporada Temporada del episodio
	 * @param episodio N�mero del episodio */
	public Episodio(final int temporada, final int episodio)
	{
		nombre = null;
		this.temporada = temporada;
		this.episodio = episodio;
		doble = false;
		used = false;

		versiones = null;
	}

	/** M�todo Constructor. Crea un episodio.
	 *
	 * @param nombre Nombre del episodio
	 * @param temporada Temporada del episodio
	 * @param episodio N�mero del episodio */
	public Episodio(final String nombre, final int temporada, final int episodio)
	{
		this.nombre = nombre;
		this.temporada = temporada;
		this.episodio = episodio;
		doble = false;

		versiones = new Vector<>();
	}

	// -----------------------------------------------------------------
	// M�todos
	// -----------------------------------------------------------------

	/** Agrega una versi�n.
	 *
	 * @param version Versi�n a agregar */
	public void addVersion(final Version version)
	{
		versiones.add(version);
		boolean soloHd = true;
		boolean soloSubs = true;
		
		for (Version v: versiones)
		{
			if (!v.isHd() && !v.getNombre().equals("Subt�tulos")) soloHd = false;
			if (!v.getNombre().equals("Subt�tulos")) soloSubs = false;
		}
		if (soloSubs) progreso = "�nicamente subt�tulos";
		else if (soloHd) progreso = "�nicamente versi�n en HD";
		else progreso = "";
	}

	@ Override
	public boolean equals(final Object obj)
	{
		final Episodio e = (Episodio) obj;
		return temporada == e.temporada && episodio == e.episodio;
	}

	/** @return the episodio */
	public int getEpisodio()
	{
		return episodio + (doble ? 1 : 0);
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

	/** @return La referencia del episodio (S01E01) */
	public String getReferencia()
	{
		return "S" + (temporada < 10 ? "0" + temporada : "" + temporada) + "E" + (episodio < 10 ? "0" + episodio : "" + episodio) + (doble ? "E" + (episodio + 1 < 10 ? "0" + (episodio + 1) : "" + (episodio + 1)) : "");
	}

	/** @return the temporada */
	public int getTemporada()
	{
		return temporada;
	}

	/** @return the versiones */
	public Vector<Version> getVersiones()
	{
		return versiones;
	}

	/** @return the doble */
	public boolean isDoble()
	{
		return doble;
	}

	/** @return Si alguna versi�n del episodio est� siendo usado */
	public boolean isUsed()
	{
		return used;
	}

	/** @param doble the doble to set */
	public void setDoble(final boolean doble)
	{
		this.doble = doble;
	}

	/** @param progreso the progreso to set */
	public void setProgreso(final String progreso)
	{
		this.progreso = progreso;
	}

	/** Establece que est� siendo usado. */
	public void setUsed()
	{
		used = true;

	}
	
	@ Override
	public String toString()
	{
		return getReferencia() + (nombre != null ? " - " + nombre : "");
	}
	
}
