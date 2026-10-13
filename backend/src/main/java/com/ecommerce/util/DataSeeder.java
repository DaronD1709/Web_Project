package com.ecommerce.util;

import com.ecommerce.dao.AutoReplyDAO;
import com.ecommerce.dao.CategoryDAO;
import com.ecommerce.dao.ProductDAO;
import com.ecommerce.dao.UserDAO;
import com.ecommerce.entity.Admin;
import com.ecommerce.entity.AutoReply;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Product;

import java.time.LocalDateTime;

/**
 * Du lieu mau de DEV - tu dong nap vao DB luc app khoi dong (xem AppInitListener), CHI khi bang con trong.
 * Nho do 3 may, moi may 1 DB rieng, van co cung danh muc/san pham de lam viec, khong can chay file SQL.
 * Them/sua san pham mau o day; xoa du lieu trong DB roi restart app de nap lai.
 *
 * Tai khoan dev (CHI DUNG TREN MAY LOCAL, doi mat khau neu deploy that):
 *   Admin    : admin@nongviet.vn / Admin@123
 *   Khach hang: khach@nongviet.vn / Khach@123
 */
public class DataSeeder {

    private static final CategoryDAO categoryDAO = new CategoryDAO();
    private static final ProductDAO productDAO = new ProductDAO();
    private static final UserDAO userDAO = new UserDAO();
    private static final AutoReplyDAO autoReplyDAO = new AutoReplyDAO();

    public static void seedIfEmpty() {
        seedUsers();
        seedCatalog();
        seedAutoReplies();
    }

    // Cac luat tra loi tu dong mac dinh cua chatbot (Admin sua/them/xoa o Admin > Tra loi tu dong). Chi nap khi bang auto_replies trong.
    private static void seedAutoReplies() {
        if (autoReplyDAO.count() > 0) return;
        rule(10, "nhân viên, gặp người, tư vấn viên, gặp shop, gọi cho",
                "Mình đã chuyển cuộc trò chuyện cho nhân viên shop. Bạn vui lòng chờ trong giây lát nhé!", true);
        rule(20, "ship, giao hàng, vận chuyển, phí giao",
                "Shop miễn phí vận chuyển cho đơn từ 500.000đ, đơn dưới mức này phí 30.000đ. Nội thành giao 1–2 ngày, tỉnh khác 2–4 ngày.", false);
        rule(30, "thanh toán, trả tiền, cod, vnpay, chuyển khoản",
                "Shop hỗ trợ thanh toán khi nhận hàng (COD) và thanh toán online qua VNPay. Bạn chọn khi đặt hàng nhé.", false);
        rule(40, "hoàn hàng, đổi trả, trả hàng, bảo hành",
                "Sau khi nhận hàng bạn có thể gửi yêu cầu hoàn hàng ở mục Đơn hàng, nhân viên sẽ xem xét và phản hồi. "
                        + "Về bảo hành, bạn cho mình biết tên sản phẩm để nhân viên hỗ trợ chính xác nhé.", false);
        rule(50, "mã giảm, voucher, khuyến mãi, giảm giá",
                "Bạn nhập mã giảm giá ở bước thanh toán, hệ thống sẽ kiểm tra mã còn hạn, còn lượt và đạt giá trị đơn tối thiểu hay không.", false);
        rule(60, "phân bón, bón phân, npk",
                "Với rau màu, bạn có thể dùng phân NPK 20-20-15 để bón thúc kết hợp phân hữu cơ vi sinh bón lót. "
                        + "Bạn xem nhóm Phân bón trong cửa hàng, hoặc nói rõ loại cây để nhân viên tư vấn kỹ hơn.", false);
        rule(70, "hạt giống, gieo, giống",
                "Shop có nhiều hạt giống rau củ (cà chua, cải xanh…) tỷ lệ nảy mầm cao. Bạn xem nhóm Hạt giống trong cửa hàng nhé.", false);
        rule(80, "tưới, máy, dụng cụ, phun thuốc, cắt cỏ",
                "Shop có máy móc, dụng cụ và hệ thống tưới tiêu cho nhà nông. Bạn cho mình biết diện tích hoặc nhu cầu để nhân viên tư vấn chi tiết nhé.", false);
        rule(90, "xin chào, hello, chào bạn, alo",
                "Chào bạn! Bạn cần tư vấn về hạt giống, phân bón, thuốc BVTV hay máy móc nông nghiệp ạ?", false);
        rule(1000, "*", "Mình chưa trả lời được câu này nên đã chuyển cho nhân viên shop, bạn vui lòng chờ phản hồi nhé!", true); // mac dinh
    }

    private static void rule(int priority, String keywords, String replyText, boolean handoff) {
        AutoReply r = new AutoReply();
        r.setPriority(priority);
        r.setKeywords(keywords);
        r.setReplyText(replyText);
        r.setHandoff(handoff);
        r.setActive(true);
        autoReplyDAO.save(r);
    }

