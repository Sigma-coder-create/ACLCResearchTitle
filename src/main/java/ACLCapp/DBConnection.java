package ACLCapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;
import java.io.InputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class DBConnection {

    private static Properties props = new Properties();
    private static boolean unlocked = false;

    private static Connection mysqlConnection = null;
    private static Connection sqliteConnection = null;

    static {
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties.enc")) {
            if (in == null) throw new RuntimeException("db.properties.enc not found in classpath!");
            byte[] encrypted = in.readAllBytes();

            byte[] keyBytes = "16ByteSecretKey!".getBytes();
            SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");

            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decrypted = cipher.doFinal(encrypted);

            props.load(new java.io.ByteArrayInputStream(decrypted));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load encrypted configuration: " + e.getMessage(), e);
        }
    }
    public static Connection getConnection() {
        Connection conn = getMySQLConnection();
        if (conn == null) {
            conn = getSQLiteConnection();
        }
        return conn;
    }

    public static boolean unlock(String password) {
        String correct = props.getProperty("app.password");
        unlocked = correct != null && correct.equals(password);
        if (unlocked) System.out.println("[DB] App unlocked successfully.");
        else System.err.println("[DB] Unlock failed. Wrong password.");
        return unlocked;
    }

    public static Connection getMySQLConnection() {
        if (!unlocked) {
            System.err.println("[MySQL] Access denied. Unlock first.");
            return null;
        }
        try {
            if (mysqlConnection == null || mysqlConnection.isClosed()) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                String url = "jdbc:mysql://" + props.getProperty("mysql.host") + ":" +
                        props.getProperty("mysql.port") + "/" + props.getProperty("mysql.db") +
                        "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
                mysqlConnection = DriverManager.getConnection(url,
                        props.getProperty("mysql.user"),
                        props.getProperty("mysql.password"));
                System.out.println("[MySQL] Connected to MySQL server.");
            }
        } catch (Exception e) {
            System.err.println("[MySQL] Connection failed: " + e.getMessage());
            mysqlConnection = null;
        }
        return mysqlConnection;
    }

    public static Connection getSQLiteConnection() {
        if (!unlocked) {
            System.err.println("[SQLite] Access denied. Unlock first.");
            return null;
        }
        try {
            if (sqliteConnection == null || sqliteConnection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                String dbFile = props.getProperty("sqlite.file"); // e.g., "aclcsqlitedb.db"

            // Use LOCALAPPDATA for writable storage (per‑user, not synced)
                String localAppData = System.getenv("LOCALAPPDATA");
                if (localAppData == null) localAppData = System.getProperty("user.home"); // fallback
                String appFolder = localAppData + java.io.File.separator + "ACLC Research Title";
                java.io.File folder = new java.io.File(appFolder);
                if (!folder.exists()) {
                folder.mkdirs(); // create folder if it doesn't exist
                }
                String dbPath = appFolder + java.io.File.separator + dbFile;
                String url = "jdbc:sqlite:" + dbPath;

                System.out.println("[SQLite] Opening DB at: " + url);
                sqliteConnection = DriverManager.getConnection(url);
                System.out.println("[SQLite] Connected to SQLite database.");
            }
        } catch (Exception e) {
            System.err.println("[SQLite] Connection failed: " + e.getMessage());
            sqliteConnection = null;
        }
        return sqliteConnection;
    }
}