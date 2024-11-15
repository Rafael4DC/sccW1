package tukano.impl.db.session;

import com.azure.cosmos.models.CosmosBatch;
import com.azure.cosmos.models.PartitionKey;
import org.hibernate.Session;

public class CosmosSession implements ISession {
    private final CosmosBatch batch;
    public CosmosSession(PartitionKey partitionKey) {
        this.batch = CosmosBatch.createCosmosBatch(partitionKey);
    }
    public <T> void createItem(T item) {
        batch.createItemOperation(item);
    }
    public void deleteItem(String id) {
        batch.deleteItemOperation(id);
    }
    public <T> void replaceItem(String id, T item) {
        batch.replaceItemOperation(id, item);
    }
    CosmosBatch getBatch() {
        return batch;
    }
}
