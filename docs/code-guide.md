# Hướng dẫn đọc code theo từng tính năng (phần Admin)

Mỗi tính năng bên dưới liệt kê **file nào đã được tạo (A) hoặc sửa (M)**, chia **BE** (Java chạy phía server) và **FE** (JSP / JS / CSS), kèm **thứ tự nên đọc**.
Đường dẫn rút gọn: `java/` = `backend/src/main/java/com/ecommerce/`, `views/` = `backend/src/main/webapp/WEB-INF/views/`, `static/` = `backend/src/main/webapp/static/`.

## Mục lục theo nhánh

| # | Nhánh | Tính năng | Mục |
|---|---|---|---|
| 1 | `feat/admin-manage-product` | Quản lý sản phẩm (kèm khung CMS dùng chung) | [Mục 1](#1-quản-lý-sản-phẩm--nhánh-featadmin-manage-product) |
| 2 | `feat/admin-login-logout` | Đăng nhập / đăng xuất Admin | [Mục 2](#2-đăng-nhập--đăng-xuất-admin--nhánh-featadmin-login-logout) |
| 3 | `feat/admin-manage-order` | Quản lý đơn hàng | [Mục 3](#3-quản-lý-đơn-hàng--nhánh-featadmin-manage-order-core) |
| 4 | `feat/admin-manage-voucher` | Quản lý voucher | [Mục 4](#4-quản-lý-voucher--nhánh-featadmin-manage-voucher) |
| 5 | `feat/admin-manage-customer-account` | Quản lý tài khoản khách hàng | [Mục 5](#5-quản-lý-tài-khoản-khách-hàng--nhánh-featadmin-manage-customer-account) |
| 6 | `feat/chat-with-shop` | Chat với khách | [Mục 6](#6-chat-với-khách--nhánh-featchat-with-shop) |
| 7 | `feat/admin-auto-reply` | Trả lời tự động của chatbot | [Mục 7](#7-trả-lời-tự-động-của-chatbot--nhánh-featadmin-auto-reply) |
| 8 | `feat/admin-manage-category` | Quản lý danh mục | [Mục 8](#8-quản-lý-danh-mục--nhánh-featadmin-manage-category-core) |
| 9 | `feat/admin-dashboard` | Tổng quan + Thống kê doanh thu | [Mục 9](#9-tổng-quan--thống-kê-doanh-thu--nhánh-featadmin-dashboard) |
| — | (làm trực tiếp trên `dev`) | Sửa lỗi / chỉnh giao diện sau khi merge | [Cuối tài liệu](#các-sửa-làm-trực-tiếp-trên-dev-sau-khi-merge) |

Thứ tự merge vào `dev` đúng như bảng (nhánh sau dựa trên nhánh trước).

---

## Cách một request đi (áp dụng cho mọi trang Admin)

```
Trình duyệt ──► Servlet (đọc tham số, kiểm tra quyền) ──► Service (rule + validate) ──► DAO (truy vấn DB)
                      │
                      └─► AdminView.render(...) ──► views/admin/admin-layout.jsp ──► nhúng file nội dung (admin-xxx.jsp)
```
- **Mở trực tiếp** (gõ URL, F5, Back) → trả cả khung (header + sidebar + nội dung). **Bấm link qua htmx** → chỉ trả phần nội dung (`partial`), sidebar/tiêu đề cập nhật bằng `hx-swap-oob`.
- Danh sách có ô lọc: htmx chỉ thay phần bảng nằm trong `fragments/` (Servlet nhận ra nhờ header `HX-Target`).
- Hành động ghi (thêm/sửa/xoá/đổi trạng thái): form POST → Service → **redirect** về danh sách kèm thông báo "flash" (`AdminView.flash`) → hiện toast.
- Đọc code theo đúng chiều: **Servlet → Service → DAO → JSP**.

---

## 0. Nền tảng dùng chung (đọc trước)

| | File | Vai trò |
|---|---|---|
| BE | `java/util/AdminView.java` | Chọn trả cả trang hay chỉ nội dung; thông báo flash sau redirect |
| BE | `java/util/SessionUtil.java` | `requireAdmin` / `requireCustomer`: chặn người chưa đăng nhập hoặc sai quyền |
| BE | `java/util/HtmxUtil.java` | Nhận biết request htmx, báo toast qua header, chuyển hướng kiểu htmx |
| BE | `java/util/ParamUtil.java` | Đọc tham số URL/form an toàn (sai định dạng không gây lỗi 500) |
| BE | `java/util/JPAUtil.java` | Tạo kết nối DB một lần; đọc `db.properties` của từng máy |
| BE | `java/util/DemoDataSeeder.java` · `DataSeeder.java` | Nạp dữ liệu mẫu khi bảng trống (khách, đơn, voucher, chat, luật chatbot...) |
| BE | `java/dao/AbstractDAO.java` | CRUD chung cho mọi entity |
| BE | `java/service/BusinessException.java` | Lỗi nghiệp vụ (1 thông báo hoặc lỗi theo từng ô form) |
| FE | `views/admin/admin-layout.jsp` | Khung chung của mọi trang admin (2 nhánh: cả trang / chỉ nội dung) |
| FE | `views/admin/common/admin-header.jspf` · `admin-sidebar.jspf` · `admin-nav.jspf` · `admin-title.jspf` | Thanh trên, menu trái, danh sách mục menu, tiêu đề |
| FE | `views/admin/common/admin-order-badge.jspf` | Nhãn trạng thái đơn (dùng ở nhiều trang) |
| FE | `static/js/admin.js` | Toast, xem trước sản phẩm/voucher, cuộn chat, nút "Xem dạng bảng" của biểu đồ |
| FE | `static/css/design.css` | Theme màu xanh lá, vài chỉnh sửa daisyUI |

---

## 1. Quản lý sản phẩm — nhánh `feat/admin-manage-product`

**Làm gì:** danh sách (lọc từ khoá/danh mục/tồn kho, sắp xếp, phân trang), thêm/sửa có upload ảnh, xoá (chặn nếu đã có trong đơn hàng).

| | File | |
|---|---|---|
| BE | `java/controller/admin/AdminProductServlet.java` | A · điều hướng `/admin/products`, `/new`, `/edit`, `/delete` |
| BE | `java/service/AdminProductService.java` | A · kiểm tra từng ô, lưu ảnh, rule chặn xoá |
| BE | `java/service/ProductService.java` | M · giữ phần đọc/tìm kiếm dùng chung |
| BE | `java/dao/ProductDAO.java` | M · lọc tồn kho, sắp xếp, đếm đơn chứa sản phẩm, xoá kèm gỡ khỏi giỏ |
| BE | `java/dto/ProductFilter.java` | M · thêm `stock`, sort `name`/`stock`, `pageSize` |
| BE | `java/util/UploadUtil.java` | A · kiểm tra ảnh bằng byte đầu file, lưu/xoá file, chống path traversal |
| BE | `java/controller/ImageServlet.java` | A · trả ảnh tại `/uploads/...` |
| FE | `views/admin/admin-product-list.jsp` | A · trang danh sách (tiêu đề, thanh lọc, bảng) |
| FE | `views/admin/admin-product-form.jsp` | A · form thêm/sửa (1 file cho cả hai) |
| FE | `views/admin/fragments/admin-product-table.jsp` | A · bảng + phân trang (phần htmx thay khi lọc) |
| FE | `views/admin/fragments/admin-product-summary.jsp` · `admin-product-deleted.jsp` | A · dòng "Hiển thị 1–10 / N"; phản hồi sau khi xoá (dòng biến mất) |

**Đọc theo thứ tự:** `AdminProductServlet` → `AdminProductService` → `ProductDAO` → `admin-product-list.jsp` → `fragments/admin-product-table.jsp` → `admin-product-form.jsp` → `UploadUtil` → `ImageServlet`.
**Điểm đáng hỏi khi vấn đáp:** ảnh lưu ngoài thư mục webapp; kiểm tra ảnh bằng byte đầu; `hx-trigger` của ô lọc; xoá bằng `hx-swap-oob`.

## 2. Đăng nhập / đăng xuất Admin — nhánh `feat/admin-login-logout`

| | File | |
|---|---|---|
| BE | `java/controller/admin/AdminLoginServlet.java` | A · dùng lại `AuthService.login` + kiểm tra là Admin, đổi phiên, `next` an toàn |
| BE | `java/controller/admin/AdminLogoutServlet.java` | A · chỉ nhận POST |
| BE | `java/util/SessionUtil.java` | M · `requireAdmin` chuyển tới `/admin/login` |
| FE | `views/admin/admin-login.jsp` | A · trang đăng nhập riêng (không qua khung CMS) |
| FE | `views/admin/common/admin-header.jspf` | M · nút đăng xuất trỏ `/admin/logout` |

**Đọc:** `AdminLoginServlet` → `SessionUtil.requireAdmin` → `admin-login.jsp`.

## 3. Quản lý đơn hàng — nhánh `feat/admin-manage-order` (Core)

**Làm gì:** danh sách có tab trạng thái + tìm kiếm + lọc COD/VNPay + khoảng ngày, chi tiết đơn, xác nhận/giao/hoàn tất/huỷ, duyệt hoặc từ chối hoàn hàng (hoàn kho, cập nhật thanh toán, thông báo cho khách).

| | File | |
|---|---|---|
| BE | `java/controller/admin/AdminOrderServlet.java` | A · `/admin/orders`, `/detail`, `/status` |
| BE | `java/service/AdminOrderService.java` | A · kiểm tra quyền chuyển trạng thái, soạn nội dung thông báo |
| BE | `java/dao/OrderDAO.java` | A · lọc/phân trang, đếm theo trạng thái, chi tiết JOIN FETCH, `applyStatusChange` (1 transaction + khoá dòng) |
| BE | `java/dto/AdminOrderFilter.java` | A · gom tham số lọc |
| BE | `java/entity/Order.java` | M · `discountAmount`, `shippingFee`, `canMoveTo`/`updateStatus` (luật chuyển trạng thái), `getOrderDateText` |
| BE | `java/entity/OrderStatus.java` | M · thêm nhãn tiếng Việt |
| BE | `java/entity/Payment.java` · `CODPayment.java` · `VNPayPayment.java` | M · `getMethod()` |
| BE | `java/util/DemoDataSeeder.java` · `java/listener/AppInitListener.java` | A / M · dữ liệu mẫu |
| FE | `views/admin/admin-order-list.jsp` | A · tiêu đề + thanh lọc |
| FE | `views/admin/fragments/admin-order-table.jsp` | A · tab + bảng + phân trang |
| FE | `views/admin/admin-order-detail.jsp` | A · chi tiết đơn, thanh tiến trình, các nút hành động |
| FE | `views/admin/common/admin-order-badge.jspf` | A · nhãn trạng thái |

**Đọc:** `Order.canMoveTo` → `AdminOrderServlet` → `AdminOrderService.changeStatus` → `OrderDAO.applyStatusChange` → `admin-order-table.jsp` → `admin-order-detail.jsp`.
**Điểm đáng hỏi:** vì sao khoá dòng đơn (`PESSIMISTIC_WRITE`) để hai Admin bấm huỷ cùng lúc không cộng kho hai lần; tab giữ trạng thái qua `<input hidden form="order-filter">`.

## 4. Quản lý voucher — nhánh `feat/admin-manage-voucher`

| | File | |
|---|---|---|
| BE | `java/controller/admin/AdminVoucherServlet.java` | A · danh sách, tạo/sửa, bật/tắt, xoá |
| BE | `java/service/AdminVoucherService.java` | A · kiểm tra mã/giá trị/ngày, chặn giảm số lượng dưới số đã dùng, chặn xoá voucher đã có đơn dùng |
| BE | `java/dao/VoucherDAO.java` | A · tìm theo mã, đếm đơn dùng voucher |
| BE | `java/entity/Voucher.java` | M · `getState()` (nơi duy nhất định nghĩa điều kiện hợp lệ), `isValid()`, ngày dạng chữ cho form |
| FE | `views/admin/admin-voucher-list.jsp` | A · tab trạng thái + bảng + công tắc bật/tắt |
| FE | `views/admin/admin-voucher-form.jsp` | A · form tạo/sửa + thẻ xem trước |
| FE | `static/js/admin.js` | M · `nvVoucherPreview` |

**Đọc:** `Voucher.getState` → `AdminVoucherServlet` → `AdminVoucherService.save` → `admin-voucher-list.jsp` → `admin-voucher-form.jsp`.

## 5. Quản lý tài khoản khách hàng — nhánh `feat/admin-manage-customer-account`

| | File | |
|---|---|---|
| BE | `java/controller/admin/AdminCustomerServlet.java` | A · `/admin/users`, `/detail`, `/lock` |
| BE | `java/service/AdminCustomerService.java` | A · chỉ khoá được khách hàng |
| BE | `java/dao/CustomerDAO.java` | A · lọc + thống kê số đơn/tổng chi tiêu bằng 1 query gộp |
| BE | `java/dao/OrderDAO.java` | M · `findRecentByCustomer` |
| BE | `java/dto/AdminCustomerFilter.java` | A · gom tham số lọc |
| BE | `java/entity/User.java` | M · cột `active` (`users.is_active`), `getCreatedAtText` |
| BE | `java/service/AuthService.java` | M · `login` từ chối tài khoản bị khoá |
| BE | `java/util/SessionUtil.java` | M · `requireCustomer` hỏi lại DB để đăng xuất khách bị khoá giữa phiên |
| FE | `views/admin/admin-customer-list.jsp` | A · tiêu đề + thanh lọc |
| FE | `views/admin/fragments/admin-customer-table.jsp` | A · bảng + nút Khoá/Mở khoá + phân trang |
| FE | `views/admin/admin-customer-detail.jsp` | A · hồ sơ + số liệu + đơn gần đây |

**Đọc:** `User.active` → `AuthService.login` → `SessionUtil.requireCustomer` → `AdminCustomerServlet` → `CustomerDAO.orderStats` → JSP.

## 6. Chat với khách — nhánh `feat/chat-with-shop`

**Làm gì:** trang chat 3 cột cho Admin (danh sách hội thoại, khung chat, thông tin khách), polling bằng htmx (không WebSocket), chuyển AI/Nhân viên, chatbot trả lời cho khách.

| | File | |
|---|---|---|
| BE | `java/controller/admin/AdminChatServlet.java` | A · `/admin/chat`, `/list`, `/messages`, `/send`, `/mode` |
| BE | `java/service/AdminChatService.java` | A · gửi tin (tự chuyển sang chế độ Nhân viên), đổi chế độ |
| BE | `java/service/ChatBotService.java` | A · chatbot (sau đó chuyển sang đọc luật từ DB, xem mục 7) |
| BE | `java/service/ChatService.java` | M · sau tin của khách, chatbot trả lời nếu chưa có nhân viên tiếp quản; `setHandledByHuman` |
| BE | `java/dao/MessageDAO.java` | M · `findLatestPerCustomer` (tin cuối mỗi khách) |
| BE | `java/entity/Customer.java` | M · cờ `handledByHuman` (`users.handled_by_human`) |
| FE | `views/admin/admin-chat.jsp` | A · khung 3 cột |
| FE | `views/admin/fragments/admin-chat-list.jsp` | A · danh sách hội thoại (tự hỏi lại 5 giây) |
| FE | `views/admin/fragments/admin-chat-messages.jsp` | A · bong bóng chat + thẻ `#poll` (3 giây) |
| FE | `views/admin/fragments/admin-chat-head.jsp` | A · đầu khung chat + nút AI / Nhân viên |
| FE | `static/js/admin.js` | M · `nvChatChip`, `nvChatScroll` |

**Đọc:** `AdminChatServlet` → `AdminChatService` → `ChatService.sendFromCustomer` → `admin-chat.jsp` → 3 file trong `fragments/`.
**Bẫy đã gặp:** phần tử polling nằm trong vùng `hx-target="#adm-main"` nên phải tự khai `hx-target="this"`.

## 7. Trả lời tự động của chatbot — nhánh `feat/admin-auto-reply`

| | File | |
|---|---|---|
| BE | `java/entity/AutoReply.java` | A · entity mới (bảng `auto_replies`): từ khoá, câu trả lời, ưu tiên, chuyển nhân viên |
| BE | `java/controller/admin/AdminAutoReplyServlet.java` | A · danh sách, `/test`, tạo/sửa, bật/tắt, xoá |
| BE | `java/service/AdminAutoReplyService.java` | A · kiểm tra từ khoá/câu trả lời/ưu tiên |
| BE | `java/dao/AutoReplyDAO.java` | A · lấy luật theo thứ tự ưu tiên |
| BE | `java/service/ChatBotService.java` | M · chọn luật đầu tiên khớp (khớp nguyên từ, bỏ dấu, không phân biệt hoa thường) |
| BE | `java/util/DataSeeder.java` | M · nạp 10 luật mặc định |
| FE | `views/admin/admin-autoreply-list.jsp` | A · ô "Thử câu hỏi" + bảng luật |
| FE | `views/admin/admin-autoreply-form.jsp` | A · form tạo/sửa |
| FE | `views/admin/fragments/admin-autoreply-test.jsp` | A · kết quả "Thử câu hỏi" (htmx) |

**Đọc:** `AutoReply` → `ChatBotService.match` → `AdminAutoReplyService.save` → `AdminAutoReplyServlet` → `admin-autoreply-list.jsp`.

## 8. Quản lý danh mục — nhánh `feat/admin-manage-category` (Core)

| | File | |
|---|---|---|
| BE | `java/controller/admin/AdminCategoryServlet.java` | A · danh sách, tạo/sửa, xoá |
| BE | `java/service/AdminCategoryService.java` | A · tên không trùng, mô tả ≤ 255, không xoá danh mục còn sản phẩm |
| BE | `java/dao/CategoryDAO.java` | M · đếm sản phẩm mỗi danh mục (1 query), tìm theo tên |
| FE | `views/admin/admin-category-list.jsp` | A · bảng + nút Xoá bị vô hiệu nếu còn sản phẩm |
| FE | `views/admin/admin-category-form.jsp` | A · form thêm/sửa |

**Đọc:** `AdminCategoryServlet` → `AdminCategoryService` → `CategoryDAO.countProducts` → 2 file JSP.

## 9. Tổng quan + Thống kê doanh thu — nhánh `feat/admin-dashboard`

| | File | |
|---|---|---|
| BE | `java/controller/admin/AdminDashboardServlet.java` | A · `/admin` |
| BE | `java/controller/admin/AdminStatisticsServlet.java` | A · `/admin/statistics`, `/export` (CSV) |
| BE | `java/service/AdminStatsService.java` | A · doanh thu theo ngày, tổng ký này/ký trước, theo danh mục, top sản phẩm |
| BE | `java/dao/StatsDAO.java` | A · các truy vấn thống kê (chỉ đọc) |
| BE | `java/dto/ChartPoint.java` · `ChartData.java` · `TopProduct.java` | A · dữ liệu để vẽ biểu đồ |
| BE | `java/controller/admin/AdminLoginServlet.java` | M · đăng nhập xong vào `/admin` |
| FE | `views/admin/admin-dashboard.jsp` | A · tổng quan (8 vùng) |
| FE | `views/admin/admin-statistics.jsp` | A · thống kê (5 vùng) |
| FE | `views/admin/common/admin-chart-columns.jspf` · `admin-chart-bars.jspf` · `admin-delta.jspf` | A · biểu đồ cột, thanh ngang (HTML/CSS thuần), dòng % tăng/giảm |
| FE | `static/js/admin.js` | M · `nvToggleChartTable` |

**Đọc:** `StatsDAO` → `AdminStatsService` → `AdminDashboardServlet` → `admin-dashboard.jsp` → `admin-chart-columns.jspf`.

---

## Phần có từ trước (phía khách hàng, đã có trên `dev`)

| Chức năng | BE | FE |
|---|---|---|
| Trang chủ | `controller/customer/HomeServlet` | `views/customer/home.jsp`, `common/product-card.jspf` |
| Sản phẩm (tìm, lọc, phân trang) | `ProductServlet` → `ProductService` → `ProductDAO`, `dto/ProductFilter`, `dto/PageResult` | `customer/products.jsp` |
| Đăng ký / đăng nhập / quên mật khẩu | `RegisterServlet`, `LoginServlet`, `LogoutServlet`, `ForgotPasswordServlet`, `ResetPasswordServlet` → `AuthService`, `EmailService`, `util/PasswordUtil` | `customer/{register,login,forgot-password,reset-password}.jsp` |
| Giỏ hàng | `CartServlet` → `CartService` → `CartDAO` | `customer/cart.jsp`, `customer/fragments/cart-*.jsp` |
| Chat (phía khách) | `ChatServlet` → `ChatService` → `MessageDAO` | `customer/chat.jsp`, `customer/fragments/chat-messages.jsp` |

## Cách chạy kiểm tra nhanh khi đọc code

1. Chạy app, đăng nhập `/admin/login` bằng tài khoản admin trong `util/DataSeeder.java`.
2. Với mỗi tính năng: mở trang, thao tác, xem Tomcat log (Hibernate in sẵn câu SQL) để thấy Servlet → Service → DAO chạy thế nào.
3. Tra cứu URL, tham số, quyền ở `docs/api-spec.md` (mục 7).

---

## Các sửa làm trực tiếp trên `dev` sau khi merge

| Việc | BE | FE |
|---|---|---|
| Nút "← Quay lại" thành nút thật (không còn là link chữ) | — | `admin-customer-detail.jsp`, `admin-product-form.jsp`, `admin-order-detail.jsp`, `admin-voucher-form.jsp`, `admin-category-form.jsp`, `admin-autoreply-form.jsp`, `admin-login.jsp`, `common/admin-sidebar.jspf`, `customer/forgot-password.jsp` |
| Sửa lỗi trang chat bị lồng trang (link hội thoại kế thừa `hx-target` của `<ul>` polling) | — | `fragments/admin-chat-list.jsp` |
| Sửa tràn ngang trên điện thoại (Tổng quan, Thống kê, Chi tiết đơn, form có dòng gợi ý dài) | — | `admin-dashboard.jsp`, `admin-statistics.jsp`, `admin-order-detail.jsp`, `common/admin-chart-columns.jspf`, `static/css/design.css` |
| Chống trình duyệt dùng CSS/JS cũ trong cache (`?v=assetVersion`) | `java/listener/AppInitListener.java` | `views/common/head.jspf`, `views/admin/admin-layout.jsp` |
