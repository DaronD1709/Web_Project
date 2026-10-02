# Bối cảnh dự án — đọc trước khi code

Đồ án web ecommerce (Java Servlet/JSP, NetBeans), **chủ đề nông nghiệp** (hạt giống, phân bón,
thuốc BVTV, dụng cụ, máy móc & tưới tiêu, nông sản; tên shop giả lập: NôngViệt).
Team 3 người, mỗi người 1 máy/1 DB Postgres riêng; 2 thành viên năm 3 chưa biết React. Deadline ~14 ngày kể từ đầu tháng 10/2026,
có vấn đáp (oral exam) trên chính code — code càng dễ hiểu/giải thích được càng tốt,
tránh magic/abstraction không cần thiết.

## 1. Tech stack (đã chốt, không đổi nữa trừ khi người dùng yêu cầu)

| Layer | Công nghệ |
|---|---|
| Backend / View | Java Servlet + JSP, Maven, Tomcat 10.1 |
| ORM | JPA (`jakarta.persistence`, **không phải** `javax.persistence`) + Hibernate 6.5 |
| DB | PostgreSQL |
| CSS/UI | Tailwind CSS (CDN) + daisyUI (CDN) |
| Tương tác không reload trang | htmx (CDN) |
| Gửi email | Jakarta Mail (thư viện `angus-mail`) qua SMTP — người dùng đã yêu cầu thêm |
| Kiến trúc | 3 lớp: Controller (Servlet) → Service → DAO. KHÔNG REST API, KHÔNG React/SPA tách riêng |

Lý do bỏ React: team có 2 bạn chưa biết, nên giữ 1 codebase JSP duy nhất, không ai phải học công nghệ mới.
daisyUI + htmx thay thế vai trò shadcn/React để giao diện vẫn đẹp và mượt mà không cần build pipeline.

## 2. Tài liệu chi tiết trong repo

- `docs/feature-list.md` — toàn bộ chức năng, đánh dấu `[Core]`/`[Optional]`. Core = bắt buộc
  để có 1 web ecommerce chạy được; Optional = bonus, làm sau nếu còn thời gian.
- `docs/api-spec.md` — danh sách endpoint (Servlet) theo từng màn hình: URL, method, tham số, attr truyền cho JSP,
  quyền truy cập, rule nghiệp vụ, thứ tự code. **Không phải REST/JSON** — Servlet trả JSP hoặc htmx fragment.
  Khi thêm/đổi Servlet phải cập nhật file này.
- `docs/class-diagram.md` — thiết kế đầy đủ 22 class/entity, bảng quan hệ (loại + multiplicity + label),
  và mermaid source để vẽ lại nếu cần. **Đây là nguồn chân lý (source of truth) cho cấu trúc dữ liệu** —
  code trong `backend/src/main/java/com/ecommerce/entity/` phải khớp với file này.

## 3. Trạng thái hiện tại (đã code)

- `backend/pom.xml`, `persistence.xml`, `web.xml` — skeleton Maven đã chạy được.
- `backend/src/main/java/com/ecommerce/entity/` — đủ 22 entity + 4 enum, đúng theo class-diagram.md.
- `backend/src/main/java/com/ecommerce/dao/AbstractDAO.java` — generic CRUD dùng chung cho mọi entity.
- **Vertical slice mẫu chạy đầy đủ (đã test trên Tomcat + Postgres):** `GET /products` có tìm kiếm/lọc/sắp xếp/phân trang:
  `ProductServlet` (đọc tham số → `ProductFilter` DTO) → `ProductService.search` → `ProductDAO.search/countSearch`
  (JPQL động, mọi giá trị qua `setParameter`) → `WEB-INF/views/customer/products.jsp` (JSTL, `<c:out>` chống XSS).
  Copy đúng mẫu này cho các chức năng khác. Contract URL ở `docs/api-spec.md`.
- `dto/` — chỉ tạo khi thật sự cần: `ProductFilter` (gom tham số request), `PageResult<T>` (kết quả phân trang).
  **Không** tạo DTO cho mọi entity; JSP dùng thẳng entity. Lưu ý `AbstractDAO` đóng `EntityManager` sau mỗi lần gọi
  → JSP chỉ đọc được field thường và `@ManyToOne` (eager); collection lazy (`order.items`, `product.reviews`)
  phải được fetch trong DAO (`JOIN FETCH`) trước khi trả ra, nếu không bị `LazyInitializationException`.
