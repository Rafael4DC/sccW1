package scc.serverless;

import com.microsoft.azure.functions.*;
import com.azure.cosmos.*;
import com.azure.cosmos.models.*;
import com.microsoft.azure.functions.annotation.*;
import scc.data.Short;
import scc.data.User;

import java.util.*;

public class FunctionTukanoRecommends {

    private static final String DATABASE_NAME = "TuKanoDB";
    private static final String CONTAINER_NAME_SHORTS = "Shorts";
    private static final String CONTAINER_NAME_USERS = "Users";
    private static final String SYSTEM_USER_ID = "TukanoRecommends";

    private final CosmosClient cosmosClient;
    private final CosmosContainer shortsContainer;
    private final CosmosContainer usersContainer;

    public FunctionTukanoRecommends() {
        cosmosClient = new CosmosClientBuilder()
                .endpoint(System.getenv("COSMOSDB_ENDPOINT"))
                .key(System.getenv("COSMOSDB_KEY"))
                .buildClient();

        shortsContainer = cosmosClient
                .getDatabase(DATABASE_NAME)
                .getContainer(CONTAINER_NAME_SHORTS);

        usersContainer = cosmosClient
                .getDatabase(DATABASE_NAME)
                .getContainer(CONTAINER_NAME_USERS);
    }

    @FunctionName("TukanoRecommends")
    public void run(
            @TimerTrigger(name = "timerInfo", schedule = "0 0 * * * *") String timerInfo,
            final ExecutionContext context) {

        context.getLogger().info("Running Tukano Recommends function...");

        try {
            ensureSystemUserExists(context);
            List<Short> topShorts = getTopShorts(context);
            republishShorts(topShorts, context);
        } catch (Exception e) {
            context.getLogger().severe("Error in Tukano Recommends: " + e.getMessage());
        }
    }

    private void ensureSystemUserExists(ExecutionContext context) {
        try {
            usersContainer.readItem(SYSTEM_USER_ID, new PartitionKey(SYSTEM_USER_ID), User.class);
            context.getLogger().info("System user TukanoRecommends exists.");
        } catch (CosmosException e) {
            if (e.getStatusCode() == 404) {
                User systemUser = new User(SYSTEM_USER_ID, "123", "tukano@recommends.com", "Tukano Recommends");
                usersContainer.createItem(systemUser, new PartitionKey(SYSTEM_USER_ID), new CosmosItemRequestOptions());
                context.getLogger().info("Created system user Tukano Recommends.");
            } else {
                throw e;
            }
        }
    }

    private List<Short> getTopShorts(ExecutionContext context) {
        SqlQuerySpec query = new SqlQuerySpec(
                "SELECT * FROM Short ORDER BY Short.totalViews DESC OFFSET 0 LIMIT 10");
        List<Short> topShorts = new ArrayList<>();

        Iterable<Short> results = shortsContainer.queryItems(query,new CosmosQueryRequestOptions() ,Short.class);
        results.forEach(topShorts::add);

        context.getLogger().info("Fetched top shorts for Tukano Recommends.");
        return topShorts;
    }

    private void republishShorts(List<Short> topShorts, ExecutionContext context) {
        topShorts.forEach(shortMetadata -> {
            Short recommendedShort = new Short(
                    UUID.randomUUID().toString(), // New unique ID
                    SYSTEM_USER_ID,
                    shortMetadata.getBlobUrl(),
                    System.currentTimeMillis(),
                    shortMetadata.getTotalLikes(),
                    shortMetadata.getTotalViews()
            );

            shortsContainer.createItem(recommendedShort, new PartitionKey(SYSTEM_USER_ID), new CosmosItemRequestOptions());
        });

        context.getLogger().info("Republished shorts to Tukano Recommends feed.");
    }
}
