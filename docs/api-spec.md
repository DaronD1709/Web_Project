# Đặc tả endpoint (Servlet) — hợp đồng giữa Controller và JSP

Dự án **không có REST/JSON API** (xem CLAUDE.md). "API" ở đây là danh sách URL mà các Servlet phục vụ:
`GET` trả về trang JSP, `POST` xử lý hành động rồi redirect (hoặc trả htmx fragment). File này là
**hợp đồng**: người viết Servlet/Service và người viết JSP chỉ cần theo bảng dưới là ghép được với nhau.

Context path khi chạy Tomcat: `/ecommerce` (theo `finalName` trong `pom.xml`). Mọi URL dưới đây tính từ đó.

---

## 1. Quy ước chung

### 1.1 Vai trò truy cập
| Ký hiệu | Ý nghĩa | Cách chặn |
|---|---|---|
| `PUBLIC` | Ai cũng vào được | — |
| `CUSTOMER` | Phải đăng nhập (role Customer) | `AuthFilter`: chưa login → redirect `/login?next=<url gốc>` |
| `ADMIN` | Chỉ Admin | `AdminFilter` trên `/admin/*`: sai role → 403 |

Thông tin đăng nhập lưu trong session: `session.getAttribute("currentUser")` (đối tượng `User`).
Giỏ hàng lưu DB (quan hệ `Customer 1—1 Cart`), **không** lưu session; khách chưa đăng nhập bấm "Thêm vào giỏ" → chuyển tới `/login`.

### 1.2 Mẫu xử lý
- **GET** → Servlet gọi Service lấy dữ liệu → `req.setAttribute(...)` → `forward` tới JSP trong `WEB-INF/views/`.
- **POST thường (form)** → Service xử lý → **redirect** (`resp.sendRedirect`) tới trang kết quả (Post/Redirect/Get, tránh submit lại khi F5).
  Nếu lỗi validate → `forward` lại chính form kèm `errors` (Map<String,String>) và giữ giá trị đã nhập.
- **POST/GET từ htmx** (request có header `HX-Request: true`) → trả **đoạn HTML nhỏ** (fragment JSP trong `views/.../fragments/`), không trả cả trang.
- Một Servlet có nhiều hành động → dùng tham số **`action`** (`create|update|delete|...`) và `switch` trong `doPost`, dễ giải thích hơn nhiều Servlet lẻ.

### 1.3 Mã trạng thái & lỗi
| Tình huống | Phản hồi |
|---|---|
| Thành công (POST) | 302 redirect |
| Dữ liệu sai (validate) | 200 forward lại form + `errors` (htmx: 422 + fragment lỗi) |
| Chưa đăng nhập | 302 `/login?next=...` |
| Sai quyền (vd huỷ đơn người khác, khách vào `/admin`) | 403 → `views/common/403.jsp` |
| Không tìm thấy (id sai) | 404 → `views/common/404.jsp` |
| Lỗi nghiệp vụ (hết hàng, huỷ sai trạng thái…) | Service ném `BusinessException(message)`; Servlet bắt, đưa `error` ra JSP |

### 1.4 Bảo mật tối thiểu (bắt buộc theo feature-list)
- Mọi truy vấn dùng tham số JPA (`setParameter`), **không** nối chuỗi SQL/JPQL.
- Mọi dữ liệu người dùng nhập khi hiển thị dùng `<c:out value="${...}"/>` (chống XSS) — đặc biệt review, chat, tên/địa chỉ.
- Mật khẩu lưu băm (`passwordHash`), không bao giờ ra JSP.
- Kiểm tra quyền sở hữu ở **Service** (vd đơn phải thuộc `currentUser`), không tin `id` từ client.

---

## 2. Tài khoản

