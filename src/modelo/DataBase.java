package modelo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import estructuras.implementations.vectores.Vector;

/** Clase encargada de gestionar la conexión y consultas con la base de datos.
 *
 * @author Ricardo Pérez Díez */
public class DataBase
{
	// -----------------------------------------------------------------
	// Constantes
	// -----------------------------------------------------------------

	// Tablas ----------------------------------------------------------

	/** Tabla de Series <br>
	 * <br>
	 * Campos de la tabla: <br>
	 *
	 * @see #NOMBRE - Nombre
	 * @see #TEMPORADA - Temporada
	 * @see #EPISODIO - Episodio
	 * @see #DIA - Dia
	 * @see #EZTV - Eztv (PK)
	 * @see #SUBTITULOS - Subtitulos.es
	 * @see #TERMINADA - Terminada */
	private static final String	SERIES		= "Series";

	// Campos ----------------------------------------------------------

	/** Campo Nombre <br>
	 * <br>
	 * Tablas en la que aparece: <br>
	 *
	 * @see #SERIES - Series */
	private static final String	NOMBRE		= "Nombre";

	/** Campo EsSerie <br>
	 * True si es serie. False si es documental<br>
	 * <br>
	 * Tablas en la que aparece: <br>
	 *
	 * @see #SERIES - Series */
	private static final String	ES_SERIE	= "EsSerie";

	/** Campo Temporada <br>
	 * <br>
	 * Tablas en la que aparece: <br>
	 *
	 * @see #SERIES - Series */
	private static final String	TEMPORADA	= "Temporada";

	/** Campo Episodio <br>
	 * <br>
	 * Tablas en la que aparece: <br>
	 *
	 * @see #SERIES - Series */
	private static final String	EPISODIO	= "Episodio";

	/** Campo Día <br>
	 * <br>
	 * Tablas en la que aparece: <br>
	 *
	 * @see #SERIES - Series
	 * @see #CHECKS - Verificaciones */
	private static final String	DIA			= "Dia";

	/** Campo Eztv <br>
	 * <br>
	 * Tablas en la que aparece: <br>
	 *
	 * @see #SERIES - Series */
	private static final String	EZTV		= "Eztv";

	/** Campo Subtitulos <br>
	 * <br>
	 * Tablas en la que aparece: <br>
	 *
	 * @see #SERIES - Series */
	private static final String	SUBTITULOS	= "Subtitulos";

	/** Campo Terminada <br>
	 * <br>
	 * Tablas en la que aparece: <br>
	 *
	 * @see #SERIES - Series */
	private static final String	TERMINADA	= "Terminada";

	// -----------------------------------------------------------------
	// Atributos
	// -----------------------------------------------------------------

	/** Conexión con la base de datos */
	private static Connection	db;

	// -----------------------------------------------------------------
	// Métodos
	// -----------------------------------------------------------------

	/** Agrega una serie a la base de datos.
	 *
	 * @param serie Serie a agregar
	 * @throws SQLException Problemas con la base de datos */
	protected static void addSerie(final Serie serie) throws SQLException
	{
		try (Statement stmt = db.createStatement())
		{
			String sql = "INSERT INTO " + SERIES + "(" + NOMBRE + ", " + ES_SERIE + ", " + DIA + ", " + TEMPORADA + ", " + EPISODIO + ", ";
			sql += EZTV + ", " + SUBTITULOS + ") VALUES ('" + serie.getNombre() + "', " + serie.isSerie() + ", '" + serie.getDia() + "', ";
			sql += serie.getEpisodioActual().getTemporada() + ", " + serie.getEpisodioActual().getEpisodio() + ", " + serie.getEztv() + ", ";
			sql += serie.getSubs() + ")";

			stmt.execute(sql);
		}
	}

	/** Crea la tabla y la pobla.
	 *
	 * @throws SQLException Problemas con la base de datos */
	private static void create() throws SQLException
	{
		try (Statement stmt = db.createStatement())
		{
			final String sql = "CREATE TABLE " + SERIES + " (" + NOMBRE + " VARCHAR(30) UNIQUE, " + TEMPORADA + " SMALLINT, " + EPISODIO + " SMALLINT, " + EZTV + " SMALLINT, " + SUBTITULOS + " SMALLINT UNIQUE, " + TERMINADA + " BOOLEAN DEFAULT FALSE, PRIMARY KEY(" + EZTV + "))";

			stmt.execute(sql);
		}
		
		final String sql = "INSERT INTO " + SERIES + "(" + NOMBRE + ", " + TEMPORADA + ", " + EPISODIO + ", " + EZTV + ", " + SUBTITULOS + ") VALUES (?, ?, ?, ?, ?)";
		try (PreparedStatement stmt = db.prepareStatement(sql))
		{
			final Vector<Serie> series = new Vector<>();
			// series.add(new Serie("American Horror Story", true, 3, 4, 562,
			// 1093));
			// series.add(new Serie("Arrow", true, 1, 23, 679, 1493));
			// series.add(new Serie("The Big Bang Theory", true, 7, 6, 23, 26));
			// series.add(new Serie("Covert Affairs", true, 4, 13, 388, 625));
			// series.add(new Serie("Da Vincis Demons", true, 1, 8, 815, 1673));
			// series.add(new Serie("Game of Thrones", true, 3, 10, 481, 770));
			// series.add(new Serie("Greys Anatomy", true, 10, 7, 111, 50));
			// series.add(new Serie("Hannibal", true, 1, 13, 763, 1669));
			// series.add(new Serie("How I Met your Mother", true, 9, 7, 125,
			// 31));
			// series.add(new Serie("Modern Family", true, 5, 6, 330, 382));
			// series.add(new Serie("Revenge", true, 3, 6, 525, 1043));
			// series.add(new Serie("Scandal", true, 3, 5, 631, 1300));
			// series.add(new Serie("True Blood", true, 6, 10, 279, 11));
			// series.add(new Serie("Two and a Half Men", true, 11, 5, 282,
			// 41));
			// series.add(new Serie("White Collar", true, 5, 3, 337, 408));

			for (int i = 0; i < series.getSize(); i++)
			{
				stmt.setString(1, series.get(i).getNombre());
				stmt.setInt(2, series.get(i).getEpisodioActual().getTemporada());
				stmt.setInt(3, series.get(i).getEpisodioActual().getEpisodio());
				stmt.setInt(4, series.get(i).getEztv());
				stmt.setInt(5, series.get(i).getSubs());

				stmt.executeUpdate();
			}
		}
	}

