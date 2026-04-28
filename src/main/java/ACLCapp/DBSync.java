package ACLCapp;

import java.sql.*;
import java.util.*;
import org.json.JSONArray;
import org.json.JSONObject;
public class DBSync {

    public static void syncResearchTitles() {
        System.out.println("[Sync] Starting two-way database sync...");

        // Auto-closes connections using try-with-resources
        try (Connection mysql = DBConnection.getMySQLConnection();
             Connection sqlite = DBConnection.getSQLiteConnection()) {

            if (mysql == null) {
                System.out.println("[Sync] MySQL offline. Skipping sync, running in local mode.");
                return;
            }
            if (sqlite == null) {
                System.err.println("[Sync] SQLite offline. Cannot perform local sync.");
                return;
            }

            // 1. Push local offline changes to the live database
            syncFromSQLiteToMySQL(sqlite, mysql);

            // 2. Pull new live changes down to the local database
            syncFromMySQLToSQLite(mysql, sqlite);

            System.out.println("[Sync] ✅ Two-way sync completed successfully.");

        } catch (SQLException e) {
            System.err.println("[Sync] ❌ Synchronization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    public static void exportForWeb() {
        Connection con = null;
        try {
            con = DBConnection.getMySQLConnection();
            if (con == null) con = DBConnection.getSQLiteConnection();
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        String sql = "SELECT `Research Title`, `Status`, `Approved by` " +
                     "FROM ACLC_research_titles WHERE record_state = 'ACTIVE'";

        JSONArray arr = new JSONArray();
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                JSONObject obj = new JSONObject();
                obj.put("title", rs.getString("Research Title"));
                obj.put("status", rs.getString("Status"));
                obj.put("approved_by", rs.getString("Approved by"));
                arr.put(obj);
            }

            // Determine portable app folder (same as SQLite DB)
            String appFolder = System.getenv("LOCALAPPDATA");
            if (appFolder == null) {
                appFolder = System.getProperty("user.home");
            }
            appFolder += java.io.File.separator + "ACLC Research Title";
            new java.io.File(appFolder).mkdirs();

            String filePath = appFolder + java.io.File.separator + "research.json";
            try (java.io.FileWriter writer = new java.io.FileWriter(filePath)) {
                writer.write(arr.toString(2));
            }
            System.out.println("✅ research.json exported to: " + filePath);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void insertTestDataIfEmpty() {
        System.out.println("[DBSync] Test data check - using actual database sync.");
    }

    private static boolean hasLastUpdatedColumn(Connection mysql) {
        try {
            DatabaseMetaData meta = mysql.getMetaData();
            try (ResultSet rs = meta.getColumns(null, null, "ACLC_research_titles", "last_updated")) {
                return rs.next();
            }
        } catch (SQLException e) {
            return false;
        }
    }

    private static void basicSyncMySQLtoSQLite(Connection mysql, Connection sqlite) throws SQLException {
        String selectSQL = "SELECT ID, `Research Title`, `SY-YR`, Status, `Approved by`, Applied, Strand, Software, Webpage, record_state FROM ACLC_research_titles";

        try (Statement stmt = sqlite.createStatement()) {
            stmt.execute("DELETE FROM ACLC_research_titles");
        }

        int count = 0;
        try (Statement stmt = mysql.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {

            String insertSQL = "INSERT INTO ACLC_research_titles " +
                "(ID, `Research Title`, `SY-YR`, Status, `Approved by`, Applied, Strand, Software, Webpage, record_state, last_updated) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (PreparedStatement ps = sqlite.prepareStatement(insertSQL)) {
                while (rs.next()) {
                    ps.setInt(1, rs.getInt("ID"));
                    ps.setString(2, rs.getString("Research Title"));
                    ps.setString(3, rs.getString("SY-YR"));
                    ps.setString(4, rs.getString("Status"));
                    ps.setString(5, rs.getString("Approved by"));
                    ps.setString(6, rs.getString("Applied"));
                    ps.setString(7, rs.getString("Strand"));
                    ps.setString(8, rs.getString("Software"));
                    ps.setString(9, rs.getString("Webpage"));
                    ps.setString(10, rs.getString("record_state"));
                    ps.setTimestamp(11, new Timestamp(System.currentTimeMillis()));
                    ps.executeUpdate();
                    count++;
                }
            }
        }
        System.out.println("[Sync] Basic sync: " + count + " records copied from MySQL to SQLite");
    }
    
    private static void syncFromMySQLToSQLite(Connection mysql, Connection sqlite) throws SQLException {
        String selectSQL = "SELECT ID, `Research Title`, `SY-YR`, Status, `Approved by`, Applied, Strand, " +
                           "Software, Webpage, record_state, last_updated FROM ACLC_research_titles";
        
        int inserted = 0, updated = 0;
        
        try (Statement stmt = mysql.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            
            while (rs.next()) {
                int id = rs.getInt("ID");
                Timestamp mysqlTimestamp = rs.getTimestamp("last_updated");
                Timestamp sqliteTimestamp = getSQLiteTimestamp(sqlite, id);
                
                if (sqliteTimestamp == null) {
                    insertOrReplaceSQLite(sqlite, rs);
                    inserted++;
                } else if (mysqlTimestamp != null && mysqlTimestamp.after(sqliteTimestamp)) {
                    insertOrReplaceSQLite(sqlite, rs);
                    updated++;
                }
            }
        }
        
        System.out.println("[Sync] MySQL → SQLite: " + inserted + " inserted, " + updated + " updated");
    }

    private static void syncFromSQLiteToMySQL(Connection sqlite, Connection mysql) throws SQLException {
        String selectSQL = "SELECT ID, `Research Title`, `SY-YR`, Status, `Approved by`, Applied, Strand, " +
                           "Software, Webpage, record_state, last_updated FROM ACLC_research_titles";
        
        int inserted = 0, updated = 0;
        
        try (Statement stmt = sqlite.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            
            while (rs.next()) {
                int id = rs.getInt("ID");
                Timestamp sqliteTimestamp = rs.getTimestamp("last_updated");
                Timestamp mysqlTimestamp = getMySQLTimestamp(mysql, id);
                
                if (mysqlTimestamp == null) {
                    insertOrReplaceMySQL(mysql, rs);
                    inserted++;
                } else if (sqliteTimestamp != null && sqliteTimestamp.after(mysqlTimestamp)) {
                    insertOrReplaceMySQL(mysql, rs);
                    updated++;
                }
            }
        }
        
        System.out.println("[Sync] SQLite → MySQL: " + inserted + " inserted, " + updated + " updated");
    }

    private static Timestamp getSQLiteTimestamp(Connection sqlite, int id) {
        try {
            String sql = "SELECT last_updated FROM ACLC_research_titles WHERE ID = ?";
            try (PreparedStatement ps = sqlite.prepareStatement(sql)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getTimestamp("last_updated") : null;
                }
            }
        } catch (SQLException e) {
            System.err.println("[Sync] Warning: last_updated column not ready yet.");
            return null;
        }
    }

    private static Timestamp getMySQLTimestamp(Connection mysql, int id) throws SQLException {
        String sql = "SELECT last_updated FROM ACLC_research_titles WHERE ID = ?";
        try (PreparedStatement ps = mysql.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getTimestamp("last_updated") : null;
            }
        }
    }

    private static void insertOrReplaceSQLite(Connection sqlite, ResultSet rs) throws SQLException {
        String sql = "INSERT OR REPLACE INTO ACLC_research_titles " +
                    "(ID, `Research Title`, `SY-YR`, Status, `Approved by`, Applied, Strand, Software, Webpage, record_state, last_updated) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = sqlite.prepareStatement(sql)) {
            ps.setInt(1, rs.getInt("ID"));
            ps.setString(2, rs.getString("Research Title"));
            ps.setString(3, rs.getString("SY-YR"));
            ps.setString(4, rs.getString("Status"));
            ps.setString(5, rs.getString("Approved by"));
            ps.setString(6, rs.getString("Applied"));
            ps.setString(7, rs.getString("Strand"));
            ps.setString(8, rs.getString("Software"));
            ps.setString(9, rs.getString("Webpage"));
            ps.setString(10, rs.getString("record_state")); // Added record_state

            Timestamp ts = rs.getTimestamp("last_updated");
            if (ts == null) {
                ts = new Timestamp(System.currentTimeMillis());
            }
            ps.setTimestamp(11, ts);
            ps.executeUpdate();
        }
    }

