package ACLCapp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Properties;
import java.io.InputStream;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;

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
        // Try SQLite FIRST (always works offline)
        Connection conn = getSQLiteConnection();
        if (conn != null) {
            System.out.println("[DB] Using SQLite (offline/local mode)");
            return conn;
        }
        
        // If SQLite fails, try MySQL
        conn = getMySQLConnection();
        if (conn != null) {
            System.out.println("[DB] Using MySQL (online mode)");
            return conn;
        }
        
        System.err.println("[DB] ❌ No database connection available!");
        return null;
    }

    public static boolean unlock(String password) {
        String correct = props.getProperty("app.password");
        unlocked = correct != null && correct.equals(password);
        if (unlocked) {
            System.out.println("[DB] ✅ App unlocked successfully.");
        } else {
            System.err.println("[DB] ❌ Unlock failed. Wrong password.");
        }
        return unlocked;
    }

    public static Connection getMySQLConnection() {
        if (!unlocked) {
            System.err.println("[MySQL] Access denied. Unlock first.");
            return null;
        }

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://" + props.getProperty("mysql.host") + ":" +
                    props.getProperty("mysql.port") + "/" + props.getProperty("mysql.db") +
                    "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true&connectTimeout=3000";

            Connection conn = DriverManager.getConnection(url,
                    props.getProperty("mysql.user"),
                    props.getProperty("mysql.password"));

            System.out.println("[MySQL] ✅ Connected successfully.");
            return conn;

        } catch (Exception e) {
            System.err.println("[MySQL] ❌ Connection failed: " + e.getMessage());
            return null;
        }
    }

    public static Connection getSQLiteConnection() {
        if (!unlocked) {
            System.err.println("[SQLite] Access denied. Unlock first.");
            return null;
        }
        try {
            if (sqliteConnection == null || sqliteConnection.isClosed()) {
                Class.forName("org.sqlite.JDBC");
                String dbFile = props.getProperty("sqlite.file", "aclcsqlitedb.db");

                // Use LOCALAPPDATA for writable storage (per-user, not synced)
                String localAppData = System.getenv("LOCALAPPDATA");
                if (localAppData == null) {
                    localAppData = System.getProperty("user.home");
                }
                String appFolder = localAppData + java.io.File.separator + "ACLC Research Title";
                java.io.File folder = new java.io.File(appFolder);
                if (!folder.exists()) {
                    boolean created = folder.mkdirs();
                    if (created) {
                        System.out.println("[SQLite] Created app folder: " + appFolder);
                    }
                }
                String dbPath = appFolder + java.io.File.separator + dbFile;
                String url = "jdbc:sqlite:" + dbPath;

                System.out.println("[SQLite] Opening DB at: " + url);
                sqliteConnection = DriverManager.getConnection(url);
                
                // Initialize basic table structure if needed
                initializeSQLiteTable(sqliteConnection);
                
                System.out.println("[SQLite] ✅ Connected to SQLite database.");
            }
        } catch (Exception e) {
            System.err.println("[SQLite] ❌ Connection failed: " + e.getMessage());
            sqliteConnection = null;
        }
        return sqliteConnection;
    }
    
// Inside DBConnection.java -> initializeSQLiteTable()
    private static void initializeSQLiteTable(Connection conn) {
        try (Statement stmt = conn.createStatement()) {
            // 1. Create table with ALL columns if it doesn't exist
            stmt.execute("CREATE TABLE IF NOT EXISTS ACLC_research_titles (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`Research Title` TEXT, " +
                "`SY-YR` TEXT, " +
                "Status TEXT, " +
                "`Approved by` TEXT, " +
                "Applied TEXT, " +
                "Strand TEXT, " +
                "Software TEXT, " +
                "Webpage TEXT, " +
                "record_state TEXT DEFAULT 'ACTIVE', " +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // 2. Check for missing columns in existing table
            DatabaseMetaData meta = conn.getMetaData();
            boolean hasRecordState = false;
            boolean hasLastUpdated = false;

            try (ResultSet rs = meta.getColumns(null, null, "ACLC_research_titles", null)) {
                while (rs.next()) {
                    String colName = rs.getString("COLUMN_NAME");
                    if ("record_state".equalsIgnoreCase(colName)) hasRecordState = true;
                    if ("last_updated".equalsIgnoreCase(colName)) hasLastUpdated = true;
                }
            }

            // 3. Add columns if they are missing
            if (!hasRecordState) {
                stmt.execute("ALTER TABLE ACLC_research_titles ADD COLUMN record_state TEXT DEFAULT 'ACTIVE'");
                System.out.println("[SQLite] Added missing record_state column.");
            }
            if (!hasLastUpdated) {
                stmt.execute("ALTER TABLE ACLC_research_titles ADD COLUMN last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                System.out.println("[SQLite] Added missing last_updated column.");
            }

        } catch (SQLException e) {
            System.err.println("[SQLite] Schema verification failed: " + e.getMessage());
        }
    }
    
      public static void closeConnections() {
        try {
            if (mysqlConnection != null && !mysqlConnection.isClosed()) {
                mysqlConnection.close();
            }
            if (sqliteConnection != null && !sqliteConnection.isClosed()) {
                sqliteConnection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}