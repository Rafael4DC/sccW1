package tukano.impl.db;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.exception.ConstraintViolationException;
import tukano.api.Result;
import tukano.api.Result.ErrorCode;
import utils.Args;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * A helper class to perform POJO (Plain Old Java Objects) persistence, using
 * Hibernate and a backing relational database.
 */
public class Hibernate {
    private static final String HIBERNATE_CFG_FILE = "scc2425-tukano/src/main/webapp/WEB-INF/classes/hibernate.cfg.xml";
    private static final Logger Log = Logger.getLogger(Hibernate.class.getName());
    private static Hibernate instance;
    private final SessionFactory sessionFactory;

    private Hibernate() {
        try {
            if (Args.contains("-deploy")) {
                Log.info("Loading Hibernate configuration for local deployment.");
                sessionFactory = new Configuration().configure(new File(HIBERNATE_CFG_FILE)).buildSessionFactory();
            } else {
                Log.info("Loading Hibernate configuration for production.");
                sessionFactory = new Configuration().configure().buildSessionFactory();
            }
        } catch (Exception e) {
            Log.severe("Error in Hibernate constructor: " + e.getMessage());
            throw e;
        }
    }


    /**
     * Returns the Hibernate instance, initializing if necessary. Requires a
     * configuration file (hibernate.cfg.xml)
     *
     * @return
     */
    synchronized public static Hibernate getInstance() {
        if (instance == null)
            instance = new Hibernate();
        return instance;
    }

    public Result<Void> persistOne(Object obj) {
        return execute((hibernate) -> {
            hibernate.persist(obj);
        });
    }

    public <T> Result<T> updateOne(T obj) {
        return execute(hibernate -> {
            var res = hibernate.merge(obj);
            if (res == null)
                return Result.error(ErrorCode.NOT_FOUND);

            return Result.ok(res);
        });
    }

    public <T> Result<T> deleteOne(T obj) {
        return execute(hibernate -> {
            hibernate.remove(obj);
            return Result.ok(obj);
        });
    }

    public <T> Result<T> getOne(Object id, Class<T> _class) {
        try (var session = sessionFactory.openSession()) {
            var res = session.find(_class, id);
            if (res == null)
                return Result.error(ErrorCode.NOT_FOUND);
            else
                return Result.ok(res);
        } catch (Exception e) {
            throw e;
        }
    }

    public <T> List<T> sql(String sqlStatement, Class<T> _class) {
        try (var session = sessionFactory.openSession()) {
            var query = session.createNativeQuery(sqlStatement, _class);
            return query.list();
        } catch (Exception e) {
            throw e;
        }
    }

    public <T> Result<T> execute(Consumer<Session> proc) {
        return execute((hibernate) -> {
            proc.accept(hibernate);
            return Result.ok();
        });
    }

    public <T> Result<T> execute(Function<Session, Result<T>> func) {
        Transaction tx = null;
        try (var session = sessionFactory.openSession()) {
            tx = session.beginTransaction();
            var res = func.apply(session);
            session.flush();
            tx.commit();
            return res;
        } catch (ConstraintViolationException __) {
            return Result.error(ErrorCode.CONFLICT);
        } catch (Exception e) {
            if (tx != null)
                tx.rollback();

            e.printStackTrace();
            throw e;
        }
    }
}