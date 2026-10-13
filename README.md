# Web Ecommerce — Đồ án

Đồ án web ecommerce: Java Servlet/JSP (NetBeans) + Tomcat + PostgreSQL.

## Tech stack (chốt)

| Layer | Công nghệ |
|---|---|
| Backend / View | Java Servlet + JSP (NetBeans), Tomcat 10.1, PostgreSQL |
| CSS | Tailwind CSS (CDN) |
| Component có sẵn | daisyUI (CDN) |
| Tương tác không reload trang | htmx (CDN) |
| Gửi email (xác nhận đơn, chào mừng, quên mật khẩu) | Jakarta Mail (Angus Mail) qua SMTP |

Không tách frontend riêng, không REST API, không build pipeline (npm/Vite) — 1 codebase JSP duy nhất, cả team code chung, không ai cần học framework mới. Xem `backend/src/main/webapp/WEB-INF/views/common/head.jspf` để biết cách nhúng 3 thư viện CDN, và `customer/products.jsp` làm trang mẫu.

## Cấu trúc repo

```
.
├── backend/     Project NetBeans (servlet/JSP) — move project hiện tại vào đây
└── docs/        Tài liệu thiết kế: feature list, class diagram
```

## Chạy thử

1. Mở `backend/` bằng NetBeans, tạo DB rỗng: `createdb ecommerce_db`.
2. **DB riêng mỗi máy:** copy `backend/src/main/resources/db.properties.example` → `db.properties`, sửa user/mật khẩu Postgres của máy bạn (file này không bị commit).
3. **Email (tuỳ chọn):** copy `mail.properties.example` → `mail.properties`. Để `mail.enabled=false` thì email chỉ được in ra log Tomcat (kể cả link đặt lại mật khẩu) — đủ để dev. Muốn gửi thật: dùng Gmail + App password (hoặc SMTP giả: `python3 -m smtpd -n -c DebuggingServer localhost:1025`).
4. Deploy lên Tomcat 10.1. Lần chạy đầu Hibernate tự tạo bảng và `DataSeeder` nạp dữ liệu mẫu (danh mục, sản phẩm, tài khoản dev).

**Khi entity thay đổi** (có commit sửa/xoá entity hoặc cột): `hibernate.hbm2ddl.auto=update` chỉ thêm bảng/cột, **không xoá** bảng/cột cũ, nên DB cũ có thể lỗi khi chạy. Cách xử lý cho DB dev: tạo lại DB (`dropdb ecommerce_db && createdb ecommerce_db`, dữ liệu mẫu tự nạp lại) và **Clean and Build** trong NetBeans (hoặc `mvn clean package`) để xoá các file `.class` cũ còn sót trong `target/`.

## Tính năng đã có
- Danh sách sản phẩm: tìm kiếm, lọc, sắp xếp, phân trang (`/products`)
- Đăng ký (gửi email chào mừng), đăng nhập, đăng xuất, **quên mật khẩu qua email**
- Gửi email xác nhận đơn hàng khi thanh toán xong (`EmailService.sendOrderConfirmation`, nối vào checkout khi làm xong luồng đặt hàng)

## Tài liệu
- [Feature list](docs/feature-list.md)
- [Class diagram & entity design](docs/class-diagram.md)
- [Đặc tả endpoint (Servlet)](docs/api-spec.md)
- Mockup giao diện: `backend/src/main/webapp/mockup/index.html`
