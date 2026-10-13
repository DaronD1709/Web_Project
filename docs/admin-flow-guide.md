# Hướng dẫn đọc code Admin — theo tính năng, từ giao diện xuống database

Tài liệu để **tự đọc code và trả lời vấn đáp**. Mỗi tính năng có: các file Backend (BE) và Frontend (FE), **thứ tự đọc**, và **luồng chạy theo từng thao tác**
(bấm gì trên giao diện → request nào → Servlet nào → Service → DAO → trả về JSP nào → hiện ở đâu).

> Đường dẫn viết tắt:
> - `BE/` = `backend/src/main/java/com/ecommerce/`
> - `FE/` = `backend/src/main/webapp/WEB-INF/views/admin/`
> - `JS/` = `backend/src/main/webapp/static/js/`

---

## 0. Bức tranh chung

```
Trình duyệt ──(1) request──▶ Servlet (controller/admin/*)       đọc tham số, gọi Service, chọn JSP
                                  │
                                  ▼
                            Service (service/Admin*Service)     LUẬT NGHIỆP VỤ + kiểm tra dữ liệu (ném BusinessException)
                                  │
                                  ▼
                              DAO (dao/*DAO)                    chỉ nói chuyện với DB (JPQL / transaction)
                                  │
                                  ▼
                         Entity (entity/*) ◀──▶ PostgreSQL       Hibernate/JPA ánh xạ class ↔ bảng
                                  │
Trình duyệt ◀─(2) HTML ── JSP (WEB-INF/views/admin/*.jsp)       Servlet forward sang JSP, JSP in HTML (EL + JSTL)
```

Quy tắc dự án: **Servlet không gọi thẳng DAO**, luôn qua Service. Mọi dữ liệu người dùng in ra JSP đều qua `<c:out>` (chống XSS).

**3 kiểu phản hồi lặp lại ở mọi tính năng** (hiểu 3 kiểu này là hiểu 80% Admin):

| Kiểu | Khi nào | Servlet trả |
|---|---|---|
| **Cả trang hoặc chỉ nội dung** (`AdminView.render`) | Mở trang | Gõ URL/F5/Back → cả khung (header + sidebar + nội dung). Bấm link trong CMS (htmx) → chỉ phần nội dung |
| **Mảnh HTML** (fragment) | Lọc/tìm/phân trang/polling | `forward` thẳng sang `FE/fragments/*.jsp`, htmx chỉ thay 1 vùng |
| **POST → Redirect → GET (PRG)** | Lưu/xoá/đổi trạng thái | Xử lý xong `sendRedirect` + **flash** (thông báo 1 lần qua session) → F5 không gửi lại form |

---

## 1. Nền tảng chung — ĐỌC TRƯỚC (áp dụng cho cả 9 tính năng)

### Thứ tự đọc

| # | File | Vai trò |
|---|---|---|
| 1 | `BE/util/SessionUtil.java` | `currentUser`, **`requireAdmin`** (chặn `/admin/*`: chưa đăng nhập → `/admin/login?next=…`, là khách → 403) |
| 2 | `BE/util/AdminView.java` | `render(...)` chọn trả cả trang hay chỉ nội dung; `flash(...)` thông báo sau redirect |
| 3 | `FE/admin-layout.jsp` | Khung CMS: **nhánh A** (htmx → chỉ nội dung + title/nav bằng `hx-swap-oob`), **nhánh B** (cả trang) |
| 4 | `FE/common/admin-header.jspf`, `admin-sidebar.jspf`, `admin-nav.jspf`, `admin-title.jspf` | Thanh trên (nút Đăng xuất), menu trái (mục đang chọn theo `adminActive`), tiêu đề |
| 5 | `BE/util/HtmxUtil.java` | `isHtmx`, `toast` (header `HX-Trigger`), `redirect` (header `HX-Redirect`) |
| 6 | `JS/admin.js` | Hiện toast, xem trước sản phẩm/voucher, cuộn khung chat, nút "Xem dạng bảng" của biểu đồ |
| 7 | `BE/util/ParamUtil.java` | Đọc tham số an toàn (`intOrNull`, `intOr`, `dateOrNull`, `trimOrNull`): sai định dạng → `null`, không văng lỗi 500 |
| 8 | `BE/service/BusinessException.java` | Lỗi nghiệp vụ: 1 thông báo chung hoặc `Map<tên ô, lỗi>` để form in lỗi dưới từng ô |
| 9 | `BE/dao/AbstractDAO.java` + `BE/util/JPAUtil.java` | CRUD chung (`findById/findAll/save/update/deleteById/count`), `EntityManagerFactory` dùng chung |

### 1.1 Luồng "mở một trang Admin" (mọi tính năng đều qua đây)

```
Bấm mục sidebar (hx-boost) ─▶ GET /admin/xxx   (header HX-Request: true)
   Servlet.doGet ─▶ SessionUtil.requireAdmin ─▶ (gọi Service lấy dữ liệu, setAttribute)
                 ─▶ AdminView.render(req, resp, "admin-xxx-list.jsp", "Tiêu đề", "xxx")
                       ├─ HX-Request có & không phải Back  ─▶ partial = true
                       └─ gõ URL / F5 / Back               ─▶ partial = false
                 ─▶ forward admin-layout.jsp
                       ├─ partial: chỉ <jsp:include nội dung> + title + nav (oob)  ─▶ htmx nhét vào #adm-main
                       └─ không partial: cả <html>, header, sidebar, #adm-main
```
`hx-boost="true"` đặt ở `<div class="drawer">` của layout: **mọi link/form trong CMS tự thành request htmx** và kết quả thay vào `#adm-main`.