- **Dữ liệu mẫu (thay cho mock API):** `util/DataSeeder` tự nạp danh mục, 12 sản phẩm, 1 admin, 1 khách (kèm giỏ hàng)
  khi app khởi động nếu bảng còn trống (gọi từ `listener/AppInitListener`). Tài khoản dev ghi trong comment của
  `DataSeeder`. Muốn nạp lại: xoá dữ liệu trong DB rồi restart app.
- **DB riêng mỗi máy:** copy `backend/src/main/resources/db.properties.example` → `db.properties` (đã `.gitignore`),
  `JPAUtil` đọc file này ghi đè url/user/password của `persistence.xml`.
- **Auth đã xong & test end-to-end (Tomcat + Postgres + SMTP giả):** `/register` (gửi email chào mừng), `/login`, `/logout` (chỉ POST),
  `/forgot-password`, `/reset-password` → `AuthService` + `RegisterServlet/LoginServlet/LogoutServlet/ForgotPasswordServlet/ResetPasswordServlet`
  + JSP `customer/{register,login,forgot-password,reset-password}.jsp`. Lỗi validate dùng `service/BusinessException` (1 thông báo chung
  hoặc `Map<ô, lỗi>`). Quên mật khẩu: `users.reset_token_hash/reset_token_expiry` (token gốc chỉ nằm trong link email, DB lưu SHA-256,
  hết hạn 30 phút, dùng 1 lần, email lạ/thật phản hồi giống hệt nhau). Login: cùng 1 thông báo cho sai email/sai mật khẩu, `next` chỉ nhận
  đường dẫn nội bộ, đổi session ID khi đăng nhập. **Chưa có** `AuthFilter`/`AdminFilter`.
- **web.xml có `<jsp-config>` ép UTF-8 cho `*.jsp`/`*.jspf` — KHÔNG xoá:** thiếu nó thì file `.jspf` (header/footer) bị lỗi font tiếng Việt.
- **Gửi email (Jakarta Mail / Angus Mail, đã test với SMTP giả):** `service/EmailService` có `sendWelcome(user)` (đăng ký),
  `sendPasswordReset(user, link)` (quên mật khẩu) và `sendOrderConfirmation(order)` (thanh toán xong — **chưa nối vì checkout chưa làm**).
  Gọi `sendOrderConfirmation` trong `OrderService.checkout()` **sau khi transaction đặt hàng đã commit**, truyền `Order` còn đủ items/address trong bộ nhớ
  (nội dung mail được dựng ngay trên thread gọi để tránh LazyInitializationException; chỉ phần gửi chạy nền, lỗi SMTP chỉ ghi log,
  KHÔNG làm hỏng đơn). Cấu hình ở `backend/src/main/resources/mail.properties` (đã `.gitignore`, mẫu `mail.properties.example`;
  Gmail cần App password). Thiếu file hoặc `mail.enabled=false` → chỉ in nội dung email ra log Tomcat, dev không cần SMTP.
  Khi tắt app, `AppInitListener.contextDestroyed` gọi `EmailService.shutdown()`. `app.base.url` trong `mail.properties` quyết định domain trong link email
  (nếu thiếu thì suy từ request — kém an toàn hơn vì header Host có thể bị giả).
- `util/PasswordUtil` (PBKDF2 có sẵn trong JDK, không thêm thư viện), `util/ParamUtil` (đọc tham số request an toàn).
- **Mockup giao diện Customer** (HTML tĩnh, Tailwind + daisyUI CDN, theme xanh lá `design.css`):
  `backend/src/main/webapp/mockup/` — 14 trang (chưa có mockup cho forgot/reset password; JSP thật đã có; home, products, product-detail, login, register, cart,
  checkout, order-success, orders, order-detail, profile, addresses, notifications, chat) + `index.html`
  làm mục lục. Xem thử: mở `mockup/index.html` hoặc `python3 -m http.server` trong thư mục đó.
  `layout.js` chứa header/footer/product card + dữ liệu mẫu (chỉ để xem thử). **Chưa vẽ Admin.**
  Dùng làm bản thiết kế khi chuyển sang JSP; không phải code chạy thật.
