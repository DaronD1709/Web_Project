package com.ecommerce.util;

import com.ecommerce.dao.UserDAO;
import com.ecommerce.entity.Admin;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.User;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Cua ngo duy nhat de biet "ai dang dang nhap" va "co duoc vao trang nay khong"
 * MOI Servlet can dang nhap deu goi requireCustomer(...) hoac requireAdmin(...) o dong dau tien cua doGet/doPost.
 *
 * Mo hinh dang nhap cua ca app (de tra loi van dap):
 *   1. LoginServlet kiem tra mat khau (AuthService.login) roi tao session MOI va luu doi tuong User vao session
 *      voi khoa CURRENT_USER. (Huy session cu truoc khi tao moi = chong "session fixation": ke tan cong khong the
 *      dat san ma phien cho nan nhan.)
 *   2. Trinh duyet giu ma phien trong cookie JSESSIONID (HttpOnly, SameSite=Lax); moi request sau gui cookie len,
 *      Tomcat tim lai session, ta doc User tu do. Server KHONG luu mat khau o dau ca trong session.
 *   3. Dang xuat = huy session + xoa cookie (LogoutServlet).
 *
 * Luu y: User trong session la BAN CHUP luc dang nhap; Admin doi ten/khoa tai khoan sau do thi ban chup khong tu cap nhat.
 * Vi vay requireCustomer hoi lai DB de biet tai khoan con hoat dong khong (xem ghi chu o ham do).
 */
public class SessionUtil {

    /** Khoa attribute trong session chua doi tuong User (Customer hoac Admin) dang dang nhap. Khong co = chua dang nhap. */
    public static final String CURRENT_USER = "currentUser";
    /** Khoa attribute trong session chua so mon trong gio de header hien badge ma khong phai query moi trang. */
    public static final String CART_COUNT = "cartCount";
    /** "Ghi nho toi": 7 ngay, tinh bang GIAY (7 ngay x 24 gio x 60 phut x 60 giay). Dung cho ca cookie maxAge lan thoi han session. */
    public static final int REMEMBER_SECONDS = 7 * 24 * 60 * 60;

    /**
     * Ghi (de) cookie ma phien thu cong de quyet dinh cookie song bao lau. Mac dinh Tomcat tu tao JSESSIONID la cookie
     * "phien" (mat khi dong trinh duyet), khong cho chon thoi han, nen khi khach tick "ghi nho" ta tu ghi lai cookie cung ten.
     *
     * @param value  ma phien (session.getId()); chuoi rong khi muon xoa cookie
     * @param maxAge -1 = cookie phien (mat khi dong trinh duyet); REMEMBER_SECONDS = 7 ngay; 0 = bao trinh duyet XOA cookie
     *
     * Cac thuoc tinh bao mat:
     *  - HttpOnly: JavaScript khong doc duoc cookie -> XSS khong an cap duoc ma phien.
     *  - Secure: chi gui qua HTTPS, bat khi request dang la HTTPS (sau reverse proxy can RemoteIpValve de req.isSecure() dung).
     *  - SameSite=Lax: trinh duyet khong gui cookie kem cac request POST tu trang web khac -> giam nguy co CSRF.
     * Name/path/domain lay tu cau hinh cookie cua container de CUNG KHOP cookie Tomcat da tao (khac thuoc tinh = tao cookie moi
     * thay vi ghi de / xoa duoc cookie cu).
     */
    public static void writeSessionCookie(HttpServletRequest req, HttpServletResponse resp, String value, int maxAge) {
        // Lay cau hinh cookie phien cua container (Tomcat): ten, duong dan, ten mien... Muc dich: cookie ta ghi phai TRUNG KHOP cookie Tomcat da tao.
        SessionCookieConfig config = req.getServletContext().getSessionCookieConfig();
        // Ten cookie: dung ten da cau hinh, khong co thi dung ten mac dinh cua Servlet la "JSESSIONID".
        String name = config.getName() == null ? "JSESSIONID" : config.getName();
        // Tao cookie moi: cap (ten, gia tri) = (JSESSIONID, ma phien). Gia tri la ma phien do session.getId() tra ve (xem tham so value).
        Cookie cookie = new Cookie(name, value);
        // Duong dan ma trinh duyet se gui cookie kem: lay cau hinh san co; neu khong co thi dung context path cua ung dung
        // ("/" khi deploy o goc nhu tren Render, "/ecommerce" khi chay local). Khac duong dan = tao cookie MOI chu khong de duoc cookie cu.
        String path = config.getPath();
        cookie.setPath(path != null ? path : (req.getContextPath().isEmpty() ? "/" : req.getContextPath()));
        // Ten mien: chi dat khi co cau hinh; khong dat thi cookie chi gui lai cho dung may chu da cap no.
        if (config.getDomain() != null) cookie.setDomain(config.getDomain());
        // HttpOnly: JavaScript khong doc duoc cookie nay -> neu trang bi XSS thi ke xau cung khong an cap duoc ma phien.
        cookie.setHttpOnly(true);
        // Secure: chi gui cookie qua HTTPS. Bat khi request hien tai la HTTPS (req.isSecure()) hoac container da cau hinh Secure.
        // (Sau reverse proxy cua Render, RemoteIpValve trong Dockerfile giup req.isSecure() tra ve dung.)
        cookie.setSecure(req.isSecure() || config.isSecure());
        // SameSite=Lax: trinh duyet khong gui cookie kem cac request POST xuat phat tu trang web khac -> giam nguy co CSRF.
        // (Class Cookie cu khong co ham rieng cho thuoc tinh nay nen dung setAttribute.)
        cookie.setAttribute("SameSite", "Lax");
        // Thoi han (giay): -1 = cookie phien (mat khi dong trinh duyet); so duong (vd 7 ngay) = cookie luu lau ("Ghi nho toi"); 0 = bao trinh duyet XOA cookie.
        cookie.setMaxAge(maxAge);
        // Gui cookie ve trinh duyet bang header "Set-Cookie" trong phan hoi.
        resp.addCookie(cookie);
    }

