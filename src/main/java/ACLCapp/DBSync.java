package ACLCapp;

import java.sql.*;
import java.util.*;

public class DBSync {

    public static void syncAllTables() {
        Connection mysql = DBConnection.getMySQLConnection();
        Connection sqlite = DBConnection.getSQLiteConnection();

        if (sqlite == null) {
            System.err.println("[Sync] SQLite connection failed - aborting sync.");
            return;
        }
        if (mysql == null) {
            System.err.println("[Sync] MySQL connection failed - skipping MySQL -> SQLite sync.");
            return;
        }

        try {
            // Get all tables from MySQL (excluding system tables)
            List<String> tables = getTablesFromMySQL(mysql);
            
            System.out.println("[Sync] Found tables in MySQL: " + tables);
            
            // Sync each table
            for (String tableName : tables) {
                syncTable(mysql, sqlite, tableName);
            }
            
            System.out.println("[Sync] All tables synced successfully!");
            
        } catch (Exception e) {
            System.err.println("[Sync] Error syncing: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try { if (mysql != null) mysql.close(); } catch (Exception ignored) {}
            try { if (sqlite != null) sqlite.close(); } catch (Exception ignored) {}
        }
    }
    
    private static List<String> getTablesFromMySQL(Connection mysql) throws SQLException {
        List<String> tables = new ArrayList<>();
        DatabaseMetaData meta = mysql.getMetaData();
        ResultSet rs = meta.getTables(null, null, "%", new String[]{"TABLE"});
        
        while (rs.next()) {
            String tableName = rs.getString("TABLE_NAME");
            // Skip system tables if any
            if (!tableName.startsWith("sys_") && !tableName.equals("information_schema")) {
                tables.add(tableName);
            }
        }
        rs.close();
        return tables;
    }
    
    private static void syncTable(Connection mysql, Connection sqlite, String tableName) throws SQLException {
        System.out.println("[Sync] Syncing table: " + tableName);
        
        // Step 1: Create table in SQLite if it doesn't exist (with proper schema)
        createTableInSQLite(mysql, sqlite, tableName);
        
        // Step 2: Sync MySQL -> SQLite
        syncMySQLtoSQLite(mysql, sqlite, tableName);
        
        // Step 3: Sync SQLite -> MySQL
        syncSQLiteToMySQL(mysql, sqlite, tableName);
    }
    
    private static void createTableInSQLite(Connection mysql, Connection sqlite, String tableName) throws SQLException {
        // Get table structure from MySQL
        DatabaseMetaData meta = mysql.getMetaData();
        ResultSet columns = meta.getColumns(null, null, tableName, "%");
        
        StringBuilder createSQL = new StringBuilder("CREATE TABLE IF NOT EXISTS \"" + tableName + "\" (");
        boolean first = true;
        String primaryKey = null;
        
        while (columns.next()) {
            if (!first) {
                createSQL.append(", ");
            }
            String columnName = columns.getString("COLUMN_NAME");
            String type = columns.getString("TYPE_NAME");
            
            // Map MySQL types to SQLite types
            String sqliteType = mapMySQLTypeToSQLite(type);
            
            createSQL.append("\"").append(columnName).append("\" ").append(sqliteType);
            
            // Check if this column is primary key
            ResultSet pk = meta.getPrimaryKeys(null, null, tableName);
            while (pk.next()) {
                if (columnName.equals(pk.getString("COLUMN_NAME"))) {
                    createSQL.append(" PRIMARY KEY");
                    break;
                }
            }
            pk.close();
            
            first = false;
        }
        createSQL.append(")");
        
        try (Statement stmt = sqlite.createStatement()) {
            stmt.execute(createSQL.toString());
            System.out.println("[SQLite] Table '" + tableName + "' ensured.");
        }
        columns.close();
    }
    
    private static void syncMySQLtoSQLite(Connection mysql, Connection sqlite, String tableName) throws SQLException {
        // Get column names
        List<String> columns = getColumnNames(mysql, tableName);
        
        // Build SELECT query for MySQL
        String selectSQL = "SELECT * FROM " + tableName;
        
        // Build INSERT/REPLACE query for SQLite
        StringBuilder insertSQL = new StringBuilder("INSERT OR REPLACE INTO \"" + tableName + "\" (");
        StringBuilder placeholders = new StringBuilder();
        
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                insertSQL.append(", ");
                placeholders.append(", ");
            }
            insertSQL.append("\"").append(columns.get(i)).append("\"");
            placeholders.append("?");
        }
        insertSQL.append(") VALUES (").append(placeholders).append(")");
        