- **Mockup giao diện Admin (CMS)** — nhánh `feat/admin-ui`: `backend/src/main/webapp/mockup/admin/` — 14 trang (login, dashboard, statistics, products,
  product-form kèm upload ảnh, categories, orders, order-detail, vouchers, voucher-form, customers, reviews, chat) + `index.html`. `admin.js` chứa khung
  (sidebar/header), dữ liệu mẫu, biểu đồ SVG thuần (cột ≤ 24px, 1 màu, tooltip, nút "Xem dạng bảng"), toast và hộp thoại xác nhận. Đã kiểm tra desktop + mobile.
  Rule hiển thị trong mockup cần giữ khi làm JSP: huỷ đơn/duyệt hoàn hàng ⇒ cộng lại kho; không xoá danh mục còn sản phẩm; voucher có 4 trạng thái
  (hoạt động/hết hạn/hết lượt/tắt) khớp `Voucher.isValid()`; khoá tài khoản khách cần thêm cột `users.is_active` (chưa có trong entity); chat có công tắc
  nhân viên tiếp quản (cần field `isHandledByHuman` của `Conversation`, chưa có).
- Thư mục view: `webapp/WEB-INF/views/{customer,admin,common}/` (đã có `products.jsp` và `common/{head,header,footer}.jspf`); CSS dùng chung ở `webapp/static/css/design.css`.
- Repo: https://github.com/DaronD1709/Web_Project (public).

### Quy ước thư mục view/asset
- **JSP thật** → `WEB-INF/views/...` (không truy cập trực tiếp bằng URL; Servlet `forward` tới).
  Header/footer dùng chung → `WEB-INF/views/common/*.jspf`.
- **CSS/JS/ảnh tĩnh** → `webapp/static/...` (NGOÀI `WEB-INF`, trình duyệt phải tải được).

## 4. Việc còn lại (TODO) — ưu tiên theo feature-list.md phần [Core] trước

- [ ] DAO cho 21 entity còn lại — copy đúng mẫu `ProductDAO` (`extends AbstractDAO<Xxx, Integer>`,
      constructor `super(Xxx.class)`, chỉ thêm method riêng nếu cần query đặc thù).
- [ ] Service cho từng nhóm nghiệp vụ (xem mục 5 — nhiều rule nghiệp vụ phải nằm ở đây, không phải DAO/Servlet).
- [x] Auth: đăng ký/login/logout/quên mật khẩu (xong). [ ] `AuthFilter` (chặn trang cần đăng nhập, chuyển `/login?next=`) và `AdminFilter`.
- [ ] Servlet cho các luồng Core còn lại: giỏ hàng (add/view/update/remove) — **badge giỏ hàng cập nhật tức thì bằng htmx**,
      checkout (**gọi `EmailService.sendOrderConfirmation` sau khi commit**), order history, track order status,
      Admin CRUD product/category, Admin quản lý đơn hàng.
- [ ] JSP views tương ứng từng Servlet ở trên (copy mẫu `WEB-INF/views/customer/products.jsp`: Tailwind + daisyUI + htmx qua `common/head.jspf`).
- [ ] Chuyển mockup (`webapp/mockup/`) thành JSP thật theo từng nhóm chức năng (cùng lúc với Servlet/Service),
      mỗi nhóm 1 commit; Auth và danh sách sản phẩm đã xong, tiếp theo là giỏ hàng → checkout.
- [x] Mockup Admin (nhánh `feat/admin-ui`). [ ] Mockup forgot/reset password.
- [ ] Đóng `EntityManagerFactory` khi app dừng (`contextDestroyed`) để không rò kết nối khi redeploy.
- [ ] (Khi cần deploy) cho `JPAUtil`/`EmailService` đọc thêm biến môi trường để chạy trong Docker/cloud, không cần file `.properties`.
- [ ] Phần Optional (Voucher, Review, Notification, Chat, AIBot, Return/Refund) — chỉ làm sau khi Core xong hết.

## 5. Nghiệp vụ quan trọng đã bàn kỹ khi thiết kế — PHẢI nhớ khi viết Service (phần Auth/email đã code; còn lại chưa code, chỉ mới quyết định)