### 1.2 Luồng "lưu / xoá / đổi trạng thái" (PRG + flash)

```
Form POST ─▶ Servlet.doPost ─▶ requireAdmin ─▶ Service (kiểm tra + ghi DB)
   thành công: AdminView.flash(req, "Đã lưu…", "success") ─▶ sendRedirect(trang danh sách)
   → trình duyệt GET danh sách ─▶ AdminView.render lấy flash ra ─▶ hiện toast 1 lần, xoá khỏi session
   lỗi nghiệp vụ (BusinessException): ở lại form, in lỗi dưới từng ô (giữ giá trị vừa nhập = "draft")
```

### 1.3 Luồng "lọc / tìm / phân trang bằng htmx"

```
Gõ ô tìm / đổi <select> ─▶ htmx GET /admin/xxx?q=…  (header HX-Target: xxx-list)
   Servlet.showList: đọc bộ lọc (ParamUtil) ─▶ Service.search ─▶ DAO (JPQL động, mọi giá trị qua setParameter)
      nếu HX-Target = "xxx-list" ─▶ forward fragments/admin-xxx-table.jsp (chỉ cái bảng)
      ngược lại                    ─▶ AdminView.render(cả trang)
   htmx thay đúng vùng #xxx-list, đổi URL (hx-push-url)
```
Servlet phân biệt bằng header **`HX-Target`**; thêm `Vary: HX-Request, HX-Target` để cache không lẫn 2 loại nội dung.

---

## 2. Đăng nhập / Đăng xuất Admin

**Làm gì:** trang đăng nhập riêng cho quản trị; chỉ tài khoản loại `Admin` mới vào được CMS; đăng xuất bằng POST.

| | File |
|---|---|
| BE | `controller/admin/AdminLoginServlet.java` (`/admin/login`), `AdminLogoutServlet.java` (`/admin/logout`), `service/AuthService.java` (`login`), `dao/UserDAO.java` (`findByEmail`), `util/PasswordUtil.java` (`verify`), `util/SessionUtil.java`, `entity/User.java`, `entity/Admin.java` |
| FE | `FE/admin-login.jsp` (trang riêng, không có header/sidebar), `FE/common/admin-header.jspf` (nút Đăng xuất) |

**Thứ tự đọc:** `admin-login.jsp` → `AdminLoginServlet` → `AuthService.login` → `UserDAO.findByEmail` + `PasswordUtil.verify` → `SessionUtil.requireAdmin` → `AdminLogoutServlet`.

**Luồng đăng nhập**
```
Gõ /admin (chưa đăng nhập) ─▶ requireAdmin ─▶ 302 /admin/login?next=/admin
GET /admin/login ─▶ AdminLoginServlet.doGet ─▶ forward admin-login.jsp (đã là Admin thì chuyển thẳng vào CMS)
Bấm "Đăng nhập" (form POST email, password, next)
  ─▶ AdminLoginServlet.doPost
       1. AuthService.login(email, pass)
            · UserDAO.findByEmail  · PasswordUtil.verify (PBKDF2)  · từ chối khách vãng lai và tài khoản bị khoá
            · sai email hay sai mật khẩu đều báo CÙNG 1 câu "Email hoặc mật khẩu không đúng."
       2. kết quả không phải `Admin` ─▶ "Tài khoản này không có quyền quản trị."
       3. hợp lệ: invalidate session cũ + tạo session MỚI (chống session fixation), lưu user vào session
       4. redirect tới safeNext(next): chỉ nhận đường dẫn bắt đầu "/admin" (chống open redirect), mặc định /admin
  lỗi ─▶ forward lại admin-login.jsp kèm error, giữ email (không giữ mật khẩu)
```
**Đăng xuất:** nút trong `admin-header.jspf` là `<form method=post action=/admin/logout hx-boost=false>` → `AdminLogoutServlet` huỷ session → redirect `/admin/login`. (Chỉ POST để link ảnh/CSRF không đăng xuất hộ.)

---

## 3. Tổng quan (Dashboard)

**Làm gì:** thẻ "việc cần xử lý", 4 thẻ số liệu (doanh thu hôm nay so với hôm qua, đơn chờ xác nhận, đơn đang giao, sản phẩm sắp hết), biểu đồ doanh thu 14 ngày, đơn theo trạng thái, 6 đơn mới nhất, hàng sắp hết, top bán chạy. **Chỉ đọc**, không có form.

| | File |
|---|---|
| BE | `controller/admin/AdminDashboardServlet.java` (`/admin`), `service/AdminStatsService.java`, `dao/StatsDAO.java`, `dao/OrderDAO.java` (`countByStatus`), `service/AdminOrderService.java` (`search` lấy đơn mới nhất), `service/AdminChatService.java` (`conversations`, đếm khách chờ chat), `dto/ChartPoint.java`, `dto/ChartData.java`, `dto/TopProduct.java` |
| FE | `FE/admin-dashboard.jsp`, `FE/common/admin-chart-columns.jspf` (biểu đồ cột HTML/CSS), `FE/common/admin-chart-bars.jspf` (thanh ngang), `FE/common/admin-order-badge.jspf`, `JS/admin.js` (`nvToggleChartTable`) |

**Thứ tự đọc:** `AdminDashboardServlet` → `AdminStatsService` → `StatsDAO` → `admin-dashboard.jsp` → `admin-chart-columns.jspf`.

