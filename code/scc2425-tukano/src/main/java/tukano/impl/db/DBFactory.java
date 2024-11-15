package tukano.impl.db;

import tukano.impl.rest.Resources;

import java.util.logging.Logger;

public class DBFactory {
    private static final Logger logger = Logger.getLogger(DBFactory.class.getName());
    private static DB instance;


    public static synchronized DB getDB() {
        if (instance == null) {
            logger.info("Initializing DB as: " + Resources.getDb());
            if ("nosql".equals(Resources.getDb())) {
                instance = DBNOSQL.getInstance("PRIMARY_REGION"); // Replace with the desired region
            } else {
                instance = new DBSQL();
            }
        }
        return instance;
    }

}
