package com.ecommerce.listener;

import com.ecommerce.service.EmailService;
import com.ecommerce.util.DataSeeder;
import com.ecommerce.util.DemoDataSeeder;
import com.ecommerce.util.JPAUtil;
import jakarta.persistence.EntityManager;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Chay khi Tomcat khoi dong / dung app. */
@WebListener
public class AppInitListener implements ServletContextListener {

    private static final Logger LOG = Logger.getLogger(AppInitListener.class.getName());

    // Luong chay nen "giu ket noi DB song" (xem startDbKeepAlive); can giu tham chieu de tat khi dung app.
    private ScheduledExecutorService dbKeepAlive;

    // Khoi dong: nap du lieu mau neu DB con trong.
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // So phien ban gan vao link CSS/JS (head.jspf, admin-layout.jsp: design.css?v=...) va doi MOI LAN khoi dong app:
        // trinh duyet khong dung ban CSS/JS cu trong cache sau khi pull code hoac deploy lai.
        sce.getServletContext().setAttribute("assetVersion", System.currentTimeMillis());
        DataSeeder.seedIfEmpty();
        DemoDataSeeder.seedIfEmpty(); // don hang/khach mau de demo trang Admin (chi khi chua co don nao)
        startDbKeepAlive();
    }

    /**
     * Giu ket noi toi database "song" bang cach hoi 1 cau rat nhe (SELECT 1) moi vai phut.
     * Vi sao can: database dam may (vd Neon goi mien phi) TU NGU sau ~5 phut khong co truy van nao va dong cac ket noi dang mo.
     * Pool ket noi co san cua Hibernate khong kiem tra ket noi con song hay khong, nen luot truy cap DAU TIEN sau luc ngu
     * lay phai ket noi chet -> loi 500, phai F5 lan nua moi vao duoc. Hoi dinh ky thi database khong bao gio ngu khi app dang chay.
     * Chu ky (phut) doc tu bien moi truong DB_KEEPALIVE_MINUTES, mac dinh 3; dat 0 de tat (vd khi dung DB co dinh, khong tu ngu).
     * Luu y: database luc nao cung thuc thi se ton "gio tinh toan" cua goi mien phi nhieu hon.
     */
    private void startDbKeepAlive() {
        int minutes = 3;
        String env = System.getenv("DB_KEEPALIVE_MINUTES");
        if (env != null && !env.isBlank()) {
            try {
                minutes = Integer.parseInt(env.trim());
            } catch (NumberFormatException e) {
                LOG.warning("DB_KEEPALIVE_MINUTES khong hop le, dung mac dinh 3 phut.");
            }
        }
        if (minutes <= 0) {
            return;
        }
        // Luong daemon: khong chan Tomcat khi tat may.
        dbKeepAlive = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread t = new Thread(task, "db-keepalive");
            t.setDaemon(true);
            return t;
        });
        dbKeepAlive.scheduleWithFixedDelay(AppInitListener::pingDatabase, minutes, minutes, TimeUnit.MINUTES);
        LOG.info("DB keep-alive: hoi database moi " + minutes + " phut.");
    }

    // Thu toi da 3 lan: neu pool dang giu ket noi chet thi moi lan lay ra se bo 1 ket noi hong, lan sau lay ket noi khac/moi.
    private static void pingDatabase() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            EntityManager em = null;
            try {
                em = JPAUtil.getEmFactory().createEntityManager();
                em.createNativeQuery("SELECT 1").getSingleResult();
                return; // thanh cong
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "DB keep-alive lan " + attempt + " that bai: " + e.getMessage());
            } finally {
                if (em != null) {
                    em.close();
                }
            }
        }
    }

    // Dung app: doi gui not cac email dang xep hang.
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (dbKeepAlive != null) {
            dbKeepAlive.shutdownNow();
        }
        EmailService.shutdown();
    }
}
