package tukano.impl.storage;

import tukano.api.Result;

import java.util.function.Consumer;

public interface BlobStorage {

    Result<Void> write(String path, byte[] bytes);

    Result<Void> delete(String path);

    Result<byte[]> read(String path);

    Result<Void> read(String path, Consumer<byte[]> sink);

}