| Method | URL | Servlet | Tham số | Phản hồi | Quyền |
|---|---|---|---|---|---|
| GET | `/register` | `RegisterServlet` | — | `customer/register.jsp` | PUBLIC |
| POST | `/register` | `RegisterServlet` | `fullName`, `email`, `phone`, `password`, `confirmPassword` | OK → tạo Customer + giỏ rỗng, **gửi email chào mừng**, 302 `/login?registered=1`; lỗi → form + `errors` (`email` trùng, `password` < 6 ký tự, không khớp) | PUBLIC |
| GET | `/login` | `LoginServlet` | `next` (tuỳ chọn) | `customer/login.jsp` | PUBLIC |
| POST | `/login` | `LoginServlet` | `email`, `password`, `next` | OK → lưu `currentUser`, 302 `next` hoặc `/`; sai → form + `error` | PUBLIC |
| POST | `/logout` | `LogoutServlet` | — | `session.invalidate()`, 302 `/products`. Chỉ nhận POST (GET → 405) | CUSTOMER/ADMIN |
| GET | `/forgot-password` | `ForgotPasswordServlet` | `sent=1` (sau khi gửi) | `customer/forgot-password.jsp` | PUBLIC |
| POST | `/forgot-password` | `ForgotPasswordServlet` | `email` | Luôn 302 `/forgot-password?sent=1` dù email có tồn tại hay không; nếu tồn tại → tạo token, **gửi email chứa link** `/reset-password?token=…` | PUBLIC |
| GET | `/reset-password` | `ResetPasswordServlet` | `token` | `customer/reset-password.jsp`: form mật khẩu mới, hoặc thông báo "liên kết không hợp lệ/hết hạn" (attr `invalid`) | PUBLIC |
| POST | `/reset-password` | `ResetPasswordServlet` | `token`, `password`, `confirmPassword` | OK → 302 `/login?reset=1`; lỗi → form + `errors`; token sai/hết hạn/đã dùng → thông báo không hợp lệ | PUBLIC |
| GET | `/account/profile` | `ProfileServlet` | — | `customer/profile.jsp` (attr `user`) | CUSTOMER |
| POST | `/account/profile` | `ProfileServlet` | `fullName`, `phone`, `csrfToken` | Thường: 302 `/account/profile?saved=1`; htmx: `fragments/profile-info.jsp`, cập nhật tên header qua OOB; lỗi: form + `profileErrors` (htmx 422) | CUSTOMER |
| POST | `/account/password` | `ProfileServlet` | `current`, `newPassword`, `confirm`, `csrfToken` | Thường: 302 `/account/profile?passwordChanged=1`; htmx: `fragments/profile-password.jsp`; lỗi: form + `passwordErrors` (htmx 422) | CUSTOMER |
| GET | `/account/addresses` | `AddressServlet` | — | `customer/addresses.jsp` (attr `addresses`) | CUSTOMER |
| POST | `/account/addresses` | `AddressServlet` | `action=create\|update\|delete\|setDefault`, `id`, `recipientName`, `phone`, `street`, `city`, `isDefault` | 302 `/account/addresses` | CUSTOMER |

Service: `AuthService.register/login/requestPasswordReset/isResetTokenValid/resetPassword`, `UserService.updateProfile/changePassword`, `AddressService.*`.

**Đã code & test (giỏ hàng, chat):** xem mục 4 và 6 — `GET/POST /cart` (add/update/remove, htmx), `GET /chat`, `GET /chat/messages`, `POST /chat/send`. Chưa có: checkout, phía Admin của chat, AI tự trả lời.

**Đã code & test:** `/register`, `/login`, `/logout`, `/forgot-password`, `/reset-password` (kèm `AuthFilter`/`AdminFilter` chưa làm).
`login`: sai email hay sai mật khẩu đều báo **cùng một** thông báo; tham số `next` chỉ chấp nhận đường dẫn nội bộ bắt đầu bằng `/` (chặn open redirect); đổi session ID khi đăng nhập (chống session fixation).
Link trong email dùng `app.base.url` trong `mail.properties` (nếu có), nếu không thì suy từ request.
Email không đổi được (chỉ đọc). Địa chỉ chỉ thao tác trên địa chỉ của `currentUser`.

