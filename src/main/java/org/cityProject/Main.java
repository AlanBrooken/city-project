package org.cityProject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisStringCommands;
import org.cityProject.dao.CityDAO;
import org.cityProject.dao.CountryDAO;
import org.cityProject.domain.City;
import org.cityProject.domain.Country;
import org.cityProject.domain.CountryLanguage;
import org.cityProject.redis.CityCountry;
import org.cityProject.redis.Language;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

        redisClient = prepareRedisClient();
        objectMapper = new ObjectMapper();
    }

    private RedisClient prepareRedisClient() {
        RedisClient redisClient = RedisClient.create(RedisURI.create("localhost", 6379));
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            System.out.println("\nConnected to Redis\n");
        }
        return redisClient;
    }

    public static void main(String[] args) {
        Main main = new Main();
        List<City> cities = main.fetchAll(main);
        List<CityCountry> preparedData = main.transformData(cities);
        main.pushToRedis(preparedData);

        main.sessionFactory.getCurrentSession().close();
        List<Integer> ids = List.of(2, 24, 546, 334, 90, 689, 1000, 1498, 67, 228);

        long startRedis = System.currentTimeMillis();
        main.testRedisData(ids);
        long stopRedis = System.currentTimeMillis();

        long startMySql = System.currentTimeMillis();
        main.testMySQLData(ids);
        long stopMySql = System.currentTimeMillis();

        System.out.printf("%s: \t%d ms\n", "Redis", (stopRedis - startRedis));
        System.out.printf("%s: \t%d ms\n", "MySql", (stopMySql - startMySql));

        main.shutdown();
    }

    private void pushToRedis(List<CityCountry> preparedData) {
        try(StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            RedisStringCommands<String, String> sync = connection.sync();
            for (CityCountry cityCountry : preparedData) {
                try {
                    sync.set(String.valueOf(cityCountry.getId()), objectMapper.writeValueAsString(cityCountry));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void testRedisData(List<Integer> ids) {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()){
            RedisStringCommands<String, String> sync = connection.sync();
            for (Integer id : ids) {
                String value = sync.get(String.valueOf(id));
                try {
                    objectMapper.readValue(value, CityCountry.class);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void testMySQLData(List<Integer> ids) {
        try (Session session = sessionFactory.getCurrentSession()) {
            session.beginTransaction();
            for (Integer id : ids) {
                City city = cityDAO.getById(id);
                Set<CountryLanguage> languages = city.getCountry().getLanguages();
            }
            session.getTransaction().commit();
        }
    }

    private List<CityCountry> transformData(List<City> cities) {
        return cities.stream().map(city -> {
            CityCountry res = new CityCountry();
            res.setId(city.getId());
            res.setName(city.getName());
            res.setPopulation(city.getPopulation());
            res.setDistrict(city.getDistrict());

            Country country = city.getCountry();
            res.setAlternativeCountryCode(country.getAlternativeCode());
            res.setContinent(country.getContinent());
            res.setCountryCode(country.getCode());
            res.setCountryPopulation(country.getPopulation());
            res.setCountryRegion(country.getRegion());
            res.setCountrySurfaceArea(country.getSurfaceArea());
            Set<CountryLanguage> countryLanguageSet = country.getLanguages();
            Set<Language> languages = countryLanguageSet.stream().map(countryLanguage -> {
                Language language = new Language();
                language.setLanguage(countryLanguage.getLanguage());
                language.setOfficial(countryLanguage.getOfficial());
                language.setPercentage(countryLanguage.getPercentage());
                return language;
            }).collect(Collectors.toSet());
            res.setLanguages(languages);
            return res;
        }).collect(Collectors.toList());
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
        try (Session session = sessionFactory.getCurrentSession()) {
            List<City> cities = new ArrayList<>();
            session.beginTransaction();

            List<Country> countries = main.countryDAO.getAll();
            int totalCount = cityDAO.getTotalCount(); //4079
            int step = 500;
            for (int i = 0; i < totalCount; i += step) {
                cities.addAll(main.cityDAO.getItems(i, step));
            }

            session.getTransaction().commit();
            return cities;
        }
    }
}