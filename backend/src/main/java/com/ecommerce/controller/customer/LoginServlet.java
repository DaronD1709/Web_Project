package com.ecommerce.controller.customer;

import com.ecommerce.entity.User;
import com.ecommerce.service.AuthService;
import com.ecommerce.entity.Customer;
import com.ecommerce.service.BusinessException;
import com.ecommerce.service.CartService;
import com.ecommerce.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

// GET /login -> form;  POST /login -> kiem tra mat khau, luu "currentUser" vao session
@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final String VIEW = "/WEB-INF/views/customer/login.jsp";
    private final AuthService authService = new AuthService();
    private final CartService cartService = new CartService();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setHeader("Cache-Control", "no-store");
        req.setAttribute("next", req.getParameter("next"));
        req.getRequestDispatcher(VIEW).forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");
        String email = req.getParameter("email");
        String next = req.getParameter("next");
        boolean remember = "on".equals(req.getParameter("remember"));
        try {
            User user = authService.login(email, req.getParameter("password"));
            // Doc du lieu can thiet truoc khi tao phien dang nhap; loi DB khong de lai phien da xac thuc mot phan.
            long cartCount = user instanceof Customer ? cartService.countItems(user.getId()) : 0;
            HttpSession previous = req.getSession(false);
            if (previous != null) previous.invalidate();
            HttpSession session = req.getSession(true); // phien moi: doi ID va bo du lieu cua tai khoan truoc
            session.setAttribute(SessionUtil.CURRENT_USER, user);
            if (user instanceof Customer) { // badge gio hang tren header
                session.setAttribute(SessionUtil.CART_COUNT, cartCount);
            }
            if (remember) session.setMaxInactiveInterval(SessionUtil.REMEMBER_SECONDS);
            SessionUtil.writeSessionCookie(req, resp, session.getId(), remember ? SessionUtil.REMEMBER_SECONDS : -1);
            resp.sendRedirect(req.getContextPath() + (isSafeInternalPath(next) ? next : "/"));
        } catch (BusinessException e) {
            req.setAttribute("error", e.getMessage());
            req.setAttribute("email", email);
            req.setAttribute("next", next);
            req.setAttribute("remember", remember);
            req.getRequestDispatcher(VIEW).forward(req, resp);
        }
    }

    // Chi cho chuyen huong toi duong dan NOI BO (vd "/orders"); chan "//evil.com", "https://evil.com" (open redirect).
    /*
     * Kiem tra tham so "next" (trang khach dinh vao truoc khi bi chuyen toi /login) co phai DUONG DAN NOI BO an toan khong.
     * Dung o doPost: resp.sendRedirect(contextPath + (isSafeInternalPath(next) ? next : "/")) -> dung thi quay lai next, sai thi ve trang chu.
     *
     * VI SAO CAN: "next" nam tren URL nen ai cung sua duoc (vd /login?next=https://trang-gia-mao.com). Neu cu redirect theo no thi sau
     * khi khach dang nhap THAT se bi dua sang trang gia ("open redirect"). Phong bang cach chi nhan duong dan cua chinh site nay.
     * Khac AdminLoginServlet.safeNext: o day cho moi duong dan noi bo (khach co the quay lai /orders, /cart, /product?id=...),
     * con ben Admin chi cho duong dan bat dau bang /admin.
     *
     * Tra ve true khi THOA CA nam dieu kien:
     *   next != null                       co tham so
     *   startsWith("/")                    la duong dan tuyet doi trong site (loai "https://evil.com", "evil.com", "javascript:...")
     *   !startsWith("//")                  loai "//evil.com" (protocol-relative URL: trinh duyet hieu la sang ten mien khac;
     *                                      dac biet nguy hiem khi app chay o goc vi contextPath rong, "" + "//evil.com" = "//evil.com")
     *   !contains("\\")                    loai dau gach cheo nguoc ("/\evil.com": nhieu trinh duyet coi "\" nhu "/")
     *   noneMatch(isISOControl)            loai ky tu dieu khien (xuong dong, tab, NUL...). Trinh duyet bo qua tab/xuong dong trong URL nen
     *                                      "/<tab>/evil.com" bi doc thanh "//evil.com"; ky tu xuong dong con co the dung de chen header gia
     *                                      vao phan hoi ("HTTP response splitting")
     *
     *   VI DU                              KET QUA
     *   "/orders"  ,  "/product?id=7"      true  (quay lai dung trang)
     *   "/orders/detail?id=12"             true  (giu ca query)
     *   null  ,  ""                        false (ve trang chu)
     *   "https://evil.com"                 false
     *   "//evil.com"                       false
     *   "/\\evil.com"                      false
     *   "/orders\\nSet-Cookie: x=1"        false (co ky tu xuong dong)
     * (Day chi loc DICH DEN sau dang nhap; viec quyet dinh trang nao can dang nhap nam o SessionUtil.requireCustomer.)
     */
    private static boolean isSafeInternalPath(String next) {
        return next != null && next.startsWith("/") && !next.startsWith("//") && !next.contains("\\")
                && next.chars().noneMatch(Character::isISOControl);
    }
}
