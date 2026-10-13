package com.ecommerce.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/** Chuẩn hoá tham số và ký/kiểm tra chữ ký VNPay. */
public final class VNPaySignature {

    private VNPaySignature() {
    }

    // sắp tham số và mã hoá URL
    public static String buildQuery(Map<String, String> fields) {
        StringBuilder query = new StringBuilder();

        // Sắp tên tham số để hai bên tạo cùng chuỗi dữ liệu ký.
        for (Map.Entry<String, String> entry
                : new TreeMap<>(fields).entrySet()) {
            String name = entry.getKey();
            String value = entry.getValue();

            // Không đưa chính chữ ký vào dữ liệu dùng để ký.
            if (!name.startsWith("vnp_")
                    || "vnp_SecureHash".equals(name)
                    || "vnp_SecureHashType".equals(name)
                    || value == null || value.isEmpty()) {
                continue;
            }

            if (query.length() > 0) {
                query.append('&');
            }

            query.append(URLEncoder.encode(
                    name, StandardCharsets.UTF_8));
            query.append('=');
            query.append(URLEncoder.encode(
                    value, StandardCharsets.UTF_8));
        }

        return query.toString();
    }

    // Thanh toán/IPN ký query đã sắp xếp và mã hoá URL.
    public static String sign(Map<String, String> fields, String secret) {
        return signText(buildQuery(fields), secret);
    }

    // QueryDr ký chuỗi theo thứ tự riêng, ngăn cách bởi dấu |.
    // Giữ nguyên chuỗi; không trim, sắp xếp hoặc mã hoá URL lần nữa.
    public static String signText(String data, String secret) {
        if (data == null || secret == null || secret.isBlank()) {
            throw new IllegalArgumentException(
                    "Thiếu dữ liệu hoặc khoá ký VNPay.");
        }

        try {
            // Mac riêng cho từng lần gọi, tránh dùng chung trạng thái.
            Mac mac = Mac.getInstance("HmacSHA512");
            mac.init(new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"));

            byte[] signature = mac.doFinal(
                    data.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(signature);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(
                    "Không thể tạo chữ ký VNPay.", e);
        }
    }

    public static boolean verify(Map<String, String> fields, String secret) {
        return verifyText(
                buildQuery(fields), fields.get("vnp_SecureHash"), secret);
    }

    public static boolean verifyText(
            String data, String supplied, String secret) {

        if (data == null || secret == null || secret.isBlank()
                || supplied == null
                || !supplied.matches("[0-9a-fA-F]{128}")) {
            return false;
        }

        byte[] expected = HexFormat.of().parseHex(signText(data, secret));
        byte[] actual = HexFormat.of().parseHex(supplied);

        return MessageDigest.isEqual(expected, actual);
    }

    //lấy tham số từ request, từ chối tham số trùng để tránh xử lý giá trị không thống nhất
    public static Map<String, String> readFields(
            Map<String, String[]> parameters) {
        Map<String, String> fields = new TreeMap<>();

        for (Map.Entry<String, String[]> entry
                : parameters.entrySet()) {
            String name = entry.getKey();
            if (!name.startsWith("vnp_")) {
                continue;
            }

            String[] values = entry.getValue();

            // Không chấp nhận cùng tên tham số có nhiều giá trị.
            if (values == null || values.length != 1
                    || values[0] == null) {
                throw new IllegalArgumentException(
                        "Tham số phản hồi VNPay không hợp lệ.");
            }

            // Servlet đã giải mã URL; giữ nguyên giá trị, không trim.
            fields.put(name, values[0]);
        }

        return fields;
    }
}