**Luồng**
```
Bấm "Tổng quan" ─▶ GET /admin ─▶ AdminDashboardServlet.doGet (requireAdmin)
   gom dữ liệu rồi setAttribute:
     pending, returnRequests      ◀ AdminStatsService.countStatus ◀ OrderDAO.countByStatus (1 query GROUP BY)
     chatWaiting                  ◀ AdminChatService.conversations: đếm hội thoại có tin cuối là của khách
     todayRevenue, revenueDelta   ◀ todayAndYesterday() + deltaPercent()   ◀ StatsDAO.revenueRows
     shipping, lowCount           ◀ countStatus(SHIPPING), countLowStock()  ◀ StatsDAO.countLowStock
     revenueChart (14 ngày)       ◀ revenueByDay(14): điền 0 cho ngày trống, gom theo ngày bằng Java
     statusChart, recentOrders, lowStock, topChart(30 ngày, 6 món)
   ─▶ AdminView.render("admin-dashboard.jsp", "Tổng quan", "dashboard")
JSP vẽ biểu đồ bằng HTML/CSS thuần (không thư viện JS); nút "Xem dạng bảng" bật/tắt bảng ẩn (nvToggleChartTable).
```
**Quy tắc quan trọng:** doanh thu chỉ tính đơn `PENDING/CONFIRMED/SHIPPING/COMPLETED` (`StatsDAO.COUNTED`), **không tính huỷ/hoàn hàng**; tiền tính theo `priceAtOrder` (giá lúc đặt).

---

## 4. Quản lý sản phẩm

**Làm gì:** danh sách có tìm/lọc/sắp xếp/phân trang; thêm, sửa (có tải ảnh), xoá.

| | File |
|---|---|
| BE | `controller/admin/AdminProductServlet.java` (`/admin/products`, `/new`, `/edit`, `/delete`, có `@MultipartConfig`), `service/AdminProductService.java` (`save`, `delete`), `service/ProductService.java` (`search`, `getProductById`), `service/CategoryService.java`, `dao/ProductDAO.java` (`search`, `countSearch`, `countOrderItems`, `deleteWithCartItems`), `dto/ProductFilter.java`, `dto/PageResult.java`, `util/UploadUtil.java` (lưu ảnh), `controller/ImageServlet.java` (phục vụ `/uploads/*`), `entity/Product.java`, `entity/Category.java` |
| FE | `FE/admin-product-list.jsp`, `FE/fragments/admin-product-table.jsp` (bảng + phân trang), `FE/fragments/admin-product-deleted.jsp` (mảnh trả sau khi xoá), `FE/admin-product-form.jsp` (thêm/sửa + xem trước), `JS/admin.js` (`nvProductPreview`) |

**Thứ tự đọc:** `admin-product-list.jsp` → `AdminProductServlet` (`showList`) → `ProductService.search` → `ProductDAO.search` → `admin-product-table.jsp` → `admin-product-form.jsp` → `AdminProductServlet.save` → `AdminProductService.save` → `UploadUtil` → `AdminProductService.delete`.

**Luồng 1 – xem & lọc danh sách**
```
Bấm "Sản phẩm" ─▶ GET /admin/products ─▶ showList: readFilter (q, cat, stock, sort, page → ProductFilter)
      ─▶ ProductService.search ─▶ ProductDAO.search + countSearch (JPQL động, setParameter)
      ─▶ AdminView.render("admin-product-list.jsp")  [include fragments/admin-product-table.jsp vào #product-list]
Gõ ô tìm (delay 300ms) / đổi select / phân trang ─▶ htmx GET /admin/products?… (HX-Target: product-list)
      ─▶ showList thấy HX-Target ─▶ forward CHỈ admin-product-table.jsp ─▶ thay #product-list
```
**Luồng 2 – thêm / sửa**
```
"Thêm sản phẩm" ─▶ GET /admin/products/new ─▶ draft rỗng ─▶ showForm ─▶ admin-product-form.jsp
"Sửa" ─▶ GET /admin/products/edit?id=… ─▶ nạp Product thành draft (không tồn tại → flash lỗi + redirect danh sách)
Gõ vào form ─▶ admin.js nvProductPreview cập nhật thẻ xem trước (tên, giá, tồn kho, ảnh vừa chọn) — thuần JS
Bấm "Lưu" (form POST multipart) ─▶ AdminProductServlet.save
     đọc name, description, price, stock, category, image (Part), removeImage
     ─▶ AdminProductService.save(...)
          1. kiểm tra từng ô → gom vào Map lỗi (tên, giá 0–1 tỷ, tồn kho ≥ 0, danh mục có thật, ảnh ≤ 2MB và đúng JPG/PNG/WEBP theo BYTE ĐẦU file)
          2. có lỗi → ném BusinessException(Map)  ← chưa ghi gì xuống DB/ổ đĩa
          3. lưu ảnh mới (UploadUtil.store → ~/nongviet-uploads/<uuid>.ext), gán giá trị, productDAO.save/update
          4. DB lỗi → xoá ảnh vừa lưu; thành công → mới xoá ảnh cũ
     thành công: flash "Đã thêm/cập nhật sản phẩm" ─▶ redirect /admin/products (PRG)
     lỗi:        dựng draft từ giá trị vừa nhập + errors ─▶ showForm (lỗi in dưới từng ô)
Ảnh hiển thị: <img src="${ctx}/uploads/…"> ─▶ ImageServlet đọc file ngoài webapp (có chống path traversal)
```
**Luồng 3 – xoá**
```
Bấm "Xoá" (hx-confirm) ─▶ POST /admin/products/delete ─▶ AdminProductServlet.delete ─▶ AdminProductService.delete
     · đã nằm trong đơn hàng (countOrderItems > 0) ─▶ từ chối ("hãy đặt tồn kho = 0")
     · còn lại: deleteWithCartItems (gỡ khỏi giỏ + xoá đánh giá) + xoá file ảnh
     Trả lời theo nơi bấm: (a) không htmx → flash + redirect; (b) nút trong form (có `redirect`) → HX-Redirect;
                           (c) nút trong bảng → trả mảnh admin-product-deleted.jsp (dòng biến mất + cập nhật "Hiển thị a–b / tổng")
     Bị từ chối: toast lỗi + HX-Reswap none (giữ nguyên dòng)
```

