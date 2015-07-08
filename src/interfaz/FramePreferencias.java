package interfaz;

import java.awt.GridLayout;
import java.awt.event.ActionListener;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import modelo.Preferencias;
import clases.Boton;

/** Frame para cambiar las preferencias.
 * 
 * @author Ricardo Pérez Diez */
public class FramePreferencias extends JFrame
{
	// -----------------------------------------------------------------
	// Constantes
	// -----------------------------------------------------------------
	
	/** Constante de Serialización */
	private static final long serialVersionUID = 1L;
	
	// -----------------------------------------------------------------
	// Atributos de la interfaz
	// -----------------------------------------------------------------
	
	/** Check Box: Base de datos local */
	public JCheckBox          cbDB;
	
	/** Check Box: Descargas local */
	public JCheckBox          cbDes;
	
	/** Combo Box: Ruta descarga */
	public JComboBox<String>  cbRuta;
	
	// -----------------------------------------------------------------
	// Atributos
	// -----------------------------------------------------------------
	
	/** Si la ventana fue cerrada */
	protected boolean         closed;
	
	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------
	
	/** Método Constructor. Crea el frame para cambiar las preferencias.
	 * 
	 * @param main Listener para los botones */
	public FramePreferencias(final ActionListener main)
	{
		setSize(350, 200);
		setTitle("Preferencias");
		setLocationRelativeTo(null);
		setLayout(new GridLayout(0, 1));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		cbDB = new JCheckBox("Usar base de datos local (asegúrese de tenerla)", Preferencias.dbLocal);
		cbDes = new JCheckBox("Descargar torrents localmente", Preferencias.descargasLocal);
		
		final String[] opciones = new String[2];
		opciones[0] = Preferencias.ruta;
		opciones[1] = Preferencias.ruta.equals(Preferencias.RUTA_RED) ? Preferencias.RUTA_LOCAL : Preferencias.RUTA_RED;
		cbRuta = new JComboBox<>(opciones);
		cbRuta.setEditable(true);
		
		add(cbDB);
		add(cbDes);
		add(new JLabel("Ruta descarga subtítulos: "));
		add(cbRuta);
		
		final JPanel panelBot = new JPanel(new GridLayout(1, 2));
		panelBot.add(new Boton(InterfazDownloader.CANCELAR, InterfazDownloader.CANCELAR, main));
		panelBot.add(new Boton(InterfazDownloader.ACEPTAR, InterfazDownloader.ACEPTAR, main));
		
		add(panelBot);
		
		if (!Preferencias.serverFound || Preferencias.isServer)
		{
			cbDB.setEnabled(false);
			cbDes.setEnabled(false);
			cbRuta.removeItemAt(1);
		}
		else if (!Preferencias.receptorFound)
		{
			cbDes.setEnabled(false);
		}
		
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
	
}
