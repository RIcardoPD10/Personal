package modelo;

/** Clase que representa una versi�n de un episodio.
 * 
 * @author Ricardo P�rez D�ez */
public class Version
{
	// -----------------------------------------------------------------
	// Atributos
	// -----------------------------------------------------------------
	
	/** Nombre de la versi�n */
	private final String  nombre;
	
	/** Si es HD (720p) */
	private final boolean isHd;
	
	/** Link magnet para descargar */
	private final String  link;
	
	/** Progreso actual de la serie */
	private String        progreso;
	
	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------
	
	/** M�todo Constructor. Crea una versi�n.
	 * 
	 * @param nombre Nombre de la versi�n
	 * @param isHd Si es HD (720p)
	 * @param link Link magnet para descargar */
	public Version(final String nombre, final boolean isHd, final String link)
	{
		super();
		this.nombre = nombre;
		this.isHd = isHd;
		this.link = link;
	}
	
	// -----------------------------------------------------------------
	// M�todos
	// -----------------------------------------------------------------
	
	/** @return the link */
	public String getLink()
	{
		return link;
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
	
	/** @return the isHd */
	public boolean isHd()
	{
		return isHd;
	}
	
	/** @param progreso the progreso to set */
	public void setProgreso(final String progreso)
	{
		this.progreso = progreso;
	}
	
	@Override
	public String toString()
	{
		return nombre + (isHd ? " HD" : "");
	}
	
}