    private static void insertOrReplaceMySQL(Connection mysql, ResultSet rs) throws SQLException {
        String sql = "INSERT INTO ACLC_research_titles " +
                    "(ID, `Research Title`, `SY-YR`, Status, `Approved by`, Applied, Strand, Software, Webpage, record_state, last_updated) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "`Research Title` = VALUES(`Research Title`), " +
                    "`SY-YR` = VALUES(`SY-YR`), " +
                    "Status = VALUES(Status), " +
                    "`Approved by` = VALUES(`Approved by`), " +
                    "Applied = VALUES(Applied), " +
                    "Strand = VALUES(Strand), " +
                    "Software = VALUES(Software), " +
                    "Webpage = VALUES(Webpage), " +
                    "record_state = VALUES(record_state), " +
                    "last_updated = VALUES(last_updated)";

        try (PreparedStatement ps = mysql.prepareStatement(sql)) {
            ps.setInt(1, rs.getInt("ID"));
            ps.setString(2, rs.getString("Research Title"));
            ps.setString(3, rs.getString("SY-YR"));
            ps.setString(4, rs.getString("Status"));
            ps.setString(5, rs.getString("Approved by"));
            ps.setString(6, rs.getString("Applied"));
            ps.setString(7, rs.getString("Strand"));
            ps.setString(8, rs.getString("Software"));
            ps.setString(9, rs.getString("Webpage"));
            ps.setString(10, rs.getString("record_state")); // Added record_state

            Timestamp ts = rs.getTimestamp("last_updated");
            if (ts == null) {
                ts = new Timestamp(System.currentTimeMillis());
            }
            ps.setTimestamp(11, ts);
            ps.executeUpdate();
        }
    }
}