---

## 5. Quản lý danh mục

| | File |
|---|---|
| BE | `controller/admin/AdminCategoryServlet.java` (`/admin/categories`, `/new`, `/edit`, `/delete`), `service/AdminCategoryService.java`, `dao/CategoryDAO.java` (`findAllOrdered`, `countProducts()`, `findByName`, `countProducts(id)`), `entity/Category.java` |
| FE | `FE/admin-category-list.jsp`, `FE/admin-category-form.jsp` |

**Thứ tự đọc:** `admin-category-list.jsp` → `AdminCategoryServlet` → `AdminCategoryService` → `CategoryDAO`.

```
Bấm "Danh mục" ─▶ GET /admin/categories ─▶ AdminCategoryService.list() + productCounts() (1 query gộp COUNT, tránh N+1)
                 ─▶ AdminView.render("admin-category-list.jsp")
"Thêm"/"Sửa" ─▶ GET /new | /edit?id ─▶ admin-category-form.jsp
"Lưu" (form POST) ─▶ AdminCategoryServlet.save ─▶ AdminCategoryService.save
      tên bắt buộc ≤100, KHÔNG trùng (không phân biệt hoa thường, gộp khoảng trắng), mô tả ≤255
      thành công: flash + redirect /admin/categories;  lỗi: về form, in lỗi dưới ô
"Xoá" (hx-confirm) ─▶ POST /admin/categories/delete ─▶ AdminCategoryService.delete
      còn sản phẩm (countProducts > 0) ─▶ từ chối: "chuyển hoặc xoá sản phẩm trước" (nút Xoá cũng bị ẩn trên giao diện)
```
Danh mục mới tự xuất hiện ở ô chọn của form sản phẩm và bộ lọc trang khách (cùng đọc từ `CategoryService`).

---

## 6. Quản lý đơn hàng

**Làm gì:** danh sách theo tab trạng thái + tìm + lọc thanh toán/ngày; xem chi tiết; **đổi trạng thái** (xác nhận → giao → hoàn tất, huỷ, duyệt/từ chối hoàn hàng).

| | File |
|---|---|
| BE | `controller/admin/AdminOrderServlet.java` (`/admin/orders`, `/detail`, `/status`), `service/AdminOrderService.java` (`search`, `tabCounts`, `countItems`, `getDetail`, `changeStatus`), `dao/OrderDAO.java` (`search`, `countSearch`, `countByStatus`, `countItems`, `findDetail`, **`applyStatusChange`**), `dto/AdminOrderFilter.java`, `entity/Order.java` (**`canMoveTo`, `updateStatus`**), `entity/OrderStatus.java`, `entity/OrderItem.java`, `entity/Payment.java` (+ `CODPayment`, `VNPayPayment`), `entity/Notification.java`, `entity/Voucher.java`, `entity/Product.java` (`updateStock`) |
| FE | `FE/admin-order-list.jsp`, `FE/fragments/admin-order-table.jsp`, `FE/admin-order-detail.jsp`, `FE/common/admin-order-badge.jspf` |

**Thứ tự đọc:** `admin-order-list.jsp` → `AdminOrderServlet.showList` → `AdminOrderService.search` → `OrderDAO.search` → `admin-order-table.jsp` → `admin-order-detail.jsp` → `AdminOrderServlet.changeStatus` → `AdminOrderService.changeStatus` → **`Order.canMoveTo`** → **`OrderDAO.applyStatusChange`**.