**Manage Profile đã triển khai:** `ProfileServlet → UserService → UserDAO`; dùng `SessionUtil.requireCustomer`
trong khi chờ AuthFilter. Khách chưa đăng nhập → `/login?next=/account/profile` (next được URL encode),
kể cả khi hết phiên lúc POST `/account/password`, để sau login quay về trang GET hợp lệ. Admin → 403. GET `/account/password` → 405.
ID lấy từ session; tham số `id`, `email`, `role` gửi thêm không được dùng để cập nhật.
Họ tên bắt buộc, trim và tối đa 255 ký tự; số điện thoại tùy chọn, nếu nhập cần 8–15 chữ số,
cho phép dấu `+` ở đầu và ký tự phân cách khoảng trắng, `.`, `-`, `(`, `)`.
Đổi mật khẩu kiểm tra mật khẩu hiện tại bằng hash mới nhất trong DB, mật khẩu mới tối thiểu 6 ký tự và phải khớp xác nhận;
không trim hay đưa mật khẩu trở lại HTML. Đổi thành công xóa token reset cũ và giữ phiên hiện tại;
các phiên đăng nhập khác chưa được thu hồi tự động. Cập nhật chỉ tác động các cột cần thiết, không merge toàn bộ Customer.
Hai POST yêu cầu token CSRF gắn với session (thiếu/sai → 403). GET/POST gửi `Cache-Control: no-store`.
Form thường dùng PRG; htmx trả fragment, lỗi 422 được trang Profile cho phép swap để hiện lỗi tại ô nhập.
Sau khi lưu, session `currentUser` được cập nhật và `cartCount` giữ nguyên; menu tài khoản trên header được thay qua OOB.
Menu dropdown từ `mockup/layout.js` mở được Hồ sơ trên cả mobile và desktop, Logout vẫn dùng POST.
UI chuyển từ `mockup/profile.html`,
menu tài khoản dùng `common/account-nav.jspf`; liên kết địa chỉ/đơn hàng/thông báo giữ URL theo hợp đồng để các nhánh tương ứng nối sau.

---

## 3. Sản phẩm (public)

| Method | URL | Servlet | Tham số | Attr cho JSP | JSP |
|---|---|---|---|---|---|
| GET | `/` , `/home` | `HomeServlet` | — | `categories`, `featuredProducts` | `customer/home.jsp` |
| GET | `/products` | `ProductServlet` | `cat` (categoryId), `q` (từ khoá), `min`, `max`, `instock=on`, `sort=new\|asc\|desc\|sold`, `page` (mặc định 1) | `products`, `categories`, `totalCount`, `page`, `totalPages`, `selectedCat`, `q` | `customer/products.jsp` |
| GET | `/product` | `ProductDetailServlet` | `id` | `product`, `relatedProducts`, `reviews`, `avgRating`, `canReview` (đã mua) | `customer/product-detail.jsp` |
| POST | `/product/review` | `ReviewServlet` | `productId`, `rating` (1–5), `comment` | 302 `/product?id=..#reviews`; chưa mua → 403 | CUSTOMER |

Service: `ProductService.search(filter)`, `ReviewService.write(user, productId, rating, comment)` —
**chỉ cho viết review nếu khách có đơn `COMPLETED`/`RETURNED` chứa sản phẩm** (rule ở Service, không phải entity).
Tìm kiếm không phân biệt hoa thường (`LOWER(p.name) LIKE :q`). Sản phẩm `stockQuantity = 0` vẫn hiện nhưng nút mua bị khoá.

---

## 4. Giỏ hàng & thanh toán (CUSTOMER)

