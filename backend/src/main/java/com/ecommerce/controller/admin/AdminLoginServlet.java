package com.ecommerce.controller.admin;

import com.ecommerce.entity.Admin;
import com.ecommerce.entity.User;
import com.ecommerce.service.AuthService;
import com.ecommerce.service.BusinessException;
import com.ecommerce.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

// Dang nhap quan tri (Admin). Dung chung bang users va AuthService.login voi khach hang, chi them buoc KIEM TRA LA ADMIN.
//   GET  /admin/login   hien form (da dang nhap Admin roi thi chuyen thang vao CMS)
//   POST /admin/login   email + password (+ next): dung va la Admin -> luu session, redirect; sai -> ve lai form kem loi
// Dang xuat: AdminLogoutServlet (/admin/logout).
@WebServlet("/admin/login")
public class AdminLoginServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/admin/admin-login.jsp";
    private static final String DEFAULT_AFTER_LOGIN = "/admin"; // dang nhap xong vao trang Tong quan

    private final AuthService authService = new AuthService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String next = req.getParameter("next");
        if (SessionUtil.currentUser(req) instanceof Admin) { // da dang nhap Admin roi thi khong hien form nua
            resp.sendRedirect(req.getContextPath() + safeNext(next));
            return;
        }
        req.setAttribute("next", next);
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String email = req.getParameter("email");
        String next = req.getParameter("next");
        try {
            // Buoc 1: kiem tra email + mat khau (cung ham voi khach hang). Sai thi AuthService nem BusinessException.
            User user = authService.login(email, req.getParameter("password"));
            // Buoc 2: dung mat khau nhung la KHACH HANG thi khong cho vao CMS. Chi nguoi biet dung mat khau moi thay thong bao nay.
            if (!(user instanceof Admin)) {
                throw new BusinessException("Tài khoản này không có quyền quản trị.");
            }
            // Dang nhap thanh cong: huy session cu (neu co, vd dang la khach hang) va tao session MOI -> doi ma session,
            // chong session fixation va khong de lai du lieu cua phien truoc (vd so luong gio hang).
            HttpSession old = req.getSession(false);
            if (old != null) old.invalidate();
            req.getSession(true).setAttribute(SessionUtil.CURRENT_USER, user);

            resp.sendRedirect(req.getContextPath() + safeNext(next));
        } catch (BusinessException e) {
            // Ve lai form, giu email da go (khong giu mat khau), nho ca "next" de lan thu sau van quay lai dung trang
            req.setAttribute("error", e.getMessage());
            req.setAttribute("email", email);
            req.setAttribute("next", next);
            req.getRequestDispatcher(VIEW).forward(req, resp);
        }
    }

    // Chi cho quay lai trang NOI BO cua CMS (bat dau bang "/admin"); chan "//evil.com", "https://evil.com" (open redirect).
    /*
     * Loc tham so "next" truoc khi redirect sau dang nhap. Tra ve chinh next neu an toan, nguoc lai tra ve DEFAULT_AFTER_LOGIN ("/admin").
     *
     * VI SAO CAN: "next" nam tren URL (/admin/login?next=...) nen AI CUNG SUA DUOC. Neu cu redirect theo no, ke xau gui Admin duong link
     * /admin/login?next=https://trang-gia-mao.com ; Admin dang nhap THAT xong bi dua sang trang gia (de lua nhap lai mat khau).
     * Day la loi "open redirect". Cach phong: chi nhan duong dan NOI BO cua khu quan tri.
     *
     * Dung o: doGet (da dang nhap Admin roi thi bo qua form) va doPost (dang nhap thanh cong). Luon ghep: req.getContextPath() + safeNext(next).
     * Luu y: ket qua duoc NOI sau contextPath, nen "/admin/orders" -> "/ecommerce/admin/orders" (hoac "/admin/orders" khi app chay o goc,
     * nhu khi deploy bang Docker). Chinh khi chay o goc moi nguy hiem: contextPath rong nen "//evil.com" tro thanh dia chi tuyet doi.
     *
     * 4 dieu kien (phai dung CA BON):
     *   next != null               co tham so (khong co thi dung mac dinh)
     *   startsWith("/admin")       chi vao khu quan tri cua chinh site. Loai "https://evil.com", "/login", "/orders"...
     *   !startsWith("//")          loai "//evil.com": khong co "http:" nhung trinh duyet hieu la sang ten mien khac (protocol-relative URL)
     *   !contains("\\")            loai dau gach cheo nguoc: nhieu trinh duyet coi "\" nhu "/" nen "/\evil.com" thanh "//evil.com"
     *
     *   VI DU                          KET QUA
     *   "/admin/orders?status=PENDING" giu nguyen (hop le, kem ca query)
     *   "/admin"                       giu nguyen
     *   null  /  ""                    "/admin"
     *   "https://evil.com"             "/admin"  (khong bat dau bang /admin)
     *   "//evil.com"                   "/admin"
     *   "/orders"                      "/admin"  (la trang cua khach, khong phai khu quan tri)
     *   "/admin\\evil"                 "/admin"  (co dau gach cheo nguoc)
     * (Khong thay the viec Admin phai dang nhap: day chi la loc DICH DEN sau khi da dang nhap xong.)
     */
    private static String safeNext(String next) {
        boolean ok = next != null && next.startsWith("/admin") && !next.startsWith("//") && !next.contains("\\");
        return ok ? next : DEFAULT_AFTER_LOGIN;
    }
}
