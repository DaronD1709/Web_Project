package com.ecommerce.util;

import com.ecommerce.dao.MessageDAO;
import com.ecommerce.dao.OrderDAO;
import com.ecommerce.dao.ProductDAO;
import com.ecommerce.dao.UserDAO;
import com.ecommerce.dao.VoucherDAO;
import com.ecommerce.entity.Address;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CODPayment;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.DiscountType;
import com.ecommerce.entity.Message;
import com.ecommerce.entity.User;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.Payment;
import com.ecommerce.entity.PaymentStatus;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.VNPayPayment;
import com.ecommerce.entity.Voucher;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Du lieu mau "dang vao viec" de DEMO trang Admin khi chua co chuc nang dat hang that: vai khach hang kem dia chi va ~10 don o du cac trang thai.
 * Tu nap luc khoi dong (AppInitListener) CHI KHI bang orders con trong; xoa don trong DB roi restart app de nap lai.
 * Don mau KHONG tru kho (chi la lich su), mat khau cac khach mau giong khach dev trong DataSeeder.
 */
public class DemoDataSeeder {

    private static final OrderDAO orderDAO = new OrderDAO();
    private static final ProductDAO productDAO = new ProductDAO();
    private static final UserDAO userDAO = new UserDAO();
    private static final VoucherDAO voucherDAO = new VoucherDAO();
    private static final MessageDAO messageDAO = new MessageDAO();

    public static void seedIfEmpty() {
        seedVouchers();
        seedExtraCustomers();
        seedChats();
        if (orderDAO.count() > 0) return;
        List<Product> products = productDAO.findAll();
        if (products.size() < 6) return; // chua co san pham mau thi thoi

        Customer a = customer("khach@nongviet.vn", "Nguyễn Văn Khách", "0901234567", "12 Lê Lợi", "TP. Hồ Chí Minh");
        Customer b = customer("mai.tran@gmail.com", "Trần Thị Mai", "0912345678", "45 Nguyễn Huệ", "TP. Hồ Chí Minh");
        Customer c = customer("nam.le@gmail.com", "Lê Hoàng Nam", "0987654321", "8 Trần Phú", "Đà Nẵng");
        Customer d = customer("lan.vo@gmail.com", "Võ Thị Lan", "0977888999", "101 Hùng Vương", "Cần Thơ");

        // order(khach, so ngay truoc, trang thai, phuong thuc thanh toan, san pham, cac dong {chi so san pham, so luong}, ly do hoan hang)
        order(a, 0, OrderStatus.PENDING, "COD", products, new int[][]{{4, 1}, {2, 4}, {8, 2}}, null);
        order(b, 0, OrderStatus.PENDING, "VNPAY", products, new int[][]{{0, 10}, {1, 3}}, null);
        order(c, 1, OrderStatus.CONFIRMED, "COD", products, new int[][]{{7, 1}}, null);
        order(a, 2, OrderStatus.SHIPPING, "COD", products, new int[][]{{5, 1}}, null);
        order(d, 2, OrderStatus.SHIPPING, "VNPAY", products, new int[][]{{3, 5}, {2, 2}}, null);
        order(b, 3, OrderStatus.COMPLETED, "COD", products, new int[][]{{6, 2}}, null);
        order(d, 5, OrderStatus.COMPLETED, "VNPAY", products, new int[][]{{0, 1}, {1, 1}}, null);
        order(c, 7, OrderStatus.RETURN_REQUESTED, "COD", products, new int[][]{{7, 1}}, "Máy chạy không ổn định, động cơ phát tiếng ồn lớn ngay khi khởi động.");
        order(d, 9, OrderStatus.CANCELLED, "COD", products, new int[][]{{8, 3}}, null);
        order(b, 12, OrderStatus.RETURNED, "COD", products, new int[][]{{11, 4}}, null);
    }