    /**
     * Nguoi dang dang nhap (Customer hoac Admin), hoac null neu chua dang nhap. KHONG kiem tra quyen/khoa tai khoan:
     * dung cho trang cong khai can biet "co ai dang nhap khong" (vd header, chat vang lai); trang can quyen thi dung require*.
     * getSession(false) = chi LAY session da co, khong tao moi (khach chi xem trang khong nen bi tao session).
     */
    public static User currentUser(HttpServletRequest req) {
        HttpSession s = req.getSession(false);
        return s == null ? null : (User) s.getAttribute(CURRENT_USER);
    }

    /**
     * Dung cho cac trang chi danh cho KHACH HANG da dang nhap. Chua dang nhap -> chuyen toi /login?next=...
     * (htmx: dung header HX-Redirect), la Admin -> 403. Tra ve null neu da tra loi, Servlet chi viec `return`.
     *
     * Cach dung trong Servlet:
     *     Customer c = SessionUtil.requireCustomer(req, resp);
     *     if (c == null) return;   // da redirect/403 roi, KHONG viet them gi vao resp
     */
    public static Customer requireCustomer(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = currentUser(req);
        if (user == null) {
            redirectToLogin(req, resp, "/login");
            return null;
        }
        // Tai khoan bi Admin khoa trong luc dang dang nhap: user trong session la ban cu nen phai hoi lai DB, khoa thi dang xuat ngay
        // (chi phi: 1 query nho theo khoa chinh moi lan vao trang khach; doi lai khoa tai khoan co hieu luc tuc thi).
        // Chi dung `fresh` de xem con hoat dong khong; Customer tra ra van la doi tuong trong session.
        User fresh = new UserDAO().findById(user.getId());
        if (fresh == null || !fresh.isActive()) {
            req.getSession().invalidate();
            redirectToLogin(req, resp, "/login");
            return null;
        }
        // Admin co mo trang khach thi bao 403 (pattern matching: `user instanceof Customer customer` vua kiem tra kieu vua ep kieu)
        if (!(user instanceof Customer customer)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Chức năng này dành cho khách hàng");
            return null;
        }
        return customer;
    }

