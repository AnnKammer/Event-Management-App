/*
 * Team Lentil — Campus Event Management System (MOD004881, Element 010)
 * File: DropAll.java
 *
 * Purpose:
 *   Development-only helper to wipe the entire PostgreSQL schema.
 *   This is useful when running repeated local tests or reseeding the database.
 *
 * WARNING:
 *   • This will DROP ALL TABLES and ALL ENUM TYPES in the connected database.
 *   • Use ONLY in development environments — never in production.
 *
 * Behaviour:
 *   1. Drops all tables discovered in information_schema.
 *   2. Drops all enum types discovered in pg_type/pg_enum.
 *   3. Uses CASCADE to handle dependencies safely.
 *
 * Typical usage:
 *     DB db = new DB();
 *     db.connect(false);
 *     DropAll.dropAll(db);
 *     db.disconnect(false);
 *
 * Notes:
 *   • Uses db.runSQLUpdate(...) so it integrates with your DB helper.
 *   • The method returns true on success and false if any drop fails.
 */
package uk.ac.aru.campusevents.database;

import java.sql.ResultSet;
import java.sql.SQLException;

public final class DropAll {

    private DropAll() {
        // Utility class; prevent instantiation
    }

    /**
     * Drops ALL tables and ALL enum types in the current database.
     * Safe to rerun — ignores missing objects.
     *
     * @param db active database connection wrapper
     * @return true if everything dropped successfully, false if any error occurred
     */
    public static boolean dropAll(DB db) {
        System.out.println("Dropping ALL tables and ALL ENUM types (development only)...");

        if (db == null || !db.isConnected()) {
            System.err.println("DropAll failed: DB is not connected.");
            return false;
        }

        try {
            dropAllTables(db);
            dropAllEnumTypes(db);

            System.out.println("Database fully wiped.");
            return true;

        } catch (Exception ex) {
            System.err.println("DropAll failed: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    // -------------------------------------------------------------------------
    //  DROP TABLES
    // -------------------------------------------------------------------------

    private static void dropAllTables(DB db) throws SQLException {
        System.out.println("→ Dropping tables...");

        try (ResultSet rs = db.runSQLQuery("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                ORDER BY table_name ASC;
                """, true)) {

            while (rs != null && rs.next()) {
                String table = rs.getString("table_name");

                String sql = "DROP TABLE IF EXISTS " + table + " CASCADE;";
                db.runSQLUpdate(sql, true);

                System.out.println("  • dropped table: " + table);
            }
        }

        System.out.println("Tables dropped.");
    }

    // -------------------------------------------------------------------------
    //  DROP ENUM TYPES
    // -------------------------------------------------------------------------

    private static void dropAllEnumTypes(DB db) throws SQLException {
        System.out.println("→ Dropping ENUM types...");

        try (ResultSet rs = db.runSQLQuery("""
                SELECT t.typname AS enum_name
                FROM pg_type t
                JOIN pg_enum e ON t.oid = e.enumtypid
                GROUP BY enum_name
                ORDER BY enum_name ASC;
                """, true)) {

            while (rs != null && rs.next()) {
                String enumName = rs.getString("enum_name");

                String sql = "DROP TYPE IF EXISTS " + enumName + " CASCADE;";
                db.runSQLUpdate(sql, true);

                System.out.println("  • dropped enum: " + enumName);
            }
        }

        System.out.println("ENUM types dropped.");
    }
}

