package tukano.impl.storage;


import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobStorageException;
import tukano.api.Result;
import utils.Hash;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.function.Consumer;

import static tukano.api.Result.ErrorCode.*;
import static tukano.api.Result.error;
import static tukano.api.Result.ok;

public class CloudStorage implements BlobStorage {
    private final BlobContainerClient containerClient;
    private final String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=tukanostorage71750;AccountKey=0fZLpV8LHC1ggTMSyZQftaRxDeXVlwEH74CnUk3B+hsYIG5Q5pAsZAe2F6rwrpNBHc/k9vw7mrmr+ASt8T1tKA==;EndpointSuffix=core.windows.net";
    private final String containerName = "shorts";

    public CloudStorage() {
        containerClient = new BlobContainerClientBuilder()
                .connectionString(storageConnectionString)
                .containerName(containerName)
                .buildClient();

    }

    @Override
    public Result<Void> write(String path, byte[] bytes) {
        if (path == null)
            return error(BAD_REQUEST);

        try {
            BlobClient blobClient = containerClient.getBlobClient(path);

            if (blobClient.exists()) {
                byte[] existingData = readBlob(blobClient);
                if (Arrays.equals(Hash.sha256(bytes), Hash.sha256(existingData))) {
                    return ok();
                } else {
                    return error(CONFLICT);
                }
            }

            blobClient.upload(new ByteArrayInputStream(bytes), bytes.length, true);
            return ok();
        } catch (Exception e) {
            e.printStackTrace();
            return error(INTERNAL_ERROR);
        }
    }

    @Override
    public Result<byte[]> read(String path) {
        if (path == null)
            return error(BAD_REQUEST);

        try {
            BlobClient blobClient = containerClient.getBlobClient(path);
            if (!blobClient.exists()) {
                return error(NOT_FOUND);
            }

            byte[] data = readBlob(blobClient);
            return ok(data);
        } catch (Exception e) {
            e.printStackTrace();
            return error(INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> read(String path, Consumer<byte[]> sink) {
        if (path == null)
            return error(BAD_REQUEST);

        try {
            BlobClient blobClient = containerClient.getBlobClient(path);
            if (!blobClient.exists()) {
                return error(NOT_FOUND);
            }

            byte[] data = readBlob(blobClient);
            sink.accept(data);
            return ok();
        } catch (Exception e) {
            e.printStackTrace();
            return error(INTERNAL_ERROR);
        }
    }

    @Override
    public Result<Void> delete(String path) {
        if (path == null)
            return error(BAD_REQUEST);

        try {
            BlobClient blobClient = containerClient.getBlobClient(path);
            if (blobClient.exists()) {
                blobClient.delete();
            }
            return ok();
        } catch (BlobStorageException e) {
            e.printStackTrace();
            return error(INTERNAL_ERROR);
        }
    }

    private byte[] readBlob(BlobClient blobClient) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        blobClient.download(outputStream);
        return outputStream.toByteArray();
    }
}
