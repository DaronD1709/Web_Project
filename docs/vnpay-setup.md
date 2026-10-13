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
