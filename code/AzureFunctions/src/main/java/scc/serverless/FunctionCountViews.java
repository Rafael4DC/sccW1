package scc.serverless;


import com.azure.cosmos.*;
import scc.data.Short;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.PartitionKey;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 * This is just the code of the cloud function, the actual deployment is done in a != project.
 * This function increments the view count of a short by downloads.
 */
public class FunctionCountViews {
    // Function and trigger names
    private static final String FUNCTION_IDENTIFIER = "incrementViewCount";
    private static final String HTTP_TRIGGER_NAME = "httpTrigger";
    private static final String ROUTE_TEMPLATE = "shorts/{itemId}";

    private static final String DB_URL = "https://tukano71750.documents.azure.com:443/";
    private static final String DB_KEY = "WEQzCZuytjiUm7cwyY0wd9Avqu4xddmKBebY53nnutc1VNDF9mPmDZibIKIuqC34H5Hd2WWAmK4JACDbeDkXCA==";
    private static final String DB_NAME = "tukano71750NOSQL";

    // Environment variable keys
    // static final String DB_URL = "DB_URL";
    //private static final String DB_KEY = "DB_KEY";
    //private static final String DB_NAME = "DB_NAME";
    private static final String CONTAINER_TABLE_NAME = "shorts";

    private static final CosmosClient cosmosClient;
    private static final CosmosContainer cosmosContainer;

    private static final String endpoint = System.getenv(DB_URL);
    private static final String key = System.getenv(DB_KEY);
    private static final String databaseName = System.getenv(DB_NAME);

    // Initialize the Cosmos client and container
    static {
        cosmosClient = new CosmosClientBuilder()
                .endpoint(endpoint)
                .key(key)
                .consistencyLevel(ConsistencyLevel.STRONG)
                .directMode() // alternatively, use gateway mode
                .buildClient();

        CosmosDatabase database = cosmosClient.getDatabase(databaseName);
        cosmosContainer = database.getContainer(CONTAINER_TABLE_NAME);

    }

    @FunctionName(FUNCTION_IDENTIFIER)
    public HttpResponseMessage run(
            @HttpTrigger(
                    name = HTTP_TRIGGER_NAME,
                    methods = {HttpMethod.GET},
                    route = ROUTE_TEMPLATE
            ) HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id,
            final ExecutionContext executionContext
    ) {
        try {
            // Retrieve the item from Cosmos DB
            Short item = cosmosContainer.readItem(id, new PartitionKey(id), Short.class).getItem();
            if (item == null) {
                return request.createResponseBuilder(HttpStatus.NOT_FOUND)
                        .body("Item not found")
                        .build();
            }

            // Increment the view count
            item.setTotalLikes(item.getTotalLikes() + 1);

            // Update the item in Cosmos DB
            CosmosItemResponse<Short> updateResponse = cosmosContainer.upsertItem(item);

            // Return the updated item
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body(updateResponse.getItem())
                    .build();

        } catch (CosmosException cosmosException) {
            executionContext.getLogger().severe("Cosmos DB error: " + cosmosException.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Database operation failed")
                    .build();
        } catch (Exception exception) {
            executionContext.getLogger().severe("Unhandled exception: " + exception.getMessage());
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An unexpected error occurred")
                    .build();
        }
    }
}