| Method | URL | Servlet | Tham số | Phản hồi |
|---|---|---|---|---|
| GET | `/cart` | `CartServlet` | — | `customer/cart.jsp` (attr `cart`, `subtotal`, `shippingFee`, `total`) |
| POST | `/cart` | `CartServlet` | `action=add` + `productId`, `qty` | htmx: fragment `#cart-count` (số lượng mới); thường: 302 `/cart` |
| POST | `/cart` | `CartServlet` | `action=update` + `itemId`, `qty` | htmx: fragment tóm tắt giỏ; thường: 302 |
| POST | `/cart` | `CartServlet` | `action=remove` + `itemId` | htmx: xoá dòng + cập nhật tóm tắt; thường: 302 |
| GET | `/checkout` | `CheckoutServlet` | — | `customer/checkout.jsp` (attr `cart`, `addresses`, `defaultAddress`); giỏ rỗng → 302 `/cart` |
| POST | `/checkout/voucher` | `VoucherServlet` | `code` | htmx fragment `#voucher-result`: thành công (số tiền giảm) hoặc **một** lỗi cụ thể |
| POST | `/checkout` | `CheckoutServlet` | `addressId`, `paymentMethod=COD\|VNPAY`, `voucherCode` (tuỳ chọn) | OK → 302 `/checkout/success?orderId=..`; thiếu hàng → quay lại `/checkout` + `error` |
| GET | `/checkout/success` | `CheckoutServlet` | `orderId` | `customer/order-success.jsp` (chỉ xem được đơn của mình) |
| GET | `/payment/vnpay/return` | `VNPayReturnServlet` | tham số VNPay trả về | (Optional) cập nhật `PaymentStatus`, 302 `/checkout/success` |

Quy tắc `OrderService.checkout(user, addressId, method, voucherCode)` — **trong 1 transaction**:
1. Validate mọi `CartItem.quantity <= Product.stockQuantity`, sai → `BusinessException` kèm tên sản phẩm.
2. Nếu có voucher: `Voucher.isValid()` + kiểm tra theo thứ tự — **hết hạn → hết lượt → chưa đạt giá trị tối thiểu** (mỗi lỗi 1 thông báo riêng).
3. Tạo `Order` (`PENDING`), `OrderItem` (snapshot `priceAtOrder`), `Payment` (`COD`/`VNPay`).
4. **Trừ kho ngay** (`Product.updateStock(-qty)`), tăng `Voucher.quantityUsed`, xoá `CartItem`.
5. Tạo `Notification` cho khách.
6. **Sau khi commit**: `EmailService.sendOrderConfirmation(order)` gửi **email xác nhận đơn hàng** tới `customer.email` (danh sách sản phẩm, tổng tiền, địa chỉ, phương thức thanh toán; chạy nền, lỗi gửi mail không rollback đơn). Với VNPay: gửi sau khi thanh toán thành công (ở `/payment/vnpay/return`), COD gửi ngay khi đặt hàng. **Phải truyền `Order` còn nguyên items/address trong bộ nhớ**, không load lại từ DB.

Phí ship: miễn phí khi tạm tính ≥ 500.000₫, ngược lại 30.000₫ (hằng số trong Service).

---

## 5. Đơn hàng (CUSTOMER)

| Method | URL | Servlet | Tham số | Phản hồi |
|---|---|---|---|---|
| GET | `/orders` | `OrderServlet` | `status` (tuỳ chọn: `PENDING`…`RETURN_REQUESTED`) | `customer/orders.jsp` (attr `orders`, `selectedStatus`) — chỉ đơn của `currentUser` |
| GET | `/orders/detail` | `OrderServlet` | `id` | `customer/order-detail.jsp` (attr `order`, `canCancel`, `canReturn`, `canReview`); đơn người khác → 403 |
| POST | `/orders/cancel` | `OrderServlet` | `id` | 302 `/orders/detail?id=..`; chỉ khi `PENDING`/`CONFIRMED` và đúng chủ đơn |
| POST | `/orders/return` | `OrderServlet` | `id`, `reason` (bắt buộc) | 302; chỉ khi `COMPLETED` → `RETURN_REQUESTED` |

