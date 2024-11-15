package tukano.impl.rest;

import jakarta.ws.rs.core.Application;
import org.glassfish.jersey.jdkhttp.JdkHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.hibernate.annotations.processing.SQL;
import tukano.impl.Token;
import tukano.impl.db.DBNOSQL;
import tukano.impl.db.DBSQL;
import utils.Args;
import utils.IP;

import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

public class TukanoRestServer extends Application {
    public static final int PORT = 8080;
    //if local
    static final String INETADDR_ANY = "0.0.0.0";
    final private static Logger Log = Logger.getLogger(TukanoRestServer.class.getName());
    public static String serverURI;
    static String SERVER_BASE_URI = "http://%s:%s/rest";
    private final boolean isCache = false;
    //if azure
    private final Set<Object> singletons = new HashSet<>();
    private final Set<Class<?>> resources = new HashSet<>();

    public TukanoRestServer() {
        Token.setSecret(Args.valueOf("-secret", ""));

        resources.add(RestUsersResource.class);
        resources.add(RestShortsResource.class);
        resources.add(RestBlobsResource.class);

        Resources.start("nosql",isCache);
        //Resources.start("sql",isCache);
    }

    public static void main(String[] args) throws Exception {
        Args.use(args);
        Token.setSecret(Args.valueOf("-secret", ""));
        serverURI = String.format(SERVER_BASE_URI, IP.hostname(), PORT);
        new TukanoRestServer().start();
    }

    public void start() throws Exception {
        ResourceConfig config = new ResourceConfig();

        config.register(RestBlobsResource.class);
        config.register(RestUsersResource.class);
        config.register(RestShortsResource.class);

        //Resources.start("nosql",isCache);
        Resources.start("sql",isCache);

        JdkHttpServerFactory.createHttpServer(URI.create(serverURI.replace(IP.hostname(), INETADDR_ANY)), config);

        Log.info(String.format("Tukano Server ready @ %s\n", serverURI));
    }

    @Override
    public Set<Class<?>> getClasses() {
        return resources;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Set<Object> getSingletons() {
        return singletons;
    }
}
