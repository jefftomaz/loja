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
import br.com.loja.entities.Cliente;
import br.com.loja.entities.Vendas;
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
public class ClienteJpaController implements Serializable {

    public ClienteJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Cliente cliente) {
        if (cliente.getVendasCollection() == null) {
            cliente.setVendasCollection(new ArrayList<Vendas>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Bairro idBairro = cliente.getIdBairro();
            if (idBairro != null) {
                idBairro = em.getReference(idBairro.getClass(), idBairro.getId());
                cliente.setIdBairro(idBairro);
            }
            Collection<Vendas> attachedVendasCollection = new ArrayList<Vendas>();
            for (Vendas vendasCollectionVendasToAttach : cliente.getVendasCollection()) {
                vendasCollectionVendasToAttach = em.getReference(vendasCollectionVendasToAttach.getClass(), vendasCollectionVendasToAttach.getId());
                attachedVendasCollection.add(vendasCollectionVendasToAttach);
            }
            cliente.setVendasCollection(attachedVendasCollection);
            em.persist(cliente);
            if (idBairro != null) {
                idBairro.getClienteCollection().add(cliente);
                idBairro = em.merge(idBairro);
            }
            for (Vendas vendasCollectionVendas : cliente.getVendasCollection()) {
                Cliente oldIdClienteOfVendasCollectionVendas = vendasCollectionVendas.getIdCliente();
                vendasCollectionVendas.setIdCliente(cliente);
                vendasCollectionVendas = em.merge(vendasCollectionVendas);
                if (oldIdClienteOfVendasCollectionVendas != null) {
                    oldIdClienteOfVendasCollectionVendas.getVendasCollection().remove(vendasCollectionVendas);
                    oldIdClienteOfVendasCollectionVendas = em.merge(oldIdClienteOfVendasCollectionVendas);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Cliente cliente) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Cliente persistentCliente = em.find(Cliente.class, cliente.getId());
            Bairro idBairroOld = persistentCliente.getIdBairro();
            Bairro idBairroNew = cliente.getIdBairro();
            Collection<Vendas> vendasCollectionOld = persistentCliente.getVendasCollection();
            Collection<Vendas> vendasCollectionNew = cliente.getVendasCollection();
            List<String> illegalOrphanMessages = null;
            for (Vendas vendasCollectionOldVendas : vendasCollectionOld) {
                if (!vendasCollectionNew.contains(vendasCollectionOldVendas)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Vendas " + vendasCollectionOldVendas + " since its idCliente field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            if (idBairroNew != null) {
                idBairroNew = em.getReference(idBairroNew.getClass(), idBairroNew.getId());
                cliente.setIdBairro(idBairroNew);
            }
            Collection<Vendas> attachedVendasCollectionNew = new ArrayList<Vendas>();
            for (Vendas vendasCollectionNewVendasToAttach : vendasCollectionNew) {
                vendasCollectionNewVendasToAttach = em.getReference(vendasCollectionNewVendasToAttach.getClass(), vendasCollectionNewVendasToAttach.getId());
                attachedVendasCollectionNew.add(vendasCollectionNewVendasToAttach);
            }
            vendasCollectionNew = attachedVendasCollectionNew;
            cliente.setVendasCollection(vendasCollectionNew);
            cliente = em.merge(cliente);
            if (idBairroOld != null && !idBairroOld.equals(idBairroNew)) {
                idBairroOld.getClienteCollection().remove(cliente);
                idBairroOld = em.merge(idBairroOld);
            }
            if (idBairroNew != null && !idBairroNew.equals(idBairroOld)) {
                idBairroNew.getClienteCollection().add(cliente);
                idBairroNew = em.merge(idBairroNew);
            }
            for (Vendas vendasCollectionNewVendas : vendasCollectionNew) {
                if (!vendasCollectionOld.contains(vendasCollectionNewVendas)) {
                    Cliente oldIdClienteOfVendasCollectionNewVendas = vendasCollectionNewVendas.getIdCliente();
                    vendasCollectionNewVendas.setIdCliente(cliente);
                    vendasCollectionNewVendas = em.merge(vendasCollectionNewVendas);
                    if (oldIdClienteOfVendasCollectionNewVendas != null && !oldIdClienteOfVendasCollectionNewVendas.equals(cliente)) {
                        oldIdClienteOfVendasCollectionNewVendas.getVendasCollection().remove(vendasCollectionNewVendas);
                        oldIdClienteOfVendasCollectionNewVendas = em.merge(oldIdClienteOfVendasCollectionNewVendas);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = cliente.getId();
                if (findCliente(id) == null) {
                    throw new NonexistentEntityException("The cliente with id " + id + " no longer exists.");
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
            Cliente cliente;
            try {
                cliente = em.getReference(Cliente.class, id);
                cliente.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The cliente with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            Collection<Vendas> vendasCollectionOrphanCheck = cliente.getVendasCollection();
            for (Vendas vendasCollectionOrphanCheckVendas : vendasCollectionOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Cliente (" + cliente + ") cannot be destroyed since the Vendas " + vendasCollectionOrphanCheckVendas + " in its vendasCollection field has a non-nullable idCliente field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            Bairro idBairro = cliente.getIdBairro();
            if (idBairro != null) {
                idBairro.getClienteCollection().remove(cliente);
                idBairro = em.merge(idBairro);
            }
            em.remove(cliente);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Cliente> findClienteEntities() {
        return findClienteEntities(true, -1, -1);
    }

    public List<Cliente> findClienteEntities(int maxResults, int firstResult) {
        return findClienteEntities(false, maxResults, firstResult);
    }

    private List<Cliente> findClienteEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Cliente.class));
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

    public Cliente findCliente(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Cliente.class, id);
        } finally {
            em.close();
        }
    }

    public int getClienteCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Cliente> rt = cq.from(Cliente.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