Luồng trạng thái hợp lệ (validate trong `Order.updateStatus()`):
`PENDING → CONFIRMED → SHIPPING → COMPLETED`; `PENDING/CONFIRMED → CANCELLED`;
`COMPLETED → RETURN_REQUESTED → RETURNED` (Admin từ chối → quay lại `COMPLETED`).
Huỷ đơn / duyệt hoàn hàng ⇒ **cộng lại kho** từng `OrderItem`. Mỗi lần đổi trạng thái ⇒ tạo `Notification`.

---

## 6. Thông báo & Chat (CUSTOMER)

| Method | URL | Servlet | Tham số | Phản hồi |
|---|---|---|---|---|
| GET | `/notifications` | `NotificationServlet` | — | `customer/notifications.jsp` (attr `notifications`) |
| POST | `/notifications/read` | `NotificationServlet` | `id` hoặc `all=true` | 302 `/notifications` |
| GET | `/notifications/count` | `NotificationServlet` | — | htmx fragment số chưa đọc (badge header), `hx-trigger="every 30s"` |
| GET | `/chat` | `ChatServlet` | — | `customer/chat.jsp` (attr `messages`, `conversation`) |
| POST | `/chat/send` | `ChatServlet` | `content` | htmx: fragment tin nhắn mới (của khách, rồi trả lời AI nếu chưa có Admin tiếp quản) |
| GET | `/chat/messages` | `ChatServlet` | `after` (id tin cuối đã có) | htmx fragment các tin mới hơn, `hx-trigger="every 3s"` (polling, không dùng WebSocket) |

`Message` gắn với khách qua `Message.customer`; `Message.sender` là `User` (Customer hoặc Admin; chatbot dùng tài khoản hệ thống loại Admin) — JSP phân biệt bubble theo người gửi.
Nội dung tin nhắn luôn qua `<c:out>`.

---

## 7. Admin (đề xuất — chưa có mockup, Core trước)

| Method | URL | Servlet | Tham số chính | Ghi chú |
|---|---|---|---|---|
| GET | `/admin` | `AdminDashboardServlet` | — | Optional: thống kê doanh thu, bán chạy |
| GET | `/admin/products` | `AdminProductServlet` | `q`, `cat`, `page` | Danh sách + tồn kho |
| POST | `/admin/products` | `AdminProductServlet` | `action=create\|update\|delete`, `id`, `name`, `description`, `price`, `stockQuantity`, `categoryId`, ảnh (`multipart/form-data`) | Upload ảnh lưu vào `webapp/static/uploads/`; xoá SP đã có trong đơn → từ chối |
| GET/POST | `/admin/categories` | `AdminCategoryServlet` | `action=create\|update\|delete`, `id`, `name`, `description` | Không xoá danh mục còn sản phẩm |
| GET | `/admin/orders` | `AdminOrderServlet` | `status`, `page` | Mọi đơn của mọi khách |
| GET | `/admin/orders/detail` | `AdminOrderServlet` | `id` | — |
| POST | `/admin/orders/status` | `AdminOrderServlet` | `id`, `status` | Đi qua `Order.updateStatus()`; huỷ ⇒ hoàn kho; tạo Notification |
| POST | `/admin/orders/return` | `AdminOrderServlet` | `id`, `decision=approve\|reject` | `RETURN_REQUESTED → RETURNED` (hoàn kho) hoặc về `COMPLETED` |
| — | `/admin/vouchers`, `/admin/users`, `/admin/reviews`, `/admin/chat` | … | … | **Optional**, làm sau khi Core xong |

---

## 8. Servlet mẫu theo hợp đồng trên (`POST /cart`, action=add)

