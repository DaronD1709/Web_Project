package com.ecommerce.service;

import com.ecommerce.dao.CheckoutDAO;
import com.ecommerce.entity.Address;
import com.ecommerce.entity.Cart;
import com.ecommerce.entity.CartItem;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.CODPayment;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.dao.OrderDAO;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Nghiệp vụ đặt hàng của Customer. */
public class OrderService {

    private final CheckoutDAO checkoutDAO = new CheckoutDAO();

    private static final Logger LOG =
        Logger.getLogger(OrderService.class.getName());

    private final CartService cartService = new CartService();
    private final VoucherService voucherService = new VoucherService();
    private final EmailService emailService = new EmailService();

    private final OrderDAO orderDAO = new OrderDAO();

    public List<Address> getAddresses(Integer customerId) {
        return checkoutDAO.findAddressesByCustomer(customerId);
    }

    private void validateCheckout(Customer customer,
                                  Cart cart,
                                  Address address) {
        // Kiểm tra lại tài khoản sau khi DAO đã lấy khoá.
        if (customer == null || !customer.isActive()) {
            throw new BusinessException(
                    "Tài khoản không tồn tại hoặc đã bị khoá.");
        }

        if (cart == null || cart.getItems().isEmpty()) {
            throw new BusinessException("Giỏ hàng của bạn đang trống.");
        }

        // DAO chỉ trả địa chỉ thuộc đúng khách đang đặt hàng.
        if (address == null) {
            throw new BusinessException(
                    "Địa chỉ giao hàng không hợp lệ.");
        }

        String[] addressParts = {
            address.getRecipientName(),
            address.getPhone(),
            address.getStreet(),
            address.getCity()
        };

        for (String part : addressParts) {
            if (part == null || part.isBlank()) {
                throw new BusinessException(
                        "Địa chỉ giao hàng chưa đầy đủ thông tin.");
            }
        }

        // Gom số lượng theo sản phẩm, kể cả giỏ cũ có dòng trùng.
        Map<Integer, Long> quantities = new HashMap<>();

        for (CartItem item : cart.getItems()) {
            Product product = item.getProduct();
            int qty = item.getQuantity();
            double price = item.getPriceAtAdd();

            if (product == null || qty < 1) {
                throw new BusinessException(
                        "Giỏ hàng có sản phẩm hoặc số lượng không hợp lệ.");
            }

            if (!Double.isFinite(price) || price < 0) {
                throw new BusinessException(
                        "Giá sản phẩm trong giỏ không hợp lệ.");
            }

            long totalQty = quantities.getOrDefault(product.getId(), 0L)
                    + qty;

            if (totalQty > product.getStockQuantity()) {
                throw new BusinessException(
                        "Chỉ còn " + product.getStockQuantity()
                        + " sản phẩm \"" + product.getName()
                        + "\" trong kho.");
            }

            quantities.put(product.getId(), totalQty);
        }
    }

    public Order checkout(Customer user,
                        Integer addressId,
                        String method,
                        String voucherCode) {
        if (user == null || user.getId() == null) {
            throw new BusinessException("Vui lòng đăng nhập để đặt hàng.");
        }

        if (addressId == null || addressId < 1) {
            throw new BusinessException("Vui lòng chọn địa chỉ giao hàng.");
        }

        if (!"COD".equals(method)) {
            throw new BusinessException(
                    "Hiện chỉ hỗ trợ thanh toán khi nhận hàng (COD).");
        }

        boolean hasVoucher = voucherCode != null && !voucherCode.isBlank();

        Order order = checkoutDAO.createOrder(
                user.getId(),
                addressId,
                voucherCode,
                "Đơn hàng của bạn đã được tiếp nhận.",
                (customer, cart, address, voucher) -> {
                    // Chạy trong transaction, sau khi DAO đã lấy khoá.
                    validateCheckout(customer, cart, address);

                    double subtotal = cart.calculateTotal();
                    if (!Double.isFinite(subtotal) || subtotal < 0) {
                        throw new BusinessException(
                                "Tổng tiền giỏ hàng không hợp lệ.");
                    }

                    // Kiểm tra lại voucher đang được khoá.
                    double discount = hasVoucher
                            ? voucherService.calculateDiscount(voucher, subtotal)
                            : 0;
                    double shipping = cartService.getShippingFee(subtotal);
                    double total = subtotal - discount + shipping;

                    if (!Double.isFinite(total) || total < 0) {
                        throw new BusinessException(
                                "Tổng thanh toán không hợp lệ.");
                    }

                    Order created = new Order();
                    created.setCustomer(customer);
                    created.setShippingAddress(address);
                    created.setOrderDate(LocalDateTime.now());
                    created.setStatus(OrderStatus.PENDING);
                    created.setVoucher(voucher);
                    created.setDiscountAmount(discount);
                    created.setShippingFee(shipping);
                    created.setTotalAmount(total);

                    for (CartItem cartItem : cart.getItems()) {
                        OrderItem orderItem = new OrderItem();
                        orderItem.setOrder(created);
                        orderItem.setProduct(cartItem.getProduct());
                        orderItem.setQuantity(cartItem.getQuantity());

                        // Snapshot giá: đơn giữ giá đã chốt trong giỏ.
                        orderItem.setPriceAtOrder(cartItem.getPriceAtAdd());
                        created.getItems().add(orderItem);

                        cartItem.getProduct().updateStock(
                                -cartItem.getQuantity());
                    }

                    CODPayment payment = new CODPayment();
                    payment.setOrder(created);
                    payment.setAmount(total);
                    payment.processPayment(); // COD còn PENDING, chưa thu tiền.
                    created.setPayment(payment);

                    if (voucher != null) {
                        voucher.setQuantityUsed(voucher.getQuantityUsed() + 1);
                    }

                    // orphanRemoval xoá các dòng giỏ khi transaction lưu.
                    cart.getItems().clear();

                    return created;
                });

        // DAO đã commit trước khi trả order về đây.
        try {
            emailService.sendOrderConfirmation(order);
        } catch (RuntimeException e) {
            // Lỗi chuẩn bị/đưa email vào hàng đợi không làm hỏng đơn đã lưu.
            LOG.log(Level.WARNING,
                    "Không thể gửi email xác nhận cho đơn #" + order.getId(),
                    e);
        }

        return order;
    }

    public Order getOwnOrder(Customer user, Integer orderId) {
        if (user == null || user.getId() == null) {
            throw new BusinessException("Vui lòng đăng nhập để xem đơn hàng.");
        }

        if (orderId == null || orderId < 1) {
            throw new BusinessException("Mã đơn hàng không hợp lệ.");
        }

        // DAO đã fetch các dòng hàng, thanh toán và địa chỉ
        // để JSP đọc được sau khi EntityManager đóng.
        Order order = orderDAO.findDetail(orderId);

        if (order == null) {
            throw new BusinessException("Đơn hàng không tồn tại.");
        }

        // Đổi orderId trên URL cũng không xem được đơn người khác.
        if (!user.getId().equals(order.getCustomer().getId())) {
            throw new SecurityException(
                    "Bạn không có quyền xem đơn hàng này.");
        }

        return order;
    }
}