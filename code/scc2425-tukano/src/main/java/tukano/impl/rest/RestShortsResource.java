package tukano.impl.rest;

import jakarta.inject.Singleton;
import redis.clients.jedis.Jedis;
import tukano.api.Short;
import tukano.api.Shorts;
import tukano.api.rest.RestShorts;
import tukano.impl.JavaShorts;
import tukano.impl.cache.RedisCache;
import utils.JSON;

import java.util.List;

@Singleton
public class RestShortsResource extends RestResource implements RestShorts {

    private static final String SHORT_CACHE = "shorts:";
    static final Shorts impl = JavaShorts.getInstance();

    @Override
    public Short createShort(String userId, String password) {

        Short shrt = super.resultOrThrow(impl.createShort(userId, password));
        if(Resources.isCache()){return shrt;}
        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            var key = SHORT_CACHE + shrt.getId();
            jedis.set( key, JSON.encode(shrt));
            return shrt;
        }

    }

    @Override
    public void deleteShort(String shortId, String password) {
        if(Resources.isCache()){super.resultOrThrow(impl.deleteShort(shortId, password));}

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            var userKey = SHORT_CACHE + shortId;
            if (jedis.exists(userKey)) {
                jedis.del(userKey);
            }
        }
        super.resultOrThrow(impl.deleteShort(shortId, password));
    }

    @Override
    public Short getShort(String shortId) {
        if(Resources.isCache()){super.resultOrThrow(impl.getShort(shortId));}

        try (var jedis = RedisCache.getCachePool().getResource()) {
            var key = SHORT_CACHE + shortId;
            var value = jedis.get(key);
            if (value != null) {
                return JSON.decode(value, Short.class);
            }
            //sets if not in cache
            var shrt = super.resultOrThrow(impl.getShort(shortId));
            jedis.set(key, JSON.encode(shrt));
            return shrt;
        }

    }

    @Override
    public List<String> getShorts(String userId) {
        return super.resultOrThrow(impl.getShorts(userId));
    }

    @Override
    public void follow(String userId1, String userId2, boolean isFollowing, String password) {
        super.resultOrThrow(impl.follow(userId1, userId2, isFollowing, password));
    }

    @Override
    public List<String> followers(String userId, String password) {
        return super.resultOrThrow(impl.followers(userId, password));
    }

    @Override
    public void like(String shortId, String userId, boolean isLiked, String password) {
        super.resultOrThrow(impl.like(shortId, userId, isLiked, password));
    }

    @Override
    public List<String> likes(String shortId, String password) {
        return super.resultOrThrow(impl.likes(shortId, password));
    }

    @Override
    public List<String> getFeed(String userId, String password) {
        return super.resultOrThrow(impl.getFeed(userId, password));
    }

    @Override
    public void deleteAllShorts(String userId, String password, String token) {
        super.resultOrThrow(impl.deleteAllShorts(userId, password, token));
    }
}
