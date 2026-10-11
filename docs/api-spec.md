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
| POST | `/account/profile` | `ProfileServlet` | `fullName`, `phone` | 302 `/account/profile?saved=1` | CUSTOMER |
| POST | `/account/password` | `ProfileServlet` | `current`, `newPassword`, `confirm` | 302 hoặc form + `errors` | CUSTOMER |
| GET | `/account/addresses` | `AddressServlet` | — | `customer/addresses.jsp` (attr `addresses`) | CUSTOMER |
| POST | `/account/addresses` | `AddressServlet` | `action=create\|update\|delete\|setDefault`, `id`, `recipientName`, `phone`, `street`, `city`, `isDefault` | 302 `/account/addresses` | CUSTOMER |

Service: `AuthService.register/login/requestPasswordReset/isResetTokenValid/resetPassword`, `UserService.updateProfile/changePassword`, `AddressService.*`.

**Đã code & test (giỏ hàng, checkout COD, chat):** xem mục 4 và 6 — `GET/POST /cart` (add/update/remove, htmx), `/checkout`, `/checkout/voucher`, `/checkout/success`, `GET /chat`, `GET /chat/messages`, `POST /chat/send`. Chat có chatbot trả lời theo luật từ khoá và phía Admin (`/admin/chat`). Checkout hiện chỉ hỗ trợ COD; VNPay chờ tích hợp sandbox.

**Đã code & test:** `/register`, `/login`, `/logout`, `/forgot-password`, `/reset-password` (kèm `AuthFilter`/`AdminFilter` chưa làm).
`login`: sai email hay sai mật khẩu đều báo **cùng một** thông báo; tham số `next` chỉ chấp nhận đường dẫn nội bộ bắt đầu bằng `/` (chặn open redirect); đổi session ID khi đăng nhập (chống session fixation).
Link trong email dùng `app.base.url` trong `mail.properties` (nếu có), nếu không thì suy từ request.
Email không đổi được (chỉ đọc). Địa chỉ chỉ thao tác trên địa chỉ của `currentUser`.

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
| GET | `/checkout` | `CheckoutServlet` | `voucherCode` (tuỳ chọn, sau POST preview thường) | `customer/checkout.jsp` (attr `cart`, `addresses`, `defaultAddress`, `selectedAddressId`, `checkoutToken`, `subtotal`, `shippingFee`, `voucherCode`, `appliedVoucherCode`, `voucher`, `voucherError`, `discountAmount`, `total`); giỏ rỗng → 302 `/cart` |
| POST | `/checkout/voucher` | `VoucherServlet` | `code` | htmx fragment `#voucher-result`: thành công (số tiền giảm) hoặc **một** lỗi cụ thể |
| POST | `/checkout` | `CheckoutServlet` | `addressId`, `paymentMethod=COD`, `voucherCode` (tuỳ chọn), `checkoutToken` | OK → 302 `/checkout/success?orderId=..`; lỗi nghiệp vụ → trang checkout + `error`; token sai/thiếu → 403 |
| GET | `/checkout/success` | `CheckoutServlet` | `orderId` | `customer/order-success.jsp` (chỉ xem được đơn của mình) |
| GET | `/payment/vnpay/return` | `VNPayReturnServlet` | tham số VNPay trả về | (Optional) cập nhật `PaymentStatus`, 302 `/checkout/success` |

