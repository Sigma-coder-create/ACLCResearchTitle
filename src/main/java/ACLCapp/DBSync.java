package ACLCapp;

import java.sql.*;
import java.util.*;

public class DBSync {

    public static void syncResearchTitles() {
        syncTwoWay();
    }
    
    public static void insertTestDataIfEmpty() {
        System.out.println("[DBSync] Test data check - using actual database sync.");
    }

    public static void syncTwoWay() {
        Connection mysql = null;
        Connection sqlite = null;
    
        try {
            // ✅ SQLITE FIRST! (offline priority)
            sqlite = DBConnection.getSQLiteConnection();

            if (sqlite == null) {
                System.err.println("[Sync] SQLite unavailable - cannot sync.");
                return;
            }

            System.out.println("[Sync] Starting two-way synchronization...");
        
            // CRITICAL: Ensure SQLite has the proper schema with last_updated column
            ensureSQLiteSchema(sqlite);
            sqlite.createStatement().execute("PRAGMA schema_version;");

            // ✅ MySQL SECOND (only if online)
            mysql = DBConnection.getMySQLConnection();
        
            if (mysql != null) {
                // Check if MySQL has last_updated column
                if (!hasLastUpdatedColumn(mysql)) {
                    System.err.println("[Sync] ⚠️ MySQL missing 'last_updated' column. Please add it:");
                    System.err.println("[Sync] ALTER TABLE ACLC_research_titles ADD COLUMN last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;");
                    basicSyncMySQLtoSQLite(mysql, sqlite);
                } else {
                // Both databases have last_updated - do full two-way sync
                    syncFromMySQLToSQLite(mysql, sqlite);
                    syncFromSQLiteToMySQL(sqlite, mysql);
                }
                System.out.println("[Sync] ✅ Two-way sync completed successfully.");
            } else {
                System.out.println("[Sync] ⚠️ MySQL unavailable - working offline with SQLite only.");
            }

        } catch (Exception e) {
            System.err.println("[Sync] ❌ Error during synchronization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void ensureSQLiteSchema(Connection sqlite) throws SQLException {

        boolean tableExists = false;

        try (Statement stmt = sqlite.createStatement();
             ResultSet rs = stmt.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type='table' AND name='ACLC_research_titles'"
             )) {
            tableExists = rs.next();
        }

        if (!tableExists) {
            String createSQL =
                "CREATE TABLE IF NOT EXISTS ACLC_research_titles (" +
                "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
                "\"Research Title\" TEXT NOT NULL," +
                "\"SY-YR\" TEXT," +
                "\"Status\" TEXT," +
                "\"Applied\" TEXT," +
                "\"Strand\" TEXT," +
                "\"Software\" TEXT," +
                "\"Webpage\" TEXT," +
                "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

            try (Statement stmt = sqlite.createStatement()) {
                stmt.execute(createSQL);
                System.out.println("[Sync] SQLite table created.");
            }

            return;
        }

        boolean hasLastUpdated = false;

        try (Statement stmt = sqlite.createStatement();
             ResultSet rs = stmt.executeQuery("PRAGMA table_info(ACLC_research_titles)")) {

            while (rs.next()) {
                if ("last_updated".equalsIgnoreCase(rs.getString("name"))) {
                    hasLastUpdated = true;
                    break;
                }
            }
        }

        if (!hasLastUpdated) {
            try (Statement stmt = sqlite.createStatement()) {
                stmt.execute("ALTER TABLE ACLC_research_titles ADD COLUMN last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
                System.out.println("[Sync] Added last_updated column.");
            }
        } else {
            System.out.println("[Sync] last_updated column already exists.");
        }
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
        String selectSQL = "SELECT ID, \"Research Title\", \"SY-YR\", Status, Applied, Strand, Software, Webpage FROM ACLC_research_titles";
        
        // Clear SQLite table first for basic sync
        try (Statement stmt = sqlite.createStatement()) {
            stmt.execute("DELETE FROM ACLC_research_titles");
        }
        
        int count = 0;
        try (Statement stmt = mysql.createStatement();
             ResultSet rs = stmt.executeQuery(selectSQL)) {
            
            String insertSQL = "INSERT INTO ACLC_research_titles " +
                "(ID, \"Research Title\", \"SY-YR\", Status, Applied, Strand, Software, Webpage, last_updated) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            
            try (PreparedStatement ps = sqlite.prepareStatement(insertSQL)) {
                while (rs.next()) {
                    ps.setInt(1, rs.getInt("ID"));
                    ps.setString(2, rs.getString("Research Title"));
                    ps.setString(3, rs.getString("SY-YR"));
                    ps.setString(4, rs.getString("Status"));
                    ps.setString(5, rs.getString("Applied"));
                    ps.setString(6, rs.getString("Strand"));
                    ps.setString(7, rs.getString("Software"));
                    ps.setString(8, rs.getString("Webpage"));
                    ps.setTimestamp(9, new Timestamp(System.currentTimeMillis()));
                    ps.executeUpdate();
                    count++;
                }
            }
        }
        System.out.println("[Sync] Basic sync: " + count + " records copied from MySQL to SQLite");
    }

    private static void syncFromMySQLToSQLite(Connection mysql, Connection sqlite) throws SQLException {
        String selectSQL = "SELECT ID, \"Research Title\", \"SY-YR\", Status, Applied, Strand, " +
                          "Software, Webpage, last_updated FROM ACLC_research_titles";
        
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
        String selectSQL = "SELECT ID, \"Research Title\", \"SY-YR\", Status, Applied, Strand, " +
                          "Software, Webpage, last_updated FROM ACLC_research_titles";
        
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
                    "(ID, \"Research Title\", \"SY-YR\", Status, Applied, Strand, Software, Webpage, last_updated) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement ps = sqlite.prepareStatement(sql)) {
            ps.setInt(1, rs.getInt("ID"));
            ps.setString(2, rs.getString("Research Title"));
            ps.setString(3, rs.getString("SY-YR"));
            ps.setString(4, rs.getString("Status"));
            ps.setString(5, rs.getString("Applied"));
            ps.setString(6, rs.getString("Strand"));
            ps.setString(7, rs.getString("Software"));
            ps.setString(8, rs.getString("Webpage"));
            
            Timestamp ts = rs.getTimestamp("last_updated");
            if (ts == null) {
                ts = new Timestamp(System.currentTimeMillis());
            }
            ps.setTimestamp(9, ts);
            ps.executeUpdate();
        }
    }

    private static void insertOrReplaceMySQL(Connection mysql, ResultSet rs) throws SQLException {
        String sql = "INSERT INTO ACLC_research_titles " +
                    "(ID, \"Research Title\", \"SY-YR\", Status, Applied, Strand, Software, Webpage, last_updated) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "\"Research Title\" = VALUES(\"Research Title\"), " +
                    "\"SY-YR\" = VALUES(\"SY-YR\"), " +
                    "Status = VALUES(Status), " +
                    "Applied = VALUES(Applied), " +
                    "Strand = VALUES(Strand), " +
                    "Software = VALUES(Software), " +
                    "Webpage = VALUES(Webpage), " +
                    "last_updated = VALUES(last_updated)";
        
        try (PreparedStatement ps = mysql.prepareStatement(sql)) {
            ps.setInt(1, rs.getInt("ID"));
            ps.setString(2, rs.getString("Research Title"));
            ps.setString(3, rs.getString("SY-YR"));
            ps.setString(4, rs.getString("Status"));
            ps.setString(5, rs.getString("Applied"));
            ps.setString(6, rs.getString("Strand"));
            ps.setString(7, rs.getString("Software"));
            ps.setString(8, rs.getString("Webpage"));
            
            Timestamp ts = rs.getTimestamp("last_updated");
            if (ts == null) {
                ts = new Timestamp(System.currentTimeMillis());
            }
            ps.setTimestamp(9, ts);
            ps.executeUpdate();
        }
    }
}