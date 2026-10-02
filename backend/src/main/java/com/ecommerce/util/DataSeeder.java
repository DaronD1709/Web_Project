package com.ecommerce.util;

import com.ecommerce.dao.CategoryDAO;
import com.ecommerce.dao.ProductDAO;
import com.ecommerce.dao.UserDAO;
import com.ecommerce.entity.Admin;
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

    public static void seedIfEmpty() {
        seedUsers();
        seedCatalog();
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
