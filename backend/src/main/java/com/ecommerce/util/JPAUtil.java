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
 *
 * LOP NAY LAM GI (noi ngan gon)
 *  Mot app dung JPA/Hibernate can 1 "nha may" EntityManagerFactory (viet tat EMF) de tao ra cac EntityManager. EntityManager la
 *  "phien lam viec voi DB" (tim, luu, xoa entity, chay JPQL). Cac DAO KHONG tu tao EMF ma xin tu lop nay:
 *      EntityManager em = JPAUtil.getEmFactory().createEntityManager();   // lay 1 phien (re, tao moi moi lan can)
 *      try { ... } finally { em.close(); }                              // dung xong dong lai
 *  EMF thi chi tao DUNG 1 LAN cho ca ung dung (rat ton kem), con EntityManager thi tao/huy lien tuc (rat re).
 *
 * THU TU CAU HINH (cai sau de len cai truoc) - day la chu de chinh cua ham createFactory() ben duoi:
 *      1. src/main/resources/META-INF/persistence.xml   : gia tri MAC DINH (localhost, mat khau mau, hbm2ddl=update, show_sql=true)
 *      2. db.properties (neu co)                        : cau hinh RIENG TUNG MAY dev (khong len GitHub)
 *      3. bien moi truong DB_URL, DB_USER, DB_PASSWORD, DB_SHOW_SQL, DB_HBM2DDL : dung khi DEPLOY (Docker/cloud, khong co file)
 *  Vi du may ban: persistence.xml tro toi localhost, db.properties doi mat khau  -> dung mat khau cua db.properties.
 *  Vi du tren Render: khong co db.properties, dat DB_URL/DB_USER/DB_PASSWORD  -> dung Postgres tren mang.
 */
public class JPAUtil {
    // static final: tao DUNG 1 LAN khi class duoc nap, ca ung dung dung chung. Tao EntityManagerFactory rat ton kem
    // (doc cau hinh, ket noi DB, dung bang) nen khong tao lai moi request. DAO lay EntityManager tu day moi lan can.
    // "Khi class duoc nap" = lan dau tien co code goi JPAUtil (thuc te la luc khoi dong, khi AppInitListener nap du lieu mau).
    // Neu luc do khong ket noi duoc DB (sai mat khau, Postgres chua chay) thi viec khoi tao nay nem loi va app khong len duoc:
    // doc ky log Tomcat, dong "Caused by" cho biet ly do that.
    // (Chua co code dong EMF khi tat app: viec nay con nam trong danh sach TODO cua du an.)
    private static final EntityManagerFactory emf = createFactory();

    /** Lay "nha may" EntityManager dung chung. Moi DAO goi: JPAUtil.getEmFactory().createEntityManager(). */
    public static EntityManagerFactory getEmFactory() {
        return emf;
    }