```java
@WebServlet("/cart")
public class CartServlet extends HttpServlet {

    private final CartService cartService = new CartService();   // Servlet chỉ gọi Service

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        User user = (User) req.getSession().getAttribute("currentUser");  // AuthFilter đã đảm bảo != null
        String action = req.getParameter("action");
        boolean isHtmx = "true".equals(req.getHeader("HX-Request"));

        try {
            switch (action) {
                case "add" -> cartService.addItem(user.getId(),
                        Integer.parseInt(req.getParameter("productId")),
                        Integer.parseInt(req.getParameter("qty")));
                case "update" -> cartService.updateQuantity(user.getId(),
                        Integer.parseInt(req.getParameter("itemId")),
                        Integer.parseInt(req.getParameter("qty")));
                case "remove" -> cartService.removeItem(user.getId(),
                        Integer.parseInt(req.getParameter("itemId")));
                default -> { resp.sendError(HttpServletResponse.SC_BAD_REQUEST); return; }
            }
        } catch (BusinessException e) {                    // vd: vượt tồn kho
            if (isHtmx) { resp.setStatus(422); resp.getWriter().write(e.getMessage()); return; }
            req.setAttribute("error", e.getMessage());
            req.getRequestDispatcher("/WEB-INF/views/customer/cart.jsp").forward(req, resp);
            return;
        }

        if (isHtmx) {                                      // htmx chỉ cần đoạn HTML nhỏ
            req.setAttribute("cartCount", cartService.countItems(user.getId()));
            req.getRequestDispatcher("/WEB-INF/views/customer/fragments/cart-count.jsp").forward(req, resp);
        } else {
            resp.sendRedirect(req.getContextPath() + "/cart"); // Post/Redirect/Get
        }
    }
}
```

Phía JSP (`mockup/product-detail.html` → `product-detail.jsp`) gắn htmx:
```html
<button hx-post="${pageContext.request.contextPath}/cart"
        hx-vals='{"action":"add","productId":"${product.id}","qty":"1"}'
        hx-target="#cart-count" hx-swap="outerHTML">Thêm vào giỏ</button>
```

---

## 8b. Email gửi đi (Jakarta Mail / SMTP)

| Sự kiện | Hàm | Người nhận | Nội dung |
|---|---|---|---|
| Đăng ký thành công | `EmailService.sendWelcome(user)` | email vừa đăng ký | Chào mừng, xác nhận tài khoản |
| Yêu cầu quên mật khẩu (email tồn tại) | `EmailService.sendPasswordReset(user, link)` | email của tài khoản | Nút/link đặt lại mật khẩu, hiệu lực 30 phút, dùng 1 lần |
| Đặt hàng xong (checkout) | `EmailService.sendOrderConfirmation(order)` | `order.customer.email` | Mã đơn, sản phẩm, tổng tiền, địa chỉ, phương thức thanh toán |
| *(Optional)* Đổi trạng thái đơn | `EmailService.sendAsync(...)` | khách của đơn | Trạng thái mới |

Quy ước: nội dung email dựng **ngay trên thread gọi** (còn đủ dữ liệu), chỉ phần gửi SMTP chạy ở thread nền; lỗi gửi chỉ ghi log. Mọi dữ liệu người dùng đưa vào HTML email đều được escape. Chi tiết cấu hình: `mail.properties.example`.

## 9. Thứ tự code đề xuất (Core trước)

1. **Auth + filter:** `/register`, `/login`, `/logout`, `/forgot-password`, `/reset-password` (**đã xong**); còn `AuthFilter`, `AdminFilter`.
2. **Sản phẩm:** `/`, `/products`, `/product`, Admin CRUD sản phẩm/danh mục (cần dữ liệu để test các bước sau).
3. **Giỏ hàng:** `/cart`.
4. **Checkout + đơn hàng:** `/checkout`, `/orders*` (có trừ/hoàn kho, `Order.updateStatus()`).
5. **Admin đơn hàng:** `/admin/orders*`.
6. Optional: voucher, review, notification, chat/AI, địa chỉ nhiều (nếu chưa xong), dashboard.