	/** Desconecta la conexión con la base de datos. */
	public static void disconnect()
	{
		try
		{
			db.close();
		}
		catch (final SQLException e)
		{
			e.printStackTrace();
		}
	}

	/** Extrae la información de la serie.
	 *
	 * @param res Registro a extraer
	 * @return Información de la serie
	 * @throws SQLException Problemas con la base de datos */
	private static Serie getSerie(final ResultSet res) throws SQLException
	{
		final String nombre = res.getString(NOMBRE);
		final boolean isSerie = res.getBoolean(ES_SERIE);
		final int temporada = res.getInt(TEMPORADA);
		final int episodio = res.getInt(EPISODIO);
		final String dia = res.getString(DIA);
		final int eztv = res.getInt(EZTV);
		final int subs = res.getInt(SUBTITULOS);

		return new Serie(nombre, isSerie, temporada, episodio, dia, eztv, subs);
	}

	/** Consulta una serie de la base de datos.
	 *
	 * @param nombre Nombre a buscar
	 * @return La serie de la base de datos
	 * @throws SQLException Problemas con la base de datos */
	public static Serie getSerie(final String nombre) throws SQLException
	{
		try (Statement stmt = db.createStatement())
		{
			String sql = "SELECT * FROM " + SERIES;
			sql += " WHERE " + NOMBRE + " = '" + nombre.replaceAll("'", "''") + "'";
			try (ResultSet res = stmt.executeQuery(sql))
			{
				if (res.next()) return getSerie(res);
				return null;
			}
		}
	}

	/** Consulta las series de la base de datos.
	 *
	 * @param noTerminadas Solo series no terminadas
	 * @return Las series de la base de datos
	 * @throws SQLException Problemas con la base de datos */
	protected static Vector<Serie> getSeries(final boolean noTerminadas) throws SQLException
	{
		final Vector<Serie> series = new Vector<>();

		try (Statement stmt = db.createStatement())
		{
			String sql = "SELECT * FROM " + SERIES;
			if (noTerminadas) sql += " WHERE " + TERMINADA + " = FALSE";
			else sql += " WHERE " + TERMINADA + " = TRUE";
			sql += " ORDER BY " + NOMBRE;
			try (ResultSet res = stmt.executeQuery(sql))
			{
				while (res.next())
					series.add(getSerie(res));
			}
		}

		return series;
	}

	/** Crea la conexión con la base de datos.
	 *
	 * @throws Exception Problemas iniciando la base de datos */
	public static void start() throws Exception
	{
		final boolean iniciar = false;

		final String driver = "org.postgresql.Driver";
		String connectString;

		final String user = "Ricardo";
		final String pass = "2758123";
		
		if (Preferencias.dbLocal) connectString = "jdbc:postgresql://localhost:2758/Series";
		else if (Preferencias.serverFound) connectString = "jdbc:postgresql://perezdiezserver:2758/Series";
		else connectString = "jdbc:postgresql://186.85.148.245:2758/Series";

		Class.forName(driver).newInstance();

		db = DriverManager.getConnection(connectString, user, pass);
		db.setAutoCommit(true);
		
		if (iniciar) create();

		Preferencias.dbFound = true;
	}

	/** Actualiza el día que la serie sale al aire.
	 *
	 * @param serie Serie con la nuevo información
	 * @throws SQLException Problemas con la base de datos o no se modificó */
	protected static void updateAirDay(final Serie serie) throws SQLException
	{
		try (Statement stmt = db.createStatement())
		{
			String sql = "UPDATE " + SERIES + " SET " + DIA + " = '" + serie.getDia() + "'";
			sql += " WHERE " + EZTV + " = " + serie.getEztv();
			
			if (stmt.executeUpdate(sql) != 1) throw new SQLException("No se modificó el día en que sale al aire");
		}
	}

	/** Actualiza el último episodio descargado.
	 *
	 * @param serie Serie con la nuevo información
	 * @throws SQLException Problemas con la base de datos o no se modificó */
	protected static void updateLastEpisode(final Serie serie) throws SQLException
	{
		try (Statement stmt = db.createStatement())
		{
			String sql = "UPDATE " + SERIES + " SET " + TEMPORADA + " = " + serie.getEpisodioEncontrado().getTemporada() + ", " + EPISODIO + " = " + serie.getEpisodioEncontrado().getEpisodio();
			sql += " WHERE " + EZTV + " = " + serie.getEztv();
			
			if (stmt.executeUpdate(sql) != 1) throw new SQLException("No se modificó el último episodio descargado");
		}
	}

}
