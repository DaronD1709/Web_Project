package com.ecommerce.service;

import com.ecommerce.entity.Address;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.User;
import com.ecommerce.entity.VNPayPayment;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.io.IOException;
import java.io.InputStream;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gui email bang Jakarta Mail (JavaMail) qua SMTP.
 *
 * Cach dung (trong OrderService.checkout, SAU KHI transaction dat hang da commit):
 *     emailService.sendOrderConfirmation(order);
 *
 * Thiet ke:
 *  - Noi dung email duoc DUNG NGAY tren thread goi (luc do Order con du items/address trong bo nho,
 *    tranh LazyInitializationException), chi viec GUI moi chay nen o thread rieng -> dat hang
 *    khong bi cham boi SMTP, va gui loi (SMTP sai/mat mang) chi ghi log, KHONG lam hong don hang.
 *  - Cau hinh o src/main/resources/mail.properties (KHONG commit, da .gitignore; mau: mail.properties.example).
 *    Neu thieu file hoac mail.enabled=false -> khong gui that, chi in noi dung email ra log (de dev khong can SMTP).
 */
public class EmailService {

    private static final Logger LOG = Logger.getLogger(EmailService.class.getName());
    private static final Locale VI = Locale.forLanguageTag("vi-VN");

    // 1 thread rieng, xep hang gui lan luot; daemon de khong chan Tomcat khi tat.
    private static final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "mail-sender");
        t.setDaemon(true);
        return t;
    });
    private static final Properties config = loadConfig();

    /** Goi khi app dung (AppInitListener.contextDestroyed): doi not cac mail dang cho gui. */
    public static void shutdown() {
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Email xac nhan don hang, gui toi email cua khach dat don. */
    public void sendOrderConfirmation(Order order) {
        String to = order.getCustomer().getEmail();
        String subject = "[NôngViệt] Xác nhận đơn hàng #" + order.getId();
        sendAsync(to, subject, buildOrderConfirmationHtml(order));
    }

    /** Email chao mung sau khi dang ky. */
    public void sendWelcome(User user) {
        String body = "<p>Xin chào <b>" + esc(user.getFullName()) + "</b>,</p>"
                + "<p>Chào mừng bạn đến với NôngViệt! Tài khoản của bạn đã được tạo thành công với email <b>"
                + esc(user.getEmail()) + "</b>.</p>"
                + "<p>Bạn có thể đăng nhập để chọn mua hạt giống, phân bón, dụng cụ và máy móc nông nghiệp, "
                + "theo dõi đơn hàng và chat với shop bất cứ lúc nào.</p>";
        sendAsync(user.getEmail(), "[NôngViệt] Chào mừng bạn đến với NôngViệt", frame(body));
    }

    /** Email chua link dat lai mat khau (link het han sau 30 phut, dung 1 lan). */
    public void sendPasswordReset(User user, String resetLink) {
        String body = "<p>Xin chào <b>" + esc(user.getFullName()) + "</b>,</p>"
                + "<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản này. Bấm vào nút bên dưới để đặt mật khẩu mới "
                + "(liên kết có hiệu lực <b>30 phút</b> và chỉ dùng được một lần):</p>"
                + "<p style=\"text-align:center;margin:24px 0\"><a href=\"" + esc(resetLink) + "\" "
                + "style=\"background:#2f7d3b;color:#fff;padding:12px 24px;border-radius:6px;text-decoration:none\">"
                + "Đặt lại mật khẩu</a></p>"
                + "<p style=\"font-size:12px;color:#666\">Nếu nút không bấm được, hãy sao chép liên kết này vào trình duyệt:<br>"
                + esc(resetLink) + "</p>"
                + "<p>Nếu bạn không yêu cầu, hãy bỏ qua email này — mật khẩu của bạn vẫn an toàn.</p>";
        sendAsync(user.getEmail(), "[NôngViệt] Đặt lại mật khẩu", frame(body));
    }

    /**
     * Dia chi goc cua web dung de tao link trong email. Uu tien "app.base.url" trong mail.properties
     * (an toan hon, vi header Host cua request co the bi gia mao); khong co thi dung gia tri lay tu request.
     */
    public static String resolveBaseUrl(String fallbackFromRequest) {
        String configured = config.getProperty("app.base.url");
        String url = (configured != null && !configured.isBlank()) ? configured.trim() : fallbackFromRequest;
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    /** Dung chung cho cac email khac sau nay (doi trang thai don, quen mat khau...). */
    public void sendAsync(String to, String subject, String htmlBody) {
        executor.submit(() -> {
            try {
                send(to, subject, htmlBody);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Gui email toi " + to + " that bai: " + e.getMessage(), e);
            }
        });
    }

    // ---------------------------------------------------------------- gui that

    private void send(String to, String subject, String htmlBody) throws Exception {
        if (!"true".equalsIgnoreCase(config.getProperty("mail.enabled"))) {
            LOG.info("[mail tat - chi ghi log] toi=" + to + " | tieu de=" + subject + "\n" + htmlBody);
            return;
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", config.getProperty("mail.host"));
        props.put("mail.smtp.port", config.getProperty("mail.port", "587"));
        props.put("mail.smtp.auth", config.getProperty("mail.auth", "true"));
        props.put("mail.smtp.starttls.enable", config.getProperty("mail.starttls", "true"));
        props.put("mail.smtp.connectiontimeout", "10000"); // tranh treo thread khi SMTP khong tra loi
        props.put("mail.smtp.timeout", "10000");

        final String username = config.getProperty("mail.username");
        final String password = config.getProperty("mail.password");
        Session session = Session.getInstance(props, "true".equalsIgnoreCase(props.getProperty("mail.smtp.auth"))
                ? new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                }
                : null);

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(config.getProperty("mail.from", username), "NôngViệt", "UTF-8"));
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject, "UTF-8");
        message.setContent(htmlBody, "text/html; charset=UTF-8");
        Transport.send(message);
        LOG.info("Da gui email '" + subject + "' toi " + to);
    }

    // ---------------------------------------------------------------- noi dung email

    private String buildOrderConfirmationHtml(Order order) {
        StringBuilder rows = new StringBuilder();
        for (OrderItem item : order.getItems()) {
            double lineTotal = item.getPriceAtOrder() * item.getQuantity();
            rows.append("<tr>")
                    .append(td(esc(item.getProduct().getName()), "left"))
                    .append(td(String.valueOf(item.getQuantity()), "center"))
                    .append(td(money(item.getPriceAtOrder()), "right"))
                    .append(td(money(lineTotal), "right"))
                    .append("</tr>");
        }

        Address a = order.getShippingAddress();
        String address = a == null ? "" :
                esc(a.getRecipientName()) + " — " + esc(a.getPhone()) + "<br>" + esc(a.getStreet()) + ", " + esc(a.getCity());
        String payment = order.getPayment() instanceof VNPayPayment ? "VNPay" : "Thanh toán khi nhận hàng (COD)";
        String date = order.getOrderDate() == null ? "" :
                order.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        String voucher = order.getVoucher() == null ? "" :
                "<p style=\"margin:4px 0\">Mã giảm giá: <b>" + esc(order.getVoucher().getCode()) + "</b></p>";

        return "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:auto;color:#222\">"
                + "<h2 style=\"color:#2f7d3b\">🌾 NôngViệt</h2>"
                + "<p>Xin chào <b>" + esc(order.getCustomer().getFullName()) + "</b>,</p>"
                + "<p>Cảm ơn bạn đã đặt hàng! Đơn <b>#" + order.getId() + "</b> (" + date + ") đã được ghi nhận "
                + "và đang chờ shop xác nhận. Bạn sẽ nhận thông báo khi trạng thái đơn thay đổi.</p>"
                + "<table style=\"width:100%;border-collapse:collapse\">"
                + "<tr style=\"background:#f1f5ee\">" + th("Sản phẩm", "left") + th("SL", "center")
                + th("Đơn giá", "right") + th("Thành tiền", "right") + "</tr>"
                + rows + "</table>"
                + "<p style=\"text-align:right;font-size:18px\">Tổng thanh toán: <b style=\"color:#2f7d3b\">"
                + money(order.getTotalAmount()) + "</b></p>"
                + voucher
                + "<p style=\"margin:4px 0\">Thanh toán: <b>" + payment + "</b></p>"
                + "<p style=\"margin:4px 0\">Giao tới: " + address + "</p>"
                + "<hr style=\"border:none;border-top:1px solid #ddd;margin:20px 0\">"
                + "<p style=\"color:#888;font-size:12px\">Email tự động từ NôngViệt, vui lòng không trả lời.</p>"
                + "</div>";
    }

    // Khung chung cho cac email don gian (tieu de thuong hieu + noi dung + chan trang)
    private static String frame(String bodyHtml) {
        return "<div style=\"font-family:Arial,sans-serif;max-width:600px;margin:auto;color:#222\">"
                + "<h2 style=\"color:#2f7d3b\">🌾 NôngViệt</h2>" + bodyHtml
                + "<hr style=\"border:none;border-top:1px solid #ddd;margin:20px 0\">"
                + "<p style=\"color:#888;font-size:12px\">Email tự động từ NôngViệt, vui lòng không trả lời.</p></div>";
    }

    private static String td(String content, String align) {
        return "<td style=\"padding:8px;border-bottom:1px solid #eee;text-align:" + align + "\">" + content + "</td>";
    }

    private static String th(String content, String align) {
        return "<th style=\"padding:8px;text-align:" + align + "\">" + content + "</th>";
    }

    private static String money(double amount) {
        return NumberFormat.getIntegerInstance(VI).format(Math.round(amount)) + "₫";
    }

    // Du lieu nguoi dung/admin nhap (ten, dia chi, ten san pham) phai escape truoc khi dua vao HTML email.
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static Properties loadConfig() {
        Properties p = new Properties();
        try (InputStream in = EmailService.class.getResourceAsStream("/mail.properties")) {
            if (in != null) p.load(in);
            else LOG.info("Khong thay mail.properties -> email chi duoc ghi log, khong gui that.");
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Khong doc duoc mail.properties", e);
        }
        return p;
    }
}
