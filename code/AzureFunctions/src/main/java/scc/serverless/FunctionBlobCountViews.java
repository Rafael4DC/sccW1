package scc.serverless;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import java.util.HashMap;


public class FunctionBlobCountViews {
    private static final String NAME = "name";
    private static final String PATH = "shorts/{" + NAME + "}";
    private static final String BLOBS_TRIGGER_NAME = "blobFunctionTrigger";
    private static final String BLOBS_FUNCTION_NAME = "blobFunctionExample";
    private static final String DATA_TYPE = "binary";
    private static final String BLOBSTORE_CONNECTION_ENV = "AzureWebJobsStorage";
    private final String containerName = "shorts";

    @FunctionName(BLOBS_FUNCTION_NAME)
    public void blobFunctionExample(
            @BlobTrigger(name = BLOBS_TRIGGER_NAME,
                    dataType = DATA_TYPE, path = PATH,
                    connection = BLOBSTORE_CONNECTION_ENV) byte[] content,
            @BindingName("name") String blobname, final ExecutionContext context) {


        context.getLogger().warning(String.format("blobFunctionExample: blob : %s, updated with %d bytes", blobname, content.length));
    }
}
