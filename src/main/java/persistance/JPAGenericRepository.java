package persistance;

import jakarta.persistence.EntityExistsException;
import jakarta.persistence.EntityManagerFactory;

import java.util.Collection;
import java.util.Optional;

public class JPAGenericRepository {

    public JPAGenericRepository(EntityManagerFactory entityManagerFactory){
        this.entityManagerFactory = entityManagerFactory;
    }

    private EntityManagerFactory entityManagerFactory;

    public <T> T safe(T entity){
        var entityManager = entityManagerFactory.createEntityManager();
        try{
            entityManager.getTransaction().begin();
            entityManager.persist(entity);
            entityManager.getTransaction().commit();
        }
        catch (EntityExistsException e){
            entityManager.getTransaction().rollback();
            entityManager.clear();
            entityManager.getTransaction().begin();
            entityManager.merge(entity);
            entityManager.getTransaction().commit();
        }
        catch (Exception e){
            entityManager.getTransaction().rollback();
            throw e;
        }
        finally {
            entityManager.close();
        }
        return entity;
    }

    public <T> Collection<T> findAll(Class<T> tClass){
        var entityManager = entityManagerFactory.createEntityManager();
        try{
            String jpql = """
                        Select t from %s t
                    """.formatted(tClass.getSimpleName());
            var query = entityManager.createQuery(jpql,tClass);
            return query.getResultList();
        }
        finally {
            entityManager.close();
        }
    }

    public <K,T> Optional<T> findByID(K key, Class<T> tClass){
        var entityManager = entityManagerFactory.createEntityManager();
        try{
            return Optional.ofNullable(entityManager.find(tClass,key));
        }
        finally {
            entityManager.close();
        }
    }
}
