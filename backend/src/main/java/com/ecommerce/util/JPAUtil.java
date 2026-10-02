package com.ecommerce.util;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Tuong duong DBUtil trong slide, nhung dung namespace jakarta.persistence
 * (bat buoc vi Tomcat 10.1) thay vi javax.persistence.
 * Ten "ecommercePU" phai khop voi persistence-unit name trong persistence.xml.
 *
 * Moi may 1 DB rieng: neu co file src/main/resources/db.properties (KHONG commit, da .gitignore)
 * thi url/user/password trong do se ghi de gia tri mac dinh trong persistence.xml.
 * Mau file: db.properties.example.
 */
public class JPAUtil {
    private static final EntityManagerFactory emf = createFactory();

    public static EntityManagerFactory getEmFactory() {
        return emf;
    }

    private static EntityManagerFactory createFactory() {
        Map<String, String> overrides = new HashMap<>();
        try (InputStream in = JPAUtil.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                putIfPresent(overrides, "jakarta.persistence.jdbc.url", p.getProperty("db.url"));
                putIfPresent(overrides, "jakarta.persistence.jdbc.user", p.getProperty("db.user"));
                putIfPresent(overrides, "jakarta.persistence.jdbc.password", p.getProperty("db.password"));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Khong doc duoc db.properties", e);
        }
        return Persistence.createEntityManagerFactory("ecommercePU", overrides);
    }

    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value.trim());
    }
}