**Luồng 1 – danh sách + lọc**
```
Bấm "Đơn hàng" ─▶ GET /admin/orders ─▶ showList: AdminOrderFilter (status tab, q, pay, from, to, page)
      ─▶ AdminOrderService.search ─▶ OrderDAO.search/countSearch ; countItems (1 query gộp) ; tabCounts (số đơn mỗi tab)
      ─▶ AdminView.render("admin-order-list.jsp")
Bấm tab / gõ tìm / chọn ngày / đổi COD-VNPay ─▶ htmx GET /admin/orders?… (HX-Target: order-list)
      ─▶ chỉ trả fragments/admin-order-table.jsp, thay #order-list, đổi URL
```
**Luồng 2 – đổi trạng thái (trong bảng hoặc trang chi tiết)**
```
Bấm nút "Xác nhận / Giao hàng / Hoàn tất / Huỷ đơn…" (form POST nhỏ, hx-confirm) ─▶ POST /admin/orders/status (id, status, back)
  ─▶ AdminOrderServlet.changeStatus ─▶ AdminOrderService.changeStatus(id, "CONFIRMED")
       1. đổi chuỗi → OrderStatus (sai → "Trạng thái không hợp lệ")
       2. Admin không được tự đặt PENDING / RETURN_REQUESTED (đó là việc của khách)
       3. getDetail(id) (không có → lỗi);  Order.canMoveTo(next) quyết định chuyển được không (LUẬT DUY NHẤT nằm ở entity)
          · VNPay chưa thanh toán thành công → chỉ được HUỶ, không được xác nhận/giao
       4. dựng câu thông báo cho khách ("Đơn hàng #12 đã được xác nhận."…)
       5. OrderDAO.applyStatusChange(id, next, text)  — 1 TRANSACTION, khoá dòng đơn bằng JPQL setLockMode(PESSIMISTIC_WRITE):
            · Order.updateStatus(next)
            · HUỶ hoặc ĐÃ HOÀN HÀNG: cộng lại kho từng OrderItem (khoá sản phẩm theo id tăng dần, chống deadlock)
            · HUỶ + có voucher: trả lại 1 lượt voucher
            · COD: giao xong → thanh toán SUCCESS; huỷ khi đang chờ → FAILED  (VNPay giữ kết quả do IPN)
            · tạo Notification cho khách (khách thấy ở trang Thông báo + badge chuông)
  ─▶ flash "Đơn #12: Đã xác nhận" ─▶ redirect về `back` (chỉ nhận đường dẫn bắt đầu "/admin/orders": chống open redirect)
  lỗi (chuyển không hợp lệ…) ─▶ flash lỗi, vẫn redirect về `back`
```
**Chuỗi trạng thái hợp lệ:** `PENDING → CONFIRMED → SHIPPING → COMPLETED`; `PENDING/CONFIRMED → CANCELLED`; `COMPLETED → RETURN_REQUESTED → RETURNED` hoặc từ chối quay lại `COMPLETED`.
**Vì sao khoá dòng:** 4 người cùng huỷ 1 đơn một lúc sẽ cộng kho nhiều lần nếu không khoá — đã test và sửa (dùng JPQL `setLockMode`, không dùng `em.find(..., lock)` vì Hibernate đọc trước khi khoá).

---

## 7. Quản lý voucher

| | File |
|---|---|
| BE | `controller/admin/AdminVoucherServlet.java` (`/admin/vouchers`, `/new`, `/edit`, `/toggle`, `/delete`), `service/AdminVoucherService.java` (`list`, `tabCounts`, `save`, `setActive`, `delete`), `dao/VoucherDAO.java` (`findAllNewestFirst`, `findByCode`, `countOrders`), `entity/Voucher.java` (**`getState()`**, `isValid()`), `entity/DiscountType.java` |
| FE | `FE/admin-voucher-list.jsp` (tab + công tắc bật/tắt), `FE/admin-voucher-form.jsp`, `JS/admin.js` (`nvVoucherPreview`) |

**Thứ tự đọc:** `Voucher.getState` → `admin-voucher-list.jsp` → `AdminVoucherServlet` → `AdminVoucherService.save` → `admin-voucher-form.jsp`.

```
Bấm "Voucher" ─▶ GET /admin/vouchers?tab=active ─▶ showList: tab hợp lệ (all/active/soldout/expired/off), sai → all
      ─▶ AdminVoucherService.list(tab) lọc theo Voucher.getState(); tabCounts()  ─▶ AdminView.render
"Tạo/Sửa" ─▶ GET /new | /edit?id ─▶ form (xem trước thẻ voucher bằng admin.js)
"Lưu" ─▶ POST ─▶ save ─▶ AdminVoucherService.save
      mã 3–20 chữ/số, lưu IN HOA, không trùng; giảm %: 1–100, cố định: > 0; đơn tối thiểu ≥ 0; số lượng ≥ 1 và ≥ số lượt đã dùng;
      ngày hết hạn ≥ ngày bắt đầu; hạn lưu 23:59:59 của ngày chọn (Postgres làm tròn LocalTime.MAX sang 00:00 ngày sau)
Công tắc bật/tắt (<form hx-post=/admin/vouchers/toggle hx-trigger=change>) ─▶ setActive(id, active MONG MUỐN) ─▶ flash + redirect back
      (gửi trạng thái mong muốn, không "đảo ngược", để bấm nhanh 2 lần không bị lệch)
"Xoá" (hx-confirm) ─▶ POST /delete ─▶ đã có đơn dùng (countOrders > 0) → từ chối, gợi ý "tắt voucher"
```
**Điểm then chốt:** `Voucher.getState()` (`active|expired|soldout|off`) là **nơi duy nhất** định nghĩa 3 điều kiện hợp lệ (đang bật, trong hạn, còn lượt). `isValid()` chỉ là `state == active`; checkout của khách dùng `isValid()`.

---

## 8. Quản lý khách hàng

| | File |
|---|---|
| BE | `controller/admin/AdminCustomerServlet.java` (`/admin/users`, `/detail`, `/lock`), `service/AdminCustomerService.java` (`search`, `stats`, `counts`, `getById`, `recentOrders`, `setLocked`), `dao/CustomerDAO.java` (`search`, `countSearch`, `countLocked`, `orderStats`), `dao/OrderDAO.java` (`findRecentByCustomer`), `dto/AdminCustomerFilter.java`, `entity/Customer.java`, `entity/User.java` (`active`, `guest`) |
| FE | `FE/admin-customer-list.jsp`, `FE/fragments/admin-customer-table.jsp`, `FE/admin-customer-detail.jsp` |

