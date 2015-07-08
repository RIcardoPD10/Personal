/**
 * 
 */
package interfaz;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import modelo.DataBase;
import modelo.Downloader;
import modelo.Preferencias;
import modelo.Receptor;
import modelo.Serie;
import modelo.Subtitler;
import updater.Updater;
import clases.Boton;
import clases.Carrier;
import clases.tiempo.Hora;
import estructuras.implementations.vectores.Vector;
import excepciones.InvalidTimeException;

/** Interfaz que muestra la información que hay en la base de datos sobre cada
 * serie. <br>
 * Adicionalmente permite realizar verificaciones en busca de nuevos episodios y
 * ordenarlos. <br>
 * Además de agregar nuevas series a la base de datos.
 * 
 * @author Ricardo Pérez Díez */
public class InterfazDownloader extends JFrame implements ActionListener, Observer
{
	// -----------------------------------------------------------------
	// Constantes
	// -----------------------------------------------------------------
	
	/** Constante de Serialización */
	private static final long     serialVersionUID = 1L;
	
	// Comandos Botones ------------------------------------------------
	
	/** Comando: Mostrar Agregar */
	protected static final String CHECK_ALL        = "Check All";
	
	/** Comando: Mostrar Agregar */
	protected static final String CHECK_SELECTION  = "Check Selection";
	
	/** Comando: Mostrar Agregar */
	protected static final String MOSTRAR_AGREGAR  = "Mostrar Agregar";
	
	/** Comando: Agregar */
	protected static final String AGREGAR          = "Agregar";
	
	/** Comando: Aceptar */
	protected static final String ACEPTAR          = "Aceptar";
	
	/** Comando: Actualizar */
	public static final String    UPDATE           = "Actualizar";
	
	/** Comando: Ordenar */
	public static final String    ORDENAR          = "Ordenar";
	
	/** Comando: Preferencias */
	public static final String    PREFERENCIAS     = "Preferencias";
	
	/** Comando: Cancelar */
	protected static final String CANCELAR         = "Cancelar";
	
	// Comandos Otros --------------------------------------------------
	
	/** Comando: Start */
	public static final String    START            = "Start";
	
	/** Comando: Done */
	public static final String    DONE             = "Done";
	
	// -----------------------------------------------------------------
	// Atributos de la interfaz
	// -----------------------------------------------------------------
	
	/** Frame para agregar series */
	private FrameAgregar          agregar;
	
	/** Frame para modificar las preferencias */
	private FramePreferencias     preferencias;
	
	/** Cola de notificaciones series solicitadas. */
	private FrameProgreso         progreso;
	
	/** Tabla con las series */
	private final JTable          table;
	
	// -----------------------------------------------------------------
	// Atributos
	// -----------------------------------------------------------------
	
	/** Modelo del mundo */
	private final Downloader      downloader;
	
	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------
	
