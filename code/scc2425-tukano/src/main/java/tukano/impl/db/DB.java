package tukano.impl.db;

import org.hibernate.Session;
import tukano.api.Result;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public interface DB {
    <T> List<T> sql(String query, Class<T> _class, String table);
    <T> Result<T> getOne(String id, Class<T> _class);
    <T> Result<T> deleteOne(T obj);
    <T> Result<T> updateOne(T obj);
    <T> Result<T> insertOne(T obj);
    <T> Result<T> transaction(Consumer<Session> c);
    <T> Result<T> transaction(Function<Session, Result<T>> func);
}
