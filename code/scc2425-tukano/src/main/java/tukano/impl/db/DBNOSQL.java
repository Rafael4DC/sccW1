package tukano.impl.db;

import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hibernate.Session;
import tukano.api.Result;
import tukano.impl.db.session.CosmosSession;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Cosmos DB No SQL database implementation
 */
public class DBNOSQL implements DB {
    private static final Logger Log = Logger.getLogger(DBNOSQL.class.getName());
    private static final String DB_URL = "https://tukano71750.documents.azure.com:443/";
    private static final String DB_KEY = "WEQzCZuytjiUm7cwyY0wd9Avqu4xddmKBebY53nnutc1VNDF9mPmDZibIKIuqC34H5Hd2WWAmK4JACDbeDkXCA==";
    private static final String DB_NAME = "tukano71750NOSQL";

    private static CosmosClient client;
    private static DBNOSQL instance;

    private DBNOSQL(String primaryRegion) {
        try {
            client = new CosmosClientBuilder()
                    .endpoint(DB_URL)
                    .key(DB_KEY)
                    .multipleWriteRegionsEnabled(true)
                    //.preferredRegions(List.of(primaryRegion))
                    //.gatewayMode() // https://learn.microsoft.com/en-us/azure/cosmos-db/nosql/sdk-connection-modes left here for eduroam purposes
                    //.directMode() // for better performance
                    .consistencyLevel(ConsistencyLevel.SESSION)
                    .connectionSharingAcrossClientsEnabled(true)
                    .contentResponseOnWriteEnabled(true) // On write, return the object written
                    .buildClient();
        } catch (Exception e) {
            Log.severe("Error: " + e.getMessage());
            throw e;
        }
    }

    synchronized public static DBNOSQL getInstance(String primaryRegion) {
        if (instance == null)
            instance = new DBNOSQL(primaryRegion);
        return instance;
    }


    /**
     * Get a container from the database with a specific class
     *
     * @param _class the class of the container
     * @return CosmosContainer - the container
     */
    public static <T> CosmosContainer getContainer(Class<T> _class) {
        return getContainer(_class.getSimpleName());
    }
    public static CosmosContainer getContainer(String containerName) {
        try {
            //Log.warning("Container is:"+containerName);
            return client.getDatabase(DB_NAME).getContainer(containerName);
        } catch (Exception e) {
            Log.severe("Error: " + e.getMessage());
            throw new RuntimeException("Failed to get container", e);
        }
    }

    public static <T> Result<T> tryCatch(Logger logger, Supplier<T> supplierFunc) {
        try {
            return Result.ok(supplierFunc.get());
        } catch (CosmosException ce) {
            logger.severe("CosmosException: " + ce.getMessage());
            return Result.error(errorCodeFromStatus(ce.getStatusCode()));
        } catch (Exception x) {
            logger.severe("Exception: " + x.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    public static Result.ErrorCode errorCodeFromStatus(int status) {
        return switch (status) {
            case 200 -> Result.ErrorCode.OK;
            case 404 -> Result.ErrorCode.NOT_FOUND;
            case 409 -> Result.ErrorCode.CONFLICT;
            default -> Result.ErrorCode.INTERNAL_ERROR;
        };
    }



    @Override
    public <T> Result<T> insertOne(T obj) {
        return tryCatch(Log, () -> getContainer(obj.getClass()).createItem(obj).getItem());
    }

    @Override
    public <T> Result<T> transaction(Consumer<Session> c) {
        return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        //return transactionCosmos((Consumer<CosmosDatabase>) c);
    }

    @Override
    public <T> Result<T> transaction(Function<Session, Result<T>> func) {
        return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        //return transactionCosmosFunc((Function<CosmosDatabase, Result<T>>) func);
    }

    private <T> Result<T> transactionCosmos(Consumer<CosmosDatabase> c) {
        try {
            c.accept(client.getDatabase(DB_NAME));
            return Result.ok();
        } catch (Exception e) {
            Log.severe("Error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    private <T> Result<T> transactionCosmosFunc(Function<CosmosDatabase, Result<T>> func) {
        try {
            return func.apply(client.getDatabase(DB_NAME));
        } catch (Exception e) {
            Log.severe("Error: " + e.getMessage());
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }



    @Override
    public <T> Result<T> getOne(String id, Class<T> _class) {
        return tryCatch(Log, () -> {
            CosmosContainer container = getContainer(_class);
            return container.readItem(id, new PartitionKey(id), _class).getItem();
        });
    }

    @Override
    public <T> Result<T> updateOne(T obj) {
        return tryCatch(Log, () -> getContainer(obj.getClass()).upsertItem(obj).getItem());
    }



    @Override
    public <T> Result<T> deleteOne(T obj) {
        try {
            tryCatch(Log, () -> getContainer(obj.getClass()).deleteItem(obj, new CosmosItemRequestOptions()).getItem());
            return Result.ok(obj);
        } catch (Exception e) {
            return Result.error(Result.ErrorCode.INTERNAL_ERROR);
        }
    }

    /**
     * Execute a SQL query on a container.
     *
     * @param query       the query string
     * @param _class       the class of the container's items
     * @param <T>         the container's item type
     * @return Result containing a list of items or an error
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> List<T> sql(String query, Class<T> _class, String table) {
        return tryCatch(Log, () -> {
            boolean isCountQuery = query.toLowerCase().contains("count(*)");
            String parsedQuery = query.replace("count(*)", "count(1)");

            var mapRes = getContainer(table).queryItems(parsedQuery, new CosmosQueryRequestOptions(), Map.class);

            var resList = new ArrayList<T>();
            mapRes.forEach(m -> {
                try {
                    Object value;
                    if (isCountQuery) {
                        // Handle count(*) query (always numeric, Long is safe here)
                        value = m.get("$1");
                        resList.add((T) castValue(value, _class));
                    } else if (m.size() == 1) {
                        // Single column select case (e.g., SELECT id)
                        value = m.values().iterator().next();
                        resList.add((T) castValue(value, _class));
                    } else {
                        // General case for multi-column object parsing
                        T result = new ObjectMapper().convertValue(m, _class);
                        resList.add(result);
                    }
                } catch (Exception e) {
                    throw new RuntimeException("Failed to cast query result to target type", e);
                }
            });
            return resList;
        }).value();
    }

    @SuppressWarnings("unchecked")
    private <T> T castValue(Object value, Class<T> _class) {
        if (value == null) {
            return null;
        }
        if (_class.isAssignableFrom(value.getClass())) {
            return _class.cast(value);
        }
        if (_class == Long.class) {
            return (T) Long.valueOf(value.toString());
        } else if (_class == Integer.class) {
            return (T) Integer.valueOf(value.toString());
        } else if (_class == String.class) {
            return (T) value.toString();
        } else if (_class == Double.class) {
            return (T) Double.valueOf(value.toString());
        }
        throw new IllegalArgumentException("Unsupported type: " + _class);
    }



}
