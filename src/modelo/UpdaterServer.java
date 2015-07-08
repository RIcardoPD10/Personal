package modelo;

import java.io.File;

import updater.InterfazUpdater;
import updater.Updater;

/** Actualiza el servidor.
 *
 * @author Ricardo Pérez Díez */
public class UpdaterServer extends Updater
{
	// -----------------------------------------------------------------
	// Métodos
	// -----------------------------------------------------------------

	@ Override
	public File getFile()
	{
		String dir = null;
		
		if (isServer()) dir = "Z:/Series";
		else dir = "C:/Users/Ricardo/Google Drive/Aplicaciones/Mis Proyectos/Downloader/dist";

		return new File(dir + "/Downloader.jar");
	}

	@ Override
	public String getIP()
	{
		return "192.168.1.100";
	}

	@ Override
	public int getPort()
	{
		return 2000;
	}

	@ Override
	public int getPortToClose()
	{
		return 8572;
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
			new InterfazUpdater(new UpdaterServer());
		}
		catch (final Exception e)
		{
			InterfazUpdater.exportError(e);
		}
	}

}