**Giỏ hàng:** cả 3 action yêu cầu tham số ID/số lượng đúng định dạng số nguyên trong phạm vi `int`;
thiếu/sai không được tự đổi thành `qty=1`. Thêm/cập nhật yêu cầu số lượng ≥ 1 và không vượt tồn kho;
`update` với 0/số âm báo lỗi, xoá chỉ dùng `action=remove`. Khi thêm sản phẩm đã có, tổng số lượng
được cộng bằng `long` rồi kiểm tra trước khi chuyển về `int`, tránh tràn số. Dòng sửa/xoá phải thuộc giỏ của khách hiện tại;
ID khách lấy từ session, không dùng `customerId` gửi thêm. Giỏ lưu DB, không trừ kho tại bước này;
giữ `priceAtAdd` của dòng đã có khi tăng số lượng.
Tạo giỏ và mỗi thao tác thêm/sửa/xoá chạy trong transaction của `CartDAO.changeCart`:
khoá dòng Customer bằng JPQL `PESSIMISTIC_WRITE` trước khi đọc giỏ, để các request cùng khách
không tạo trùng giỏ/dòng hoặc ghi đè số lượng của nhau. Service kiểm tra lại `Customer.isActive()`
sau khi lấy khoá; lỗi nghiệp vụ rollback toàn bộ thao tác. Khoá này không giữ hàng;
checkout vẫn phải kiểm tra và trừ tồn kho trong transaction riêng.
GET/POST `/cart` gửi `Cache-Control: no-store`; GET làm mới session `cartCount` (số dòng sản phẩm khác nhau).
Lỗi htmx trả **422**, `HX-Trigger` hiện toast và `HX-Reswap: none` giữ nội dung giỏ;
request thường forward trang giỏ với `error`. POST thành công thường redirect `/cart`;
htmx add trả badge, update/remove trả nội dung giỏ + badge OOB. Các nút dùng form POST
để vẫn hoạt động khi htmx không tải được; màu/bố cục dựa trên mockup. Nút thanh toán dẫn tới `/checkout`.

**Checkout COD:** GET/POST gửi `Cache-Control: no-store`, luôn dùng `SessionUtil.requireCustomer`.
GET đọc giỏ/địa chỉ đúng khách, ưu tiên địa chỉ mặc định; giỏ trống redirect `/cart`.
POST tạo đơn và thanh toán `PENDING`, chưa có `paymentDate`; từ chối `VNPAY` tới khi tích hợp sandbox.
Giá/tổng tiền lấy từ DB, snapshot `CartItem.priceAtAdd` sang `OrderItem.priceAtOrder`.
Thứ tự khoá: Customer giống CartDAO → địa chỉ thuộc khách → Product theo ID tăng dần trước khi fetch giỏ → voucher;
kiểm tra tổng số lượng theo Product kể cả giỏ cũ có dòng trùng. Đơn, trừ kho, lượt voucher, xoá giỏ và Notification cùng transaction.
POST kiểm tra token ngẫu nhiên trong session; lỗi giữ token để sửa form. Request cùng session được phối hợp
bằng `synchronized(session)`; gửi lại token vừa thành công redirect tới đơn cũ, không dùng giỏ mới.
GET `/checkout/success`: orderId sai → 400, không tồn tại → 404, đơn người khác → 403.
`SessionUtil` giữ query của GET trong `next` khi chuyển tới login, để đăng nhập lại vẫn xem đúng `orderId`.
POST `/checkout/voucher` khi chưa đăng nhập quay về GET `/checkout` sau login, tránh URL voucher chỉ nhận POST (GET → 405).

**Áp dụng voucher:** POST `/checkout/voucher` nhận `code`, chỉ xem mức giảm, không giữ/tăng lượt sử dụng.
htmx trả fragment `checkout-voucher.jsp` (200/422) + tổng tiền/mã đã áp dụng OOB vào `#checkout-summary`;
lỗi xoá mức giảm của mã cũ. POST thường redirect GET `/checkout?voucherCode=..`, GET kiểm tra lại để hiển thị.
JSP dùng hai form riêng; input `code` thuộc form preview, hidden `voucherCode` thuộc form đặt hàng.
JS chỉ cho swap 422 tại `#voucher-result`, không đổi hành vi Cart/Profile; giữ nút đặt hàng chờ áp dụng
khi nội dung mã đang nhập khác mã đã được áp dụng. Địa chỉ CRUD và trang theo dõi đơn chờ nhánh tương ứng.

