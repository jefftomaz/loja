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
import br.com.loja.entities.Produto;
import br.com.loja.entities.Cliente;
import br.com.loja.entities.Vendas;
import br.com.loja.jpa.controller.exceptions.NonexistentEntityException;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

/**
 *
 * @author jefferson.tomaz
 */
public class VendasJpaController implements Serializable {

    public VendasJpaController(EntityManagerFactory emf) {
        this.emf = emf;
    }
    private EntityManagerFactory emf = null;

    public EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    public void create(Vendas vendas) {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Produto idProduto = vendas.getIdProduto();
            if (idProduto != null) {
                idProduto = em.getReference(idProduto.getClass(), idProduto.getId());
                vendas.setIdProduto(idProduto);
            }
            Cliente idCliente = vendas.getIdCliente();
            if (idCliente != null) {
                idCliente = em.getReference(idCliente.getClass(), idCliente.getId());
                vendas.setIdCliente(idCliente);
            }
            em.persist(vendas);
            if (idProduto != null) {
                idProduto.getVendasCollection().add(vendas);
                idProduto = em.merge(idProduto);
            }
            if (idCliente != null) {
                idCliente.getVendasCollection().add(vendas);
                idCliente = em.merge(idCliente);
            }
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void edit(Vendas vendas) throws NonexistentEntityException, Exception {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Vendas persistentVendas = em.find(Vendas.class, vendas.getId());
            Produto idProdutoOld = persistentVendas.getIdProduto();
            Produto idProdutoNew = vendas.getIdProduto();
            Cliente idClienteOld = persistentVendas.getIdCliente();
            Cliente idClienteNew = vendas.getIdCliente();
            if (idProdutoNew != null) {
                idProdutoNew = em.getReference(idProdutoNew.getClass(), idProdutoNew.getId());
                vendas.setIdProduto(idProdutoNew);
            }
            if (idClienteNew != null) {
                idClienteNew = em.getReference(idClienteNew.getClass(), idClienteNew.getId());
                vendas.setIdCliente(idClienteNew);
            }
            vendas = em.merge(vendas);
            if (idProdutoOld != null && !idProdutoOld.equals(idProdutoNew)) {
                idProdutoOld.getVendasCollection().remove(vendas);
                idProdutoOld = em.merge(idProdutoOld);
            }
            if (idProdutoNew != null && !idProdutoNew.equals(idProdutoOld)) {
                idProdutoNew.getVendasCollection().add(vendas);
                idProdutoNew = em.merge(idProdutoNew);
            }
            if (idClienteOld != null && !idClienteOld.equals(idClienteNew)) {
                idClienteOld.getVendasCollection().remove(vendas);
                idClienteOld = em.merge(idClienteOld);
            }
            if (idClienteNew != null && !idClienteNew.equals(idClienteOld)) {
                idClienteNew.getVendasCollection().add(vendas);
                idClienteNew = em.merge(idClienteNew);
            }
            em.getTransaction().commit();
        } catch (Exception ex) {
            String msg = ex.getLocalizedMessage();
            if (msg == null || msg.length() == 0) {
                Integer id = vendas.getId();
                if (findVendas(id) == null) {
                    throw new NonexistentEntityException("The vendas with id " + id + " no longer exists.");
                }
            }
            throw ex;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public void destroy(Integer id) throws NonexistentEntityException {
        EntityManager em = null;
        try {
            em = getEntityManager();
            em.getTransaction().begin();
            Vendas vendas;
            try {
                vendas = em.getReference(Vendas.class, id);
                vendas.getId();
            } catch (EntityNotFoundException enfe) {
                throw new NonexistentEntityException("The vendas with id " + id + " no longer exists.", enfe);
            }
            Produto idProduto = vendas.getIdProduto();
            if (idProduto != null) {
                idProduto.getVendasCollection().remove(vendas);
                idProduto = em.merge(idProduto);
            }
            Cliente idCliente = vendas.getIdCliente();
            if (idCliente != null) {
                idCliente.getVendasCollection().remove(vendas);
                idCliente = em.merge(idCliente);
            }
            em.remove(vendas);
            em.getTransaction().commit();
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    public List<Vendas> findVendasEntities() {
        return findVendasEntities(true, -1, -1);
    }

    public List<Vendas> findVendasEntities(int maxResults, int firstResult) {
        return findVendasEntities(false, maxResults, firstResult);
    }

    private List<Vendas> findVendasEntities(boolean all, int maxResults, int firstResult) {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            cq.select(cq.from(Vendas.class));
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

    public Vendas findVendas(Integer id) {
        EntityManager em = getEntityManager();
        try {
            return em.find(Vendas.class, id);
        } finally {
            em.close();
        }
    }

    public int getVendasCount() {
        EntityManager em = getEntityManager();
        try {
            CriteriaQuery cq = em.getCriteriaBuilder().createQuery();
            Root<Vendas> rt = cq.from(Vendas.class);
            cq.select(em.getCriteriaBuilder().count(rt));
            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } finally {
            em.close();
        }
    }
    
}
