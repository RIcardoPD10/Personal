package interfaz;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import modelo.Serie;
import modelo.Subtitler;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import estructuras.implementations.vectores.Vector;

/** Interfaz que muestra el progreso de búsqueda o copia de series.
 * 
 * @author Ricardo Pérez Díez */
public class FrameProgreso extends JFrame implements Observer
{
	// -----------------------------------------------------------------
	// Constantes
	// -----------------------------------------------------------------
	
	/** Constante de Serialización */
	private static final long   serialVersionUID = 1L;
	
	// -----------------------------------------------------------------
	// Atributos de la interfaz
	// -----------------------------------------------------------------
	
	/** Árbol de progreso */
	private final JXTreeTable   progreso;
	
	// -----------------------------------------------------------------
	// Atributos
	// -----------------------------------------------------------------
	
	/** Series a mostrar */
	private final Vector<Serie> series;
	
	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------
	
	/** Método Constructor. Crea la interfaz para ver el progreso de descarga.
	 * 
	 * @param series Series a mostrar */
	public FrameProgreso(final Vector<Serie> series)
	{
		this.series = series;
		final AbstractTreeTableModel model = new TreeTableModel(series, false);
		progreso = new JXTreeTable(model);
		
		init();
	}
	
	/** Método Constructor. Crea la interfaz para ver el progreso de ordenación.
	 * 
	 * @param series Series a mostrar
	 * @param subs Subtitler que ordena */
	public FrameProgreso(final Vector<Serie> series, final Subtitler subs)
	{
		this.series = series;
		final AbstractTreeTableModel model = new TreeTableModel(series, true);
		progreso = new JXTreeTable(model);
		
		init();
		
		final JButton btn = new JButton("Ordenar");
		btn.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				btn.setEnabled(false);
				final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
					@Override
					protected Void doInBackground()
					{
						subs.ordenar();
						
						JOptionPane.showMessageDialog(null, "Se terminó de ordenar. ", "Complete", JOptionPane.INFORMATION_MESSAGE);
						
						return null;
					}
				};
				
				worker.execute();
			}
		});
		
		add(btn, BorderLayout.SOUTH);
	}
	
	// -----------------------------------------------------------------
	// Métodos
	// -----------------------------------------------------------------
	
	/** Inicia toda la interfaz. */
	private void init()
	{
		setSize(550, 600);
		setResizable(false);
		setLocationRelativeTo(null);
		setTitle("Visor de progreso");
		setLayout(new BorderLayout());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		progreso.setExpandsSelectedPaths(true);
		progreso.setRootVisible(false);
		progreso.setFillsViewportHeight(true);
		progreso.expandAll();
		progreso.getColumns().get(0).setPreferredWidth(300);
		progreso.getColumns().get(1).setPreferredWidth(75);
		progreso.getColumns().get(2).setPreferredWidth(185);
		
		add(new JScrollPane(progreso), BorderLayout.CENTER);
	}
	
	@Override
	public void update(final Observable o, final Object arg)
	{
		if (arg instanceof Serie)
		{
			final Serie serie = (Serie) arg;
			series.set(series.getIndex(serie), serie);
			progreso.setTreeTableModel(new TreeTableModel(series, false));
			progreso.getColumns().get(0).setPreferredWidth(300);
			progreso.getColumns().get(1).setPreferredWidth(75);
			progreso.getColumns().get(2).setPreferredWidth(185);
			progreso.expandAll();
		}
	}
	
	// /** @param args ars */
	// public static void main(final String[] args)
	// {
	// final Vector<Serie> series = new Vector<>();
	//
	// final Serie s = new Serie("pruebva", true, 1, 2, 99, 555);
	// final Episodio e = new Episodio("epiiiii", 1, 3);
	// e.setProgreso("No se puede modificar");
	// e.addVersion(new Version("hdtv", false, "No se puede modificar"));
	//
	// s.addEpisodio(e);
	// series.add(s);
	//
	// new FrameProgreso(series).setVisible(true);
	// }
}
