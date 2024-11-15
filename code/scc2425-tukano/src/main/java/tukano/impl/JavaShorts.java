package tukano.impl;

import com.azure.cosmos.models.CosmosBatch;
import com.azure.cosmos.models.CosmosBatchResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import tukano.api.Short;
import tukano.api.*;
import tukano.impl.data.Following;
import tukano.impl.data.Likes;
import tukano.impl.db.DB;
import tukano.impl.db.DBFactory;
import tukano.impl.db.DBNOSQL;
import tukano.impl.db.DBSQL;
import tukano.impl.rest.TukanoRestServer;
import tukano.impl.rest.Resources;

import java.lang.reflect.Type;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static java.lang.String.format;
import static tukano.api.Result.*;
import static tukano.api.Result.ErrorCode.*;

public class JavaShorts implements Shorts {

    private static final Logger Log = Logger.getLogger(JavaShorts.class.getName());

    private static Shorts instance;
    private static final DB DB = DBFactory.getDB();

    private JavaShorts() {
    }

    synchronized public static Shorts getInstance() {
        if (instance == null)
            instance = new JavaShorts();
        return instance;
    }

    @Override
    public Result<Short> createShort(String userId, String password) {
        Log.info(() -> format("createShort : userId = %s, pwd = %s\n", userId, password));

        return errorOrResult(okUser(userId, password), user -> {

            var shortId = format("%s+%s", userId, UUID.randomUUID());
            var blobUrl = format("%s/%s/%s", TukanoRestServer.serverURI, Blobs.NAME, shortId);
            var shrt = new Short(shortId, userId, blobUrl);

            return errorOrValue(DB.insertOne(shrt), s -> s.copyWithLikes_And_Token(0));
        });
    }

    @Override
    public Result<Short> getShort(String shortId) {
        Log.info(() -> format("getShort : shortId = %s\n", shortId));

        if (shortId == null)
            return error(BAD_REQUEST);

        var query = format("SELECT count(*) FROM Likes l WHERE l.shortId = '%s'", shortId);
        var likes = DB.sql(query, Long.class,"Likes");
        return errorOrValue(DB.getOne(shortId, Short.class), shrt -> shrt.copyWithLikes_And_Token(likes.get(0)));
    }


    @Override
    public Result<Void> deleteShort(String shortId, String password) {
        Log.info(() -> format("deleteShort : shortId = %s, pwd = %s\n", shortId, password));
        if( DB instanceof DBSQL){
            return errorOrResult(getShort(shortId), shrt -> {
                return errorOrResult(okUser(shrt.getOwnerId(), password), user -> {
                    return DB.transaction(hibernate -> {

                        hibernate.remove(shrt);

                        var query = format("DELETE Likes l WHERE l.shortId = '%s'", shortId);
                        hibernate.createNativeQuery(query, Likes.class).executeUpdate();

                        JavaBlobs.getInstance().delete(shrt.getBlobUrl(), Token.get());
                    });
                });
            });
        }else{
            return errorOrResult(getShort(shortId), shrt -> {
                return errorOrResult(okUser(shrt.getOwnerId(), password), user -> {
                    try {
                        CosmosBatch batch = CosmosBatch.createCosmosBatch(new PartitionKey(shortId));
                        batch.deleteItemOperation(shortId);
                        batch.deleteItemOperation(format("Likes-%s", shortId));
                        CosmosBatchResponse response = DBNOSQL.getContainer(Short.class).executeCosmosBatch(batch);

                        if (response.getStatusCode() == 200) {
                            JavaBlobs.getInstance().delete(shrt.getBlobUrl(), Token.get());
                            return ok();
                        } else {
                            return error(INTERNAL_ERROR);
                        }

                    } catch (Exception e) {
                        Log.warning(() -> "Failed to delete short in Cosmos DB: " + e.getMessage());
                        return error(INTERNAL_ERROR);
                    }
                });
            });
        }
    }

    @Override
    public Result<List<String>> getShorts(String userId) {
        Log.info(() -> format("getShorts : userId = %s\n", userId));

        var query = format("SELECT s.id FROM Short s WHERE s.ownerId = '%s'", userId);
        return errorOrValue(okUser(userId), DB.sql(query, String.class,"Short"));
    }

    @Override
    public Result<Void> follow(String userId1, String userId2, boolean isFollowing, String password) {
        Log.info(() -> format("follow : userId1 = %s, userId2 = %s, isFollowing = %s, pwd = %s\n", userId1, userId2, isFollowing, password));


        return errorOrResult(okUser(userId1, password), user -> {
            var f = new Following(userId1, userId2);
            return errorOrVoid(okUser(userId2), isFollowing ? DB.insertOne(f) : DB.deleteOne(f));
        });
    }

