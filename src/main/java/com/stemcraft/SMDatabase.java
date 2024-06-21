package com.stemcraft;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.sqlite.SQLiteDataSource;
import com.stemcraft.interfaces.SMSQLConsumer;
import com.stemcraft.exceptions.SMException;

public class SMDatabase {
    private static Connection connection = null;
    private static final String DATABASE_NAME = "database.db";
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Return if connected to the database.
     * 
     * @return Boolean if connected
     */
    public static Boolean isConnected() {
        return SMDatabase.connection != null;
    }

    /**
     * Connect to the database (if not already connected).
     *
     * @return Boolean if connected
     */
    public static Boolean connect() {
        if (connection != null) {
            return true;
        }

        try {
            DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl("jdbc:sqlite:" + STEMCraft.getPlugin().getDataFolder().getAbsolutePath() + "/"
                + SMDatabase.DATABASE_NAME);
            connection = dataSource.getConnection();

            initialize();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Disconnect from the database if connected.
     */
    public static void disconnect() {
        try {
            if (connection != null) {
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            STEMCraft.error(e);
        }
    }

    /**
     * Initialize the database
     */
    private static void initialize() {
        String tableName = "migration";
        String createTableSQL =
            "CREATE TABLE IF NOT EXISTS " + tableName + " (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT)";

        try (Statement statement = SMDatabase.connection.createStatement()) {
            statement.execute(createTableSQL);
        } catch (SQLException e) {
            STEMCraft.error(e);
        }
    }

    /**
     * Prepare a database statement.
     * 
     * @param statement
     * @return
     */
    public static PreparedStatement prepareStatement(String statement) {
        if (connection != null) {
            try {
                return connection.prepareStatement(statement);
            } catch (SQLException e) {
                STEMCraft.error(e);
            }
        }

        return null;
    }

    /**
     * Run a database migration if it is not yet executed in the database.
     * 
     * @param name
     * @param callback
     */
    public static void runMigration(String name, SMSQLConsumer callback) throws SMException {
        if (!isConnected()) {
            throw new SMException("Database is not connected");
        }

        try {
            PreparedStatement statement = connection.prepareStatement("SELECT id FROM migration WHERE name = ?");
            statement.setString(1, name);
            ResultSet resultSet = statement.executeQuery();

            if (!resultSet.next()) {
                resultSet.close();
                statement.close();

                STEMCraft.info("Running migration " + name);
                try {
                    callback.accept();
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                statement = connection.prepareStatement("INSERT INTO migration (name) VALUES (?)");
                statement.setString(1, name);
                statement.executeUpdate();
            }

            resultSet.close();
            statement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