    private static void seedUsers() {
        if (userDAO.findByEmail("admin@nongviet.vn") == null) {
            Admin admin = new Admin();
            admin.setEmail("admin@nongviet.vn");
            admin.setPasswordHash(PasswordUtil.hash("Admin@123"));
            admin.setFullName("Quản trị viên");
            admin.setCreatedAt(LocalDateTime.now());
            userDAO.save(admin);
        }
        if (userDAO.findByEmail("bot@nongviet.vn") == null) {
            // Tai khoan he thong cho chatbot (Message.sender phai la User; khong co class AIBot rieng nen dung loai Admin).
            // Khong ai dang nhap duoc: mat khau la chuoi ngau nhien bi bo di.
            Admin bot = new Admin();
            bot.setEmail("bot@nongviet.vn");
            bot.setPasswordHash(PasswordUtil.hash(java.util.UUID.randomUUID().toString()));
            bot.setFullName("Trợ lý AI");
            bot.setCreatedAt(LocalDateTime.now());
            userDAO.save(bot);
        }
        if (userDAO.findByEmail("khach@nongviet.vn") == null) {
            Customer customer = new Customer();
            customer.setEmail("khach@nongviet.vn");
            customer.setPasswordHash(PasswordUtil.hash("Khach@123"));
            customer.setFullName("Nguyễn Văn Khách");
            customer.setPhone("0901234567");
            customer.setCreatedAt(LocalDateTime.now());
            // Customer 1-1 Cart (composition): tao san gio hang rong, cascade tu luu theo Customer.
            Cart cart = new Cart();
            cart.setCustomer(customer);
            customer.setCart(cart);
            userDAO.save(customer);
        }
    }

    private static void seedCatalog() {
        if (categoryDAO.count() > 0) return;

        Category hatGiong = category("Hạt giống", "Hạt giống rau, củ, quả các loại");
        Category phanBon = category("Phân bón", "Phân hữu cơ, phân vô cơ, phân vi sinh");
        Category thuocBVTV = category("Thuốc BVTV", "Thuốc bảo vệ thực vật, chế phẩm sinh học");
        Category dungCu = category("Dụng cụ cầm tay", "Cuốc, xẻng, kéo cắt tỉa và dụng cụ làm vườn");
        Category mayMoc = category("Máy móc & tưới tiêu", "Máy phun thuốc, máy cắt cỏ, hệ thống tưới");
        Category nongSan = category("Nông sản", "Gạo, rau củ và nông sản sạch");

        product("Hạt giống cà chua Beef F1 (gói 50 hạt)", "Cà chua quả to, ít sâu bệnh, năng suất cao. Tỷ lệ nảy mầm trên 90%.", 25000, 200, hatGiong);
        product("Hạt giống rau cải xanh (combo 3 gói)", "Cải xanh sinh trưởng nhanh, thu hoạch sau 30 ngày.", 30000, 150, hatGiong);
        product("Phân bón NPK 20-20-15 (bao 5kg)", "Phân tổng hợp cân đối đạm, lân, kali cho rau màu và cây ăn trái.", 185000, 80, phanBon);
        product("Phân hữu cơ vi sinh (bao 10kg)", "Cải tạo đất, tăng độ tơi xốp và lượng vi sinh vật có lợi.", 120000, 100, phanBon);
        product("Máy phun thuốc đeo vai chạy điện 16L", "Pin lithium dùng liên tục 4 giờ, đầu phun điều chỉnh được.", 980000, 15, mayMoc);
        product("Bộ tưới nhỏ giọt 50m", "Trọn bộ ống, đầu nhỏ giọt và van khóa cho vườn 200m².", 250000, 40, mayMoc);
        product("Cuốc chim thép cán gỗ", "Thép carbon cứng, cán gỗ sồi chắc tay.", 165000, 30, dungCu);
        product("Máy cắt cỏ cầm tay chạy xăng", "Động cơ 2 thì, lưỡi cắt kép, phù hợp vườn và bờ ruộng.", 3200000, 3, mayMoc);
        product("Thuốc trừ sâu sinh học Abamectin 100ml", "Phòng trừ sâu tơ, sâu xanh, nhện đỏ; an toàn cho người sử dụng.", 95000, 60, thuocBVTV);
        product("Kéo cắt tỉa cành cao cấp", "Lưỡi thép SK5, lò xo êm, cắt cành đến 20mm.", 120000, 45, dungCu);
        product("Máy xới đất mini 7HP", "Xới đất vườn rau, ruộng nhỏ; dễ vận hành, tiết kiệm nhiên liệu.", 8500000, 0, mayMoc);
        product("Gạo hữu cơ ST25 (túi 5kg)", "Gạo ST25 canh tác hữu cơ, dẻo thơm, đóng gói hút chân không.", 160000, 70, nongSan);
    }

    private static Category category(String name, String description) {
        Category c = new Category();
        c.setName(name);
        c.setDescription(description);
        return categoryDAO.save(c); // save() persist -> entity co id ngay sau khi luu
    }

    private static void product(String name, String description, double price, int stock, Category category) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(description);
        p.setPrice(price);
        p.setStockQuantity(stock);
        p.setCategory(category);
        productDAO.save(p);
    }
}
