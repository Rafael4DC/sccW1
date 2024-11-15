package tukano.impl.rest;

public class Resources {
    private static String dbName = null;
    private static boolean isCache = false;
    private static String connectionString = "eu";

    public static void start(String dbName,boolean isCache,String connectionString) {
        Resources.dbName = dbName;
        Resources.isCache = isCache;
        Resources.connectionString = connectionString;
    }

    public static String getDb() {
        return dbName;
    }
    public static boolean isCache() {
        return !isCache;
    }

    public static String getConnectionString() {
        return connectionString;
    }
}
