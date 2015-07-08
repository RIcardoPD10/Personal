package modelo;

import interfaz.InterfazDownloader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Observable;

/** Recibe las solicitudes de otros equipos.
 * 
 * @author Ricardo Pérez Díez */
public class Receptor extends Observable
{
	// -----------------------------------------------------------------
	// Constantes
	// -----------------------------------------------------------------
	
	/** Ip del servidor */
	public static final String IP   = "192.168.1.100";
	
	/** Puerto del receptor */
	public static final int    PORT = 8572;
	
	/** OK */
	public static final String OK   = "OK";
	
	// -----------------------------------------------------------------
	// Atributos
	// -----------------------------------------------------------------
	
	/** Servidor */
	private final ServerSocket servidor;
	
	// -----------------------------------------------------------------
	// Constructor
	// -----------------------------------------------------------------
	
	/** Método Constructor. Inicia un servidor para recibir órdenes.
	 * 
	 * @throws IOException Problemas con los canales */
	public Receptor() throws IOException
	{
		servidor = new ServerSocket(PORT);
		
		final Thread listen = new Thread() {
			@Override
			public void run()
			{
				while (true)
				{
					try
					{
						try (final Socket socket = servidor.accept())
						{
							try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())))
							{
								final String solicitud = in.readLine();
								
								if (solicitud != null)
								{
									if (solicitud.startsWith("magnet:"))
										Downloader.downloadLinkTorrent(new Version(null, false, solicitud));
									else if (solicitud.equals(OK))
										Preferencias.receptorFound = true;
									else
									{
										setChanged();
										notifyObservers(solicitud);
									}
								}
							}
						}
					}
					catch (final Exception e)
					{
						InterfazDownloader.exportError(e);
					}
				}
			}
		};
		
		listen.start();
	}
	
}
