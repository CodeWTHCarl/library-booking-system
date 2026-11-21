package data;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Gives the repositories a connection to MySQL. The url, user and password are read once
 * from "db.properties" so the password never appears in the source code.
 */
public class DatabaseConnection {
    private static final String CONFIG_FILE = "db.properties"; // looked up in the folder you run java from

    private static String url;
    private static String user;
    private static String password;
    private static boolean loaded = false;

    private DatabaseConnection() {
        // utility class: nobody should create an object of it
    }

    private static void loadConfig() throws SQLException {
        if (loaded) {
            return;
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
        } catch (IOException e) {
            throw new SQLException("Cannot read " + CONFIG_FILE + ". Copy db.properties.example "
                    + "to db.properties and type your MySQL password in it.", e);
        }
        url = props.getProperty("db.url");
        user = props.getProperty("db.user");
        password = props.getProperty("db.password");
        loaded = true;
    }

    /** Opens a new connection. The caller closes it (use try-with-resources). */
    public static Connection getConnection() throws SQLException {
        loadConfig();
        return DriverManager.getConnection(url, user, password);
    }
}