    // 3 cuoc tro chuyen mau (chi khi bang messages trong): 1 nhan vien dang tiep quan + khach cho phan hoi, 1 do chatbot tra loi, 1 khach moi hoi chua ai tra loi
    private static void seedChats() {
        if (messageDAO.count() > 0) return;
        User bot = userDAO.findByEmail("bot@nongviet.vn");
        User admin = userDAO.findByEmail("admin@nongviet.vn");
        Customer nam = (Customer) userDAO.findByEmail("nam.le@gmail.com");
        Customer mai = (Customer) userDAO.findByEmail("mai.tran@gmail.com");
        Customer lan = (Customer) userDAO.findByEmail("lan.vo@gmail.com");
        if (bot == null || admin == null || nam == null || mai == null || lan == null) return;

        chat(nam, bot, "Xin chào! Mình là trợ lý AI của Nông Việt. Bạn cần tư vấn gì?", 40);
        chat(nam, nam, "Mình muốn mua máy cắt cỏ, còn hàng không?", 38);
        chat(nam, bot, "Máy cắt cỏ cầm tay chạy xăng hiện còn 3 máy. Bạn muốn mình chuyển nhân viên hỗ trợ không?", 38);
        chat(nam, nam, "Có, gọi nhân viên giúp mình", 37);
        chat(nam, admin, "Chào bạn, mình là nhân viên Nông Việt. Bạn cần hỗ trợ gì về máy cắt cỏ ạ?", 30);
        chat(nam, nam, "Cho mình hỏi máy cắt cỏ còn bảo hành bao lâu?", 25);
        setHuman(nam, true);

        chat(mai, mai, "Rau màu nên bón phân gì?", 120);
        chat(mai, bot, "Với rau màu, bạn có thể dùng phân NPK 20-20-15 để bón thúc kết hợp phân hữu cơ vi sinh bón lót.", 120);

        chat(lan, lan, "Đơn #5 bao giờ giao vậy shop?", 5);
    }

    private static void chat(Customer owner, User sender, String content, int minutesAgo) {
        Message m = new Message();
        m.setCustomer(owner);
        m.setSender(sender);
        m.setContent(content);
        m.setSentAt(LocalDateTime.now().minusMinutes(minutesAgo));
        messageDAO.save(m);
    }

    private static void setHuman(Customer c, boolean human) {
        c.setHandledByHuman(human);
        userDAO.update(c);
    }

    // Them 2 khach mau chua co don (1 dang hoat dong, 1 bi KHOA) de demo trang Khach hang; chi tao khi email chua co
    private static void seedExtraCustomers() {
        extraCustomer("hanh.ht@gmail.com", "Hoàng Thị Hạnh", "0966777000", 10, true);
        extraCustomer("bao.pq@outlook.com", "Phạm Quốc Bảo", "0933111222", 18, false);
    }

    private static void extraCustomer(String email, String name, String phone, int joinedDaysAgo, boolean active) {
        if (userDAO.findByEmail(email) != null) return;
        Customer c = new Customer();
        c.setEmail(email);
        c.setPasswordHash(PasswordUtil.hash("Khach@123"));
        c.setFullName(name);
        c.setPhone(phone);
        c.setCreatedAt(LocalDateTime.now().minusDays(joinedDaysAgo));
        c.setActive(active);
        Cart cart = new Cart();
        cart.setCustomer(c);
        c.setCart(cart);
        userDAO.save(c);
    }

    // 5 voucher mau du 4 trang thai (dang dung / het luot / het han / da tat), chi nap khi bang vouchers trong
    private static void seedVouchers() {
        if (voucherDAO.count() > 0) return;
        LocalDateTime now = LocalDateTime.now();
        voucher("NONG10", DiscountType.PERCENTAGE, 10, 300_000, 100, 37, now.minusDays(5), now.plusDays(25), true);
        voucher("FREESHIP", DiscountType.FIXED_AMOUNT, 30_000, 200_000, 200, 148, now.minusDays(20), now.plusDays(80), true);
        voucher("KHAITRUONG50", DiscountType.FIXED_AMOUNT, 50_000, 500_000, 50, 50, now.minusDays(30), now.plusDays(10), true);   // het luot
        voucher("THU15", DiscountType.PERCENTAGE, 15, 400_000, 80, 21, now.minusDays(60), now.minusDays(20), true);                // het han
        voucher("VIP20", DiscountType.PERCENTAGE, 20, 1_000_000, 30, 4, now.minusDays(5), now.plusDays(80), false);                 // da tat
    }

