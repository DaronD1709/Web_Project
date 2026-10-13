# Đề tài: Website Thương mại điện tử (Java Servlet/JSP)

Chủ đề: **Nông nghiệp** (hạt giống, phân bón, thuốc BVTV, dụng cụ, máy móc & tưới tiêu, nông sản) — đã chốt
Stack: NetBeans + Servlet/JSP + Tomcat + PostgreSQL + Jakarta Mail (gửi email qua SMTP)

Ký hiệu: [Core] = bắt buộc để chạy đủ luồng mua hàng — [Optional] = làm sau nếu còn thời gian

---

## 1. Chức năng Customer

### Tài khoản
- [Core] Đăng ký (Register) — thành công thì **gửi email chào mừng** tới email vừa đăng ký
- [Core] Đăng nhập / Đăng xuất (Login/Logout)
- [Core] **Quên mật khẩu (Forgot password)** — nhập email → nhận email chứa liên kết đặt lại mật khẩu (hiệu lực 30 phút, dùng 1 lần) → đặt mật khẩu mới
- [Core] Phân quyền hồ sơ theo role (RBAC) — chỉnh sửa thông tin cá nhân
- [Optional] Quản lý địa chỉ giao hàng (thêm/sửa/xoá nhiều địa chỉ)

### Sản phẩm
- [Core] Xem theo danh mục (Categories)
- [Core] Tìm kiếm (Search)
- [Core] Trang chi tiết sản phẩm (ảnh, mô tả, giá, tồn kho)
- [Optional] Lọc & sắp xếp (theo giá, danh mục)
- [Optional] Wishlist / sản phẩm yêu thích
- [Optional] Đánh giá & review sản phẩm sau khi mua

### Mua hàng
- [Core] Giỏ hàng (Shopping cart)
- [Core] Thanh toán (Payment) — gợi ý: COD giả lập hoặc sandbox VNPay/Momo. Đặt hàng xong **gửi email xác nhận đơn hàng** (danh sách sản phẩm, tổng tiền, địa chỉ, phương thức thanh toán) tới email khách
- [Optional] Áp dụng mã giảm giá (Voucher) khi checkout
- [Core] Lịch sử mua hàng (Purchase history)
- [Core] Theo dõi trạng thái đơn hàng (Chờ xác nhận → Đang giao → Hoàn tất → Huỷ)

### Khác
- [Optional] Chat real-time với admin/shop
- [Optional] AI chatbot hỗ trợ hỏi đáp sản phẩm

---

## 2. Chức năng Admin

### Sản phẩm
- [Core] Quản lý kho / CRUD sản phẩm (Inventory management)
- [Core] Phân loại sản phẩm theo danh mục (Classification)
- [Core] Upload/quản lý ảnh sản phẩm

### Đơn hàng
- [Core] Quản lý đơn hàng: xác nhận, cập nhật trạng thái giao, huỷ đơn

### Voucher (Optional)
- [Optional] CRUD voucher: mã code, loại giảm (% hoặc số tiền cố định), giá trị đơn tối thiểu
- [Optional] Số lượng phát hành + số lượng đã dùng
- [Optional] Ngày bắt đầu / ngày hết hạn
- [Optional] Giới hạn số lần dùng mỗi khách hàng
- [Optional] Bật/tắt voucher thủ công

### Người dùng & nội dung
- [Optional] Quản lý tài khoản khách hàng (khoá/mở, xem danh sách)
- [Optional] Duyệt/xoá đánh giá sản phẩm
- [Optional] Thống kê doanh thu / sản phẩm bán chạy (dashboard, vài chart)

### Khác
- [Optional] Chat real-time với khách hàng

---

## 3. Chức năng chung / hệ thống

- [Core] RBAC rõ 2 role: Customer / Admin (có thể thêm Staff nếu muốn phức tạp hơn)
- [Core] Validate input cơ bản: PreparedStatement (chống SQL injection), escape output (chống XSS ở review/chat)
- [Core] **Gửi email (Jakarta Mail/SMTP):** chào mừng khi đăng ký, đặt lại mật khẩu, xác nhận đơn hàng. Gửi ở luồng nền; lỗi gửi mail chỉ ghi log, không làm hỏng đăng ký/đặt hàng
- [Optional] Email/notification khi đơn hàng đổi trạng thái

---

## 4. Ghi chú kỹ thuật

**Kiến trúc gợi ý:** MVC cổ điển — Servlet đóng vai Controller, DAO tầng truy xuất PostgreSQL, JSP tầng View. Session lưu giỏ hàng + trạng thái đăng nhập.

**Voucher — thiết kế DB:**
- Bảng `Voucher`: mã code, loại giảm, giá trị, đơn tối thiểu, số lượng phát hành, ngày bắt đầu/kết thúc, trạng thái
- Bảng `VoucherUsage` (hoặc lưu `voucher_id` trong `Orders`): mỗi lần áp dụng thành công insert 1 record → số lượng đã dùng = COUNT từ bảng này, tránh cộng dồn thủ công gây race condition khi nhiều người dùng gần hết hạn mức cùng lúc

**Validate voucher khi checkout** (theo thứ tự, báo lỗi cụ thể từng bước thay vì lỗi chung chung):
1. Còn hạn sử dụng không
2. Còn số lượng không
3. Đơn hàng có đạt giá trị tối thiểu không

**Chat real-time không cần WebSocket:** có thể giả lập bằng AJAX polling (fetch tin nhắn mới mỗi vài giây) — đủ hiệu quả demo mà không cần setup thêm hạ tầng.

**Payment:** với đồ án sinh viên, ưu tiên COD giả lập hoặc tích hợp sandbox VNPay/Momo thay vì cổng thanh toán thật.

**Email (Jakarta Mail + SMTP):** cấu hình ở `backend/src/main/resources/mail.properties` (không commit; mẫu `mail.properties.example`). Dev không có SMTP thì để `mail.enabled=false` — nội dung email (kể cả link đặt lại mật khẩu) được in ra log Tomcat. Dùng Gmail: bật xác thực 2 bước và tạo "App password".

**Quên mật khẩu — thiết kế an toàn:** token ngẫu nhiên 32 byte chỉ nằm trong link email; DB chỉ lưu bản băm SHA-256 + thời điểm hết hạn (cột `users.reset_token_hash/reset_token_expiry`); token dùng 1 lần; email có tồn tại hay không đều trả cùng một thông báo (chống dò email).