        try (Statement mysqlStmt = mysql.createStatement();
             ResultSet rs = mysqlStmt.executeQuery(selectSQL);
             PreparedStatement sqliteInsert = sqlite.prepareStatement(insertSQL.toString())) {
            
            int rowCount = 0;
            while (rs.next()) {
                for (int i = 0; i < columns.size(); i++) {
                    sqliteInsert.setObject(i + 1, rs.getObject(columns.get(i)));
                }
                sqliteInsert.executeUpdate();
                rowCount++;
            }
            System.out.println("[Sync] " + rowCount + " rows synced from MySQL -> SQLite for table: " + tableName);
        }
    }
    
    private static void syncSQLiteToMySQL(Connection mysql, Connection sqlite, String tableName) throws SQLException {
        List<String> columns = getColumnNames(mysql, tableName);
        
        // Build SELECT query for SQLite
        String selectSQL = "SELECT * FROM \"" + tableName + "\"";
        
        // Build INSERT/UPDATE query for MySQL
        StringBuilder insertSQL = new StringBuilder("INSERT INTO " + tableName + " (");
        StringBuilder updateSQL = new StringBuilder(" ON DUPLICATE KEY UPDATE ");
        
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                insertSQL.append(", ");
                updateSQL.append(", ");
            }
            String column = columns.get(i);
            insertSQL.append("`").append(column).append("`");
            updateSQL.append("`").append(column).append("`=VALUES(`").append(column).append("`)");
        }
        insertSQL.append(") VALUES (");
        
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) insertSQL.append(", ");
            insertSQL.append("?");
        }
        insertSQL.append(")").append(updateSQL);
        
        try (Statement sqliteStmt = sqlite.createStatement();
             ResultSet rs = sqliteStmt.executeQuery(selectSQL);
             PreparedStatement mysqlInsert = mysql.prepareStatement(insertSQL.toString())) {
            
            int rowCount = 0;
            while (rs.next()) {
                for (int i = 0; i < columns.size(); i++) {
                    mysqlInsert.setObject(i + 1, rs.getObject(columns.get(i)));
                }
                mysqlInsert.executeUpdate();
                rowCount++;
            }
            System.out.println("[Sync] " + rowCount + " rows synced from SQLite -> MySQL for table: " + tableName);
        }
    }
    
    private static List<String> getColumnNames(Connection mysql, String tableName) throws SQLException {
        List<String> columns = new ArrayList<>();
        DatabaseMetaData meta = mysql.getMetaData();
        ResultSet rs = meta.getColumns(null, null, tableName, "%");
        
        while (rs.next()) {
            columns.add(rs.getString("COLUMN_NAME"));
        }
        rs.close();
        return columns;
    }
    
    private static String mapMySQLTypeToSQLite(String mysqlType) {
        mysqlType = mysqlType.toUpperCase();
        if (mysqlType.contains("INT")) return "INTEGER";
        if (mysqlType.contains("CHAR") || mysqlType.contains("TEXT") || mysqlType.contains("VARCHAR")) return "TEXT";
        if (mysqlType.contains("DOUBLE") || mysqlType.contains("FLOAT") || mysqlType.contains("DECIMAL")) return "REAL";
        if (mysqlType.contains("DATE") || mysqlType.contains("TIME")) return "TEXT";
        if (mysqlType.contains("BLOB")) return "BLOB";
        return "TEXT"; // default
    }
    
    // Keep your original method for backward compatibility if needed
    public static void syncResearchTitles() {
        syncAllTables();
    }
    
    public static void insertTestDataIfEmpty() {
        // This method is no longer needed since we're syncing actual data
        System.out.println("[TestData] Test data insertion disabled - using actual MySQL data");
    }
}