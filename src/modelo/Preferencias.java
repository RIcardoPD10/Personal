package modelo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Clase que representa la configuración de la aplicación.
 * 
 * @author Ricardo */
public class Preferencias
{
	// -----------------------------------------------------------------
	// Constantes
	// -----------------------------------------------------------------
	
	/** Ruta de la carpeta de descarga del servidor */
	public static final String RUTA_RED   = "Z:/Descargas/";
	
	/** Ruta de la carpeta de descarga del computador */
	public static final String RUTA_LOCAL = "C:/Users/" + System.getProperty("user.name") + "/Downloads/";
	
	// -----------------------------------------------------------------
	// Atributos
	// -----------------------------------------------------------------
	
	/** Se esta ejecutando en el servidor */
	public static boolean      isServer;
	
	/** El servidor fue encontrado */
	public static boolean      serverFound;
	
	/** El receptor fue encontrado */
	public static boolean      receptorFound;
	
	/** El receptor fue encontrado */
	public static boolean      dbFound;
	
	/** Usar base de datos local si existe */
	public static boolean      dbLocal;
	
	/** Descargar las series en el computador local */
	public static boolean      descargasLocal;
	
	/** Ruta elegida para descargar subtítulos y renombrar series */
	public static String       ruta;
	
	// -----------------------------------------------------------------
	// Métodos
	// -----------------------------------------------------------------
	
	/** Construye el mensaje a mostrar al inicio.
	 * 
	 * @return Mensaje a mostrar al inicio */
	public static String getMsg()
	{
		String msg = null;
		
		if (receptorFound)
			return msg;
		else if (isServer && !receptorFound)
			msg = "No se está ejecutando el receptor de comandos. ";
		else if (serverFound && !receptorFound)
		{
			msg = "Se pudo encontrar el servidor. \n";
			msg += "Sin embargo no está ejecutando el receptor de comandos. \n";
			msg += "Las series serán descargados localmente. ";
		}
		else if (!serverFound && dbFound)
		{
			msg = "No se pudo ubicar el servidor. \n";
			msg += "Se usará la base de datos local. \n";
			msg += "Las series serán descargados localmente. ";
		}
		else if (!serverFound && !dbFound)
		{
			msg = "No se pudo ubicar el servidor. \n";
			msg += "No se encontró una base de datos local. \n";
			msg += "No se podrá iniciar la aplicación";
		}
		
		return msg;
	}
	
	/** Establece los valores iniciales de las preferencias.
	 * 
	 * @throws IOException Problemas obteniendo información del servidor */
	public static void inic() throws IOException
	{
		if (InetAddress.getLocalHost().getHostAddress().equals(Receptor.IP))
		{
			isServer = true;
			serverFound = true;
			dbFound = true;
			ruta = RUTA_RED;
			dbLocal = true;
			descargasLocal = true;
			
			final Thread t = new Thread() {
				@Override
				public void run()
				{
					try
					{
						Downloader.sendToServer(Receptor.OK);
					}
					catch (final Exception e)
					{
						receptorFound = false;
					}
				}
			};
			
			t.start();
		}
		else
		{
			isServer = false;
			dbFound = false;
			
			try
			{
				InetAddress.getByName("PerezDiezServer");
				// Si no tira excepción es porque encontró el servidor y
				// continua.
				
				serverFound = true;
				dbLocal = false;
				try
				{
					try (Socket socket = new Socket())
					{
						socket.connect(new InetSocketAddress(Receptor.IP, Receptor.PORT), 1000);
						receptorFound = true;
						descargasLocal = false;
						ruta = RUTA_RED;
					}
				}
				catch (final Exception e)
				{
					receptorFound = false;
					descargasLocal = true;
					ruta = RUTA_LOCAL;
				}
			}
			catch (final Exception e)
			{
				dbLocal = true;
				descargasLocal = true;
				ruta = RUTA_LOCAL;
			}
		}
	}
	
	/** @return Si se debe descargar el torrent en el servidor */
	public static boolean sendTorrent()
	{
		return !isServer && !descargasLocal;
	}
	
}
