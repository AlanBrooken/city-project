package org.cityProject;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import org.cityProject.dao.CityDAO;
import org.cityProject.dao.CountryDAO;
import org.cityProject.domain.City;
import org.cityProject.domain.Country;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

public class Main {
    private final SessionFactory sessionFactory;
    private final RedisClient redisClient;
    private final ObjectMapper objectMapper;
    private final CityDAO cityDAO;
    private final CountryDAO countryDAO;


    public Main() {
        sessionFactory = new Configuration().configure().buildSessionFactory();
        cityDAO = new CityDAO(sessionFactory);
        countryDAO = new CountryDAO(sessionFactory);

        redisClient = prepareRedisClien();
        objectMapper = new ObjectMapper();
    }

    public static void main(String[] args) {
        Main main = new Main();
        List<City> cities = main.fetchAll(main);
        main.shutdown();
    }

    private void shutdown() {
        if (nonNull(sessionFactory)) {
            sessionFactory.close();
        }
        if (nonNull(redisClient)) {
            redisClient.shutdown();
        }
    }
    private List<City> fetchAll(Main main) {
        try(Session session = sessionFactory.getCurrentSession()) {
            List<City> cities = new ArrayList<>();
            session.beginTransaction();

            List<Country> countries = main.countryDAO.getAll();
            int totalCount = cityDAO.getTotal(); //4079
            int step = 500;
            for (int i = 0; i < totalCount; i += step) {
                cities.addAll(main.cityDAO.getItems(i, step));
            }

            session.getTransaction().commit();
            return cities;
        }
    }
}