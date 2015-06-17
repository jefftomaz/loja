/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.loja.jpa.controller;

import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import br.com.loja.entities.Bairro;
import br.com.loja.entities.Ser;
import br.com.loja.jpa.controller.exceptions.IllegalOrphanException;
import br.com.loja.jpa.controller.exceptions.NonexistentEntityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author jefferson.tomaz
 */
public class SerJpaController implements Serializable {

    public SerJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Ser ser) {
        if (ser.getBairroCollection() == null) {
            ser.setBairroCollection(new ArrayList<Bairro>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Collection<Bairro> attachedBairroCollection = new ArrayList<Bairro>();
            for (Bairro bairroCollectionBairroToAttach : ser.getBairroCollection()) {
                bairroCollectionBairroToAttach = em.getReference(bairroCollectionBairroToAttach.getClass(), bairroCollectionBairroToAttach.getId());
                attachedBairroCollection.add(bairroCollectionBairroToAttach);
            }
            ser.setBairroCollection(attachedBairroCollection);
            em.persist(ser);
            for (Bairro bairroCollectionBairro : ser.getBairroCollection()) {
                Ser oldIdSerOfBairroCollectionBairro = bairroCollectionBairro.getIdSer();
                bairroCollectionBairro.setIdSer(ser);
                bairroCollectionBairro = em.merge(bairroCollectionBairro);
                if (oldIdSerOfBairroCollectionBairro != null) {
                    oldIdSerOfBairroCollectionBairro.getBairroCollection().remove(bairroCollectionBairro);
                    oldIdSerOfBairroCollectionBairro = em.merge(oldIdSerOfBairroCollectionBairro);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Ser ser) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Ser persistentSer = em.find(Ser.class, ser.getId());
            Collection<Bairro> bairroCollectionOld = persistentSer.getBairroCollection();
            Collection<Bairro> bairroCollectionNew = ser.getBairroCollection();
            List<String> illegalOrphanMessages = null;
            for (Bairro bairroCollectionOldBairro : bairroCollectionOld) {
                if (!bairroCollectionNew.contains(bairroCollectionOldBairro)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Bairro " + bairroCollectionOldBairro + " since its idSer field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            Collection<Bairro> attachedBairroCollectionNew = new ArrayList<Bairro>();
            for (Bairro bairroCollectionNewBairroToAttach : bairroCollectionNew) {
                bairroCollectionNewBairroToAttach = em.getReference(bairroCollectionNewBairroToAttach.getClass(), bairroCollectionNewBairroToAttach.getId());
                attachedBairroCollectionNew.add(bairroCollectionNewBairroToAttach);
            }
            bairroCollectionNew = attachedBairroCollectionNew;
            ser.setBairroCollection(bairroCollectionNew);
            ser = em.merge(ser);
            for (Bairro bairroCollectionNewBairro : bairroCollectionNew) {
                if (!bairroCollectionOld.contains(bairroCollectionNewBairro)) {
                    Ser oldIdSerOfBairroCollectionNewBairro = bairroCollectionNewBairro.getIdSer();
                    bairroCollectionNewBairro.setIdSer(ser);
                    bairroCollectionNewBairro = em.merge(bairroCollectionNewBairro);
                    if (oldIdSerOfBairroCollectionNewBairro != null && !oldIdSerOfBairroCollectionNewBairro.equals(ser)) {
                        oldIdSerOfBairroCollectionNewBairro.getBairroCollection().remove(bairroCollectionNewBairro);
                        oldIdSerOfBairroCollectionNewBairro = em.merge(oldIdSerOfBairroCollectionNewBairro);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = ser.getId();
                if (findSer(id) == null) {
                    throw new NonexistentEntityException("The ser with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws IllegalOrphanException, NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Ser ser;
            try {
                ser = em.getReference(Ser.class, id);
                ser.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The ser with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            Collection<Bairro> bairroCollectionOrphanCheck = ser.getBairroCollection();
            for (Bairro bairroCollectionOrphanCheckBairro : bairroCollectionOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Ser (" + ser + ") cannot be destroyed since the Bairro " + bairroCollectionOrphanCheckBairro + " in its bairroCollection field has a non-nullable idSer field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            em.remove(ser);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Ser> findSerEntities() {
        return findSerEntities(true, -1, -1);
    }

    public List<Ser> findSerEntities(int maxResults, int firstResult) {
        return findSerEntities(false, maxResults, firstResult);
    }

    private List<Ser> findSerEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Ser.class));
            Query q = em.createQuery(cq);
            if (!all) {
                q.setMaxResults(maxResults);
                q.setFirstResult(firstResult);
            }
            return q.getResultList();
        } finally {
            em.close();
        }
    }

    public Ser findSer(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Ser.class, id);
        } finally {
            em.close();
        }
    }

    public int getSerCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Ser> rt = cq.from(Ser.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
