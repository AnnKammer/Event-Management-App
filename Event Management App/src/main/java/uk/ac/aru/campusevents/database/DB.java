/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: DB.java
 *
 * Purpose:
 *   Centralised helper class for establishing a JDBC connection to the PostgreSQL
 *   database and executing SQL queries (both prepared and simple).
 *
 * Why this exists:
 *   • Keeps JDBC boilerplate out of repositories.
 *   • Ensures one consistent connection handling strategy.
 *   • Provides simple, clean helper methods for INSERT/UPDATE/DELETE and SELECT.
 */

package uk.ac.aru.campusevents.database;

import java.sql.*;

public class DB {

    // -------------------------------------------------------------------------
    //  Default database configuration
    // -------------------------------------------------------------------------

    /** Default JDBC URL for the PostgreSQL "campusevent" database. */
    private static final String DEFAULT_URL =
            "jdbc:postgresql://localhost:5432/campusevent";

    /** Default PostgreSQL username. */
    private static final String DEFAULT_USER = "postgres";

    /** Default PostgreSQL password. */
    private static final String DEFAULT_PASSWORD = "postgres";

    // -------------------------------------------------------------------------
    //  Instance fields
    // -------------------------------------------------------------------------

    private final String dbURL;
    private final String dbUsername;
    private final String dbPassword;

    /** Active JDBC connection. */
    private Connection dbConnection;

    // -------------------------------------------------------------------------
    //  Constructors (corrected)
    // -------------------------------------------------------------------------

    /**
     * Default constructor using shared dev credentials.
     * Automatically opens a DB connection (this is the critical fix).
     */
    public DB() {
        this(DEFAULT_URL, DEFAULT_USER, DEFAULT_PASSWORD);
    }

    /**
     * Constructor allowing configuration overrides.
     * Also automatically connects.
     *
     * @param dbURL      JDBC connection string
     * @param dbUsername database user
     * @param dbPassword database password
     */
    public DB(String dbURL, String dbUsername, String dbPassword) {
        this.dbURL = dbURL;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;

        // --- IMPORTANT FIX ---
        // Automatically connect when creating the DB object.
        // Without this, every repository sees "not connected".
        connect(false);
    }

    // -------------------------------------------------------------------------
    //  Connection methods
    // -------------------------------------------------------------------------

    /**
     * Opens a JDBC connection using the configured URL and credentials.
     *
     * @param silent  if true, skips printing "Connected" message
     * @return true if connected successfully, false on failure
     */
    public boolean connect(boolean silent) {
        try {
            dbConnection = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
            if (!silent) {
                System.out.println("Connected successfully to database: " + dbURL);
            }
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Failed to connect to database:");
            System.err.println("   URL      = " + dbURL);
            System.err.println("   Username = " + dbUsername);
            System.err.println("   Reason   = " + e.getMessage());
            return false;
        }
    }

    /** Connect silently (no output). */
    public boolean connect() {
        return connect(true);
    }