    /**
     * Dung EntityManagerFactory. Thuc chat la: GOM cac gia tri cau hinh ghi de vao 1 Map, roi nho thu vien Persistence tao EMF
     * tu persistence.xml + Map do. Chay dung 1 lan (xem bien emf o tren). Cac buoc:
     *
     *   Buoc 1: tao Map "overrides" rong. Day la danh sach cac gia tri se DE LEN cau hinh mac dinh trong persistence.xml.
     *           Khoa la ten thuoc tinh chuan cua JPA/Hibernate (jakarta.persistence.jdbc.url = dia chi DB, ...user, ...password;
     *           hibernate.show_sql = in SQL ra log; hibernate.hbm2ddl.auto = cach Hibernate tao/sua bang).
     *   Buoc 2: neu co file db.properties (dat canh persistence.xml, trong thu muc resources) thi doc va bo db.url/db.user/db.password
     *           vao Map. Khong co file thi bo qua, EMF se dung gia tri trong persistence.xml.
     *   Buoc 3: doc bien moi truong (DB_URL, DB_USER, ...) va bo vao Map. Vi buoc nay chay SAU buoc 2 nen bien moi truong
     *           THANG file db.properties neu cung khoa (put sau de len put truoc).
     *   Buoc 4: Persistence.createEntityManagerFactory("ecommercePU", overrides):
     *             - tim persistence-unit ten "ecommercePU" trong persistence.xml (ten phai KHOP TUNG CHU)
     *             - lay cau hinh trong do, roi thay bang cac gia tri trong Map overrides
     *             - ket noi that toi DB, quet cac lop @Entity, tao/cap nhat bang neu hbm2ddl=update
     *             - tra ve EMF san sang dung. Loi o buoc nay (sai mat khau/DB chua chay) -> nem exception, app khong khoi dong.
     */
    private static EntityManagerFactory createFactory() {
        // overrides = cac gia tri ghi de len persistence.xml (neu co db.properties)
        Map<String, String> overrides = new HashMap<>();
        // getResourceAsStream("/db.properties"): doc file ten db.properties o GOC cua classpath (sau khi build, src/main/resources
        // duoc chep vao WEB-INF/classes). Tra ve null neu khong co file. try (...) la "try-with-resources": tu dong dong luong doc file
        // du xay ra loi hay khong, khong can finally.
        try (InputStream in = JPAUtil.class.getResourceAsStream("/db.properties")) {
            if (in != null) { // khong co db.properties thi dung nguyen cau hinh mac dinh trong persistence.xml
                Properties p = new Properties(); // Properties = ban do "khoa=gia tri" doc tu file .properties (dong bat dau # la ghi chu)
                p.load(in);
                // Doi ten khoa: trong file dung ten ngan (db.url) cho de nho, JPA can ten chuan (jakarta.persistence.jdbc.url)
                putIfPresent(overrides, "jakarta.persistence.jdbc.url", p.getProperty("db.url"));
                putIfPresent(overrides, "jakarta.persistence.jdbc.user", p.getProperty("db.user"));
                putIfPresent(overrides, "jakarta.persistence.jdbc.password", p.getProperty("db.password"));
            }
        } catch (IOException e) {
            throw new IllegalStateException("Khong doc duoc db.properties", e);
        }
        // Khi deploy (Docker/cloud) khong co db.properties: cau hinh qua bien moi truong, UU TIEN hon db.properties.
        //   DB_URL (jdbc:postgresql://host:5432/ten_db?sslmode=require), DB_USER, DB_PASSWORD
        //   DB_SHOW_SQL=false (tat in SQL ra log), DB_HBM2DDL=update|validate|none (mac dinh theo persistence.xml)
        // System.getenv("TEN") tra ve null neu may khong dat bien do -> putIfPresent bo qua, khong ghi de gi ca.
        putIfPresent(overrides, "jakarta.persistence.jdbc.url", System.getenv("DB_URL"));
        putIfPresent(overrides, "jakarta.persistence.jdbc.user", System.getenv("DB_USER"));
        putIfPresent(overrides, "jakarta.persistence.jdbc.password", System.getenv("DB_PASSWORD"));
        putIfPresent(overrides, "hibernate.show_sql", System.getenv("DB_SHOW_SQL"));
        putIfPresent(overrides, "hibernate.hbm2ddl.auto", System.getenv("DB_HBM2DDL"));
        // "ecommercePU" = ten khoi cau hinh trong persistence.xml (phai khop tung chu)
        return Persistence.createEntityManagerFactory("ecommercePU", overrides);
    }

    // Chi ghi de khi gia tri co that (khong null/rong)
    // Ly do: dong "db.password=" bo trong, hoac bien moi truong khong duoc dat, KHONG duoc xoa mat khau mac dinh trong persistence.xml.
    // trim() cat khoang trang/xuong dong thua o hai dau (loi hay gap khi copy-paste vao file hay vao trang cau hinh hosting).
    private static void putIfPresent(Map<String, String> map, String key, String value) {
        if (value != null && !value.isBlank()) map.put(key, value.trim());
    }
}
