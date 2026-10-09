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
    // static final: tao DUNG 1 LAN khi class duoc nap, ca ung dung dung chung. Tao EntityManagerFactory rat ton kem
    // (doc cau hinh, ket noi DB, dung bang) nen khong tao lai moi request. DAO lay EntityManager tu day moi lan can.
    private static final EntityManagerFactory emf = createFactory();

    public static EntityManagerFactory getEmFactory() {
        return emf;
    }

    private static EntityManagerFactory createFactory() {
        // overrides = cac gia tri ghi de len persistence.xml (neu co db.properties)
        Map<String, String> overrides = new HashMap<>();
        try (InputStream in = JPAUtil.class.getResourceAsStream("/db.properties")) {
            if (in != null) { // khong co db.properties thi dung nguyen cau hinh mac dinh trong persistence.xml
                Properties p = new Properties();
                p.load(in);
                putIfPresent(overrides, "jakarta.persistence.jdbc.url", p.getProperty("db.url"));
                putIfPresent(overrides, "jakarta.persistence.jdbc.user", p.getProperty("db.user"));
                putIfPresent(overrides, "jakarta.persistence.jdbc.password", p.getProperty("db.password"));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Khong doc duoc db.properties", e);
        }
        // "ecommercePU" = ten khoi cau hinh trong persistence.xml (phai khop tung chu)
        return Persistence.createEntityManagerFactory("ecommercePU", overrides);
    }

    // Chi ghi de khi gia tri co that (khong null/rong)
    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value.trim());
    }
}
