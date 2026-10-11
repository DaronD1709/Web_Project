package com.ecommerce.service;

import com.ecommerce.entity.Order;
import com.ecommerce.util.VNPayConfig;
import com.ecommerce.util.VNPayJson;
import com.ecommerce.util.VNPaySignature;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/** Gọi API đối soát server-to-server; không ghi DB hay gửi email. */
public class VNPayQueryClient {

    private static final String[] REQUEST_FIELDS = {
        "vnp_RequestId", "vnp_Version", "vnp_Command", "vnp_TmnCode",
        "vnp_TxnRef", "vnp_TransactionDate", "vnp_CreateDate",
        "vnp_IpAddr", "vnp_OrderInfo"
    };
    private static final String[] RESPONSE_FIELDS = {
        "vnp_ResponseId", "vnp_Command", "vnp_ResponseCode", "vnp_Message",
        "vnp_TmnCode", "vnp_TxnRef", "vnp_Amount", "vnp_BankCode",
        "vnp_PayDate", "vnp_TransactionNo", "vnp_TransactionType",
        "vnp_TransactionStatus", "vnp_OrderInfo",
        "vnp_PromotionCode", "vnp_PromotionAmount"
    };

    private final VNPayConfig config;

    public VNPayQueryClient(VNPayConfig config) {
        this.config = config;
    }

    public Map<String, String> query(Order order) {
        if (!config.isReady()) {
            throw new BusinessException("VNPay chưa sẵn sàng để đối soát.");
        }
        Map<String, String> fields = new LinkedHashMap<>();
        DateTimeFormatter time = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        fields.put("vnp_RequestId", UUID.randomUUID().toString().replace("-", ""));
        fields.put("vnp_Version", "2.1.0");
        fields.put("vnp_Command", "querydr");
        fields.put("vnp_TmnCode", config.getTmnCode());
        fields.put("vnp_TxnRef", order.getId().toString());
        // Phải dùng thời điểm tạo yêu cầu pay gốc, không phải thời điểm Return.
        fields.put("vnp_TransactionDate", order.getOrderDate().format(time));
        fields.put("vnp_CreateDate", LocalDateTime.now(
                ZoneId.of("Asia/Ho_Chi_Minh")).format(time));
        fields.put("vnp_IpAddr", config.getQueryIpAddress());
        fields.put("vnp_OrderInfo", "Query order " + order.getId());
        fields.put("vnp_SecureHash", VNPaySignature.signText(
                signatureText(fields, REQUEST_FIELDS), config.getHashSecret()));

        Map<String, String> response = post(fields);
        // Xác thực cả các ô tuỳ chọn rỗng ở cuối chuỗi, không bỏ dấu |.
        if (!VNPaySignature.verifyText(signatureText(response, RESPONSE_FIELDS),
                response.get("vnp_SecureHash"), config.getHashSecret())) {
            throw new BusinessException("Không xác thực được kết quả đối soát VNPay.");
        }
        return response;
    }

    private String signatureText(Map<String, String> fields, String[] names) {
        String[] values = new String[names.length];
        for (int i = 0; i < names.length; i++) {
            values[i] = fields.getOrDefault(names[i], "");
        }
        return String.join("|", values);
    }

    private Map<String, String> post(Map<String, String> fields) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(config.getQueryUrl())
                    .toURL().openConnection();
            connection.setRequestMethod("POST");
            connection.setInstanceFollowRedirects(false);
            connection.setConnectTimeout(5_000);
            connection.setReadTimeout(10_000);
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("Accept", "application/json");
            byte[] body = VNPayJson.write(fields).getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(body.length);
            connection.setDoOutput(true);
            try (var output = connection.getOutputStream()) {
                output.write(body);
            }
            if (connection.getResponseCode() != 200) {
                throw new IOException("Gateway response unavailable");
            }
            try (var input = connection.getInputStream()) {
                byte[] response = input.readNBytes(16_385);
                if (response.length > 16_384) {
                    throw new IOException("Gateway response too large");
                }
                return VNPayJson.read(new String(response, StandardCharsets.UTF_8));
            }
        } catch (IOException | IllegalArgumentException e) {
            // Lỗi mạng/định dạng không được biến thành kết quả thất bại của đơn.
            throw new BusinessException(
                    "Chưa đối soát được VNPay. Vui lòng thử lại sau hoặc liên hệ cửa hàng.");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