    @Override
    public Result<List<String>> followers(String userId, String password) {
        Log.info(() -> format("followers : userId = %s, pwd = %s\n", userId, password));

        var query = format("SELECT f.follower FROM Following f WHERE f.followee = '%s'", userId);
        return errorOrValue(okUser(userId, password), DB.sql(query, String.class,"Following"));
    }

    @Override
    public Result<Void> like(String shortId, String userId, boolean isLiked, String password) {
        Log.info(() -> format("like : shortId = %s, userId = %s, isLiked = %s, pwd = %s\n", shortId, userId, isLiked, password));


        return errorOrResult(getShort(shortId), shrt -> {
            var l = new Likes(userId, shortId, shrt.getOwnerId());
            return errorOrVoid(okUser(userId, password), isLiked ? DB.insertOne(l) : DB.deleteOne(l));
        });
    }

    @Override
    public Result<List<String>> likes(String shortId, String password) {
        Log.info(() -> format("likes : shortId = %s, pwd = %s\n", shortId, password));

        return errorOrResult(getShort(shortId), shrt -> {

            var query = format("SELECT l.userId FROM Likes l WHERE l.shortId = '%s'", shortId);

            return errorOrValue(okUser(shrt.getOwnerId(), password), DB.sql(query, String.class,"Likes"));
        });
    }

    @Override
    public Result<List<String>> getFeed(String userId, String password) {
        Log.info(() -> format("getFeed : userId = %s, pwd = %s\n", userId, password));

        final var QUERY_FMT = """
                SELECT s.shortId, s.timestamp FROM Short s WHERE	s.ownerId = '%s'				
                UNION			
                SELECT s.shortId, s.timestamp FROM Short s, Following f 
                	WHERE 
                		f.followee = s.ownerId AND f.follower = '%s' 
                ORDER BY s.timestamp DESC""";

        return errorOrValue(okUser(userId, password), DB.sql(format(QUERY_FMT, userId, userId), String.class,"Short"));
    }

    protected Result<User> okUser(String userId, String pwd) {
        return JavaUsers.getInstance().getUser(userId, pwd);
    }

    private Result<Void> okUser(String userId) {
        var res = okUser(userId, "");
        if (res.error() == FORBIDDEN)
            return ok();
        else
            return error(res.error());
    }

    @Override
    public Result<Void> deleteAllShorts(String userId, String password, String token) {
        Log.info(() -> format("deleteAllShorts : userId = %s, password = %s, token = %s\n", userId, password, token));

        if (DB instanceof DBSQL) {
            // SQL-based logic
            return DB.transaction((hibernate) -> {

                // Validate user password
                var userResult = okUser(userId, password);
                if (!userResult.isOK()) {
                    return error(FORBIDDEN);
                }

                // Delete shorts
                var query1 = format("DELETE FROM Short s WHERE s.ownerId = '%s'", userId);
                hibernate.createQuery(query1).executeUpdate();

                // Delete follows
                var query2 = format("DELETE FROM Following f WHERE f.follower = '%s' OR f.followee = '%s'", userId, userId);
                hibernate.createQuery(query2).executeUpdate();

                // Delete likes
                var query3 = format("DELETE FROM Likes l WHERE l.ownerId = '%s' OR l.userId = '%s'", userId, userId);
                hibernate.createQuery(query3).executeUpdate();

                // Optionally delete user's blobs (if necessary, for each short deleted)
                // Example: JavaBlobs.getInstance().delete(blobUrl, token);

                return ok(); // Return success
            });
        } else {
            return errorOrResult(okUser(userId, password), user -> {
                try {
                    var shorts = DBNOSQL.getContainer(Short.class)
                            .queryItems("SELECT * FROM Short WHERE c.ownerId = @ownerId",
                                    new CosmosQueryRequestOptions().setPartitionKey(new PartitionKey(userId)),
                                    Short.class)
                            .stream()
                            .toList();

                    CosmosBatch batch = CosmosBatch.createCosmosBatch(new PartitionKey(userId));

                    for (Short s : shorts) {
                        batch.deleteItemOperation(s.getId());
                    }

                    CosmosBatchResponse response = DBNOSQL.getContainer(Short.class).executeCosmosBatch(batch);
                    if (response.getStatusCode() == 200) {
                        return ok();
                    } else {
                        return error(INTERNAL_ERROR);
                    }


                } catch (Exception e) {
                    Log.severe(() -> "Failed deleting batch: " + e.getMessage());
                    return error(INTERNAL_ERROR);
                }
            });
        }
    }


}