    /**
     * @return true if connection exists and is open.
     */
    public boolean isConnected() {
        try {
            return dbConnection != null && !dbConnection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Closes the DB connection safely.
     *
     * @param silent if true, suppresses output
     */
    public boolean disconnect(boolean silent) {
        if (dbConnection == null) {
            return true;
        }
        try {
            dbConnection.close();
            if (!silent) {
                System.out.println("Disconnected from database.");
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Error disconnecting: " + e.getMessage());
            return false;
        }
    }

    /** Disconnect silently. */
    public boolean disconnect() {
        return disconnect(true);
    }

    // -------------------------------------------------------------------------
    //  Simple SQL statements (Statement API)
    // -------------------------------------------------------------------------

    /**
     * Runs INSERT / UPDATE / DELETE without parameters.
     *
     * @param sqlUpdate SQL update string
     * @param silent    if false, prints success message
     * @return true if update succeeded
     */
    public boolean runSQLUpdate(String sqlUpdate, boolean silent) {
        if (!isConnected()) {
            System.err.println("Cannot run update: not connected to database.");
            return false;
        }

        try (Statement stmt = dbConnection.createStatement()) {
            stmt.executeUpdate(sqlUpdate);
            if (!silent) {
                System.out.println("SQL update executed successfully.");
            }
            return true;
        } catch (SQLException e) {
            System.err.println("SQL update error: " + e.getMessage());
            System.err.println("SQL was: " + sqlUpdate);
            return false;
        }
    }

    public boolean runSQLUpdate(String sqlUpdate) {
        return runSQLUpdate(sqlUpdate, true);
    }

    /**
     * Runs a SELECT query without parameters.
     *
     * @param sqlQuery SQL query string
     * @param silent   if false, prints success message
     * @return ResultSet from the query (caller must close it)
     */
    public ResultSet runSQLQuery(String sqlQuery, boolean silent) {
        if (!isConnected()) {
            System.err.println("Cannot run query: not connected to database.");
            return null;
        }

        try {
            Statement stmt = dbConnection.createStatement();
            ResultSet rs = stmt.executeQuery(sqlQuery);
            if (!silent) {
                System.out.println("SQL query executed successfully.");
            }
            return rs;
        } catch (SQLException e) {
            System.err.println("SQL query error: " + e.getMessage());
            System.err.println("SQL was: " + sqlQuery);
            return null;
        }
    }

    public ResultSet runSQLQuery(String sqlQuery) {
        return runSQLQuery(sqlQuery, true);
    }

    // -------------------------------------------------------------------------
    //  Prepared statements (PreparedStatement API)
    // -------------------------------------------------------------------------

    /**
     * Runs a parametrised INSERT / UPDATE / DELETE.
     *
     * @param sql    SQL with ? placeholders
     * @param params array of values
     * @param silent if false, prints success message
     */
    public boolean prepSQLUpdate(String sql, Object[] params, boolean silent) {
        if (!isConnected()) {
            System.err.println("Cannot run prepared update: not connected.");
            return false;
        }

        try (PreparedStatement ps = dbConnection.prepareStatement(sql)) {
            int index = 1;
            for (Object param : params) {
                ps.setObject(index++, param);
            }
            ps.executeUpdate();

            if (!silent) {
                System.out.println("Prepared SQL update executed successfully.");
            }
            return true;
        } catch (SQLException e) {
            System.err.println("Prepared update error: " + e.getMessage());
            System.err.println("SQL was: " + sql);
            return false;
        }
    }

    public boolean prepSQLUpdate(String sql, Object[] params) {
        return prepSQLUpdate(sql, params, true);
    }

    /**
     * Runs a parametrised SELECT query.
     *
     * @param sql    SQL with ? placeholders
     * @param params values for placeholders
     * @param silent if false, prints extra debug info
     */
    public ResultSet prepSQLQuery(String sql, Object[] params, boolean silent) {
        if (!isConnected()) {
            System.err.println("Cannot run prepared query: not connected.");
            return null;
        }

        try {
            PreparedStatement ps = dbConnection.prepareStatement(sql);
            int index = 1;
            for (Object param : params) {
                ps.setObject(index++, param);
            }

            ResultSet rs = ps.executeQuery();

            if (!silent) {
                System.out.println("Prepared SQL query executed successfully.");
            }
            return rs;

        } catch (SQLException e) {
            System.err.println("Prepared query error: " + e.getMessage());
            System.err.println("SQL was: " + sql);
            return null;
        }
    }

    public ResultSet prepSQLQuery(String sql, Object[] params) {
        return prepSQLQuery(sql, params, true);
    }

    // -------------------------------------------------------------------------
    //  Debug helper — prints a ResultSet nicely
    // -------------------------------------------------------------------------

    public boolean printResultSet(ResultSet rs) {
        if (rs == null) {
            System.err.println("ResultSet is null, cannot print.");
            return false;
        }

        try (rs) {
            ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    System.out.print(meta.getColumnName(i) + ": " + rs.getString(i));
                    if (i < cols) System.out.print(", ");
                }
                System.out.println();
            }
            return true;

        } catch (SQLException e) {
            System.err.println("Error printing ResultSet: " + e.getMessage());
            return false;
        }
    }
}
