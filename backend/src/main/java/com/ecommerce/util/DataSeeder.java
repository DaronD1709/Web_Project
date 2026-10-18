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
import com.ecommerce.entity.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Map;

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

    // DB da seed tu truoc: bot ten "Tro ly AI" va cac tin chao cu nhac "tro ly AI" -> doi thanh "tu dong" (day la tra loi theo tu khoa, khong phai AI)
    private static void renameBotLabel() {
        User bot = userDAO.findByEmail("bot@nongviet.vn");
        if (bot != null && "Trợ lý AI".equals(bot.getFullName())) {
            bot.setFullName("Trợ lý tự động");
            userDAO.update(bot);
        }
        jakarta.persistence.EntityManager em = com.ecommerce.util.JPAUtil.getEmFactory().createEntityManager();
        try {
            em.getTransaction().begin();
            em.createQuery("UPDATE Message m SET m.content = REPLACE(m.content, 'trợ lý AI', 'trợ lý tự động') WHERE m.content LIKE '%trợ lý AI%'").executeUpdate();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    // Anh mau cua 12 san pham mau (file nam trong webapp/static/img/products/, nguon + giay phep ghi o docs/credits.md).
    // imageUrl la duong dan tuong doi so voi ung dung (JSP ghep "${ctx}/" + imageUrl), giong anh Admin tai len ("uploads/...").
    private static final Map<String, String> SAMPLE_IMAGES = Map.ofEntries(
            Map.entry("Hạt giống cà chua Beef F1 (gói 50 hạt)", "static/img/products/hat-giong-ca-chua.jpg"),
            Map.entry("Hạt giống rau cải xanh (combo 3 gói)", "static/img/products/hat-giong-cai-xanh.jpg"),
            Map.entry("Phân bón NPK 20-20-15 (bao 5kg)", "static/img/products/phan-bon-npk.jpg"),
            Map.entry("Phân hữu cơ vi sinh (bao 10kg)", "static/img/products/phan-huu-co-vi-sinh.jpg"),
            Map.entry("Máy phun thuốc đeo vai chạy điện 16L", "static/img/products/may-phun-thuoc.jpg"),
            Map.entry("Bộ tưới nhỏ giọt 50m", "static/img/products/bo-tuoi-nho-giot.jpg"),
            Map.entry("Cuốc chim thép cán gỗ", "static/img/products/cuoc-chim.jpg"),
            Map.entry("Máy cắt cỏ cầm tay chạy xăng", "static/img/products/may-cat-co.jpg"),
            Map.entry("Thuốc trừ sâu sinh học Abamectin 100ml", "static/img/products/thuoc-tru-sau.jpg"),
            Map.entry("Kéo cắt tỉa cành cao cấp", "static/img/products/keo-cat-tia.jpg"),
            Map.entry("Máy xới đất mini 7HP", "static/img/products/may-xoi-dat.jpg"),
            Map.entry("Gạo hữu cơ ST25 (túi 5kg)", "static/img/products/gao-st25.jpg"));

    public static void seedIfEmpty() {
        seedUsers();
        seedCatalog();
        backfillSampleImages();
        seedMoreProducts();
        seedAutoReplies();
    }

    // DB da co san pham mau tu truoc khi co anh: gan anh cho san pham mau CHUA CO anh (khong de len anh Admin da tai len).
    private static void backfillSampleImages() {
        for (Product p : productDAO.findAll()) {
            String image = SAMPLE_IMAGES.get(p.getName());
            if (image != null && (p.getImageUrl() == null || p.getImageUrl().isBlank())) {
                p.setImageUrl(image);
                productDAO.update(p);
            }
        }
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
            // Deploy that: dat bien moi truong ADMIN_PASSWORD de KHONG dung mat khau mac dinh (repo cong khai).
            String adminPassword = System.getenv("ADMIN_PASSWORD");
            admin.setPasswordHash(PasswordUtil.hash(adminPassword != null && !adminPassword.isBlank() ? adminPassword : "Admin@123"));
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
            bot.setFullName("Trợ lý tự động");
            bot.setCreatedAt(LocalDateTime.now());
            userDAO.save(bot);
        }
        renameBotLabel();
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

    // >>> MORE_PRODUCTS (sinh tu dong, sua thi sua ca danh sach) >>>
    // ---- San pham mau bo sung (~12 san pham moi danh muc, co anh). Chi them san pham co TEN chua ton tai trong DB nen chay lai khong bi trung.
    // Cot: ten danh muc | ten file anh (webapp/static/img/products/<slug>.jpg, nguon o docs/credits.md) | ten | mo ta | gia | ton kho
    private record Sample(String category, String slug, String name, String description, double price, int stock) { }

    private static final List<Sample> MORE_PRODUCTS = List.of(
        new Sample("Hạt giống", "hat-giong-dua-leo", "Hạt giống dưa leo F1 (gói 20 hạt)", "Dưa leo quả thẳng, giòn, ra trái sớm sau 35 ngày; kháng bệnh phấn trắng.", 22000, 180),
        new Sample("Hạt giống", "hat-giong-ot-hiem", "Hạt giống ớt hiểm (gói 30 hạt)", "Ớt hiểm cay nồng, sai quả, trồng chậu hoặc ngoài vườn đều tốt.", 18000, 220),
        new Sample("Hạt giống", "hat-giong-rau-muong", "Hạt giống rau muống (gói 100g)", "Rau muống lá to, thân giòn, thu hoạch sau 25 ngày gieo.", 15000, 300),
        new Sample("Hạt giống", "hat-giong-xa-lach", "Hạt giống xà lách lô lô xanh (gói 5g)", "Xà lách lá xoăn đẹp mắt, ăn sống ngon, hợp trồng thuỷ canh.", 20000, 160),
        new Sample("Hạt giống", "hat-giong-bi-do", "Hạt giống bí đỏ hồ lô (gói 10 hạt)", "Bí đỏ thịt vàng dẻo, ngọt, quả to trung bình 2–3kg.", 28000, 120),
        new Sample("Hạt giống", "hat-giong-dau-cove", "Hạt giống đậu cove leo (gói 50g)", "Đậu cove leo giàn, quả dài xanh, năng suất cao.", 24000, 140),
        new Sample("Hạt giống", "hat-giong-ca-tim", "Hạt giống cà tím dài (gói 30 hạt)", "Cà tím dài vỏ tím bóng, thịt mềm ít hạt.", 21000, 130),
        new Sample("Hạt giống", "hat-giong-huong-duong", "Hạt giống hoa hướng dương (gói 50 hạt)", "Hướng dương hoa lớn, trồng làm cảnh hoặc lấy hạt.", 35000, 90),
        new Sample("Hạt giống", "hat-giong-dua-hau", "Hạt giống dưa hấu không hạt (gói 10 hạt)", "Dưa hấu ruột đỏ giòn ngọt, vỏ mỏng, ít hạt.", 45000, 70),
        new Sample("Hạt giống", "hat-giong-bap-ngot", "Hạt giống bắp ngọt Mỹ (gói 100g)", "Bắp ngọt hạt vàng, thu hoạch sau 65 ngày, ăn tươi hoặc luộc.", 32000, 110),
        new Sample("Hạt giống", "hat-giong-muop", "Hạt giống mướp hương (gói 20 hạt)", "Mướp hương thơm, quả dài, leo giàn tốt.", 16000, 150),
        new Sample("Hạt giống", "hat-giong-ngo-ri", "Hạt giống rau mùi (ngò rí) (gói 50g)", "Ngò rí thơm, mọc nhanh, gieo quanh năm.", 14000, 200),
        new Sample("Phân bón", "phan-ure", "Phân đạm Urê (bao 50kg)", "Đạm 46%, thúc lá xanh nhanh cho lúa, ngô và rau màu.", 650000, 60),
        new Sample("Phân bón", "phan-lan", "Phân lân nung chảy (bao 25kg)", "Lân đa lượng kèm canxi, magie; thích hợp đất chua phèn.", 210000, 70),
        new Sample("Phân bón", "phan-kali", "Phân Kali Clorua – KCl (bao 25kg)", "Kali 60%, giúp chắc hạt, tăng độ ngọt cho trái.", 420000, 55),
        new Sample("Phân bón", "phan-dap", "Phân DAP (bao 50kg)", "Đạm và lân cao, bón lót cho lúa và cây công nghiệp.", 980000, 40),
        new Sample("Phân bón", "phan-bon-la", "Phân bón lá Growmore (gói 500g)", "Bổ sung vi lượng, giúp cây phục hồi sau úng hạn.", 85000, 150),
        new Sample("Phân bón", "phan-trun-que", "Phân trùn quế (bao 5kg)", "Phân trùn quế nguyên chất, tơi xốp, an toàn cho rau ăn lá.", 75000, 120),
        new Sample("Phân bón", "phan-ga", "Phân gà hữu cơ vi sinh (bao 20kg)", "Phân gà ủ hoai, giàu đạm, bón lót cho vườn rau và cây ăn trái.", 95000, 90),
        new Sample("Phân bón", "voi-bot", "Vôi bột cải tạo đất (bao 25kg)", "Khử chua, diệt mầm bệnh trong đất trước vụ gieo trồng.", 70000, 100),
        new Sample("Phân bón", "npk-16-16-8", "Phân bón NPK 16-16-8 (bao 25kg)", "Phân tổng hợp bón thúc cho lúa, cà phê và cây ăn trái.", 520000, 65),
        new Sample("Phân bón", "dam-ca", "Đạm cá hữu cơ (chai 1 lít)", "Đạm cá thuỷ phân từ cá tươi, kích rễ và tăng đề kháng cho cây.", 110000, 80),
        new Sample("Phân bón", "mun-dua", "Mùn dừa xử lý (bao 10kg)", "Giá thể mùn dừa giữ ẩm, thoát nước tốt, trồng rau và hoa chậu.", 60000, 130),
        new Sample("Phân bón", "tro-tau", "Tro trấu hun (bao 15kg)", "Tro trấu hun làm thông thoáng đất, bổ sung kali tự nhiên.", 45000, 110),
        new Sample("Thuốc BVTV", "thuoc-diet-co", "Thuốc diệt cỏ Glyphosate 480 (chai 1 lít)", "Diệt cỏ lá rộng và cỏ hoà thảo sau khi cỏ mọc, an toàn khi dùng đúng liều.", 135000, 80),
        new Sample("Thuốc BVTV", "thuoc-tru-nam", "Thuốc trừ nấm Mancozeb 80WP (gói 500g)", "Phòng sương mai, đốm lá, thán thư trên rau màu và cây ăn trái.", 92000, 100),
        new Sample("Thuốc BVTV", "thuoc-tru-ray", "Thuốc trừ rầy Imidacloprid (chai 100ml)", "Đặc trị rầy nâu, rệp sáp, bọ trĩ; tác động lưu dẫn.", 78000, 120),
        new Sample("Thuốc BVTV", "bay-dinh-vang", "Bẫy dính côn trùng màu vàng (xấp 20 tấm)", "Bẫy ruồi trắng, rầy, bọ trĩ không dùng thuốc hoá học.", 65000, 200),
        new Sample("Thuốc BVTV", "thuoc-oc-buou", "Thuốc diệt ốc bươu vàng (gói 1kg)", "Diệt ốc bươu vàng hại lúa và rau thuỷ sinh, viên rải tiện lợi.", 58000, 140),
        new Sample("Thuốc BVTV", "trichoderma", "Chế phẩm sinh học Trichoderma (gói 1kg)", "Nấm đối kháng phòng bệnh thối rễ, héo rũ cho cây trồng.", 98000, 90),
        new Sample("Thuốc BVTV", "dau-neem", "Dầu neem xua đuổi sâu (chai 500ml)", "Chiết xuất hạt neem tự nhiên, xua đuổi rệp và sâu ăn lá.", 145000, 60),
        new Sample("Thuốc BVTV", "binh-xit-tay", "Bình xịt tay 2 lít", "Bình xịt áp lực tay, phun phân bón lá và thuốc cho vườn nhỏ.", 95000, 150),
        new Sample("Thuốc BVTV", "thuoc-diet-chuot", "Mồi sinh học diệt chuột (hộp 200g)", "Mồi diệt chuột hiệu quả cao, ít ảnh hưởng vật nuôi khi dùng đúng cách.", 52000, 100),
        new Sample("Thuốc BVTV", "thuoc-sau-to", "Thuốc trừ sâu tơ Emamectin (gói 10g)", "Diệt sâu tơ, sâu xanh, sâu khoang hại rau họ thập tự.", 42000, 130),
        new Sample("Dụng cụ cầm tay", "xeng", "Xẻng đào đất cán dài", "Lưỡi thép dày, cán gỗ chắc; đào hố và xúc đất dễ dàng.", 135000, 50),
        new Sample("Dụng cụ cầm tay", "cao-co", "Cào cỏ 14 răng", "Răng thép chắc, gom cỏ và lá khô gọn gàng.", 98000, 60),
        new Sample("Dụng cụ cầm tay", "binh-tuoi", "Bình tưới cây 5 lít", "Bình tưới nhựa PP dày, vòi sen tản nước nhẹ cho cây con.", 75000, 100),
        new Sample("Dụng cụ cầm tay", "gang-tay", "Găng tay làm vườn (đôi)", "Găng cao su phủ, chống gai và trơn trượt khi làm việc.", 45000, 200),
        new Sample("Dụng cụ cầm tay", "liem", "Liềm cắt lúa lưỡi răng cưa", "Lưỡi răng cưa bén, cán gỗ vừa tay, cắt lúa và cỏ.", 55000, 90),
        new Sample("Dụng cụ cầm tay", "dao-phat", "Dao phát bờ cán gỗ", "Dao phát cỏ, chặt cành nhỏ; thép mangan chịu va đập.", 125000, 45),
        new Sample("Dụng cụ cầm tay", "cuoc-xoi", "Cuốc xoi 2 mũi", "Làm cỏ, xới đất và vun luống rau cực tiện.", 85000, 70),
        new Sample("Dụng cụ cầm tay", "xe-rua", "Xe rùa chở đất 1 bánh", "Thùng thép chịu tải 100kg, bánh hơi êm, chở phân và nông sản.", 890000, 15),
        new Sample("Dụng cụ cầm tay", "xoi-3-chau", "Cào xới 3 chấu cầm tay", "Xới đất chậu, vun gốc nhanh gọn, tay cầm chống trượt.", 48000, 120),
        new Sample("Dụng cụ cầm tay", "cua-cat-canh", "Cưa cắt cành lưỡi cong", "Lưỡi cưa cong kéo ra cắt ngọt, cành đến 10cm.", 118000, 55),
        new Sample("Dụng cụ cầm tay", "gio-thu-hoach", "Giỏ nhựa đựng nông sản", "Giỏ nhựa thoáng khí, xếp chồng, đựng trái cây khi thu hoạch.", 65000, 180),
        new Sample("Dụng cụ cầm tay", "non-la", "Nón lá chống nắng", "Nón lá truyền thống nhẹ, che nắng khi làm đồng.", 40000, 160),
        new Sample("Máy móc & tưới tiêu", "may-bom-nuoc", "Máy bơm nước 1HP", "Bơm ly tâm 1HP, lưu lượng lớn, tưới ruộng và hút nước giếng.", 1650000, 20),
        new Sample("Máy móc & tưới tiêu", "dau-phun-mua", "Đầu phun mưa xoay (bộ 10 cái)", "Đầu tưới xoay 360°, bán kính tưới 8m cho vườn rau và cỏ.", 190000, 80),
        new Sample("Máy móc & tưới tiêu", "ong-tuoi", "Ống tưới phun mưa PE (cuộn 100m)", "Ống PE dẻo, lỗ phun đều, lắp nhanh cho luống rau.", 420000, 35),
        new Sample("Máy móc & tưới tiêu", "may-tuot-lua", "Máy tuốt lúa mini", "Tuốt lúa nhanh, ít rơi vãi, động cơ xăng 6.5HP.", 12500000, 6),
        new Sample("Máy móc & tưới tiêu", "may-xay-xat", "Máy xay xát gạo mini", "Xay xát gia đình, công suất 300kg/giờ, gạo ít vỡ.", 9800000, 7),
        new Sample("Máy móc & tưới tiêu", "bon-nuoc", "Bồn chứa nước nhựa 1000 lít", "Bồn nhựa 3 lớp chống rêu, dùng chứa nước tưới và sinh hoạt.", 1900000, 18),
        new Sample("Máy móc & tưới tiêu", "may-cay-mini", "Máy cày mini 9HP", "Cày xới đất ruộng nhỏ, bánh lồng có thể thay, dễ vận hành.", 18500000, 4),
        new Sample("Máy móc & tưới tiêu", "may-lam-co", "Máy làm cỏ cầm tay chạy xăng", "Làm cỏ giữa luống, xới nhẹ đất tơi xốp, động cơ 2 thì.", 2850000, 12),
        new Sample("Máy móc & tưới tiêu", "may-gieo-hat", "Máy gieo hạt đẩy tay", "Gieo hạt đều hàng, điều chỉnh khoảng cách theo loại hạt.", 1450000, 14),
        new Sample("Máy móc & tưới tiêu", "may-bom-tang-ap", "Máy bơm tăng áp mini", "Tăng áp cho hệ thống tưới và rửa xe, vận hành êm.", 1250000, 22),
        new Sample("Máy móc & tưới tiêu", "may-say", "Máy sấy nông sản 12 khay", "Sấy trái cây, thảo mộc, hạt; điều chỉnh nhiệt tới 80°C.", 6900000, 5),
        new Sample("Máy móc & tưới tiêu", "may-cat-canh", "Máy cắt tỉa hàng rào chạy xăng", "Lưỡi kép 60cm, tỉa hàng rào và bờ cây gọn gàng.", 3450000, 9),
        new Sample("Nông sản", "gao-lut", "Gạo lứt huyết rồng (túi 2kg)", "Gạo lứt đỏ giàu chất xơ, nấu cơm dẻo, tốt cho người ăn kiêng.", 95000, 100),
        new Sample("Nông sản", "nep-cai-hoa-vang", "Nếp cái hoa vàng (túi 2kg)", "Nếp thơm dẻo, nấu xôi và gói bánh chưng đều ngon.", 88000, 90),
        new Sample("Nông sản", "ca-phe-robusta", "Cà phê nhân Robusta (gói 500g)", "Hạt Robusta Tây Nguyên rang xay vừa, đậm vị, hậu ngọt.", 120000, 70),
        new Sample("Nông sản", "tieu-den", "Hạt tiêu đen Phú Quốc (hũ 200g)", "Tiêu đen thơm cay đặc trưng, hạt chắc, đóng hũ kín.", 85000, 120),
        new Sample("Nông sản", "mat-ong", "Mật ong hoa nhãn (chai 500ml)", "Mật ong nguyên chất, màu hổ phách, vị ngọt thanh.", 165000, 60),
        new Sample("Nông sản", "hat-dieu", "Hạt điều rang muối (hũ 500g)", "Điều Bình Phước rang củi, vỏ lụa, giòn bùi.", 155000, 80),
        new Sample("Nông sản", "tra-xanh", "Chè xanh Thái Nguyên (gói 200g)", "Chè búp sao tay, nước xanh trong, hậu vị ngọt.", 110000, 100),
        new Sample("Nông sản", "xoai-cat", "Xoài cát Hoà Lộc (kg)", "Xoài cát chín cây, thịt vàng, ngọt thơm, ít xơ.", 75000, 40),
        new Sample("Nông sản", "thanh-long", "Thanh long ruột đỏ (kg)", "Thanh long ruột đỏ ngọt đậm, trồng đạt chuẩn VietGAP.", 55000, 50),
        new Sample("Nông sản", "sau-rieng", "Sầu riêng Ri6 (kg)", "Sầu riêng cơm vàng, hạt lép, thơm béo đặc trưng miền Tây.", 185000, 15),
        new Sample("Nông sản", "dua-xiem", "Dừa xiêm xanh Bến Tre (trái)", "Dừa xiêm nước ngọt mát, cơm mềm, giao theo thùng 10 trái.", 18000, 200),
        new Sample("Nông sản", "buoi-da-xanh", "Bưởi da xanh Bến Tre (trái)", "Bưởi da xanh ruột hồng, múi mọng, vị ngọt không đắng.", 85000, 45)
    );

    private static void seedMoreProducts() {
        // 1. Danh muc theo ten + ten san pham da co (de khong them trung)
        Map<String, Category> categories = new HashMap<>();
        for (Category c : categoryDAO.findAll()) categories.put(c.getName(), c);
        Set<String> existing = new HashSet<>();
        for (Product p : productDAO.findAll()) existing.add(p.getName());

        // 2. Them tung san pham con thieu
        for (Sample s : MORE_PRODUCTS) {
            Category category = categories.get(s.category());
            if (category == null || existing.contains(s.name())) continue; // danh muc bi doi ten/xoa hoac san pham da co thi bo qua
            Product p = new Product();
            p.setName(s.name());
            p.setDescription(s.description());
            p.setPrice(s.price());
            p.setStockQuantity(s.stock());
            p.setCategory(category);
            p.setImageUrl("static/img/products/" + s.slug() + ".jpg");
            productDAO.save(p);
        }
    }
    // <<< MORE_PRODUCTS <<<

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
