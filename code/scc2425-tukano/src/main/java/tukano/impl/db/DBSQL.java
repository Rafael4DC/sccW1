package tukano.impl.db;

import org.hibernate.Session;
import tukano.api.Result;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class DBSQL implements DB {

    public <T> List<T> sql(String query, Class<T> _class, String table) {
        return Hibernate.getInstance().sql(query, _class);
    }

    public <T> List<T> sql(Class<T> _class, String fmt, Object... args) {
        return Hibernate.getInstance().sql(String.format(fmt, args), _class);
    }

    public <T> Result<T> getOne(String id, Class<T> _class) {
        return Hibernate.getInstance().getOne(id, _class);
    }

    public <T> Result<T> deleteOne(T obj) {
        return Hibernate.getInstance().deleteOne(obj);
    }

    public <T> Result<T> updateOne(T obj) {
        return Hibernate.getInstance().updateOne(obj);
    }

    public <T> Result<T> insertOne(T obj) {
        return Result.errorOrValue(Hibernate.getInstance().persistOne(obj), obj);
    }

    @Override
    public <T> Result<T> transaction(Consumer<Session> c) {
        return Hibernate.getInstance().execute(c);
    }

    @Override
    public <T> Result<T> transaction(Function<Session, Result<T>> func) {
        return Hibernate.getInstance().execute(func);
    }
}
