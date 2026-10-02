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
            if (!(user instanceof Customer customer)) throw new BusinessException("Tài khoản không có giỏ hàng.");
            Cart fresh = new Cart();
            fresh.setCustomer(customer);
            cartDAO.save(fresh);
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
        if (qty < 1) throw new BusinessException("Số lượng phải từ 1 trở lên.");
        Product product = productDAO.findById(productId);
        if (product == null) throw new BusinessException("Sản phẩm không tồn tại.");
        if (product.getStockQuantity() <= 0) throw new BusinessException("Sản phẩm \"" + product.getName() + "\" đã hết hàng.");

        Cart cart = getCart(customerId);
        CartItem existing = findItemByProduct(cart, productId);
        int newQty = (existing == null ? 0 : existing.getQuantity()) + qty;
        if (newQty > product.getStockQuantity()) {
            throw new BusinessException("Chỉ còn " + product.getStockQuantity() + " sản phẩm \"" + product.getName() + "\" trong kho.");
        }
        if (existing != null) {
            existing.setQuantity(newQty);
        } else {
            CartItem item = new CartItem();
            item.setProduct(product);
            item.setQuantity(qty);
            item.setPriceAtAdd(product.getPrice()); // chot gia luc them vao gio
            cart.addItem(item);
        }
        cartDAO.update(cart);
    }

    public void updateQuantity(Integer customerId, Integer itemId, int qty) {
        if (qty < 1) {
            removeItem(customerId, itemId);
            return;
        }
        Cart cart = getCart(customerId);
        CartItem item = findItem(cart, itemId); // chi tim trong gio cua CHINH khach nay -> khong sua duoc gio nguoi khac
        if (qty > item.getProduct().getStockQuantity()) {
            throw new BusinessException("Chỉ còn " + item.getProduct().getStockQuantity() + " sản phẩm trong kho.");
        }
        item.setQuantity(qty);
        cartDAO.update(cart);
    }

    public void removeItem(Integer customerId, Integer itemId) {
        Cart cart = getCart(customerId);
        CartItem item = findItem(cart, itemId);
        cart.getItems().remove(item); // orphanRemoval=true tren Cart.items -> dong bi xoa khoi DB
        cartDAO.update(cart);
    }

    private CartItem findItem(Cart cart, Integer itemId) {
        return cart.getItems().stream().filter(i -> i.getId().equals(itemId)).findFirst()
                .orElseThrow(() -> new BusinessException("Sản phẩm không có trong giỏ hàng."));
    }

    private CartItem findItemByProduct(Cart cart, Integer productId) {
        return cart.getItems().stream().filter(i -> i.getProduct().getId().equals(productId)).findFirst().orElse(null);
    }
}
