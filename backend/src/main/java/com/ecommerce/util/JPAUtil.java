package com.ecommerce.util;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Tuong duong DBUtil trong slide, nhung dung namespace jakarta.persistence
 * (bat buoc vi Tomcat 10.1) thay vi javax.persistence.
 * Ten "ecommercePU" phai khop voi persistence-unit name trong persistence.xml.
 */
public class JPAUtil {
    private static final EntityManagerFactory emf =
            Persistence.createEntityManagerFactory("ecommercePU");

    public static EntityManagerFactory getEmFactory() {
        return emf;
    }
}
