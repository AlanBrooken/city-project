package org.cityProject.dao;

import org.cityProject.domain.City;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;

public class CityDAO {
    private final SessionFactory sessionFactory;

    public CityDAO(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public List<City> getItems(int offset, int limit) {
        try (Session session = sessionFactory.getCurrentSession()) {
            Query<City> query = session.createQuery("select c from City c", City.class);
            query.setFirstResult(offset);
            query.setMaxResults(limit);
            return query.getResultList();
        }
    }

    public int getTotal() {
        try(Session session = sessionFactory.getCurrentSession()) {
            Query<Integer> query = session.createQuery("select count(c) from City c", Integer.class);
            return query.getSingleResult();
        }
    }
}
