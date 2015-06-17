/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.loja.jpa.controller;

import br.com.loja.entities.Bairro;
import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import br.com.loja.entities.Ser;
import br.com.loja.entities.Cliente;
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
public class BairroJpaController implements Serializable {

    public BairroJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Bairro bairro) {
        if (bairro.getClienteCollection() == null) {
            bairro.setClienteCollection(new ArrayList<Cliente>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Ser idSer = bairro.getIdSer();
            if (idSer != null) {
                idSer = em.getReference(idSer.getClass(), idSer.getId());
                bairro.setIdSer(idSer);
            }
            Collection<Cliente> attachedClienteCollection = new ArrayList<Cliente>();
            for (Cliente clienteCollectionClienteToAttach : bairro.getClienteCollection()) {
                clienteCollectionClienteToAttach = em.getReference(clienteCollectionClienteToAttach.getClass(), clienteCollectionClienteToAttach.getId());
                attachedClienteCollection.add(clienteCollectionClienteToAttach);
            }
            bairro.setClienteCollection(attachedClienteCollection);
            em.persist(bairro);
            if (idSer != null) {
                idSer.getBairroCollection().add(bairro);
                idSer = em.merge(idSer);
            }
            for (Cliente clienteCollectionCliente : bairro.getClienteCollection()) {
                Bairro oldIdBairroOfClienteCollectionCliente = clienteCollectionCliente.getIdBairro();
                clienteCollectionCliente.setIdBairro(bairro);
                clienteCollectionCliente = em.merge(clienteCollectionCliente);
                if (oldIdBairroOfClienteCollectionCliente != null) {
                    oldIdBairroOfClienteCollectionCliente.getClienteCollection().remove(clienteCollectionCliente);
                    oldIdBairroOfClienteCollectionCliente = em.merge(oldIdBairroOfClienteCollectionCliente);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Bairro bairro) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Bairro persistentBairro = em.find(Bairro.class, bairro.getId());
            Ser idSerOld = persistentBairro.getIdSer();
            Ser idSerNew = bairro.getIdSer();
            Collection<Cliente> clienteCollectionOld = persistentBairro.getClienteCollection();
            Collection<Cliente> clienteCollectionNew = bairro.getClienteCollection();
            List<String> illegalOrphanMessages = null;
            for (Cliente clienteCollectionOldCliente : clienteCollectionOld) {
                if (!clienteCollectionNew.contains(clienteCollectionOldCliente)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Cliente " + clienteCollectionOldCliente + " since its idBairro field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            if (idSerNew != null) {
                idSerNew = em.getReference(idSerNew.getClass(), idSerNew.getId());
                bairro.setIdSer(idSerNew);
            }
            Collection<Cliente> attachedClienteCollectionNew = new ArrayList<Cliente>();
            for (Cliente clienteCollectionNewClienteToAttach : clienteCollectionNew) {
                clienteCollectionNewClienteToAttach = em.getReference(clienteCollectionNewClienteToAttach.getClass(), clienteCollectionNewClienteToAttach.getId());
                attachedClienteCollectionNew.add(clienteCollectionNewClienteToAttach);
            }
            clienteCollectionNew = attachedClienteCollectionNew;
            bairro.setClienteCollection(clienteCollectionNew);
            bairro = em.merge(bairro);
            if (idSerOld != null && !idSerOld.equals(idSerNew)) {
                idSerOld.getBairroCollection().remove(bairro);
                idSerOld = em.merge(idSerOld);
            }
            if (idSerNew != null && !idSerNew.equals(idSerOld)) {
                idSerNew.getBairroCollection().add(bairro);
                idSerNew = em.merge(idSerNew);
            }
            for (Cliente clienteCollectionNewCliente : clienteCollectionNew) {
                if (!clienteCollectionOld.contains(clienteCollectionNewCliente)) {
                    Bairro oldIdBairroOfClienteCollectionNewCliente = clienteCollectionNewCliente.getIdBairro();
                    clienteCollectionNewCliente.setIdBairro(bairro);
                    clienteCollectionNewCliente = em.merge(clienteCollectionNewCliente);
                    if (oldIdBairroOfClienteCollectionNewCliente != null && !oldIdBairroOfClienteCollectionNewCliente.equals(bairro)) {
                        oldIdBairroOfClienteCollectionNewCliente.getClienteCollection().remove(clienteCollectionNewCliente);
                        oldIdBairroOfClienteCollectionNewCliente = em.merge(oldIdBairroOfClienteCollectionNewCliente);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = bairro.getId();
                if (findBairro(id) == null) {
                    throw new NonexistentEntityException("The bairro with id " + id + " no longer exists.");
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
            Bairro bairro;
            try {
                bairro = em.getReference(Bairro.class, id);
                bairro.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The bairro with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            Collection<Cliente> clienteCollectionOrphanCheck = bairro.getClienteCollection();
            for (Cliente clienteCollectionOrphanCheckCliente : clienteCollectionOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Bairro (" + bairro + ") cannot be destroyed since the Cliente " + clienteCollectionOrphanCheckCliente + " in its clienteCollection field has a non-nullable idBairro field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            Ser idSer = bairro.getIdSer();
            if (idSer != null) {
                idSer.getBairroCollection().remove(bairro);
                idSer = em.merge(idSer);
            }
            em.remove(bairro);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Bairro> findBairroEntities() {
        return findBairroEntities(true, -1, -1);
    }

    public List<Bairro> findBairroEntities(int maxResults, int firstResult) {
        return findBairroEntities(false, maxResults, firstResult);
    }

    private List<Bairro> findBairroEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Bairro.class));
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

    public Bairro findBairro(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Bairro.class, id);
        } finally {
            em.close();
        }
    }

    public int getBairroCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Bairro> rt = cq.from(Bairro.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
