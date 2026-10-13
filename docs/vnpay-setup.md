# Cấu hình VNPay cho Nông Việt

Ứng dụng dùng Servlet → Service → DAO, VNPay PAY 2.1.0 / HMAC-SHA512.
Trang khách vẫn là JSP/htmx. Không cần đổi Maven, Java, Tomcat hoặc thư viện để chọn môi trường VNPay.

## Sandbox để chạy đồ án

1. Copy `backend/src/main/resources/vnpay.properties.example` thành `vnpay.properties`.
2. Điền TmnCode / HashSecret của merchant **sandbox**; giữ `environment=sandbox` và các endpoint sandbox trong mẫu.
3. Điền Return/IPN HTTPS cùng domain và context path của ứng dụng. Đăng ký IPN ở phía VNPay; chỉ sửa file local chưa đủ.
4. Khi cấu hình xong, đặt `enabled=true`, Clean and Build → Run trong NetBeans.

File local chứa bí mật được Git bỏ qua. Tunnel trong `backend/tests/` chỉ hỗ trợ kiểm thử trên máy cá nhân, không deploy cùng WAR.

## Production để nhận thanh toán thật

### Bước 1 — đăng ký đúng dịch vụ

Liên hệ VNPay qua trang chính thức và yêu cầu **Cổng thanh toán VNPAY PAY cho website**.
Đăng ký merchant sandbox ở `sandbox.vnpayment.vn/devreg` chỉ phục vụ kiểm thử;
thay số tài khoản ngân hàng cá nhân hoặc dùng thẻ thật ở sandbox không biến giao dịch thành thanh toán thật.
VNPay xác nhận điều kiện đăng ký, hồ sơ, hợp đồng, tài khoản nhận tiền và thông tin kết nối cho từng merchant;
nhóm không tự giả định một tài khoản sandbox đã đủ điều kiện production.

