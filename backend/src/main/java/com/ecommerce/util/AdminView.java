package com.ecommerce.util;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * Ve 1 trang Admin: Servlet chi chon "noi dung" (vd admin-product-list.jsp), con khung (header + sidebar) do admin/admin-layout.jsp lo.
 *  - Mo trang truc tiep (go URL, F5, Back) -> tra CA KHUNG + noi dung.
 *  - Bam link/form qua htmx (hx-boost)      -> chi tra NOI DUNG (+ sidebar/tieu de cap nhat bang hx-swap-oob).
 * Nho do chuyen trang khong nhay. Servlet chi can 1 dong: AdminView.render(req, resp, "admin-product-list.jsp", "San pham", "products");
 */
public class AdminView {

    private static final String FLASH = "flash";

    public static void render(HttpServletRequest req, HttpServletResponse resp, String contentJsp, String title, String active)
            throws ServletException, IOException {

        // (1) Tra ca trang hay chi noi dung? Bam Back thi htmx xin lai trang kem header nay -> phai tra TRON TRANG.
        boolean historyRestore = "true".equals(req.getHeader("HX-History-Restore-Request"));
        boolean partial = HtmxUtil.isHtmx(req) && !historyRestore; // true = chi tra noi dung

        // (2) Dua thong tin cho admin-layout.jsp (doc bang ${adminContent}, ${partial}, ...)
        req.setAttribute("adminContent", contentJsp);
        req.setAttribute("adminTitle", title);
        req.setAttribute("adminActive", active);
        req.setAttribute("partial", partial);

        // (3) Co thong bao "flash" dang cho thi hien toast (flash[0] = noi dung, flash[1] = success | error)
        String[] flash = takeFlash(req);
        if (flash != null) {
            if (partial) HtmxUtil.toast(resp, flash[0], flash[1]); // manh HTML: bao qua header, htmx + admin.js hien toast
            else req.setAttribute("flash", flash);                  // ca trang: layout in ra the #flash, admin.js doc va hien toast
        }

        // (4) Cung 1 URL tra 2 kieu noi dung -> bao cache luu rieng, roi chuyen tay sang layout ve
        resp.addHeader("Vary", "HX-Request");
        req.getRequestDispatcher("/WEB-INF/views/admin/admin-layout.jsp").forward(req, resp);
    }

    /** Cat 1 thong bao de hien o trang KE TIEP: goi truoc sendRedirect (redirect la request moi nen setAttribute cu bi mat, phai dung session). */
    public static void flash(HttpServletRequest req, String message, String type) {
        req.getSession().setAttribute(FLASH, new String[]{message, type});
    }

    /** Lay thong bao ra (neu co) va xoa luon -> chi hien 1 lan, F5 khong hien lai. */
    private static String[] takeFlash(HttpServletRequest req) {
        HttpSession s = req.getSession(false); // chi lay session dang co, khong tao moi
        if (s == null) return null;
        String[] f = (String[]) s.getAttribute(FLASH);
        s.removeAttribute(FLASH);
        return f;
    }
}