    private static void voucher(String code, DiscountType type, double value, double min, int issued, int used,
                                LocalDateTime start, LocalDateTime end, boolean active) {
        Voucher v = new Voucher();
        v.setCode(code);
        v.setDiscountType(type);
        v.setDiscountValue(value);
        v.setMinOrderValue(min);
        v.setQuantityIssued(issued);
        v.setQuantityUsed(used);
        v.setStartDate(start);
        v.setEndDate(end);
        v.setActive(active);
        voucherDAO.save(v);
    }

    // Tao khach hang (kem gio rong va 1 dia chi mac dinh) neu chua co; da co thi lay lai. Dia chi cascade luu theo khach.
    private static Customer customer(String email, String name, String phone, String street, String city) {
        Customer existing = (Customer) userDAO.findByEmail(email);
        if (existing != null) {
            existing.setAddresses(new java.util.ArrayList<>());
            return ensureAddress(existing, name, phone, street, city);
        }
        Customer c = new Customer();
        c.setEmail(email);
        c.setPasswordHash(PasswordUtil.hash("Khach@123"));
        c.setFullName(name);
        c.setPhone(phone);
        c.setCreatedAt(LocalDateTime.now().minusDays(20));
        Cart cart = new Cart();
        cart.setCustomer(c);
        c.setCart(cart);
        Address ad = address(c, name, phone, street, city);
        c.getAddresses().add(ad);
        userDAO.save(c);
        return c;
    }

    // Khach da co san (khach dev) nhung chua co dia chi: them 1 dia chi bang cach luu rieng
    private static Customer ensureAddress(Customer c, String name, String phone, String street, String city) {
        Address ad = address(c, name, phone, street, city);
        new com.ecommerce.dao.AbstractDAO<Address, Integer>(Address.class) { }.save(ad);
        c.getAddresses().add(ad);
        return c;
    }

    private static Address address(Customer c, String name, String phone, String street, String city) {
        Address ad = new Address();
        ad.setCustomer(c);
        ad.setRecipientName(name);
        ad.setPhone(phone);
        ad.setStreet(street);
        ad.setCity(city);
        ad.setDefault(true);
        return ad;
    }

    private static Order order(Customer customer, int daysAgo, OrderStatus status, String pay, List<Product> products, int[][] lines, String returnReason) {
        Order o = new Order();
        o.setCustomer(customer);
        o.setOrderDate(LocalDateTime.now().minusDays(daysAgo).minusMinutes(daysAgo * 37L));
        o.setStatus(status);
        o.setReturnReason(returnReason); // chi co o don RETURN_REQUESTED
        o.setShippingAddress(customer.getAddresses().get(0));

        double sub = 0;
        for (int[] line : lines) {
            Product p = products.get(line[0] % products.size());
            OrderItem item = new OrderItem();
            item.setOrder(o);
            item.setProduct(p);
            item.setQuantity(line[1]);
            item.setPriceAtOrder(p.getPrice()); // snapshot gia luc dat
            o.getItems().add(item);
            sub += p.getPrice() * line[1];
        }
        o.setShippingFee(sub >= 500_000 ? 0 : 30_000); // mien phi van chuyen tu 500.000d (khop CartService.FREE_SHIPPING_FROM)
        o.setDiscountAmount(0);
        o.setTotalAmount(sub + o.getShippingFee());

        Payment payment = "VNPAY".equals(pay) ? new VNPayPayment() : new CODPayment();
        payment.setOrder(o);
        payment.setAmount(o.getTotalAmount());
        boolean paid = status == OrderStatus.COMPLETED || status == OrderStatus.RETURN_REQUESTED || status == OrderStatus.RETURNED
                || ("VNPAY".equals(pay) && status != OrderStatus.CANCELLED);
        payment.setStatus(status == OrderStatus.CANCELLED ? PaymentStatus.FAILED : paid ? PaymentStatus.SUCCESS : PaymentStatus.PENDING);
        if (paid) payment.setPaymentDate(o.getOrderDate());
        if (payment instanceof VNPayPayment vn) vn.setTransactionId("VNP" + (100000 + daysAgo * 7919 + lines.length));
        o.setPayment(payment);

        orderDAO.save(o);
        return o;
    }
}