- **Email:** 3 email Core — chào mừng khi đăng ký, đặt lại mật khẩu, xác nhận đơn hàng khi thanh toán xong. Dựng nội dung trên thread gọi,
  gửi nền, lỗi gửi chỉ ghi log (không rollback đăng ký/đơn). Với VNPay gửi sau khi thanh toán thành công, COD gửi ngay khi đặt hàng.
- **Quên mật khẩu:** token chỉ nằm trong link email, DB lưu SHA-256 + hết hạn 30 phút, dùng 1 lần, luôn phản hồi như nhau dù email có tồn tại hay không.

- **Trừ/hoàn tồn kho:** `Cart.checkout()` phải validate đủ `stockQuantity` trước khi cho đặt, rồi trừ kho
  ngay lúc checkout (không đợi admin confirm). Khi đơn bị `CANCELLED` hoặc `RETURNED`, phải cộng lại kho
  cho từng `OrderItem` (`Product.updateStock(+qty)`) — dễ quên, gây bug bán vượt tồn kho khi demo.
- **Luồng OrderStatus hợp lệ** (validate trong `Order.updateStatus()`, đừng để ai set tuỳ tiện):
  `PENDING → CONFIRMED → SHIPPING → COMPLETED`, nhánh `PENDING/CONFIRMED → CANCELLED`, nhánh
  `COMPLETED → RETURN_REQUESTED → RETURNED` (hoặc Admin từ chối thì quay lại `COMPLETED`).
- **Quyền huỷ đơn:** Customer chỉ huỷ được đơn **của chính mình** (check `order.customer.id == currentUser.id`),
  và chỉ khi status đang `PENDING`/`CONFIRMED`. Admin huỷ được đơn bất kỳ.
- **Điều kiện viết Review:** chỉ cho `Customer.writeReview()` nếu khách có ít nhất 1 Order `COMPLETED`
  hoặc `RETURNED` chứa đúng Product đó — đây là business rule check ở Service, KHÔNG phải ràng buộc DB/entity.
- **Voucher.isValid():** gộp đúng 3 điều kiện (còn hạn `startDate`/`endDate`, `isActive`,
  `quantityUsed < quantityIssued`) vào đúng 1 chỗ, gọi khi áp dụng voucher lúc checkout — tránh if-else rải rác.
- **Notification:** tạo tự động mỗi khi `Order.updateStatus()` được gọi (ví dụ trong `NotificationService`
  gọi kèm theo `OrderService.updateStatus()`).
- **AIBot:** `generateReply()` là nơi gọi API AI ngoài thật (OpenAI/Anthropic...) — cần quyết định khi nào
  AI tự trả lời vs khi nào để Admin trả lời tay (nếu cần, thêm field `isHandledByHuman : boolean` vào
  `Conversation`, hiện chưa có).
- **Không có class `Account` riêng:** Admin/Customer/AIBot đều là `User` — "khoá/mở tài khoản" chỉ cần
  thêm field `isActive : boolean` vào `User` (chưa có trong entity hiện tại, cần thêm nếu làm tính năng này).

## 6. Quy ước code khi mở rộng

- Luôn dùng `jakarta.*`, không dùng `javax.*` (bắt buộc vì Tomcat 10.1).
- Servlet KHÔNG gọi thẳng DAO — luôn qua Service, để validate/rule nằm đúng 1 chỗ.
- Composition trong class diagram → JPA `cascade = CascadeType.ALL, orphanRemoval = true`.
  Aggregation → `@ManyToOne`/`@OneToMany` trơn, không cascade (vd `CartItem → Product`).
- `User` và `Payment` dùng `@Inheritance(SINGLE_TABLE)` — Customer/Admin/AIBot chung bảng `users`,
  CODPayment/VNPayPayment chung bảng `payments`, phân biệt qua cột discriminator.
- Không thêm thư viện/framework mới (không React, không REST framework) trừ khi người dùng yêu cầu rõ —
  giữ đúng tinh thần "đơn giản, cả team làm được, giải thích được khi vấn đáp".

## 7. Quy ước commit

- Mọi commit theo **Conventional Commits**: `type(scope): subject`, ví dụ `feat(auth): add login servlet`,
  `fix(cart): ...`, `docs: ...`, `chore(db): ...`, `refactor`, `test`, `style`.
- KHÔNG thêm dòng `Co-Authored-By: Claude` hay bất kỳ attribution nào của Claude vào commit/PR
  (người dùng đã yêu cầu gỡ khỏi lịch sử).