**Thứ tự đọc:** `admin-customer-list.jsp` → `AdminCustomerServlet.showList` → `CustomerDAO.search` + `orderStats` → `admin-customer-table.jsp` → `showDetail` → `lock` → `AdminCustomerService.setLocked`.

```
Bấm "Khách hàng" ─▶ GET /admin/users ─▶ showList: AdminCustomerFilter (q, status, page)
      ─▶ CustomerDAO.search/countSearch (tên/email/SĐT; LOẠI khách vãng lai)  ─▶ orderStats: số đơn + tổng chi tiêu bằng 1 query gộp
      Lọc/tìm/phân trang ─▶ htmx (HX-Target: customer-list) ─▶ chỉ trả admin-customer-table.jsp
"Chi tiết" ─▶ GET /admin/users/detail?id ─▶ getById + stats + recentOrders(5) ─▶ admin-customer-detail.jsp
"Khoá/Mở khoá" (hx-confirm, POST /admin/users/lock: id, locked, back) ─▶ AdminCustomerService.setLocked
      chỉ khoá được Customer (không khoá Admin / bot) ─▶ User.active = false ─▶ flash + redirect back
```
**Khoá có tác dụng thế nào:** `AuthService.login` từ chối tài khoản bị khoá (báo sau khi đúng mật khẩu); `SessionUtil.requireCustomer` hỏi lại DB mỗi lần → khách đang đăng nhập bị đăng xuất ngay. Đơn đang xử lý vẫn chạy bình thường.

---

## 9. Chat với khách (phía Admin) + chatbot tự động + khách vãng lai

**Làm gì:** 3 cột — danh sách hội thoại, khung chat, thông tin khách. Không WebSocket: **polling bằng htmx** (danh sách 5 giây, khung chat 3 giây). Admin nhắn thì **tự chuyển sang chế độ Nhân viên** (bot im); có công tắc đổi lại "Tự động".

| | File |
|---|---|
| BE – Admin | `controller/admin/AdminChatServlet.java` (`/admin/chat`, `/list`, `/messages`, `/send`, `/mode`), `service/AdminChatService.java` (`conversations`, `getCustomer`, `messagesAfter`, `recentOrders`, `send`, `setHuman`), `dao/MessageDAO.java` (`findLatestPerCustomer`, `findAfter`), `entity/Message.java`, `entity/Customer.java` (`handledByHuman`), `entity/User.java` (`guest`) |
| BE – phía khách (để hiểu bot trả lời) | `controller/customer/ChatServlet.java` (`/chat`, `/chat/messages`, `/chat/send`, `/chat/guest`), `service/ChatService.java` (`sendFromCustomer`, `setHandledByHuman`, `startGuest`, `findGuest`), `service/ChatBotService.java` (`reply`, `match`, `normalize`), `dao/AutoReplyDAO.java` |
| FE – Admin | `FE/admin-chat.jsp`, `FE/fragments/admin-chat-list.jsp`, `admin-chat-head.jsp`, `admin-chat-messages.jsp`, `JS/admin.js` (`nvChatScroll`, `nvChatChip`) |
| FE – khách | `views/customer/chat.jsp`, `chat-start.jsp` (nhập tên), `fragments/chat-messages.jsp` |

**Thứ tự đọc:** `Message` + `Customer.handledByHuman` → `ChatService.sendFromCustomer` → `ChatBotService.reply` → `AdminChatServlet.showPage` → `admin-chat.jsp` → 3 fragment → `AdminChatServlet` `/send`, `/mode` → `AdminChatService.send/setHuman`.

**Luồng 1 – khách nhắn, bot trả lời**
```
Khách gõ + Gửi (form hx-post /chat/send) ─▶ ChatServlet.doPost ─▶ ChatService.sendFromCustomer
      lưu Message(customer = khách, sender = khách)
      nếu Customer.handledByHuman = false:  ChatBotService.reply(text)
            · AutoReplyDAO.findActiveOrdered (luật đang bật, ưu tiên nhỏ trước)
            · normalize câu (bỏ dấu, chữ thường) → luật ĐẦU TIÊN có 1 từ khoá khớp NGUYÊN TỪ; "*" = bắt mọi câu
            · không luật nào khớp → câu mặc định + chuyển nhân viên
            · lưu Message(sender = tài khoản bot "Trợ lý tự động"); luật `handoff` → setHandledByHuman(true), bot im
      trả mảnh tin mới; khung khách tự hỏi /chat/messages?after=ID mỗi 3 giây
```
**Luồng 2 – khách chưa đăng nhập:** `GET /chat` → `ChatServlet` không thấy `currentUser` và không có `guestChatId` trong session → forward `chat-start.jsp` (nhập tên) → `POST /chat/guest` → `ChatService.startGuest` tạo `Customer` với `User.guest = true` (email giả `…@guest.invalid`, mật khẩu ngẫu nhiên, không giỏ hàng), lưu id vào session → từ đó chat như thường.

