package com.ecommerce.service;

import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.PaymentStatus;
import com.ecommerce.entity.VNPayPayment;
import com.ecommerce.util.VNPayConfig;
import com.ecommerce.util.VNPaySignature;
import com.ecommerce.dao.VNPayDAO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.logging.Logger;

/** Tạo yêu cầu thanh toán và xử lý kết quả đã xác thực từ IPN/QueryDr. */
public class VNPayService {

    private static final ZoneId VN_ZONE =
            ZoneId.of("Asia/Ho_Chi_Minh");

    private static final DateTimeFormatter GATEWAY_TIME =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VNPayConfig config;
    private final VNPayQueryClient queryClient;

    private static final Logger LOG =
        Logger.getLogger(VNPayService.class.getName());

    private final VNPayDAO vnPayDAO = new VNPayDAO();
    private final EmailService emailService = new EmailService();

    public VNPayService() {
        this(new VNPayConfig());
    }

    private VNPayService(VNPayConfig config) {
        this(config, new VNPayQueryClient(config));
    }

    // Constructor nội bộ giúp kiểm thử gateway lỗi mà không gọi thanh toán thật.
    VNPayService(VNPayConfig config, VNPayQueryClient queryClient) {
        this.config = config;
        this.queryClient = queryClient;
    }

    public boolean isAvailable() {
        return config.isReady();
    }

    public boolean isSandbox() {
        return !config.isProduction();
    }

    public void requireReady() {
        if (!isAvailable()) {
            throw new BusinessException(
                    "VNPay chưa sẵn sàng. Vui lòng chọn COD.");
        }
    }

    public long toGatewayAmount(double amount) {
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new BusinessException(
                    "Số tiền thanh toán VNPay không hợp lệ.");
        }