Quy tắc `OrderService.checkout(user, addressId, method, voucherCode)` — **trong 1 transaction**:
1. Validate mọi `CartItem.quantity <= Product.stockQuantity`, sai → `BusinessException` kèm tên sản phẩm.
2. Nếu có voucher: `Voucher.isValid()` + kiểm tra theo thứ tự — **hết hạn → hết lượt → chưa đạt giá trị tối thiểu** (mỗi lỗi 1 thông báo riêng).
3. Tạo `Order` (`PENDING`), `OrderItem` (snapshot `priceAtOrder`), `CODPayment` (`PENDING`). VNPay chưa được tích hợp và bị từ chối.
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
`OrderDAO.applyStatusChange` khoá đơn trước khi kiểm tra chuyển trạng thái; huỷ/hoàn khoá Product theo ID tăng dần,
đọc lại entity bằng `refresh` sau khi có khoá rồi cộng kho. Huỷ đơn có voucher khoá và `refresh` voucher sau sản phẩm
trước khi trả một lượt sử dụng; hoàn hàng giữ lượt voucher theo luật hiện tại. Các thay đổi cùng transaction,
tránh ghi đè tồn kho/lượt sử dụng khi checkout và Admin xử lý đơn đồng thời.

---

## 6. Thông báo & Chat (CUSTOMER)

| Method | URL | Servlet | Tham số | Phản hồi |
|---|---|---|---|---|
| GET | `/notifications` | `NotificationServlet` | — | `customer/notifications.jsp` (attr `notifications`) |
| POST | `/notifications/read` | `NotificationServlet` | `id` hoặc `all=true` | 302 `/notifications` |
| GET | `/notifications/count` | `NotificationServlet` | — | htmx fragment số chưa đọc (badge header), `hx-trigger="every 30s"` |
| GET | `/chat` | `ChatServlet` | — | `customer/chat.jsp` (attr `messages`, `conversation`) |
| POST | `/chat/send` | `ChatServlet` | `content` | htmx: fragment tin nhắn mới (của khách, rồi **chatbot trả lời theo luật từ khoá** nếu chưa có Admin tiếp quản; không hiểu / xin gặp nhân viên ⇒ chuyển sang chế độ Nhân viên) |
| GET | `/chat/messages` | `ChatServlet` | `after` (id tin cuối đã có) | htmx fragment các tin mới hơn, `hx-trigger="every 3s"` (polling, không dùng WebSocket) |

`Message` gắn với khách qua `Message.customer`; `Message.sender` là `User` (Customer hoặc Admin; chatbot dùng tài khoản hệ thống loại Admin) — JSP phân biệt bubble theo người gửi.
Nội dung tin nhắn luôn qua `<c:out>`.

---

## 7. Admin (Core trước; Quản lý sản phẩm đã code)

Mọi URL `/admin/*` (trừ `/admin/login`) dùng `SessionUtil.requireAdmin` (chưa đăng nhập → `/admin/login?next=`, khách hàng → 403). Khung trang (header + sidebar) nằm ở `WEB-INF/views/admin/admin-layout.jsp`;
Servlet chỉ gọi `AdminView.render(req, resp, "xxx.jsp", "Tiêu đề", "mục-sidebar")` — mở trực tiếp thì trả cả khung, bấm link qua htmx (`hx-boost`) thì chỉ trả nội dung.

