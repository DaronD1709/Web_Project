# Hướng dẫn deploy Nông Việt (cho người làm deploy)

App là **WAR chạy trên Tomcat 10.1 + PostgreSQL**, đóng gói sẵn bằng `Dockerfile` ở thư mục gốc repo.
Nền tảng nào chạy được Docker đều dùng được (Render, Railway, Fly.io, Cloud Run). Ví dụ dưới đây dùng **Render** (chạy app) + **Neon** (Postgres miễn phí).

> Không bao giờ dán mật khẩu/khoá vào file trong repo hay vào chat. Mọi bí mật nhập vào ô **Environment** của trang hosting.

## 1. Tạo database (Neon)
1. Đăng ký neon.tech → tạo project → lấy thông tin kết nối: host, tên DB, user, password.
2. Ghép thành chuỗi JDBC, **bắt buộc có `sslmode=require`**:
   `jdbc:postgresql://<host>/<tên_db>?sslmode=require`
3. Bảng được app **tự tạo** khi chạy lần đầu (`hbm2ddl=update`); dữ liệu mẫu (82 sản phẩm, tài khoản demo) tự nạp.

## 2. Tạo web service (Render)
1. New → **Web Service** → nối repo GitHub `DaronD1709/Web_Project`, nhánh `main` (hoặc `dev`).
2. Runtime: **Docker** (Render tự thấy `Dockerfile` ở gốc). Không cần Build/Start command.
3. Chọn gói Free, đặt tên → Render cấp địa chỉ HTTPS dạng `https://<tên>.onrender.com`.
4. (Khuyến nghị) gắn **Disk** vào `/data/uploads` để ảnh Admin tải lên không mất khi khởi động lại (gói Free không có disk: ảnh tải lên sẽ mất, ảnh sản phẩm mẫu vẫn còn vì nằm trong WAR).

## 3. Biến môi trường (Environment)
| Biến | Giá trị | Bắt buộc |
|---|---|---|
| `DB_URL` | chuỗi JDBC ở bước 1 | ✔ |
| `DB_USER` | user Neon | ✔ |
| `DB_PASSWORD` | password Neon | ✔ |
| `ADMIN_PASSWORD` | mật khẩu Admin mới (đừng dùng `Admin@123`: repo công khai) | ✔ |
| `DB_SHOW_SQL` | `false` (tắt in SQL ra log) | nên có |
| `APP_BASE_URL` | `https://<tên>.onrender.com` (không có `/` cuối) — dùng cho link trong email | ✔ |
| `MAIL_ENABLED` | `true` để gửi email thật | tuỳ chọn |
| `MAIL_HOST` / `MAIL_PORT` | `smtp.gmail.com` / `587` | nếu gửi mail |
| `MAIL_USERNAME` / `MAIL_FROM` | Gmail gửi | nếu gửi mail |
| `MAIL_PASSWORD` | **App password** Gmail 16 ký tự (không phải mật khẩu Gmail) | nếu gửi mail |
| `UPLOAD_DIR` | mặc định `/data/uploads` | không |
| `VNPAY_*` | xem mục 4 | nếu dùng VNPay |

`ADMIN_PASSWORD` chỉ có tác dụng **lần đầu** (lúc tạo tài khoản Admin trên DB trống). Nếu DB đã có Admin với mật khẩu cũ thì đổi trực tiếp trong DB.

## 4. VNPay sandbox (chỉ Lộc — người giữ merchant sandbox)
Thêm các biến sau (địa chỉ phải là HTTPS của chính web vừa deploy, **cùng host**, đúng đuôi):

| Biến | Giá trị |
|---|---|
| `VNPAY_ENABLED` | `true` |
| `VNPAY_ENVIRONMENT` | `sandbox` |
| `VNPAY_TMN_CODE` | TmnCode sandbox (8 ký tự) |
| `VNPAY_HASH_SECRET` | HashSecret sandbox |
| `VNPAY_PAYMENT_URL` | `https://sandbox.vnpayment.vn/paymentv2/vpcpay.html` |
| `VNPAY_QUERY_URL` | `https://sandbox.vnpayment.vn/merchant_webapi/api/transaction` |
| `VNPAY_QUERY_IP_ADDRESS` | `127.0.0.1` (sandbox chấp nhận) |
| `VNPAY_RETURN_URL` | `https://<tên>.onrender.com/payment/vnpay/return` |
| `VNPAY_IPN_URL` | `https://<tên>.onrender.com/payment/vnpay/ipn` |

Sau khi deploy, **vào trang quản lý sandbox của VNPay và đổi URL IPN** thành `VNPAY_IPN_URL` ở trên. VNPay chỉ cho đăng ký 1 IPN mỗi merchant, nên từ lúc đó máy Lộc không nhận IPN nữa.
Nếu IPN không tới, khách vẫn bấm nút "Kiểm tra thanh toán" ở trang kết quả để đối soát (QueryDr).
Chi tiết giao thức: `docs/vnpay-setup.md`.

## 5. Kiểm tra sau khi deploy
- [ ] Mở trang chủ, xem được sản phẩm + ảnh.
- [ ] `/admin/login` đăng nhập bằng `admin@nongviet.vn` + `ADMIN_PASSWORD` (mật khẩu mặc định `Admin@123` phải **không** vào được).
- [ ] Đăng ký tài khoản khách bằng email thật → nhận email chào mừng (xem cả Spam).
- [ ] Đặt 1 đơn COD → nhận email xác nhận; Admin đổi trạng thái → khách thấy thông báo.
- [ ] (Nếu bật VNPay) đặt 1 đơn VNPay bằng thẻ test → đơn thành "đã thanh toán".
- [ ] Chat khách vãng lai ↔ Admin.

## 6. Lưu ý gói miễn phí
- Server **ngủ** sau một thời gian không có truy cập; lần mở đầu chậm 30–60 giây → mở link trước giờ demo 5 phút.
- Database Neon Free có giới hạn dung lượng/thời gian; kiểm tra trước ngày nộp.
- Mỗi lần deploy lại hoặc khởi động lại, ảnh tải lên ở gói không có Disk sẽ mất.

## 7. Chạy thử Docker trên máy (tuỳ chọn)
```bash
docker build -t nongviet .
docker run -p 8080:8080 \
  -e DB_URL=jdbc:postgresql://host.docker.internal:5432/ecommerce_db \
  -e DB_USER=postgres -e DB_PASSWORD=... -e ADMIN_PASSWORD=... -e DB_SHOW_SQL=false nongviet
# mở http://localhost:8080
```