Thông tin cần nhận: TmnCode/HashSecret **production**, payment URL, QueryDr URL,
Merchant Admin production và yêu cầu đăng ký IPN/IP server. Không gửi HashSecret hoặc mật khẩu lên chat/PR.
Nguồn: [VNPay chính thức](https://vnpay.vn/),
[giới thiệu dịch vụ và điều kiện đăng ký đối tác](https://doitac.vnpay.vn/hdsd/pages/hdsd-merchant-omni/faq.html).

### Bước 2 — chuẩn bị triển khai cùng nhóm

Giữ nguyên Java/Servlet/Maven/Tomcat của dự án. Cần một deployment có tên miền HTTPS ổn định,
Tomcat và PostgreSQL riêng; có thể đặt HTTPS tại reverse proxy do nhóm cấu hình.
Chọn context path trước khi gửi URL cho VNPay: deploy ở `/ecommerce` thì callback chứa `/ecommerce`,
deploy ở `/` thì không có phần đó. IPN nhận GET trực tiếp, không bắt đăng nhập hoặc redirect tới trang login.

`queryIpAddress` là **IPv4 public của server gọi QueryDr**, không phải IP máy khách hay IP nội bộ của container.
Nếu server qua NAT, xác nhận IP public phía VNPay nhìn thấy và yêu cầu whitelist với họ.
Production từ chối private/loopback/link-local, CGNAT (`100.64.0.0/10`), các dải tài liệu và benchmark.
Đây là kiểm tra cú pháp/dải địa chỉ, không thay cho kiểm tra DNS, TLS và khả năng kết nối thực tế.
Nguồn phân loại: [IANA IPv4 Special-Purpose Address Registry](https://www.iana.org/assignments/iana-ipv4-special-registry).

**Dữ liệu mẫu:** `AppInitListener` hiện gọi `DataSeeder` và `DemoDataSeeder` khi app khởi động.
Trước khi đưa production cho khách, nhóm phải thống nhất cách không nạp tài khoản/đơn mẫu và cách tạo Admin thật
trên nhánh deploy; việc đó chưa được thay trong nhánh VNPay này.
Không dùng DB local đã có đơn sandbox hoặc gửi nguyên WAR build bằng credential sandbox lên server production.

### Bước 3 — cấu hình và đăng ký callback

1. Làm việc với VNPay để đăng ký merchant dùng dịch vụ PAY thật và hoàn tất điều kiện kết nối/kiểm thử mà VNPay yêu cầu.
   Bộ tài khoản sandbox đã nhận **không** dùng để thu tiền thật. Nhận riêng TmnCode, HashSecret, URL thanh toán và API QueryDr production.
2. Chuẩn bị deployment Tomcat/PostgreSQL **riêng** với tên miền HTTPS ổn định và IPN truy cập được từ máy chủ VNPay.
   Không chuyển môi trường trên DB còn đơn sandbox. Cấu hình DB, SMTP, seed accounts và HTTPS/proxy theo nhánh deploy của nhóm.
3. Điền cấu hình production bên dưới bằng thông tin VNPay cấp. Code hiện chấp nhận endpoint production HTTPS trên host `pay.vnpay.vn`;
   nếu VNPay cấp host khác, cần xác minh với họ và cập nhật danh sách host được phép trước khi sử dụng.
4. Gửi/đăng ký **URL IPN production** với VNPay; xác nhận IP máy chủ gọi QueryDr và whitelist nếu phía VNPay yêu cầu.
5. Khi deployment và merchant sẵn sàng, bật VNPay, build/deploy lại và thực hiện kiểm thử production theo hướng dẫn của VNPay.
   Kiểm tra đúng số tiền, IPN cập nhật DB, Return hiển thị kết quả, email, gọi lặp và đối soát. Đối chiếu giao dịch với Merchant Admin thật.

```properties
# Mẫu minh hoạ; không đưa credential thật vào Git hoặc chat.
vnpay.enabled=false
vnpay.environment=production
vnpay.tmnCode=MA_MERCHANT_PRODUCTION
vnpay.hashSecret=KHOA_PRODUCTION_DO_VNPAY_CAP
vnpay.paymentUrl=URL_THANH_TOAN_PRODUCTION_DO_VNPAY_CAP
vnpay.queryUrl=URL_QUERYDR_PRODUCTION_DO_VNPAY_CAP
vnpay.queryIpAddress=IPV4_CONG_KHAI_CUA_SERVER
vnpay.returnUrl=https://shop.example/ecommerce/payment/vnpay/return
vnpay.ipnUrl=https://shop.example/ecommerce/payment/vnpay/ipn
```

`shop.example`, các giá trị in hoa và `/ecommerce` là minh hoạ, phải thay bằng thông tin deployment thật.
TmnCode phải gồm đúng 8 chữ/số. Nếu ứng dụng deploy ở `/`, bỏ `/ecommerce` khỏi cả hai callback.
Giữ `enabled=false` khi chưa sẵn sàng; bật `true` khi chuẩn bị kiểm thử merchant production được chấp thuận.

Có thể dùng biến môi trường thay file: `VNPAY_ENABLED`, `VNPAY_ENVIRONMENT`, `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`,
`VNPAY_PAYMENT_URL`, `VNPAY_QUERY_URL`, `VNPAY_QUERY_IP_ADDRESS`, `VNPAY_RETURN_URL`, `VNPAY_IPN_URL`.
Biến môi trường có ưu tiên hơn file. Cấu hình được nạp khi khởi tạo app, nên cần redeploy/restart sau khi đổi.
Không đặt khoá trong source, URL trình duyệt, PR hoặc ảnh chụp log.

Nếu server đứng sau reverse proxy, tầng deploy cần thiết lập proxy tin cậy để `request.getRemoteAddr()` phản ánh IP khách;
code thanh toán không tự tin header `X-Forwarded-For` từ mọi nguồn. Kiểm tra HTTPS và cookie phiên đăng nhập trên deployment thật.

### Bước 4 — kiểm tra trước khi bật

- Ưu tiên đặt `VNPAY_*` trong môi trường tiến trình/container Tomcat trên server; NetBeans vẫn có thể dùng file local.
  File trong `src/main/resources` được Maven đóng vào WAR dù Git bỏ qua, nên build cho production từ checkout sạch
  và cấp cấu hình bí mật riêng trên server. Không commit `db.properties`, `mail.properties` hoặc `vnpay.properties`.
- Để `enabled=false` trong lúc chuẩn bị. Nhập đúng bộ production và callback/IP thực tế, đăng ký IPN với VNPay,
  rồi restart/redeploy Tomcat để nạp cấu hình mới. Không cần sửa thuật toán ký/IPN chỉ để chọn production
  nếu bộ endpoint do VNPay cấp phù hợp với contract PAY 2.1.0 hiện tại.
- Kiểm tra IPN public không bị chuyển tới login. Mở URL IPN không có tham số hợp lệ chỉ nhận JSON lỗi,
  không phải bằng chứng thanh toán; một callback có chữ ký/số tiền đúng mới được ghi kết quả.
- Chỉ khi merchant và deployment được chấp thuận mới thực hiện giao dịch production theo kế hoạch kiểm thử của VNPay.
  Đối chiếu số tiền và mã giao dịch giữa Order/Payment, email và Merchant Admin; thử callback lặp để xác nhận
  không cộng/trừ kho, tạo Notification hoặc xếp email hai lần. Return thành công không thay cho IPN.
- Nếu khách đã trả tiền nhưng app còn chờ, dùng **Kiểm tra thanh toán** trên kết quả đơn của chính khách để QueryDr.
  Không tạo lại đơn và không tự sửa Payment thành SUCCESS trong DB. Huỷ đơn đã trả tiền cần cửa hàng xử lý hoàn tiền;
  nhánh này chưa gọi API refund tự động.

Trong NetBeans: mở `backend` → Clean and Build → Run; kiểm tra lại Login → Profile → Cart → Checkout →
History → Track/Notifications trên bản `dev` đã merge trước khi phát hành bản triển khai.

## Kết quả thanh toán và giới hạn phạm vi

- Return chỉ kiểm tra và hiển thị trạng thái DB. IPN hoặc QueryDr đã xác thực mới cập nhật thanh toán.
- Nút **Kiểm tra thanh toán** trên trang kết quả dùng POST có token session, mỗi phiên chờ 5 phút giữa các lần kiểm tra.
- Một lần thành công tạo một Notification và xếp một email sau commit; gọi lại không tạo bản sao. SMTP lỗi không rollback đơn.
- Thành công đến sau khi huỷ: lưu đã thanh toán, giữ đơn huỷ, báo cửa hàng đối soát/hoàn tiền thủ công.
- Chưa có hoàn tiền tự động hoặc đối soát theo lịch. Lịch sử/theo dõi đơn thuộc hai nhánh Registered Customer tương ứng.
- Đã kiểm thử sandbox, DB và SMTP local; chưa thực hiện giao dịch production hoặc chứng nhận deployment production.

Tài liệu chính thức: [PAY/IPN/Return](https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html),
[QueryDr và mã trạng thái](https://sandbox.vnpayment.vn/apis/docs/truy-van-hoan-tien/querydr&refund.html),
[Điều khoản dịch vụ VNPay](https://vnpay.vn/chinh-sach/dieu-khoan-dieu-kien-sddv-vnpay-bsddv.html).
