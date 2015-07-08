package interfaz;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import modelo.Downloader;
import estructuras.implementations.vectores.Vector;

/** Interfaz para enviar links magnet al servidor para descargar.
 * 
 * @author Ricardo Pérez Díez */
public class InterfazSender extends JFrame
{
	// -----------------------------------------------------------------
	// Constantes
	// -----------------------------------------------------------------
	
	/** Constante de Serialización */
	private static final long        serialVersionUID = 1L;
	
	// -----------------------------------------------------------------
	// Atributos de la interfaz
	// -----------------------------------------------------------------
	
	/** Links a enviar */
	private final Vector<JTextField> txtLinks;
	
	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------
	
	/** Método Constructor. Crea la interfaz. */
	public InterfazSender()
	{
		setSize(300, 100);
		setTitle("Sender");
		setLocationRelativeTo(null);
		setLayout(new GridLayout(0, 1));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		final JPanel pnlBotones = new JPanel(new GridLayout(1, 2));
		final JButton btnAgregar = new JButton("Agregar Campo");
		btnAgregar.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(final ActionEvent arg0)
			{
				txtLinks.add(new JTextField());
				add(txtLinks.get(txtLinks.getSize() - 1));
				setSize(300, (int) (getSize().getHeight() + 30));
			}
		});
		final JButton btnEnviar = new JButton("Enviar Links");
		btnEnviar.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(final ActionEvent arg0)
			{
				for (int i = 0; i < txtLinks.getSize(); i++)
				{
					try
					{
						Downloader.sendToServer(txtLinks.get(i).getText());
					}
					catch (final Exception e)
					{
						JOptionPane.showMessageDialog(null, "Error enviando link. ", "Error", JOptionPane.ERROR_MESSAGE);
						e.printStackTrace();
					}
				}
				
				JOptionPane.showMessageDialog(null, "Envío finalizado. ", "Envío", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		
		pnlBotones.add(btnAgregar);
		pnlBotones.add(btnEnviar);
		
		add(pnlBotones);
		
		txtLinks = new Vector<>();
		txtLinks.add(new JTextField());
		
		add(txtLinks.get(txtLinks.getSize() - 1));
	}
	
	// -----------------------------------------------------------------
	// Main
	// -----------------------------------------------------------------
	
	/** Método main.
	 * 
	 * @param args Argumentos */
	public static void main(final String[] args)
	{
		final InterfazSender sender = new InterfazSender();
		sender.setVisible(true);
	}
}