        try {
            // Dùng BigDecimal để nhân 100 chính xác.
            // Không ép double sang long vì có thể mất phần tiền lẻ.
            long scaled = BigDecimal.valueOf(amount)
                    .movePointRight(2).longValueExact();

            if (scaled < 1 || scaled > 999_999_999_999L) {
                throw new ArithmeticException();
            }

            return scaled;
        } catch (ArithmeticException e) {
            throw new BusinessException(
                    "Số tiền vượt giới hạn hoặc có phần lẻ "
                    + "không hợp lệ với VNPay.");
        }
    }

    //chỉ tạo URL cho đơn VNPay đang chờ thanh toán; kiểm tra tiền của Order khớp Payment.
    public String buildPaymentUrl(Order order, String clientIp) {
        requireReady();

        if (order == null
                || order.getId() == null || order.getId() < 1
                || order.getOrderDate() == null
                || order.getStatus() != OrderStatus.PENDING
                || !(order.getPayment() instanceof VNPayPayment payment)
                || payment.getStatus() != PaymentStatus.PENDING) {
            throw new BusinessException(
                    "Đơn hàng không thể thanh toán VNPay.");
        }

        // Lấy số tiền đã chốt trong đơn, không lấy từ trình duyệt.
        long amount = toGatewayAmount(order.getTotalAmount());

        if (amount != toGatewayAmount(payment.getAmount())) {
            throw new BusinessException(
                    "Số tiền của đơn và thanh toán không khớp.");
        }

        if (clientIp == null
                || clientIp.length() < 3 || clientIp.length() > 45
                || !clientIp.matches("[0-9a-fA-F:.]+")) {
            throw new BusinessException(
                    "Địa chỉ IP thanh toán không hợp lệ.");
        }

        // Giữ nguyên hạn theo lúc đặt đơn.
        // Gửi lại form không kéo dài thời gian thanh toán.
        LocalDateTime created = order.getOrderDate().withNano(0);
        LocalDateTime expires = created.plusMinutes(15);
        LocalDateTime now = LocalDateTime.now(VN_ZONE);

        if (created.isAfter(now) || !now.isBefore(expires)) {
            throw new BusinessException(
                    "Đơn đã hết hạn hoặc có thời gian không hợp lệ.");
        }

        Map<String, String> fields = new TreeMap<>();

        fields.put("vnp_Version", "2.1.0");
        fields.put("vnp_Command", "pay");
        fields.put("vnp_TmnCode", config.getTmnCode());
        fields.put("vnp_Amount", Long.toString(amount));
        fields.put("vnp_CurrCode", "VND");
        fields.put("vnp_TxnRef", order.getId().toString());
        fields.put("vnp_OrderInfo",
                "Thanh toan don hang " + order.getId());
        fields.put("vnp_OrderType", "other");
        fields.put("vnp_Locale", "vn");
        fields.put("vnp_IpAddr", clientIp);
        fields.put("vnp_ReturnUrl", config.getReturnUrl());
        fields.put("vnp_CreateDate", created.format(GATEWAY_TIME));
        fields.put("vnp_ExpireDate", expires.format(GATEWAY_TIME));

        // IPN URL đăng ký với VNPay,
        // không truyền trong URL thanh toán.
        String query = VNPaySignature.buildQuery(fields);
        String hash = VNPaySignature.sign(
                fields, config.getHashSecret());

        return config.getPaymentUrl()
                + "?" + query + "&vnp_SecureHash=" + hash;
    }

    /** Kiểm tra chữ ký và đúng merchant; không cập nhật dữ liệu. */
    public boolean verifyResponse(Map<String, String> fields) {
        return fields != null && isAvailable()
                && VNPaySignature.verify(fields, config.getHashSecret())
                && config.getTmnCode().equals(fields.get("vnp_TmnCode"));
    }

    public String processIpn(Map<String, String[]> parameters) {
        if (!isAvailable()) {
            return "99";
        }

        Map<String, String> fields;
        try {
            fields = VNPaySignature.readFields(parameters);
        } catch (IllegalArgumentException e) {
            return "97";
        }

        // Kiểm tra chữ ký trước khi đọc nghiệp vụ hoặc cập nhật DB.
        if (!verifyResponse(fields)) {
            return "97";
        }

        try {
            int orderId = Integer.parseInt(requiredField(
                    fields, "vnp_TxnRef", "[1-9][0-9]{0,9}"));

            long amount = Long.parseLong(requiredField(
                    fields, "vnp_Amount", "[0-9]{1,12}"));

            String response = requiredField(
                    fields, "vnp_ResponseCode", "[0-9]{2}");

            String status = requiredField(
                    fields, "vnp_TransactionStatus", "[0-9]{2}");

            String transactionNo = requiredField(
                    fields, "vnp_TransactionNo", "[0-9]{1,15}");

            requiredField(fields, "vnp_BankCode", "[a-zA-Z0-9]{3,20}");

            if (!("Thanh toan don hang " + orderId)
                    .equals(fields.get("vnp_OrderInfo"))
                    || (fields.containsKey("vnp_CurrCode")
                        && !"VND".equals(fields.get("vnp_CurrCode")))) {
                return "99";
            }

            boolean success = "00".equals(response)
                    && "00".equals(status);

            boolean failed = Set.of(
                    "09", "10", "11", "12", "13", "24",
                    "51", "65", "75", "79", "99").contains(response)
                    && "02".equals(status);

            // Mã 07 hoặc kết quả chưa rõ cần đối soát,
            // không tự kết luận thất bại rồi hoàn kho.
            if ((!success && !failed)
                    || (success && Long.parseLong(transactionNo) == 0)) {
                return "99";
            }

            String payDate = fields.get("vnp_PayDate");

            // PayDate là tuỳ chọn; thiếu thì ghi thời điểm app nhận kết quả.
            LocalDateTime paidAt = payDate == null || payDate.isEmpty()
                    ? LocalDateTime.now(VN_ZONE)
                    : LocalDateTime.parse(
                            payDate,
                            DateTimeFormatter.ofPattern("uuuuMMddHHmmss")
                                    .withResolverStyle(ResolverStyle.STRICT));

            return applyVerifiedResult(orderId, amount, success,
                    transactionNo, paidAt, null);

        } catch (RuntimeException e) {
            // Không ghi payload, chữ ký hoặc khoá bí mật vào log.
            LOG.warning("Chưa xử lý được IPN VNPay: "
                    + e.getClass().getSimpleName());
            return "99";
        }
    }

    /** Chỉ OrderService gọi sau khi kiểm tra đơn thuộc Customer đang đăng nhập. */
    public void reconcile(Order ownOrder, Integer customerId) {
        requireReady();
        if (ownOrder == null || customerId == null
                || !customerId.equals(ownOrder.getCustomer().getId())) {
            throw new SecurityException("Bạn không có quyền đối soát đơn hàng này.");
        }
        if (!(ownOrder.getPayment() instanceof VNPayPayment payment)
                || ownOrder.getOrderDate() == null) {
            throw new BusinessException("Đơn hàng không dùng thanh toán VNPay.");
        }
        if (payment.getStatus() == PaymentStatus.SUCCESS) {
            return; // Gửi lại POST không gọi gateway hay gửi lại email.
        }

        // Gọi mạng trước khi khoá DB, không giữ khoá đơn trong lúc chờ VNPay.
        Map<String, String> fields = queryClient.query(ownOrder);
        String command = fields.getOrDefault("vnp_Command", "");
        if (!config.getTmnCode().equals(fields.get("vnp_TmnCode"))
                || !ownOrder.getId().toString().equals(fields.get("vnp_TxnRef"))
                || (!command.isEmpty() && !"querydr".equals(command))) {
            throw new BusinessException("Kết quả đối soát không khớp đơn hàng.");
        }
        String response = fields.get("vnp_ResponseCode");
        if (!"00".equals(response)) {
            throw new BusinessException("94".equals(response)
                    ? "VNPay giới hạn đối soát. Vui lòng chờ ít nhất 5 phút rồi thử lại."
                    : "Chưa có kết quả đối soát VNPay. Trạng thái thanh toán được giữ nguyên.");
        }

        try {
            long amount = Long.parseLong(requiredField(fields, "vnp_Amount", "[0-9]{1,12}"));
            String status = requiredField(fields, "vnp_TransactionStatus", "[0-9]{2}");
            String transactionNo = requiredField(fields, "vnp_TransactionNo", "[0-9]{1,15}");
            requiredField(fields, "vnp_BankCode", "[a-zA-Z0-9]{3,20}");
            // QueryDr.ResponseCode=00 chỉ nói truy vấn thành công, chưa nói đã trả tiền.
            // Không tự xử lý refund/đảo giao dịch/đang chờ/nghi ngờ gian lận.
            boolean success = "00".equals(status);
            if (!"01".equals(fields.get("vnp_TransactionType"))
                    || (!success && !"02".equals(status))
                    || (success && Long.parseLong(transactionNo) == 0)) {
                throw new BusinessException(
                        "VNPay chưa có kết quả thanh toán cuối cùng. Vui lòng liên hệ cửa hàng nếu đã bị trừ tiền.");
            }
            String payDate = fields.get("vnp_PayDate");
            LocalDateTime paidAt = payDate == null || payDate.isEmpty()
                    ? LocalDateTime.now(VN_ZONE)
                    : LocalDateTime.parse(payDate, DateTimeFormatter.ofPattern("uuuuMMddHHmmss")
                            .withResolverStyle(ResolverStyle.STRICT));
            String code = applyVerifiedResult(ownOrder.getId(), amount, success,
                    transactionNo, paidAt, customerId);
            if (!"00".equals(code) && !"02".equals(code)) {
                throw new BusinessException("Không thể cập nhật kết quả đối soát cho đơn này.");
            }
        } catch (IllegalArgumentException | java.time.DateTimeException e) {
            throw new BusinessException("Kết quả đối soát VNPay không hợp lệ.");
        }
    }

    private String applyVerifiedResult(Integer orderId, long amount, boolean success,
            String transactionNo, LocalDateTime paidAt, Integer customerId) {
        VNPayDAO.IpnResult result = vnPayDAO.processIpn(orderId, order -> {
                // Kiểm tra lại quyền bên trong transaction, sau khi đã có khoá.
                if (customerId != null && !customerId.equals(order.getCustomer().getId())) {
                    throw new SecurityException("Bạn không có quyền đối soát đơn hàng này.");
                }
                if (!(order.getPayment() instanceof VNPayPayment payment)) {
                    return new VNPayDAO.IpnResult("01", null, null);
                }

                // So tiền gateway với cả snapshot đơn và thanh toán.
                if (amount != toGatewayAmount(order.getTotalAmount())
                        || amount != toGatewayAmount(payment.getAmount())) {
                    return new VNPayDAO.IpnResult("04", null, null);
                }

                // Phản hồi lặp không tạo thêm email hoặc Notification.
                if (payment.getStatus() == PaymentStatus.SUCCESS) {
                    String code = success
                            && transactionNo.equals(payment.getTransactionId())
                            ? "02" : "99";

                    return new VNPayDAO.IpnResult(code, null, null);
                }

                if (payment.getStatus() == PaymentStatus.FAILED && !success) {
                    return new VNPayDAO.IpnResult("02", null, null);
                }

                if ((payment.getStatus() != PaymentStatus.PENDING
                        && payment.getStatus() != PaymentStatus.FAILED)
                        || (order.getStatus() != OrderStatus.PENDING
                            && order.getStatus() != OrderStatus.CANCELLED)) {
                    return new VNPayDAO.IpnResult("99", null, null);
                }

                payment.setTransactionId(transactionNo);
                payment.setStatus(
                        success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
                payment.setPaymentDate(success ? paidAt : null);

                boolean cancelled =
                        order.getStatus() == OrderStatus.CANCELLED;

                String text;
                if (success && cancelled) {
                    // Ghi nhận đã trả tiền nhưng không khôi phục đơn bị huỷ.
                    text = "VNPay ghi nhận thanh toán cho đơn #" + orderId
                            + " đã huỷ. Vui lòng liên hệ cửa hàng "
                            + "để đối soát/hoàn tiền.";

                } else if (success) {
                    text = "Đơn hàng #" + orderId
                            + " đã thanh toán VNPay thành công.";

                } else {
                    text = "Thanh toán VNPay của đơn #" + orderId
                            + " không thành công. Đơn hàng đã được huỷ.";
                }

                return new VNPayDAO.IpnResult(
                        "00", success && !cancelled ? order : null, text);
            });

            // DAO đã commit; lỗi email không làm đổi kết quả thanh toán.
            if (result.orderForEmail() != null) {
                try {
                    emailService.sendOrderConfirmation(result.orderForEmail());
                } catch (RuntimeException e) {
                    LOG.warning(
                            "Không thể xếp email xác nhận cho đơn #" + orderId);
                }
            }

        return result.responseCode();
    }

    private String requiredField(
            Map<String, String> fields, String name, String pattern) {
        String value = fields.get(name);

        if (value == null || !value.matches(pattern)) {
            throw new IllegalArgumentException(
                    "Tham số VNPay không hợp lệ.");
        }

        return value;
    }

    /** Kiểm tra Return khớp đơn đã được xác định thuộc khách. */
    public void validateReturnOrder(
            Order order, Map<String, String> fields) {
        if (!(order.getPayment() instanceof VNPayPayment payment)) {
            throw new BusinessException(
                    "Đơn hàng không dùng thanh toán VNPay.");
        }

        long amount;
        try {
            amount = Long.parseLong(requiredField(
                    fields, "vnp_Amount", "[0-9]{1,12}"));
        } catch (IllegalArgumentException e) {
            throw new BusinessException(
                    "Số tiền phản hồi VNPay không hợp lệ.");
        }

        if (amount != toGatewayAmount(order.getTotalAmount())
                || amount != toGatewayAmount(payment.getAmount())
                || !("Thanh toan don hang " + order.getId())
                    .equals(fields.get("vnp_OrderInfo"))
                || (fields.containsKey("vnp_CurrCode")
                    && !"VND".equals(fields.get("vnp_CurrCode")))) {
            throw new BusinessException(
                    "Phản hồi VNPay không khớp đơn hàng.");
        }
    }
}
