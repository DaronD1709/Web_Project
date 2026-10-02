# Bối cảnh dự án — đọc trước khi code

Đồ án web ecommerce (Java Servlet/JSP, NetBeans), chủ đề thuỷ sinh/nông nghiệp/bánh.
Team có 2 thành viên năm 3 chưa biết React. Deadline ~14 ngày kể từ đầu tháng 10/2026,
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
| Kiến trúc | 3 lớp: Controller (Servlet) → Service → DAO. KHÔNG REST API, KHÔNG React/SPA tách riêng |

Lý do bỏ React: team có 2 bạn chưa biết, nên giữ 1 codebase JSP duy nhất, không ai phải học công nghệ mới.
daisyUI + htmx thay thế vai trò shadcn/React để giao diện vẫn đẹp và mượt mà không cần build pipeline.

## 2. Tài liệu chi tiết trong repo

- `docs/feature-list.md` — toàn bộ chức năng, đánh dấu `[Core]`/`[Optional]`. Core = bắt buộc
  để có 1 web ecommerce chạy được; Optional = bonus, làm sau nếu còn thời gian.
- `docs/class-diagram.md` — thiết kế đầy đủ 22 class/entity, bảng quan hệ (loại + multiplicity + label),
  và mermaid source để vẽ lại nếu cần. **Đây là nguồn chân lý (source of truth) cho cấu trúc dữ liệu** —
  code trong `backend/src/main/java/com/ecommerce/entity/` phải khớp với file này.

## 3. Trạng thái hiện tại (đã code)

- `backend/pom.xml`, `persistence.xml`, `web.xml` — skeleton Maven đã chạy được.
- `backend/src/main/java/com/ecommerce/entity/` — đủ 22 entity + 4 enum, đúng theo class-diagram.md.
- `backend/src/main/java/com/ecommerce/dao/AbstractDAO.java` — generic CRUD dùng chung cho mọi entity.
- 1 vertical slice mẫu chạy đầy đủ: `ProductDAO` → `ProductService` → `ProductServlet` (`/products`)
  → `sample-product-card.jsp` (có sẵn htmx + daisyUI).

## 4. Việc còn lại (TODO) — ưu tiên theo feature-list.md phần [Core] trước

- [ ] DAO cho 21 entity còn lại — copy đúng mẫu `ProductDAO` (`extends AbstractDAO<Xxx, Integer>`,
      constructor `super(Xxx.class)`, chỉ thêm method riêng nếu cần query đặc thù).
- [ ] Service cho từng nhóm nghiệp vụ (xem mục 5 — nhiều rule nghiệp vụ phải nằm ở đây, không phải DAO/Servlet).
- [ ] Servlet cho các luồng Core: đăng ký/login/logout, giỏ hàng (add/view/update/remove),
      checkout, order history, track order status, Admin CRUD product/category, Admin quản lý đơn hàng.
- [ ] JSP views tương ứng từng Servlet ở trên (dùng Tailwind + daisyUI + htmx như `sample-product-card.jsp`).
- [ ] Tạo DB PostgreSQL thật, sửa user/password trong `persistence.xml` (hiện là placeholder).
- [ ] Phần Optional (Voucher, Review, Notification, Chat, AIBot, Return/Refund) — chỉ làm sau khi Core xong hết.

## 5. Nghiệp vụ quan trọng đã bàn kỹ khi thiết kế — PHẢI nhớ khi viết Service (chưa code, chỉ mới quyết định)

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
