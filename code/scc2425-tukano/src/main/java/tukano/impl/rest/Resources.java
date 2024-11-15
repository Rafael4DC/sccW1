package tukano.impl.rest;

import tukano.impl.db.DB;
import tukano.impl.db.DBNOSQL;

public class Resources {
    private static String dbName = null;
    private static boolean isCache = false;

    public static void start(String dbName,boolean isCache) {
        Resources.dbName = dbName;
        Resources.isCache = isCache;
    }

    public static String getDb() {
        return dbName;
    }
    public static boolean isCache() {
        return isCache;
    }
}