- Mỗi nhóm chức năng 1 commit riêng; không commit mật khẩu DB thật (repo public).

## 8. Deploy (BẮT BUỘC theo scope đồ án — người dùng sẽ mua 1 tên miền rẻ; chưa chốt nơi chạy)

App là **WAR chạy trên Tomcat + PostgreSQL**. Firebase Hosting và Cloudflare Workers/Pages **không chạy được WAR/Tomcat trực tiếp**
(Firestore là NoSQL, không dùng được với JPA/Hibernate này). Hướng khả thi nếu cần deploy: đóng gói Docker (Tomcat 10.1 + WAR) chạy trên
dịch vụ chạy container (Google Cloud Run — đứng sau Firebase Hosting được; Render/Railway/Fly.io; hoặc Cloudflare Containers nếu còn khả dụng)
+ Postgres managed (Neon, Supabase, Cloud SQL…). Gửi email khi deploy: dùng SMTP bình thường; Cloudflare Email Service (REST API, cần domain đã
onboard vào Cloudflare) chỉ là phương án thay thế, không dùng SMTP. Dev: mỗi máy dùng Postgres local riêng (`db.properties`). Production: 1 DB duy nhất + 1 container Tomcat, trỏ tên miền vào đó, bật HTTPS.
Việc cần làm khi tới bước deploy (sau khi xong Core): `Dockerfile` (Tomcat 10.1 + WAR), cho `JPAUtil`/`EmailService` đọc cấu hình từ biến môi trường,
đổi `hibernate.hbm2ddl.auto` sang `validate`/`none` (hoặc giữ `update` có chủ đích), tắt/đổi tài khoản của `DataSeeder`, đặt `app.base.url` = https://tên-miền,
bỏ `hibernate.show_sql`, cấu hình SMTP thật.

## 9. Phân công (theo use case diagram; mỗi use case 1 nhánh `feat/<tên>` tách từ `dev`)

| Người | Use case → nhánh |
|---|---|
| **Hữu Danh** (Admin + Chat) | Manage Product → `feat/admin-manage-product` · Manage Voucher → `feat/admin-manage-voucher` · Login/Logout (admin) → `feat/admin-login-logout` · Manage Order → `feat/admin-manage-order` · Manage Customer Account → `feat/admin-manage-customer-account` · Chat with Shop → `feat/chat-with-shop` |
| **Lộc** (Registered Customer) | Login/Logout → `feat/login-logout` · Manage Profile → `feat/manage-profile` · View Order History → `feat/view-order-history` · Manage Shopping Cart → `feat/manage-shopping-cart` · Place Order & Pay → `feat/place-order-pay` · Track Order Status → `feat/track-order-status` |
| **Thang** (Unregistered Customer) | Register → `feat/register` · View Products by Category → `feat/view-products-by-category` · Search Product → `feat/search-product` · View Product Detail → `feat/view-product-detail` · Manage Address Shipping → `feat/manage-address-shipping` |
| *Chưa phân* | `feat/auth-filter` (AuthFilter/AdminFilter), `feat/product-review`, `feat/notifications`, `feat/admin-dashboard`, `feat/deploy-docker` |

Lưu ý khi làm song song:
- **Đã có sẵn trên `dev`:** Register, Login/Logout, quên mật khẩu, và `/products` (tìm kiếm/lọc/sắp xếp/phân trang). Nhánh `register`, `login-logout`, `search-product` chủ yếu là chỉnh sửa/bổ sung, không viết lại từ đầu.
- **Phụ thuộc:** Place Order & Pay (Lộc) cần Address (Thang) và Cart (Lộc); Track Order Status cần Order từ Place Order & Pay. Thống nhất sớm tên method của `AddressService`/`CartService`.
- **Dễ xung đột file:** `LoginServlet`/`AuthService` (login-logout của Lộc vs admin-login-logout của Danh); `OrderService` (place-order-pay, track-order-status, view-order-history, admin-manage-order). Commit nhỏ, merge `dev` vào nhánh mình thường xuyên.
- Clone: `git clone -b dev https://github.com/DaronD1709/Web_Project.git`. Cập nhật `docs/api-spec.md` khi thêm/đổi Servlet.
