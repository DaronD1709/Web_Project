package com.ecommerce.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Properties;
import java.util.logging.Logger;

/** Đọc cấu hình VNPay riêng của máy, không chứa khoá trong code. */
public class VNPayConfig {

    private static final Logger LOG =
            Logger.getLogger(VNPayConfig.class.getName());

    private static final String SANDBOX_URL =
            "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private static final String SANDBOX_QUERY_URL =
            "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";

    private final Properties properties = loadProperties();

    public boolean isEnabled() {
        return "true".equalsIgnoreCase(value("vnpay.enabled"));
    }

    public String getTmnCode() {
        return value("vnpay.tmnCode");
    }

    public String getHashSecret() {
        return value("vnpay.hashSecret");
    }

    public String getPaymentUrl() {
        String configured = value("vnpay.paymentUrl");
        return configured.isEmpty() && !isProduction() ? SANDBOX_URL : configured;
    }

    public String getQueryUrl() {
        String configured = value("vnpay.queryUrl");
        return configured.isEmpty() && !isProduction()
                ? SANDBOX_QUERY_URL : configured;
    }

    public String getEnvironment() {
        String configured = value("vnpay.environment");
        return configured.isEmpty() ? "sandbox" : configured;
    }

    public boolean isProduction() {
        return "production".equals(getEnvironment());
    }

    public String getQueryIpAddress() {
        String configured = value("vnpay.queryIpAddress");
        return configured.isEmpty() && !isProduction() ? "127.0.0.1" : configured;
    }

    public String getReturnUrl() {
        return value("vnpay.returnUrl");
    }

    public String getIpnUrl() {
        return value("vnpay.ipnUrl");
    }

    public boolean isReady() {
        // Production phải được chọn rõ; không tự chuyển môi trường theo URL.
        return isEnabled()
                && isEnvironmentValid()
                && getTmnCode().matches("[a-zA-Z0-9]{8}")
                && !getHashSecret().isBlank()
                && isGatewayUrl(getPaymentUrl(), SANDBOX_URL)
                && isGatewayUrl(getQueryUrl(), SANDBOX_QUERY_URL)
                && isQueryIpValid()
                && isHttpsUrl(getReturnUrl())
                && isHttpsUrl(getIpnUrl())
                && callbacksMatch()
                && (!isProduction()
                    || (isPublicCallback(getReturnUrl()) && isPublicCallback(getIpnUrl())));
    }

    private boolean isEnvironmentValid() {
        return "sandbox".equals(getEnvironment()) || isProduction();
    }

    private String value(String key) {
        // Chỉ VNPay đọc thêm biến môi trường; không đổi cấu hình DB/SMTP của nhóm.
        String envName = key.replace("vnpay.", "VNPAY_")
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .toUpperCase(java.util.Locale.ROOT);
        String configured = System.getenv(envName);
        return (configured == null ? properties.getProperty(key, "") : configured).trim();
    }

    private boolean isGatewayUrl(String value, String sandboxUrl) {
        if (!isProduction()) {
            return sandboxUrl.equals(value);
        }
        if (!isHttpsUrl(value)) {
            return false;
        }
        URI uri = URI.create(value);
        // Chỉ gửi khoá/chữ ký tới host cổng VNPay thật; URL do VNPay cấp.
        return "pay.vnpay.vn".equalsIgnoreCase(uri.getHost())
                && (uri.getPort() == -1 || uri.getPort() == 443)
                && uri.getPath() != null && !uri.getPath().isBlank();
    }

    private boolean callbacksMatch() {
        URI callback = URI.create(getReturnUrl());
        String path = callback.getRawPath();
        String suffix = "/payment/vnpay/return";
        if (path == null || !path.endsWith(suffix)) {
            return false;
        }
        String expectedIpnPath = path.substring(0, path.length() - suffix.length())
                + "/payment/vnpay/ipn";
        URI ipn = URI.create(getIpnUrl());
        // HTTPS không ghi port và :443 là cùng origin.
        int returnPort = callback.getPort() == -1 ? 443 : callback.getPort();
        int ipnPort = ipn.getPort() == -1 ? 443 : ipn.getPort();
        return callback.getHost().equalsIgnoreCase(ipn.getHost())
                && returnPort == ipnPort
                && expectedIpnPath.equals(ipn.getRawPath());
    }

    private boolean isQueryIpValid() {
        String value = getQueryIpAddress();
        if (!value.matches("[0-9]{1,3}(\\.[0-9]{1,3}){3}")) {
            return false;
        }
        String[] parts = value.split("\\.");
        for (String part : parts) {
            if (Integer.parseInt(part) > 255) {
                return false;
            }
        }
        return !isProduction() || !isNonPublicIpv4(parts);
    }

    private boolean isNonPublicIpv4(String[] parts) {
        if (parts.length != 4) {
            return true;
        }
        for (String part : parts) {
            if (!part.matches("[0-9]{1,3}") || Integer.parseInt(part) > 255) {
                return true;
            }
        }
        int first = Integer.parseInt(parts[0]);
        int second = Integer.parseInt(parts[1]);
        int third = Integer.parseInt(parts[2]);
        int fourth = Integer.parseInt(parts[3]);
        // Ngoài private/loopback, CGNAT và các dải tài liệu/benchmark
        // cũng không phải IP public để VNPay kết nối production.
        return first == 0 || first == 10 || first == 127 || first >= 224
                || (first == 100 && second >= 64 && second <= 127)
                || (first == 169 && second == 254)
                || (first == 172 && second >= 16 && second <= 31)
                || (first == 192 && second == 168)
                || (first == 192 && second == 0 && third == 0
                    && fourth != 9 && fourth != 10)
                || (first == 192 && second == 0 && third == 2)
                || (first == 198 && (second == 18 || second == 19))
                || (first == 198 && second == 51 && third == 100)
                || (first == 203 && second == 0 && third == 113);
    }

    private boolean isPublicCallback(String value) {
        String host = URI.create(value).getHost().toLowerCase(java.util.Locale.ROOT);
        if (!host.contains(".") || host.endsWith(".localhost")
                || host.endsWith(".local") || host.endsWith(".invalid")
                || host.endsWith(".trycloudflare.com") || host.contains(":")) {
            return false;
        }
        return !host.matches("[0-9.]+") || !isNonPublicIpv4(host.split("\\."));
    }

    private boolean isHttpsUrl(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getRawUserInfo() == null
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private Properties loadProperties() {
        Properties result = new Properties();

        try (InputStream in = VNPayConfig.class
                .getResourceAsStream("/vnpay.properties")) {
            if (in != null) {
                result.load(in);
            }
        } catch (IOException | IllegalArgumentException e) {
            // Cấu hình lỗi chỉ khiến VNPay chưa sẵn sàng.
            // Không in nội dung cấu hình hoặc khoá bí mật.
            result.clear();
            LOG.warning("Không đọc được cấu hình VNPay.");
        }

        return result;
    }
}