	/** Método Constructor. Crea la interfaz principal.
	 * 
	 * @throws Exception Problema iniciando el modelo */
	public InterfazDownloader() throws Exception
	{
		setSize(575, 418);// +16 x serie
		setTitle("Downloader");
		setLocationRelativeTo(null);
		setLayout(new BorderLayout());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		if (InetAddress.getLocalHost().getHostAddress().equals(Receptor.IP))
			new Receptor().addObserver(this);
		
		downloader = new Downloader();
		downloader.addObserver(this);
		table = new JTable();
		table.setFillsViewportHeight(true);
		table.addMouseListener(new MouseAdapter()
		{
			@ Override
			public void mouseClicked(MouseEvent e)
			{
				JTable t = (JTable) e.getSource();
				
				try
				{
					if (t.getSelectedColumn() == 3) Desktop.getDesktop().browse(Serie.getEztvUri((int) t.getModel().getValueAt(t.getSelectedRow(), 3)));
					else if (t.getSelectedColumn() == 4) Desktop.getDesktop().browse(Serie.getSubsUri((int) t.getModel().getValueAt(t.getSelectedRow(), 4)));
				}
				catch (IOException e1)
				{
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		
		makeTable();
		
		final JScrollPane scrollPane = new JScrollPane(table);
		final JPanel botones = new JPanel(new GridLayout(2, 0));
		
		final Boton agregar = new Boton(AGREGAR + " Serie", MOSTRAR_AGREGAR, this);
		final Boton checkAll = new Boton(CHECK_ALL, CHECK_ALL, this);
		final Boton checkSelection = new Boton(CHECK_SELECTION, CHECK_SELECTION, this);
		final Boton update = new Boton(UPDATE, UPDATE, this);
		final Boton ordenar = new Boton(ORDENAR, ORDENAR, this);
		final Boton opciones = new Boton(PREFERENCIAS, PREFERENCIAS, this);
		
		botones.add(checkAll);
		botones.add(update);
		botones.add(agregar);
		botones.add(checkSelection);
		botones.add(ordenar);
		botones.add(opciones);
		
		final JMenuBar menu = new JMenuBar();
		final JMenu autoCheck = new JMenu("AutoCheck");
		final JMenuItem checkHour = new JMenuItem("Check Hour");
		checkHour.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				JOptionPane.showMessageDialog(null, "Hora de la próxima verificación programada: \n" + downloader.getCheckHour(), "Autoverificación",
				        JOptionPane.INFORMATION_MESSAGE);
			}
		});
		final JMenuItem lastHour = new JMenuItem("Last");
		lastHour.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				JOptionPane.showMessageDialog(null, "Hora de la última revisión: \n" + downloader.getLastCheckHour(), "Autoverificación",
				        JOptionPane.INFORMATION_MESSAGE);
			}
		});
		final JMenuItem setNext = new JMenuItem("Set Next");
		setNext.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent e)
			{
				final String h = JOptionPane.showInputDialog(null, "Nueva hora: ", "Autoverificación", JOptionPane.QUESTION_MESSAGE);
				
				try
				{
					downloader.setNextCheckHour(new Hora(h));
				}
				catch (final InvalidTimeException e1)
				{
					JOptionPane.showMessageDialog(null, e1.getMessage(), "Autoverificación", JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		
		if (Preferencias.isServer)
		{
			downloader.autoCheck();
			menu.add(autoCheck);
			autoCheck.add(checkHour);
			autoCheck.add(lastHour);
			autoCheck.add(setNext);
			
			final Dimension d = getSize();
			d.height += 23;
			
			setSize(d);
			setJMenuBar(menu);
		}
		add(scrollPane, BorderLayout.CENTER);
		add(botones, BorderLayout.SOUTH);
	}
	
	// -----------------------------------------------------------------
	// Métodos
	// -----------------------------------------------------------------
	
	@Override
	public void actionPerformed(final ActionEvent e)
	{
		try
		{
			switch ((String) ((Carrier) e.getSource()).getObject())
			{
				case CHECK_ALL:
					
					progreso = new FrameProgreso(Downloader.getSeries());
					downloader.addObserver(progreso);
					progreso.setVisible(true);
					
					final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
						@Override
						protected Void doInBackground()
						{
							try
							{
								downloader.checkAll();
								JOptionPane.showMessageDialog(null, "Búsqueda terminada.", "Complete", JOptionPane.INFORMATION_MESSAGE);
								makeTable();
							}
							catch (final Exception e)
							{
								deployError(e);
							}
							
							return null;
						}
					};
					
					worker.execute();
					
					break;
				
				case CHECK_SELECTION:
					
					if (table.getSelectedRowCount() == 0)
						throw new Exception("Ninguna serie está seleccionada. ");
					
					final Vector<Serie> series = new Vector<>();
					final int[] rows = table.getSelectedRows();
					for (final int row : rows)
					{
						final String nombre = (String) table.getValueAt(row, 0);
						series.add(DataBase.getSerie(nombre));
					}
					
					progreso = new FrameProgreso(series);
					downloader.addObserver(progreso);
					progreso.setVisible(true);
					
					final SwingWorker<Void, Void> worker2 = new SwingWorker<Void, Void>() {
						@Override
						protected Void doInBackground()
						{
							try
							{
								for (int i = 0; i < series.getSize(); i++)
									downloader.checkSerie(series.get(i));
								
								JOptionPane.showMessageDialog(null, "Búsqueda terminada.", "Complete", JOptionPane.INFORMATION_MESSAGE);
								makeTable();
							}
							catch (final Exception e)
							{
								deployError(e);
							}
							
							return null;
						}
					};
					
					worker2.execute();
					
					break;
				
				case MOSTRAR_AGREGAR:
					
					if (agregar == null || agregar.closed)
						agregar = new FrameAgregar(this);
					else
						agregar.toFront();
					
					break;
				
				case AGREGAR:
					
					Downloader.addSerie(agregar.getSerie());
					agregar.dispose();
					agregar = null;
					makeTable();
					
					break;
				
				case UPDATE:
					
					makeTable();
					
					break;
				
				case ORDENAR:
					
					final Subtitler subs = new Subtitler();
					progreso = new FrameProgreso(subs.getSeries(), subs);
					subs.addObserver(progreso);
					progreso.setVisible(true);
					
					break;
				
				case PREFERENCIAS:
					
					if (preferencias == null || preferencias.closed)
						preferencias = new FramePreferencias(this);
					else
						preferencias.toFront();
					break;
				
				case ACEPTAR:
					
					preferencias.dispose();
					Preferencias.dbLocal = preferencias.cbDB.isSelected();
					Preferencias.descargasLocal = preferencias.cbDes.isSelected();
					Preferencias.ruta = (String) preferencias.cbRuta.getSelectedItem();
					preferencias = null;
					DataBase.disconnect();
					DataBase.start();
					makeTable();
					break;
				
				case CANCELAR:
					
					if (agregar != null)
						agregar.dispose();
					agregar = null;
					
					if (preferencias != null)
						preferencias.dispose();
					preferencias = null;
					
					break;
			}
		}
		catch (final Exception sql)
		{
			deployError(sql);
		}
	}
	
	@Override
	public void dispose()
	{
		DataBase.disconnect();
		super.dispose();
		System.exit(0);
	}
	
	/** Crea la tabla. */
	private void makeTable()
	{
		try
		{
			final DefaultTableModel modelo = new DefaultTableModel(Downloader.getSeries(true), Serie.CAMPOS);
			table.setModel(modelo);
			final int[] anchos = new int[] { 225, 125, 65, 60, 100 };
			for (int i = 0; i < table.getColumnCount(); i++)
			{
				table.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
			}
			
			final TableRowSorter<TableModel> modelOrder = new TableRowSorter<TableModel>(modelo);
			table.setRowSorter(modelOrder);
			// modelOrder.setRowFilter(RowFilter.regexFilter("2", 1));
		}
		catch (final SQLException e)
		{
			deployError(e);
		}
	}
	
	@Override
	public void update(final Observable o, final Object arg)
	{
		if (arg.equals(UPDATE))
			makeTable();
		else if (arg.equals(Updater.CLOSE))
			dispose();
		// else if (arg.equals(START))
		// progreso.setVisible(true);
	}
	
	/** Muestra los errores.
	 * 
	 * @param e Excepción a mostrar */
	public static void deployError(final Exception e)
	{
		if (e.getClass() == SQLException.class)
		{
			JOptionPane.showMessageDialog(null, "Hubo un problema en la base de datos. \nPor favor tome una foto al próximo mensaje y guárdela: ",
			        "Excepción en la base de datos", JOptionPane.ERROR_MESSAGE);
			JOptionPane.showMessageDialog(null, e.getStackTrace(), e.getMessage(), JOptionPane.ERROR_MESSAGE);
		}
		else if (e.getMessage().startsWith("Connection refused: connect"))
		{
			JOptionPane.showMessageDialog(null, "El servidor no está recibiendo solicitudes para descargar. ", "Advertencia",
			        JOptionPane.WARNING_MESSAGE);
		}
		else if (e.getMessage().endsWith("no se podrá iniciar la aplicación"))
		{
			JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
		}
		else
		{
			JOptionPane.showMessageDialog(null, e.getMessage(), "Advertencia", JOptionPane.WARNING_MESSAGE);
		}
		
		exportError(e);
	}
	
	/** Escribe la excepción en un archivo.
	 * 
	 * @param e Excepción */
	public static void exportError(final Exception e)
	{
		try
		{
			File file = new File("C:/Users/" + System.getProperty("user.name") + "/Errores.txt");
			int i = 1;
			while (file.exists())
			{
				file = new File("C:/Users/" + System.getProperty("user.name") + "/Errores" + i + ".txt");
				i++;
			}
			
			try (final PrintWriter wt = new PrintWriter(file))
			{
				e.printStackTrace(wt);
			}
		}
		catch (final FileNotFoundException e1)
		{}
	}
	
	// -----------------------------------------------------------------
	// Main
	// -----------------------------------------------------------------
	
	/** Método Main.
	 * 
	 * @param args Argumentos */
	public static void main(final String[] args)
	{
		try
		{
			final InterfazDownloader interfaz = new InterfazDownloader();
			interfaz.setVisible(true);
			if (Preferencias.getMsg() != null)
				JOptionPane.showMessageDialog(null, Preferencias.getMsg(), "Configuración no ideal", JOptionPane.WARNING_MESSAGE);
		}
		catch (final Exception e)
		{
			deployError(e);
		}
	}
}