    /**
     * Giong requireCustomer nhung cho cac trang /admin/*: chua dang nhap -> /admin/login?next=..., la khach hang -> 403.
     * Khong hoi lai DB vi he thong khong cho khoa tai khoan Admin (chi khoa duoc Customer).
     */
    public static Admin requireAdmin(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        User user = currentUser(req);
        if (user == null) {
            redirectToLogin(req, resp, "/admin/login"); // Admin co trang dang nhap rieng
            return null;
        }
        if (!(user instanceof Admin admin)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN, "Chức năng này dành cho quản trị viên");
            return null;
        }
        return admin;
    }

    /**
     * Chuyen nguoi chua dang nhap toi trang dang nhap, kem tham so next = trang ho dang vao de dang nhap xong quay lai.
     * loginPath: "/login" cho khach hang, "/admin/login" cho Admin. next chi gom duong dan noi bo (khong co domain);
     * LoginServlet con kiem tra lai next truoc khi redirect (chong open redirect).
     *
     * Van de can nho: sau khi dang nhap trinh duyet quay lai next bang request GET. Neu trang bi chan la mot endpoint chi nhan
     * POST (huy don, doi mat khau, gui danh gia...) thi GET se bi loi 405. Vi vay cac endpoint POST thuong gap duoc
     * ANH XA sang trang GET gan nhat (xem tung nhanh ben duoi). Endpoint POST moi them ma chua co trong danh sach thi nen
     * them 1 nhanh o day.
     */
    // loginPath: trang dang nhap can chuyen toi ("/login" cho khach hang, "/admin/login" cho Admin); next = trang dang truy cap de quay lai
    private static void redirectToLogin(HttpServletRequest req,
                                        HttpServletResponse resp,
                                        String loginPath) throws IOException {
        // BUOC 1 - Xac dinh "trang khach dang muon vao" de sau khi dang nhap quay lai dung trang do (se thanh tham so next).
        // Duong dan KHONG gom contextPath (vd "/orders/detail"); contextPath duoc them vao luc dung URL dang nhap ben duoi.
        // getServletPath() = phan duong dan khop voi Servlet (vd "/admin/orders"); getPathInfo() = phan con lai sau do (vd "/detail"), co the null.
        // Ghep hai phan lai ra duong dan day du, vd "/admin/orders" + "/detail" = "/admin/orders/detail".
        String path = req.getServletPath()
                + (req.getPathInfo() == null ? "" : req.getPathInfo());

        // BUOC 2 - Sau khi dang nhap, trinh duyet quay lai "next" bang request GET. Nen phai chon duong dan "next" sao cho GET toi do chay duoc:
        //   - Request GET: giu nguyen duong dan, them query de xem dung nhu cu.
        //   - Request POST toi endpoint chi nhan POST: doi sang trang GET gan nhat (cac nhanh else-if ben duoi), neu khong GET se bi loi 405.
        if ("GET".equals(req.getMethod())) {
            // Giữ query để đăng nhập xong vẫn xem đúng đơn hàng.
            // getQueryString() la phan sau dau "?" cua URL (vd "id=12&tab=2"), null neu URL khong co query.
            String query = req.getQueryString();
            if (query != null && !query.isBlank()) {
                path += "?" + query;
            }
        } else if ("/orders/cancel".equals(path) || "/orders/return".equals(path)) {
            // Huy don / yeu cau hoan hang la POST: sau login ve trang chi tiet cua DON DO (id lay tu tham so cua form); khong co id hop le thi ve danh sach don.
            Integer id = ParamUtil.intOrNull(req.getParameter("id"));
            path = id != null && id > 0 ? "/orders/detail?id=" + id : "/orders";
        } else if ("/account/password".equals(path)) {
            // Sau login trinh duyet gui GET; endpoint doi mat khau chi nhan POST nen quay ve trang ho so.
            path = "/account/profile";
        } else if ("/product/review".equals(path)) {
            // Gui danh gia luc het phien: /product/review chi nhan POST nen sau login khong quay lai duoc (405).
            // Ve trang chi tiet san pham de lay form + token moi.
            Integer productId = ParamUtil.intOrNull(req.getParameter("productId"));
            path = productId != null && productId > 0 ? "/product?id=" + productId + "#reviews" : "/products";
        } else if ("/notifications/read".equals(path)) {
            // Danh dau da doc la POST: sau login ve trang Thong bao.
            path = "/notifications";
        } else if ("/checkout/voucher".equals(path)) {
            // Login xong trình duyệt gửi GET; quay về trang checkout.
            path = "/checkout";
        } else if ("/payment/vnpay/reconcile".equals(path)) {
            // Đối soát chỉ POST: sau login quay về GET kết quả để lấy token mới.
            Integer orderId = ParamUtil.intOrNull(req.getParameter("orderId"));
            path = "/checkout/success";
            if (orderId != null && orderId > 0) {
                path += "?orderId=" + orderId;
            }
        }

        // BUOC 3 - Truong hop dac biet: request htmx GET (polling).
        // Request htmx GET la "tho do" nen (vd /notifications/count moi 1 giay) tra ve MANH HTML, khong phai trang.
        // Het phien thi next phai la TRANG khach dang xem (header HX-Current-URL), neu khong login xong se hien manh "3".
        // Vi du: khach dang xem /orders/detail?id=12, phien het, chuong thong bao tu goi /notifications/count -> request nay bi chan.
        // Neu lay duong dan cua request hien tai thi sau login khach bi dua toi /notifications/count (chi co so "3") thay vi don hang dang xem.
        if (HtmxUtil.isHtmx(req) && "GET".equals(req.getMethod())) {
            String current = currentPagePath(req); // trang trinh duyet DANG MO (khong phai manh dang duoc hoi)
            if (current != null) {                 // null = khong doc duoc (thieu header / khac may chu) -> giu duong dan cua request
                path = current;
            }
        }

        // BUOC 4 - Dung URL trang dang nhap kem tham so next.
        // URLEncoder: next chua "?", "=", "#" ... phai ma hoa de khong bi lan voi tham so cua chinh URL dang nhap.
        // Vi du: path = "/orders/detail?id=12" -> next = "%2Forders%2Fdetail%3Fid%3D12" -> "/login?next=%2Forders%2Fdetail%3Fid%3D12".
        // getContextPath() dung dau URL: "" khi deploy o goc (Render), "/ecommerce" khi chay local.
        String login = req.getContextPath() + loginPath + "?next="
                + URLEncoder.encode(path, StandardCharsets.UTF_8);

        // BUOC 5 - Chuyen khach den trang dang nhap, cach chuyen phu thuoc loai request.
        // htmx gui request nen (AJAX): redirect thuong chi doi noi dung mot vung. HX-Redirect bao htmx chuyen CA TRANG sang URL dang nhap.
        // Request thuong (go URL, bam link, gui form): redirect 302 binh thuong bang sendRedirect.
        if (HtmxUtil.isHtmx(req)) {
            HtmxUtil.redirect(resp, login);
        } else {
            resp.sendRedirect(login);
        }
    }

    /**
     * Lay duong dan (khong gom contextPath, kem query) cua TRANG ma trinh duyet dang mo, tu header HX-Current-URL
     * do htmx gui kem moi request. Tra null neu thieu/khong hop le/khac may chu, de dung duong dan cua request hien tai.
     * Gia tri nay van duoc kiem tra lai o trang dang nhap (chi nhan duong dan noi bo), nen khong mo ra open redirect.
     *
     * VI SAO CAN: htmx tu goi nen nhieu request (polling) toi cac duong dan tra ve MANH HTML (vd /notifications/count, /orders/detail?id=12
     * voi HX-Target). Khi phien het, cac request nay bi redirectToLogin chan. Duong dan cua CHINH request do khong phai la "trang" de quay lai.
     * Trang that su khach dang xem nam trong header HX-Current-URL (URL day du tren thanh dia chi luc request duoc gui), vd
     * "https://nongviet.store/orders/detail?id=12". Ham nay doi no thanh duong dan noi bo "/orders/detail?id=12" de dung lam tham so next.
     */
    private static String currentPagePath(HttpServletRequest req) {
        // Doc header do htmx tu gui. Khong phai request htmx, hoac header rong -> khong co gi de lay.
        String header = req.getHeader("HX-Current-URL");
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            // Tach URL day du thanh cac phan (giao thuc, may chu, duong dan, query). URI ky hon cat chuoi bang tay.
            java.net.URI uri = new java.net.URI(header);
            // getRawPath: phan duong dan, GIU NGUYEN ma hoa (vd tieng Viet dang %C3%A1...) de khong bi giai ma roi ma hoa lai sai.
            String path = uri.getRawPath();
            String ctx = req.getContextPath();
            // Phai nam TRONG ung dung nay: duong dan phai bat dau bang contextPath + "/" (hoac bang dung contextPath).
            // Neu khong (vd header tro toi web khac) thi bo qua -> chan viec lay URL ngoai (chong open redirect).
            // Chu y thu tu uu tien cua toan tu: (a || (b && c)) - path null thi tra null truoc khi goi startsWith.
            if (path == null || !path.startsWith(ctx + "/") && !path.equals(ctx)) {
                return null;
            }
            // Cat bo contextPath o dau: "/ecommerce/orders" -> "/orders" (vi next khong gom contextPath, xem redirectToLogin). Deploy o goc thi ctx rong, khong cat gi.
            path = path.substring(ctx.length());
            if (path.isEmpty()) {
                path = "/";   // URL chi la ".../ecommerce" -> duong dan la trang chu "/"
            }
            // Chan hai dang nguy hiem: "//evil.com" (trinh duyet hieu la dia chi ngoai, "protocol-relative") va dau "\" (mot so trinh duyet coi nhu "/").
            if (path.startsWith("//") || path.contains("\\")) {
                return null;
            }
            // Ghep lai query neu co (vd "?id=12") de dang nhap xong xem dung don hang do.
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (java.net.URISyntaxException e) {
            // Header co dinh dang khong hop le (bi sua tay) -> bo qua, dung duong dan cua request hien tai.
            return null;
        }
    }
}
