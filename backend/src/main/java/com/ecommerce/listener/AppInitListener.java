package com.ecommerce.listener;

import com.ecommerce.service.EmailService;
import com.ecommerce.util.DataSeeder;
import com.ecommerce.util.DemoDataSeeder;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/** Chay khi Tomcat khoi dong / dung app. */
@WebListener
public class AppInitListener implements ServletContextListener {

    // Khoi dong: nap du lieu mau neu DB con trong.
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // So phien ban gan vao link CSS/JS (head.jspf, admin-layout.jsp: design.css?v=...) va doi MOI LAN khoi dong app:
        // trinh duyet khong dung ban CSS/JS cu trong cache sau khi pull code hoac deploy lai.
        sce.getServletContext().setAttribute("assetVersion", System.currentTimeMillis());
        DataSeeder.seedIfEmpty();
        DemoDataSeeder.seedIfEmpty(); // don hang/khach mau de demo trang Admin (chi khi chua co don nao)
    }

    // Dung app: doi gui not cac email dang xep hang.
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        EmailService.shutdown();
    }
}
