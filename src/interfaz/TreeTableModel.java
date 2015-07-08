package interfaz;

import modelo.Episodio;
import modelo.Serie;
import modelo.Version;

import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import estructuras.implementations.vectores.Vector;

/** Modelo para crear el TreeTable.
 * 
 * @author Ricardo Pérez Díez */
public class TreeTableModel extends AbstractTreeTableModel
{
	// -----------------------------------------------------------------
	// Constantes
	// -----------------------------------------------------------------
	
	/** Columnas */
	private final static String[] COLUMN_NAMES = { "Nombre", "Referencia", "Progreso" };
	
	// -----------------------------------------------------------------
	// Atributos
	// -----------------------------------------------------------------
	
	/** Series a mostrar */
	private final Vector<Serie>   series;
	
	/** Si columna progreso será para checkear si se desea descargar */
	// private final boolean check;
	
	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------
	
	/** Método Constructor. Inicia el modelo.
	 * 
	 * @param series Series a mostrar
	 * @param check Si columna progreso será para checkear si se desea descargar */
	public TreeTableModel(final Vector<Serie> series, final boolean check)
	{
		super(new Object());
		this.series = series;
		// this.check = check;
	}
	
	// -----------------------------------------------------------------
	// Métodos
	// -----------------------------------------------------------------
	
	@Override
	public Object getChild(final Object parent, final int index)
	{
		if (parent instanceof Serie)
		{
			final Serie serie = (Serie) parent;
			return serie.getEpisodios().get(index);
		}
		else if (parent instanceof Episodio)
		{
			final Episodio episodio = (Episodio) parent;
			return episodio.getVersiones().get(index);
		}
		
		return series.get(index);
	}
	
	@Override
	public int getChildCount(final Object parent)
	{
		if (parent instanceof Serie)
		{
			final Serie serie = (Serie) parent;
			return serie.getEpisodios().getSize();
		}
		else if (parent instanceof Episodio)
		{
			final Episodio episodio = (Episodio) parent;
			return episodio.getVersiones().getSize();
		}
		return series.getSize();
	}
	
	@Override
	public Class<?> getColumnClass(final int column)
	{
		// if (check && column == 2)
		// return Boolean.class;
		
		return super.getColumnClass(column);
	}
	
	@Override
	public int getColumnCount()
	{
		return COLUMN_NAMES.length;
	}
	
	@Override
	public String getColumnName(final int column)
	{
		return COLUMN_NAMES[column];
	}
	
	@Override
	public int getIndexOfChild(final Object parent, final Object child)
	{
		if (parent instanceof Serie)
		{
			final Serie serie = (Serie) parent;
			final Episodio epi = (Episodio) child;
			return serie.getEpisodios().getIndex(epi);
		}
		else if (parent instanceof Episodio)
		{
			final Episodio episodio = (Episodio) parent;
			final Version ver = (Version) child;
			return episodio.getVersiones().getIndex(ver);
		}
		return 0;
	}
	
	@Override
	public Object getValueAt(final Object node, final int column)
	{
		if (node instanceof Serie)
		{
			final Serie serie = (Serie) node;
			switch (column)
			{
				case 0:
					return serie.getNombre();
				case 1:
					return serie.getEpisodioActual();
				case 2:
					return serie.getProgreso();
			}
		}
		else if (node instanceof Episodio)
		{
			final Episodio epi = (Episodio) node;
			switch (column)
			{
				case 0:
					return epi.getNombre();
				case 1:
					return epi.getReferencia();
				case 2:
					return epi.getProgreso();
			}
		}
		else if (node instanceof Version)
		{
			final Version ver = (Version) node;
			switch (column)
			{
				case 0:
					return ver.getNombre();
				case 1:
					return ver.getLink().startsWith("magnet") ? "" : ver.getLink();
				case 2:
					return ver.getProgreso();
			}
		}
		return "";
	}
	
	@Override
	public boolean isLeaf(final Object node)
	{
		return node instanceof Version;
	}
}
