package interfaz;

import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;

import modelo.Serie;
import clases.Boton;
import clases.CampoTexto;
import clases.tiempo.Fecha;

/** Ventana para introducir datos de nueva serie.
 * 
 * @author Ricardo Pérez Díez */
public class FrameAgregar extends JFrame
{
	// -----------------------------------------------------------------
	// Constantes
	// -----------------------------------------------------------------
	
	/** Constante de Serialización */
	private static final long       serialVersionUID = 1L;
	
	// -----------------------------------------------------------------
	// Atributos de la interfaz
	// -----------------------------------------------------------------
	
	/** Nombre */
	private final CampoTexto        nombre;
	
	/** Tipo: Serie o documental */
	private final JComboBox<String> tipo;
	
	/** Temporada */
	private final CampoTexto        temporada;
	
	/** Episodio */
	private final CampoTexto        episodio;
	
	/** Día */
	private final JComboBox<String>	dia;

	/** Id eztv */
	private final CampoTexto        eztv;
	
	/** Id subtitulos.es */
	private final CampoTexto        subs;
	
	// -----------------------------------------------------------------
	// Atributos
	// -----------------------------------------------------------------
	
	/** Si la ventana fue cerrada */
	protected boolean               closed;
	
	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------
	
	/** Método Constructor. Crea el frame para agregar series.
	 * 
	 * @param main Listener para los botones */
	public FrameAgregar(final ActionListener main)
	{
		setSize(450, 300);
		setTitle("Agregar Serie");
		setLocationRelativeTo(null);
		setLayout(new GridLayout(0, 2));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		closed = false;
		
		nombre = new CampoTexto(30);
		temporada = new CampoTexto(new String[] { CampoTexto.NUMEROS, CampoTexto.NO_SPACES }, 2);
		tipo = new JComboBox<>(new String[] { "Serie", "Documental" });
		episodio = new CampoTexto(new String[] { CampoTexto.NUMEROS, CampoTexto.NO_SPACES }, 2);
		dia = new JComboBox<>(Fecha.DIAS);
		eztv = new CampoTexto(new String[] { CampoTexto.NUMEROS, CampoTexto.NO_SPACES }, 3);
		subs = new CampoTexto(new String[] { CampoTexto.NUMEROS, CampoTexto.NO_SPACES }, 4);
		
		add(new JLabel("Nombre: "));
		add(nombre);
		add(new JLabel("Tipo: "));
		add(tipo);
		add(new JLabel("Temporada del último episodio visto: "));
		add(temporada);
		add(new JLabel("Episodio visto por última vez: "));
		add(episodio);
		add(new JLabel("Día de transmisión: "));
		add(dia);
		add(new JLabel("Id eztv: "));
		add(eztv);
		add(new JLabel("Id Subtitulos.es: "));
		add(subs);
		
		final Boton cancelar = new Boton("Cancelar", InterfazDownloader.CANCELAR, main);
		
		final Boton agregar = new Boton("Agregar", InterfazDownloader.AGREGAR, main);
		
		add(cancelar);
		add(agregar);
		
		setVisible(true);
	}
	
	// -----------------------------------------------------------------
	// Métodos
	// -----------------------------------------------------------------
	
	@Override
	public void dispose()
	{
		closed = true;
		super.dispose();
	}
	
	/** @return Serie creada a partir de los datos */
	protected Serie getSerie()
	{
		return new Serie(nombre.getText(), tipo.getSelectedIndex() == 0, temporada.getInt(), episodio.getInt(), (String) dia.getSelectedItem(), eztv.getInt(), subs.getInt());
	}
	
}