**Luồng 3 – Admin xem và trả lời**
```
Bấm "Chat với khách" ─▶ GET /admin/chat?c=ID&filter=all&q= ─▶ AdminChatServlet.showPage
      conversations (tin cuối mỗi khách, mới nhất trước; lọc all|human|ai; tìm theo tên/email)
      selected = khách theo ?c= (sai/thiếu → hội thoại mới nhất);  messagesAfter(id,0);  recentOrders(3)
      ─▶ AdminView.render("admin-chat.jsp")
Danh sách tự làm mới: #chat-list hx-get /admin/chat/list hx-trigger="every 5s"  ─▶ forward admin-chat-list.jsp
Khung chat polling: #poll hx-get /admin/chat/messages?c&after hx-trigger="every 3s" ─▶ admin-chat-messages.jsp (tin mới + #poll mới)
Bấm 1 hội thoại (link trong danh sách, phải tự đặt hx-target="#adm-main") ─▶ GET /admin/chat?c=…
Gõ + Gửi ─▶ POST /admin/chat/send (c, content, after) ─▶ AdminChatService.send:
      lưu Message(sender = Admin) + ChatService.setHandledByHuman(true)  (nhân viên đã vào cuộc → bot im)
      trả tin mới + đầu khung chat (hx-swap-oob) đã đổi sang "Nhân viên đang trả lời"
Nút "Tự động" / "Nhân viên" ─▶ POST /admin/chat/mode (c, human) ─▶ AdminChatService.setHuman ─▶ trả lại admin-chat-head.jsp
```
**Bẫy htmx đã gặp (hay bị hỏi):** `hx-target`/`hx-swap` được **kế thừa** xuống phần tử con. (1) Phần tử polling nằm trong vùng `hx-target="#adm-main"` phải tự khai `hx-target="this"`. (2) Ngược lại, link bên trong `<ul hx-target="this" hx-swap="outerHTML">` phải khai lại `hx-target="#adm-main"`, nếu không cả trang chat bị nhét vào trong danh sách.
**Khách vãng lai ở Admin:** có nhãn "Khách vãng lai" (danh sách, đầu khung, panel phải ẩn email/đơn); bị loại khỏi trang Khách hàng.

---

## 10. Trả lời tự động (soạn câu cho chatbot)

**Làm gì:** Admin tự soạn các luật "từ khoá → câu trả lời" cho chatbot (**không gọi AI bên ngoài**); thử câu hỏi ngay trên trang xem bot sẽ trả lời gì.

| | File |
|---|---|
| BE | `controller/admin/AdminAutoReplyServlet.java` (`/admin/auto-replies`, `/test`, `/new`, `/edit`, `/toggle`, `/delete`), `service/AdminAutoReplyService.java` (`list`, `getById`, `suggestPriority`, `save`, `setActive`, `delete`, `test`), `service/ChatBotService.java` (`match`, `normalize`), `dao/AutoReplyDAO.java`, `entity/AutoReply.java` (`getKeywordList`) |
| FE | `FE/admin-autoreply-list.jsp` (kèm ô "Thử câu hỏi"), `FE/admin-autoreply-form.jsp`, `FE/fragments/admin-autoreply-test.jsp` |

**Thứ tự đọc:** `AutoReply` → `ChatBotService.match` → `admin-autoreply-list.jsp` → `AdminAutoReplyServlet` → `AdminAutoReplyService.save`.

```
Bấm "Trả lời tự động" ─▶ GET /admin/auto-replies ─▶ list() (theo ưu tiên) ─▶ AdminView.render
Gõ vào ô "Thử câu hỏi" (hx-get /admin/auto-replies/test?text= , delay 400ms, target #test-result)
      ─▶ AdminAutoReplyService.test ─▶ ChatBotService.match(câu)   ← CHÍNH hàm chatbot dùng thật
      ─▶ fragments/admin-autoreply-test.jsp: luật khớp (ID, ưu tiên, từ khoá), câu trả lời, có chuyển nhân viên không
"Thêm/Sửa" ─▶ GET /new | /edit?id ─▶ form (gợi ý ưu tiên = lớn hơn luật thường lớn nhất, nhỏ hơn luật "*")
"Lưu" ─▶ POST ─▶ save ─▶ AdminAutoReplyService.save
      từ khoá: ≥1, ≤20, mỗi từ ≤40 ký tự và ≥3 chữ/số (tránh khớp nhầm), "*" phải đứng một mình
      câu trả lời bắt buộc ≤ giới hạn; ưu tiên 1–9999; checkbox handoff (chuyển nhân viên) và active
Công tắc bật/tắt (hx-trigger=change) ─▶ POST /toggle ─▶ setActive(trạng thái mong muốn) ─▶ redirect back
"Xoá" (hx-confirm) ─▶ POST /delete
```

---

## 11. Bảng tra nhanh: URL → Servlet → JSP

| URL | Servlet | Trả về |
|---|---|---|
| `GET/POST /admin/login`, `POST /admin/logout` | `AdminLoginServlet`, `AdminLogoutServlet` | `admin-login.jsp` / redirect |
| `GET /admin` | `AdminDashboardServlet` | `admin-dashboard.jsp` |
| `GET /admin/products` (+`/new`, `/edit`), `POST …/new`, `/edit`, `/delete` | `AdminProductServlet` | `admin-product-list.jsp` · mảnh `admin-product-table.jsp` · `admin-product-form.jsp` |
| `GET /admin/categories` (+`/new`, `/edit`), `POST …` | `AdminCategoryServlet` | `admin-category-list.jsp`, `admin-category-form.jsp` |
| `GET /admin/orders`, `/detail`, `POST /status` | `AdminOrderServlet` | `admin-order-list.jsp` · mảnh `admin-order-table.jsp` · `admin-order-detail.jsp` |
| `GET /admin/vouchers` (+`/new`, `/edit`), `POST /toggle`, `/delete`, `/new`, `/edit` | `AdminVoucherServlet` | `admin-voucher-list.jsp`, `admin-voucher-form.jsp` |
| `GET /admin/users`, `/detail`, `POST /lock` | `AdminCustomerServlet` | `admin-customer-list.jsp` · mảnh `admin-customer-table.jsp` · `admin-customer-detail.jsp` |
| `GET /admin/chat`, `/list`, `/messages`, `POST /send`, `/mode` | `AdminChatServlet` | `admin-chat.jsp` · mảnh `admin-chat-list/messages/head.jsp` |
| `GET /admin/auto-replies` (+`/test`, `/new`, `/edit`), `POST /toggle`, `/delete`, `/new`, `/edit` | `AdminAutoReplyServlet` | `admin-autoreply-list.jsp`, `admin-autoreply-form.jsp`, mảnh `admin-autoreply-test.jsp` |

