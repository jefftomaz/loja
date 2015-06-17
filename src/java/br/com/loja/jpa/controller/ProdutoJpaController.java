/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.loja.jpa.controller;

import br.com.loja.entities.Produto;
import java.io.Serializable;
import javax.persistence.Query;
import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
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
public class ProdutoJpaController implements Serializable {

    public ProdutoJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Produto produto) {
        if (produto.getVendasCollection() == null) {
            produto.setVendasCollection(new ArrayList<Vendas>());
        }
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Collection<Vendas> attachedVendasCollection = new ArrayList<Vendas>();
            for (Vendas vendasCollectionVendasToAttach : produto.getVendasCollection()) {
                vendasCollectionVendasToAttach = em.getReference(vendasCollectionVendasToAttach.getClass(), vendasCollectionVendasToAttach.getId());
                attachedVendasCollection.add(vendasCollectionVendasToAttach);
            }
            produto.setVendasCollection(attachedVendasCollection);
            em.persist(produto);
            for (Vendas vendasCollectionVendas : produto.getVendasCollection()) {
                Produto oldIdProdutoOfVendasCollectionVendas = vendasCollectionVendas.getIdProduto();
                vendasCollectionVendas.setIdProduto(produto);
                vendasCollectionVendas = em.merge(vendasCollectionVendas);
                if (oldIdProdutoOfVendasCollectionVendas != null) {
                    oldIdProdutoOfVendasCollectionVendas.getVendasCollection().remove(vendasCollectionVendas);
                    oldIdProdutoOfVendasCollectionVendas = em.merge(oldIdProdutoOfVendasCollectionVendas);
                }
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Produto produto) throws IllegalOrphanException, NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Produto persistentProduto = em.find(Produto.class, produto.getId());
            Collection<Vendas> vendasCollectionOld = persistentProduto.getVendasCollection();
            Collection<Vendas> vendasCollectionNew = produto.getVendasCollection();
            List<String> illegalOrphanMessages = null;
            for (Vendas vendasCollectionOldVendas : vendasCollectionOld) {
                if (!vendasCollectionNew.contains(vendasCollectionOldVendas)) {
                    if (illegalOrphanMessages == null) {
                        illegalOrphanMessages = new ArrayList<String>();
                    }
                    illegalOrphanMessages.add("You must retain Vendas " + vendasCollectionOldVendas + " since its idProduto field is not nullable.");
                }
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            Collection<Vendas> attachedVendasCollectionNew = new ArrayList<Vendas>();
            for (Vendas vendasCollectionNewVendasToAttach : vendasCollectionNew) {
                vendasCollectionNewVendasToAttach = em.getReference(vendasCollectionNewVendasToAttach.getClass(), vendasCollectionNewVendasToAttach.getId());
                attachedVendasCollectionNew.add(vendasCollectionNewVendasToAttach);
            }
            vendasCollectionNew = attachedVendasCollectionNew;
            produto.setVendasCollection(vendasCollectionNew);
            produto = em.merge(produto);
            for (Vendas vendasCollectionNewVendas : vendasCollectionNew) {
                if (!vendasCollectionOld.contains(vendasCollectionNewVendas)) {
                    Produto oldIdProdutoOfVendasCollectionNewVendas = vendasCollectionNewVendas.getIdProduto();
                    vendasCollectionNewVendas.setIdProduto(produto);
                    vendasCollectionNewVendas = em.merge(vendasCollectionNewVendas);
                    if (oldIdProdutoOfVendasCollectionNewVendas != null && !oldIdProdutoOfVendasCollectionNewVendas.equals(produto)) {
                        oldIdProdutoOfVendasCollectionNewVendas.getVendasCollection().remove(vendasCollectionNewVendas);
                        oldIdProdutoOfVendasCollectionNewVendas = em.merge(oldIdProdutoOfVendasCollectionNewVendas);
                    }
                }
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = produto.getId();
                if (findProduto(id) == null) {
                    throw new NonexistentEntityException("The produto with id " + id + " no longer exists.");
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
            Produto produto;
            try {
                produto = em.getReference(Produto.class, id);
                produto.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The produto with id " + id + " no longer exists.", enfe);
            }
            List<String> illegalOrphanMessages = null;
            Collection<Vendas> vendasCollectionOrphanCheck = produto.getVendasCollection();
            for (Vendas vendasCollectionOrphanCheckVendas : vendasCollectionOrphanCheck) {
                if (illegalOrphanMessages == null) {
                    illegalOrphanMessages = new ArrayList<String>();
                }
                illegalOrphanMessages.add("This Produto (" + produto + ") cannot be destroyed since the Vendas " + vendasCollectionOrphanCheckVendas + " in its vendasCollection field has a non-nullable idProduto field.");
            }
            if (illegalOrphanMessages != null) {
                throw new IllegalOrphanException(illegalOrphanMessages);
            }
            em.remove(produto);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Produto> findProdutoEntities() {
        return findProdutoEntities(true, -1, -1);
    }

    public List<Produto> findProdutoEntities(int maxResults, int firstResult) {
        return findProdutoEntities(false, maxResults, firstResult);
    }

    private List<Produto> findProdutoEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Produto.class));
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

    public Produto findProduto(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Produto.class, id);
        } finally {
            em.close();
        }
    }

    public int getProdutoCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Produto> rt = cq.from(Produto.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
