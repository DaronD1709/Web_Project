package com.ecommerce.util;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Ve 1 trang Admin: Servlet chi chon "noi dung" (vd admin-product-list.jsp), con khung (header + sidebar) do admin/admin-layout.jsp lo.
 *  - Mo trang truc tiep (go URL, F5)        -> tra CA KHUNG + noi dung.
 *  - Bam link/form qua htmx (hx-boost)       -> chi tra NOI DUNG (+ sidebar/tieu de cap nhat bang hx-swap-oob).
 * Nho do chuyen trang khong nhay, ma moi trang van chi la 1 file JSP chua noi dung.
 */
public class AdminView {

    private static final String FLASH = "flash";

    /** contentJsp: ten file trong WEB-INF/views/admin/ ; active: muc sidebar duoc to sang (products, orders, ...). */
    public static void render(HttpServletRequest req, HttpServletResponse resp, String contentJsp, String title, String active)
            throws ServletException, IOException {
        // Khi bam Back, htmx xin lai trang bang request co header nay va can TRON TRANG chu khong phai manh.
        boolean historyRestore = "true".equals(req.getHeader("HX-History-Restore-Request"));
        boolean partial = HtmxUtil.isHtmx(req) && !historyRestore;

        req.setAttribute("adminContent", contentJsp);
        req.setAttribute("adminTitle", title);
        req.setAttribute("adminActive", active);
        req.setAttribute("partial", partial);

        String[] flash = takeFlash(req);
        if (flash != null) {
            if (partial) HtmxUtil.toast(resp, flash[0], flash[1]); // manh HTML: bao toast qua header
            else req.setAttribute("flash", flash);                  // ca trang: layout in ra de JS hien toast
        }

        resp.addHeader("Vary", "HX-Request"); // cung 1 URL tra 2 kieu noi dung -> cache khong duoc lan lon
        req.getRequestDispatcher("/WEB-INF/views/admin/admin-layout.jsp").forward(req, resp);
    }

    /** Nho 1 thong bao de hien o trang KE TIEP (sau sendRedirect, request moi nen attribute cua request cu da mat). */
    public static void flash(HttpServletRequest req, String message, String type) {
        req.getSession().setAttribute(FLASH, new String[]{message, type});
    }

    private static String[] takeFlash(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        if (s == null) return null;
        String[] f = (String[]) s.getAttribute(FLASH);
        s.removeAttribute(FLASH);
        return f;
    }
}
