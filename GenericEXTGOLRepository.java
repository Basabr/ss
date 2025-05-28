package it.polito.extgol;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

public class GenericExtGOLRepository<E, I> {

    private final Class<E> entityClass;
    protected final String entityName;

    protected GenericExtGOLRepository(Class<E> entityClass) {
        Objects.requireNonNull(entityClass, "Entity class must not be null");
        this.entityClass = entityClass;
        this.entityName = getEntityName(entityClass);
    }

    protected static String getEntityName(Class<?> entityClass) {
        Entity annotation = entityClass.getAnnotation(Entity.class);
        if (annotation == null) {
            throw new IllegalArgumentException(
                "Class " + entityClass.getName() + " must be annotated as @Entity");
        }
        String name = annotation.name();
        return name == null || name.isEmpty() ? entityClass.getSimpleName() : name;
    }

    public Optional<E> findById(I id) {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            E entity = em.find(entityClass, id);
            return Optional.ofNullable(entity);
        } finally {
            em.close();
        }
    }

    public List<E> findAll() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.createQuery("SELECT e FROM " + entityName + " e", entityClass)
                     .getResultList();
        } finally {
            em.close();
        }
    }

    public void create(E entity) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.persist(entity);
            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public void update(E entity) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            em.merge(entity);
            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }

    public void delete(E entity) {
        EntityManager em = JPAUtil.getEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            E managed = em.contains(entity) ? entity : em.merge(entity);
            em.remove(managed);
            tx.commit();
        } catch (RuntimeException ex) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw ex;
        } finally {
            em.close();
        }
    }
}