| Method | URL | Servlet | Tham số chính | Ghi chú |
|---|---|---|---|---|
| GET | `/admin` | `AdminDashboardServlet` | — | **Đã code.** Tổng quan: việc cần xử lý (đơn chờ, yêu cầu hoàn hàng, khách chờ chat), 4 thẻ số liệu (doanh thu hôm nay so với hôm qua, đơn chờ, đang giao, sản phẩm sắp hết), biểu đồ doanh thu 14 ngày, đơn theo trạng thái, 6 đơn mới nhất, hàng sắp hết, sản phẩm bán chạy. Đăng nhập admin xong vào trang này |
| GET | `/admin/statistics` | `AdminStatisticsServlet` | `days=7\|14\|30` (mặc định 30) | **Đã code.** 4 thẻ số liệu kèm % so với kỳ trước cùng độ dài, biểu đồ doanh thu theo ngày / theo danh mục, top 10 sản phẩm. Doanh thu = đơn đang xử lý + hoàn tất, KHÔNG tính huỷ / hoàn hàng |
| GET | `/admin/statistics/export` | `AdminStatisticsServlet` | `days` | **Đã code.** Tải CSV doanh thu theo ngày (UTF-8 có BOM để Excel đọc đúng tiếng Việt) |
| GET | `/admin/login` | `AdminLoginServlet` | `next` | **Đã code.** Form đăng nhập quản trị (`admin-login.jsp`, trang riêng không có header/sidebar). Đã đăng nhập Admin → 302 vào CMS |
| POST | `/admin/login` | `AdminLoginServlet` | `email`, `password`, `next` | **Đã code.** Dùng `AuthService.login`, thêm bước kiểm tra là `Admin` (khách hàng đúng mật khẩu → "không có quyền quản trị"). Thành công: huỷ session cũ, tạo session mới, 302 tới `next` (chỉ nhận đường dẫn bắt đầu bằng `/admin`, ngược lại `/admin/products`) |
| POST | `/admin/logout` | `AdminLogoutServlet` | — | **Đã code.** Huỷ session, 302 `/admin/login`. Chỉ POST (GET → 405) |
| GET | `/admin/products` | `AdminProductServlet` | `q`, `cat`, `stock=in\|low\|out`, `sort=new\|name\|asc\|desc\|stock`, `page` (10 SP/trang) | **Đã code.** Trang đầy đủ; request htmx có `HX-Target: product-list` (ô lọc, phân trang) chỉ trả bảng `fragments/admin-product-table.jsp` |
| GET | `/admin/products/new` | `AdminProductServlet` | — | **Đã code.** Form thêm |
| POST | `/admin/products/new` | `AdminProductServlet` | `name`, `description`, `price`, `stock`, `category`, `image` (`multipart/form-data`) | **Đã code.** Lỗi → vẽ lại form kèm lỗi từng ô; đúng → flash + 302 `/admin/products`. Ảnh: JPG/PNG/WEBP ≤ 2MB, nhận diện bằng byte đầu file, lưu ngoài webapp (`~/nongviet-uploads` hoặc biến môi trường `UPLOAD_DIR`), phục vụ qua `GET /uploads/<uuid>.<ext>` |
| GET | `/admin/products/edit` | `AdminProductServlet` | `id` | **Đã code.** Form sửa (id lạ → flash lỗi + 302 danh sách) |
| POST | `/admin/products/edit` | `AdminProductServlet` | `id` (query), các trường như thêm, `removeImage=on` | **Đã code.** Ảnh mới thay ảnh cũ (file cũ bị xoá) |
| POST | `/admin/products/delete` | `AdminProductServlet` | `id`, (kèm bộ lọc hiện tại), `redirect` (từ trang form) | **Đã code (htmx).** SP đã có trong đơn hàng → từ chối (toast lỗi, `HX-Reswap: none`); còn trong giỏ → gỡ khỏi giỏ rồi xoá; đánh giá xoá theo. Từ bảng: trả mảnh rỗng (dòng biến mất) + `#product-summary` cập nhật bằng `hx-swap-oob` |
| GET/POST | `/admin/categories` | `AdminCategoryServlet` | `action=create\|update\|delete`, `id`, `name`, `description` | Không xoá danh mục còn sản phẩm |
| GET | `/admin/categories` | `AdminCategoryServlet` | — | **Đã code.** Danh sách danh mục kèm số sản phẩm (bấm số → `/admin/products?cat=`) |
| GET/POST | `/admin/categories/new` | `AdminCategoryServlet` | `name` (≤ 100, không trùng, không phân biệt hoa thường), `description` (≤ 255) | **Đã code.** Lỗi → vẽ lại form kèm lỗi từng ô |
| GET/POST | `/admin/categories/edit` | `AdminCategoryServlet` | `id` (query) + các trường như thêm | **Đã code** |
| POST | `/admin/categories/delete` | `AdminCategoryServlet` | `id` | **Đã code.** Danh mục còn sản phẩm → từ chối (nút cũng bị vô hiệu); xoá xong redirect + toast |
| GET | `/admin/orders` | `AdminOrderServlet` | `status=all\|pending\|confirmed\|shipping\|completed\|cancelled\|return`, `q` (mã đơn/tên/email), `pay=cod\|vnpay`, `from`, `to` (yyyy-MM-dd), `page` (10 đơn/trang) | **Đã code.** Mọi đơn của mọi khách. Trang đầy đủ; request htmx có `HX-Target: order-list` (ô lọc, tab, phân trang) chỉ trả tab + bảng `fragments/admin-order-table.jsp`. Tab "return" gồm `RETURN_REQUESTED` + `RETURNED` |
| GET | `/admin/orders/detail` | `AdminOrderServlet` | `id` | **Đã code.** Chi tiết đơn (khách, dòng hàng, tiền, địa chỉ, thanh toán); id lạ → flash lỗi + 302 danh sách |
| POST | `/admin/orders/status` | `AdminOrderServlet` | `id`, `status` (tên `OrderStatus`: CONFIRMED, SHIPPING, COMPLETED, CANCELLED, RETURNED), `back` (chỉ nhận đường dẫn bắt đầu bằng `/admin/orders`) | **Đã code.** Luật chuyển trạng thái ở `Order.canMoveTo`; Admin không tự đặt `RETURN_REQUESTED`/`PENDING`. `COMPLETED` từ `RETURN_REQUESTED` = từ chối hoàn hàng; `RETURNED` = duyệt hoàn hàng. Huỷ/duyệt hoàn ⇒ cộng lại kho từng dòng hàng (huỷ còn trả 1 lượt voucher); giao xong ⇒ thanh toán COD `SUCCESS`; huỷ ⇒ thanh toán đang chờ `FAILED`; tạo `Notification` cho khách. Tất cả trong 1 transaction, khoá dòng đơn (`PESSIMISTIC_WRITE`) nên 2 Admin bấm cùng lúc không cộng kho 2 lần. Kết quả: flash + 302 về `back` |
| GET | `/admin/vouchers` | `AdminVoucherServlet` | `tab=all\|active\|soldout\|expired\|off` | **Đã code.** Danh sách + tab trạng thái (`Voucher.getState()`: tắt / hết hạn hoặc chưa tới hạn / hết lượt / đang dùng) |
| GET/POST | `/admin/vouchers/new` | `AdminVoucherServlet` | `code`, `type=percent\|fixed`, `value`, `min`, `issued`, `start`, `end`, `active=on` | **Đã code.** Mã chữ+số 3–20 ký tự (lưu IN HOA, không trùng, không phân biệt hoa thường); % ≤ 100; hết hạn ≥ bắt đầu (lưu 23:59:59 ngày cuối). Lỗi → vẽ lại form kèm lỗi từng ô |
| GET/POST | `/admin/vouchers/edit` | `AdminVoucherServlet` | `id` (query) + các trường như tạo | **Đã code.** Không giảm số lượng phát hành dưới số lượt đã dùng |
| POST | `/admin/vouchers/toggle` | `AdminVoucherServlet` | `id`, `active=true\|false` (trạng thái MONG MUỐN), `back` | **Đã code (htmx).** Bật/tắt; `back` chỉ nhận đường dẫn bắt đầu bằng `/admin/vouchers` |
| POST | `/admin/vouchers/delete` | `AdminVoucherServlet` | `id`, `back` | **Đã code.** Voucher đã có đơn dùng → từ chối (hãy tắt thay vì xoá) |
| GET | `/admin/users` | `AdminCustomerServlet` | `q` (tên/email/SĐT), `status=active\|locked`, `page` (10/trang) | **Đã code.** Danh sách khách (không gồm Admin/chatbot) kèm số đơn + tổng chi tiêu (không tính đơn huỷ). Request htmx có `HX-Target: customer-list` chỉ trả bảng |
| GET | `/admin/users/detail` | `AdminCustomerServlet` | `id` | **Đã code.** Hồ sơ + 3 số liệu + 5 đơn gần nhất (không có mật khẩu); id lạ → flash lỗi + 302 danh sách |
| POST | `/admin/users/lock` | `AdminCustomerServlet` | `id`, `locked=true\|false` (trạng thái MONG MUỐN), `back` | **Đã code.** Chỉ khoá được `Customer` (không khoá Admin/chatbot). Khách bị khoá: không đăng nhập được (`AuthService.login`, chỉ báo "bị khoá" SAU KHI đúng mật khẩu) và đang dùng thì bị đăng xuất ở lần gọi `requireCustomer` kế tiếp; đơn đang xử lý vẫn chạy bình thường |
| GET | `/admin/chat` | `AdminChatServlet` | `c` (id khách đang mở, mặc định hội thoại mới nhất), `filter=all\|human\|ai`, `q` (tên/email) | **Đã code.** 3 cột: danh sách hội thoại (mỗi khách 1 dòng = tin cuối; "Chờ" nếu tin cuối là của khách) / khung chat / thông tin khách + 3 đơn gần nhất |
| GET | `/admin/chat/list` | `AdminChatServlet` | `c`, `filter`, `q` | **Đã code (htmx).** Chỉ danh sách hội thoại, tự hỏi lại mỗi 5 giây (`hx-target="this"` để không thay nhầm `#adm-main`) và khi gõ ô tìm kiếm |
| GET | `/admin/chat/messages` | `AdminChatServlet` | `c`, `after` (id tin cuối đã có) | **Đã code (htmx).** Các tin mới hơn + thẻ `#poll` hỏi tiếp mỗi 3 giây (polling, không WebSocket) |
| POST | `/admin/chat/send` | `AdminChatServlet` | `c`, `content` (≤ 1000 ký tự), `after` | **Đã code (htmx).** Lưu tin với `sender` = Admin đang đăng nhập, tự chuyển hội thoại sang chế độ Nhân viên; trả tin mới + đầu khung chat cập nhật (`hx-swap-oob`) |
| POST | `/admin/chat/mode` | `AdminChatServlet` | `c`, `human=true\|false` (trạng thái MONG MUỐN) | **Đã code (htmx).** Bật/tắt chế độ nhân viên tiếp quản; trả lại đầu khung chat + toast |
| GET | `/admin/auto-replies` | `AdminAutoReplyServlet` | — | **Đã code.** Danh sách luật trả lời tự động của chatbot (ưu tiên nhỏ trước) + ô "Thử câu hỏi" |
| GET | `/admin/auto-replies/test` | `AdminAutoReplyServlet` | `text` | **Đã code (htmx).** Mô phỏng: luật nào khớp câu này và chatbot sẽ trả lời gì (dùng đúng `ChatBotService.match`) |
| GET/POST | `/admin/auto-replies/new` | `AdminAutoReplyServlet` | `keywords`, `replyText`, `priority`, `handoff=on`, `active=on` | **Đã code.** Từ khoá: 1–20 cái, mỗi cái ≥ 3 chữ/số và ≤ 40 ký tự, `*` phải đứng một mình; trả lời ≤ 1000 ký tự; ưu tiên 1–9999 (gợi ý = luật thường lớn nhất + 10). Lỗi → vẽ lại form kèm lỗi từng ô |
| GET/POST | `/admin/auto-replies/edit` | `AdminAutoReplyServlet` | `id` (query) + các trường như tạo | **Đã code** |
| POST | `/admin/auto-replies/toggle` | `AdminAutoReplyServlet` | `id`, `active=true\|false` (trạng thái MONG MUỐN), `back` | **Đã code (htmx).** Bật/tắt; `back` chỉ nhận đường dẫn bắt đầu bằng `/admin/auto-replies` |
| POST | `/admin/auto-replies/delete` | `AdminAutoReplyServlet` | `id`, `back` | **Đã code** |
| — | `/admin/reviews` | … | … | **Optional**, làm sau khi Core xong |

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
