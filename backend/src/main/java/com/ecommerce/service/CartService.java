package com.ecommerce.service;

import com.ecommerce.dao.CartDAO;
import com.ecommerce.dao.ProductDAO;
import com.ecommerce.dao.UserDAO;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.User;

/** Gio hang cua khach. Moi thao tac deu kiem tra: san pham co that, ton kho du, dong gio thuoc ve DUNG khach nay. */
public class CartService {

    public static final double FREE_SHIPPING_FROM = 500_000;
    public static final double SHIPPING_FEE = 30_000;

    private final CartDAO cartDAO = new CartDAO();
    private final ProductDAO productDAO = new ProductDAO();
    private final UserDAO userDAO = new UserDAO();

    /** Lay gio hang (kem cac dong); chua co thi tao gio rong. */
    public Cart getCart(Integer customerId) {
        Cart cart = cartDAO.findByCustomerId(customerId);

        if (cart == null) {
            User user = userDAO.findById(customerId);
            if (!(user instanceof Customer)) {
                throw new BusinessException("Tài khoản không có giỏ hàng.");
            }

            // DAO lấy khoá rồi kiểm tra lại giỏ trước khi tạo.
            // Nếu request khác đã tạo giỏ, DAO dùng giỏ đó.
            cartDAO.changeCart(customerId, currentCart -> {
                requireActiveCustomer(currentCart);
            });

            // Đọc lại giỏ sau khi transaction đã commit.
            cart = cartDAO.findByCustomerId(customerId);
        }
        return cart;
    }

    public long countItems(Integer customerId) {
        return cartDAO.countItems(customerId);
    }

    public double getShippingFee(double subtotal) {
        return subtotal <= 0 || subtotal >= FREE_SHIPPING_FROM ? 0 : SHIPPING_FEE;
    }

    public void addItem(Integer customerId, Integer productId, int qty) {
        if (qty < 1) {
            throw new BusinessException("Số lượng phải từ 1 trở lên.");
        }

        // DAO khoá chủ giỏ trước, rồi mới chạy phần xử lý bên trong.
        cartDAO.changeCart(customerId, cart -> {
            requireActiveCustomer(cart);

            Product product = productDAO.findById(productId);
            if (product == null) {
                throw new BusinessException("Sản phẩm không tồn tại.");
            }

            int stock = product.getStockQuantity();
            if (stock <= 0) {
                throw new BusinessException(
                        "Sản phẩm \"" + product.getName() + "\" đã hết hàng.");
            }

            CartItem existing = findItemByProduct(cart, productId);
            int currentQty = existing == null ? 0 : existing.getQuantity();

            // Dùng long để phép cộng không bị tràn số nguyên.
            long newQty = (long) currentQty + qty;
            if (newQty > stock) {
                throw new BusinessException(
                        "Chỉ còn " + stock
                        + " sản phẩm \"" + product.getName() + "\" trong kho.");
            }

            if (existing != null) {
                existing.setQuantity((int) newQty);
            } else {
                CartItem item = new CartItem();
                item.setProduct(product);
                item.setQuantity(qty);
                item.setPriceAtAdd(product.getPrice()); // Chốt giá khi thêm lần đầu.
                cart.addItem(item);
            }

            // DAO sẽ commit các thay đổi trong cùng transaction.
        });
    }

    public void updateQuantity(Integer customerId, Integer itemId, int qty) {
        if (qty < 1) {
            throw new BusinessException("Số lượng phải từ 1 trở lên.");
        }

        cartDAO.changeCart(customerId, cart -> {
            requireActiveCustomer(cart);

            // Chỉ tìm trong giỏ của khách đang đăng nhập.
            CartItem item = findItem(cart, itemId);
            Product product = item.getProduct();
            int stock = product.getStockQuantity();

            if (stock <= 0) {
                throw new BusinessException(
                        "Sản phẩm \"" + product.getName() + "\" đã hết hàng.");
            }

            if (qty > stock) {
                throw new BusinessException(
                        "Chỉ còn " + stock
                        + " sản phẩm \"" + product.getName() + "\" trong kho.");
            }

            item.setQuantity(qty);
        });
    }

    public void removeItem(Integer customerId, Integer itemId) {
        cartDAO.changeCart(customerId, cart -> {
            requireActiveCustomer(cart);

            CartItem item = findItem(cart, itemId);

            // orphanRemoval của Cart.items xoá dòng khỏi DB khi commit.
            cart.getItems().remove(item);
        });
    }

    private void requireActiveCustomer(Cart cart) {
        // Kiểm tra lại sau khi lấy khoá, phòng trường hợp tài khoản vừa bị khoá.
        if (!cart.getCustomer().isActive()) {
            throw new BusinessException("Tài khoản của bạn đã bị khoá.");
        }
    }

    private CartItem findItem(Cart cart, Integer itemId) {
        return cart.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new BusinessException("Sản phẩm không có trong giỏ hàng."));
    }

    private CartItem findItemByProduct(Cart cart, Integer productId) {
        return cart.getItems().stream().filter(i -> i.getProduct().getId().equals(productId)).findFirst().orElse(null);
    }
}
