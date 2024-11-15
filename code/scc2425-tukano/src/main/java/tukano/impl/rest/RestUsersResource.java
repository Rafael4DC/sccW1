package tukano.impl.rest;

import jakarta.inject.Singleton;
import redis.clients.jedis.Jedis;
import tukano.api.User;
import tukano.api.Users;
import tukano.api.rest.RestUsers;
import tukano.impl.JavaUsers;
import tukano.impl.cache.RedisCache;
import utils.JSON;

import java.util.List;

@Singleton
public class RestUsersResource extends RestResource implements RestUsers {

    private static final String USER_CACHE = "users:";

    final Users impl;

    public RestUsersResource() {
        this.impl = JavaUsers.getInstance();
    }

    @Override
    public String createUser(User user) {
        if(!Resources.isCache()){return super.resultOrThrow(impl.createUser(user));}

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            var key = USER_CACHE + user.getId();
            var value = jedis.get(key);
            if (value != null) {
                return "Already Exists";
            }
            var createdUser = super.resultOrThrow(impl.createUser(user));
            jedis.set( key, JSON.encode(user));
            return createdUser;
        }
    }

    @Override
    public User getUser(String name, String pwd) {
        if(!Resources.isCache()){return super.resultOrThrow(impl.getUser(name, pwd));}

        try (var jedis = RedisCache.getCachePool().getResource()) {
            var key = USER_CACHE + name;
            var value = jedis.get(key);
            if (value != null) {
                return JSON.decode(value, User.class);
            }
            //sets if not in cache
            var user = super.resultOrThrow(impl.getUser(name, pwd));
            jedis.set(key, JSON.encode(user));
            return user;
        }
    }

    @Override
    public User updateUser(String name, String pwd, User user) {
        if(!Resources.isCache()){return super.resultOrThrow(impl.updateUser(name, pwd, user));}

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            var newUser = super.resultOrThrow(impl.updateUser(name, pwd, user));
            var userKey = USER_CACHE + user.getDisplayName();
            if (jedis.exists(userKey)) {
                jedis.del(userKey);
                jedis.set(userKey, newUser.toString());
            }
            return newUser;
        }
    }

    @Override
    public User deleteUser(String name, String pwd) {
        if(!Resources.isCache()){return super.resultOrThrow(impl.deleteUser(name, pwd));}

        try (Jedis jedis = RedisCache.getCachePool().getResource()) {
            var userKey = USER_CACHE + name;
            if (jedis.exists(userKey)) {
                jedis.del(userKey);
            }
        }
        return super.resultOrThrow(impl.deleteUser(name, pwd));
    }

    @Override
    public List<User> searchUsers(String pattern) {
        return super.resultOrThrow(impl.searchUsers(pattern));
    }
}