Dữ liệu mẫu (để chạy thử): `BE/util/DataSeeder.java` (tài khoản, 82 sản phẩm, luật bot), `BE/util/DemoDataSeeder.java` (khách, đơn, voucher, chat mẫu). Tài khoản dev: `admin@nongviet.vn` / `Admin@123`.

---

## 12. Câu hỏi vấn đáp thường gặp (và đáp nhanh)

1. **Servlet khác Service khác DAO thế nào?** Servlet nhận request, đọc tham số, chọn view; Service chứa luật và kiểm tra dữ liệu; DAO chỉ truy vấn DB. Servlet không gọi thẳng DAO để luật nằm đúng 1 chỗ.
2. **`forward` khác `sendRedirect`?** `forward` chuyển nội bộ trong cùng request (giữ attribute, URL không đổi, dùng để vẽ JSP). `sendRedirect` bảo trình duyệt gọi URL mới (request mới, mất attribute → dùng session/flash). Sau POST luôn redirect (PRG) để F5 không gửi lại form.
3. **htmx làm gì ở đây?** Gửi request không tải lại trang và thay đúng 1 vùng HTML bằng HTML do server trả. `hx-boost` biến link/form thường thành request htmx; `hx-target` chọn vùng thay; `hx-swap-oob` thay thêm vùng khác (sidebar/tiêu đề); `HX-Trigger` để server bảo trình duyệt hiện toast; `HX-Redirect` để chuyển trang.
4. **Server biết request nào từ htmx?** Header `HX-Request: true` (`HtmxUtil.isHtmx`), vùng đích qua `HX-Target`. Bấm Back thì htmx gửi `HX-History-Restore-Request` → phải trả cả trang.
5. **`AdminView.render` để làm gì?** Một chỗ quyết định trả cả khung hay chỉ nội dung, để Servlet chỉ cần 1 dòng.
6. **Flash là gì?** Thông báo lưu tạm trong session trước khi redirect, trang kế tiếp lấy ra hiện 1 lần rồi xoá.
7. **Chống XSS?** Mọi dữ liệu người nhập in ra bằng `<c:out>` (escape HTML). Chống JPQL injection: giá trị luôn qua `setParameter`, chỉ nối vào chuỗi các điều kiện cố định.
8. **Admin được bảo vệ thế nào?** `SessionUtil.requireAdmin` ở đầu mọi `doGet/doPost` của `/admin/*`; khách vào bị 403; chống session fixation khi đăng nhập; `next`/`back` chỉ nhận đường dẫn nội bộ (chống open redirect); chưa có CSRF token cho Admin (chỉ POST + kiểm tra quyền) — **nói thẳng nếu bị hỏi**.
9. **Vì sao đổi trạng thái đơn phải trong 1 transaction và khoá dòng?** Đổi trạng thái + hoàn kho + trả voucher + tạo thông báo phải cùng thành công hoặc cùng thất bại; khoá `PESSIMISTIC_WRITE` chống 2 người cùng huỷ làm cộng kho 2 lần.
10. **Luật chuyển trạng thái nằm ở đâu?** Duy nhất ở `Order.canMoveTo/updateStatus` (entity). Service chỉ gọi để báo lỗi thân thiện.
11. **Vì sao không xoá sản phẩm/voucher/danh mục đã dùng?** Đơn hàng cần giữ lịch sử (khoá ngoại); thay vào đó đặt tồn kho = 0 / tắt voucher / chuyển sản phẩm sang danh mục khác.
12. **Chatbot hoạt động ra sao? Có phải AI không?** Không. Là luật "từ khoá → câu trả lời" do Admin soạn trong bảng `auto_replies`; so khớp sau khi bỏ dấu, theo nguyên từ, ưu tiên nhỏ trước; không khớp thì chuyển nhân viên. Muốn dùng AI thật chỉ cần thay `ChatBotService.reply`.
13. **Không WebSocket thì tin nhắn "tức thời" bằng gì?** Polling: trình duyệt hỏi `/…/messages?after=ID` mỗi 3 giây (htmx `hx-trigger="every 3s"`).
14. **Khách vãng lai lưu ở đâu?** Là 1 dòng `users` loại Customer với `is_guest = true`, không đăng nhập được, không giỏ hàng; id lưu trong session nên hết phiên là mất cuộc chat.
15. **Upload ảnh an toàn thế nào?** Chỉ nhận JPG/PNG/WEBP kiểm tra bằng byte đầu file (không tin tên file), ≤ 2MB, lưu tên UUID ngoài thư mục webapp, phục vụ qua `ImageServlet` có chặn `../`.
16. **`hbm2ddl=update` là gì?** Hibernate tự thêm bảng/cột còn thiếu khi khởi động (tiện cho dev; production nên đổi `validate`/`none`).
