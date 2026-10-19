package com.ecommerce.dao;

import com.ecommerce.dto.AdminOrderFilter;
import com.ecommerce.dto.PageResult;
import com.ecommerce.entity.Notification;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.OrderItem;
import com.ecommerce.entity.OrderStatus;
import com.ecommerce.entity.Payment;
import com.ecommerce.entity.PaymentStatus;
import com.ecommerce.util.JPAUtil;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Voucher;
import com.ecommerce.entity.CODPayment;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.Query;

import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Đọc đơn của Customer/Admin; đổi trạng thái và hoàn kho trong một transaction.
 *
 * Các nhóm method (theo đúng thứ tự trong file):
 *   1. Danh sách: findHistory (khách) · search / countSearch / countByStatus / countItems (Admin) · findRecentByCustomer
 *   2. Chi tiết: findDetail (JOIN FETCH đủ để JSP đọc được sau khi EntityManager đóng)
 *   3. Đổi trạng thái: applyStatusChange (Admin) · changeCustomerStatus (Khách) → cùng gọi changeStatusInTransaction
 *
 * Lưu ý chung: AbstractDAO đóng EntityManager sau mỗi lần gọi, nên mọi collection mà JSP cần
 * (items, product, payment...) phải được JOIN FETCH ngay trong query, nếu không sẽ bị LazyInitializationException.
 */
public class OrderDAO extends AbstractDAO<Order, Integer> {

    public OrderDAO() {
        super(Order.class);
    }

    // ------------------------------------------------------------------ danh sach

    /**
     * Lịch sử đơn của KHÁCH (trang "Đơn hàng của tôi"). Chỉ lấy đơn có customer.id = customerId nên khách không đọc được đơn người khác.
     * Làm 3 bước: (1) đếm tổng để biết số trang  (2) lấy ID của đúng trang cần xem  (3) nạp đầy đủ các đơn theo danh sách ID đó.
     * Vì sao tách bước 2 và 3: nếu vừa JOIN FETCH items vừa setMaxResults thì Hibernate phân trang trong bộ nhớ (chậm, dễ sai),
     * còn phân trang trên ID rồi mới fetch thì đúng và nhanh.
     */
    public PageResult<Order> findHistory(Integer customerId,
            List<OrderStatus> statuses, int page, int pageSize) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            String where = " WHERE o.customer.id = :customerId"
                    + (statuses.isEmpty() ? "" : " AND o.status IN :statuses");
            var count = em.createQuery("SELECT COUNT(o) FROM Order o" + where, Long.class)
                    .setParameter("customerId", customerId);
            if (!statuses.isEmpty()) count.setParameter("statuses", statuses);
            long total = count.getSingleResult();
            // Kẹp số trang vào [1, trang cuối] để người dùng gõ page=999 vẫn ra trang hợp lệ thay vì trang trống.
            int currentPage = (int) Math.min(page, Math.max(1L, (total + pageSize - 1) / pageSize));

            var idQuery = em.createQuery("SELECT o.id FROM Order o" + where + " ORDER BY o.id DESC", Integer.class)
                    .setParameter("customerId", customerId)
                    .setFirstResult(Math.toIntExact(((long) currentPage - 1) * pageSize))
                    .setMaxResults(pageSize);
            if (!statuses.isEmpty()) idQuery.setParameter("statuses", statuses);
            List<Integer> ids = idQuery.getResultList();
            if (ids.isEmpty()) return new PageResult<>(List.of(), total, currentPage, pageSize);

            // Không setMaxResults trên JOIN FETCH items: Hibernate có thể phân trang trong bộ nhớ.
            List<Order> orders = em.createQuery(
                    "SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items i "
                    + "LEFT JOIN FETCH i.product LEFT JOIN FETCH o.payment "
                    + "WHERE o.customer.id = :customerId AND o.id IN :ids ORDER BY o.id DESC", Order.class)
                    .setParameter("customerId", customerId)
                    .setParameter("ids", ids)
                    .getResultList();
            return new PageResult<>(orders, total, currentPage, pageSize);
        } finally {
            em.close();
        }
    }

    /**
     * ADMIN: 1 trang don hang khop bo loc, moi nhat truoc (ORDER BY o.id DESC).
     * JOIN FETCH khach + thanh toan de JSP doc duoc ${order.customer.fullName} / ${order.payment.method} sau khi EntityManager dong.
     * Dieu kien WHERE do buildWhere() dung, gia tri that gan bang setParams() (khong noi chuoi nguoi dung vao JPQL).
     * setFirstResult/setMaxResults an toan o day vi chi JOIN FETCH quan he *-1 (customer, payment), khong fetch danh sach items.
     */
    public List<Order> search(AdminOrderFilter f) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            String jpql = "SELECT o FROM Order o JOIN FETCH o.customer c LEFT JOIN FETCH o.payment pm"
                    + buildWhere(f) + " ORDER BY o.id DESC";
            Query query = em.createQuery(jpql, Order.class);
            setParams(query, f);
            query.setFirstResult(f.getOffset());
            query.setMaxResults(f.getPageSize());
            @SuppressWarnings("unchecked")
            List<Order> result = query.getResultList();
            return result;
        } finally {
            em.close();
        }
    }

    /** ADMIN: tong so don khop bo loc (khong phan trang) de tinh so trang. Dung CHUNG buildWhere/setParams voi search() nen 2 con so luon khop nhau. */
    public long countSearch(AdminOrderFilter f) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            Query query = em.createQuery("SELECT COUNT(o) FROM Order o JOIN o.customer c LEFT JOIN o.payment pm" + buildWhere(f), Long.class);
            setParams(query, f);
            return (Long) query.getSingleResult();
        } finally {
            em.close();
        }
    }

    // Chi noi cac DIEU KIEN CO DINH vao chuoi; gia tri nguoi dung nhap luon qua setParameter (chong JPQL injection).
    // "WHERE 1=1" la meo de moi dieu kien phia sau deu bat dau bang " AND ..." ma khong phai kiem tra "da co WHERE chua".
    // Ten tham so (:status, :kw, :from...) phai trung voi ten trong setParams() ben duoi.
    private String buildWhere(AdminOrderFilter f) {
        StringBuilder w = new StringBuilder(" WHERE 1=1");
        switch (f.getStatus()) {
            case "all" -> { }
            case "return" -> w.append(" AND o.status IN (:retA, :retB)"); // tab "Hoan hang" gom yeu cau hoan + da hoan
            default -> w.append(" AND o.status = :status");
        }
        if (f.getKeyword() != null) {
            w.append(" AND (LOWER(c.fullName) LIKE :kw OR LOWER(c.email) LIKE :kw");
            if (keywordAsId(f) != null) w.append(" OR o.id = :kwId"); // go "1042" hoac "#1042" thi tim ca theo ma don
            w.append(")");
        }
        if ("cod".equals(f.getPay())) w.append(" AND TYPE(pm) = CODPayment");
        if ("vnpay".equals(f.getPay())) w.append(" AND TYPE(pm) = VNPayPayment");
        if (f.getFrom() != null) w.append(" AND o.orderDate >= :from");
        if (f.getTo() != null) w.append(" AND o.orderDate < :toExclusive");
        return w.toString();
    }

    // Gan gia tri cho dung cac tham so ma buildWhere() da khai bao; dieu kien nao khong dung thi KHONG gan (JPA bao loi neu gan thua).
    private void setParams(Query q, AdminOrderFilter f) {
        switch (f.getStatus()) {
            case "all" -> { }
            case "return" -> {
                q.setParameter("retA", OrderStatus.RETURN_REQUESTED);
                q.setParameter("retB", OrderStatus.RETURNED);
            }
            default -> q.setParameter("status", OrderStatus.valueOf(f.getStatus().toUpperCase()));
        }
        if (f.getKeyword() != null) {
            q.setParameter("kw", "%" + f.getKeyword().toLowerCase() + "%");
            Integer id = keywordAsId(f);
            if (id != null) q.setParameter("kwId", id);
        }
        if (f.getFrom() != null) q.setParameter("from", f.getFrom().atStartOfDay());
        if (f.getTo() != null) q.setParameter("toExclusive", f.getTo().plusDays(1).atStartOfDay()); // het ngay "to" = dau ngay ke tiep
    }

    // Neu tu khoa la so ("1042" hoac "#1042") thi tra ve ma don de tim them theo o.id; khong phai so thi null (chi tim theo ten/email).
    private Integer keywordAsId(AdminOrderFilter f) {
        try {
            return Integer.valueOf(f.getKeyword().replace("#", "").trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** So don theo tung trang thai (cho so tren cac tab), tinh tren TOAN BO don, khong theo bo loc. */
    public Map<OrderStatus, Long> countByStatus() {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            Map<OrderStatus, Long> result = new EnumMap<>(OrderStatus.class);
            for (Object[] row : em.createQuery("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status", Object[].class).getResultList()) {
                result.put((OrderStatus) row[0], (Long) row[1]);
            }
            return result;
        } finally {
            em.close();
        }
    }

    /** Moi don trong danh sach co bao nhieu mat hang: 1 query gop (id don -> so dong), thay vi nap ca danh sach dong cua tung don. */
    public Map<Integer, Long> countItems(List<Integer> orderIds) {
        Map<Integer, Long> result = new HashMap<>();
        if (orderIds.isEmpty()) return result;
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            for (Object[] row : em.createQuery(
                    "SELECT i.order.id, COUNT(i) FROM OrderItem i WHERE i.order.id IN :ids GROUP BY i.order.id", Object[].class)
                    .setParameter("ids", orderIds).getResultList()) {
                result.put((Integer) row[0], (Long) row[1]);
            }
            return result;
        } finally {
            em.close();
        }
    }

    /** Cac don gan day nhat cua 1 khach (moi nhat truoc), toi da `limit` don. Dung o trang chi tiet khach hang cua Admin. */
    public List<Order> findRecentByCustomer(Integer customerId, int limit) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            return em.createQuery("SELECT o FROM Order o WHERE o.customer.id = :cid ORDER BY o.id DESC", Order.class)
                    .setParameter("cid", customerId)
                    .setMaxResults(limit)
                    .getResultList();
        } finally {
            em.close();
        }
    }

    // ------------------------------------------------------------------ chi tiet

    /** 1 don kem day du: khach, cac dong hang + san pham, thanh toan, dia chi, voucher (JOIN FETCH vi EntityManager dong ngay sau khi tra ve). */
    public Order findDetail(Integer id) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        try {
            List<Order> result = em.createQuery(
                    "SELECT DISTINCT o FROM Order o JOIN FETCH o.customer LEFT JOIN FETCH o.items i LEFT JOIN FETCH i.product "
                            + "LEFT JOIN FETCH o.payment LEFT JOIN FETCH o.shippingAddress LEFT JOIN FETCH o.voucher WHERE o.id = :id", Order.class)
                    .setParameter("id", id)
                    .getResultList();
            return result.isEmpty() ? null : result.get(0);
        } finally {
            em.close();
        }
    }

    // ------------------------------------------------------------------ doi trang thai

    /**
     * KHÁCH tự huỷ đơn / xin hoàn hàng. Điểm khác với applyStatusChange: việc kiểm tra "đơn có phải của khách này không" và
     * "trạng thái hiện tại có cho phép không" được làm TRONG transaction, SAU khi đã khoá dòng đơn (hàm validate do Service truyền vào).
     * Nhờ vậy khách bấm huỷ cùng lúc Admin bấm xác nhận thì chỉ một bên thắng, bên còn lại thấy trạng thái mới và bị từ chối.
     */
    public void changeCustomerStatus(Integer orderId, OrderStatus next,
            String notificationText, Consumer<Order> validate) {
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            // Khoá ghi dòng đơn (SELECT ... FOR UPDATE): giao dịch khác muốn đổi cùng đơn phải đợi giao dịch này xong.
            List<Order> matches = em.createQuery("SELECT o FROM Order o WHERE o.id = :id", Order.class)
                    .setParameter("id", orderId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getResultList();
            Order order = matches.isEmpty() ? null : matches.get(0);
            if (order != null) {
                // refresh = đọc lại từ DB để chắc chắn thấy dữ liệu MỚI NHẤT (đề phòng entity còn trong bộ nhớ đệm của EntityManager).
                em.refresh(order.getCustomer());
                if (order.getPayment() != null) em.refresh(order.getPayment());
            }
            // Service kiểm tra quyền, trạng thái và đặt lý do hoàn; lỗi rollback.
            validate.accept(order);
            changeStatusInTransaction(em, order, next, notificationText);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Doi trang thai don va moi viec di kem trong CUNG 1 transaction: hoac xong het hoac khong gi ca.
     *  - huy don / duyet hoan hang: cong lai kho cho tung dong hang (don huy con tra lai 1 luot voucher)
     *  - giao thanh cong: thanh toan COD chuyen sang SUCCESS; huy don: thanh toan dang cho chuyen sang FAILED
     *  - tao Notification cho khach
     * Luat "chuyen duoc hay khong" do Order.canMoveTo quyet dinh; Service kiem tra truoc de bao loi than thien.
     */
    public void applyStatusChange(
            Integer orderId, OrderStatus next, String notificationText) {
        // Gọi từ AdminOrderService.changeStatus. Service đã kiểm tra luật chuyển; transaction này khoá đơn và kiểm tra lại lần nữa.
        EntityManager em = JPAUtil.getEmFactory().createEntityManager();
        EntityTransaction tx = em.getTransaction();

        try {
            tx.begin();

            // Khoá đơn trước khi kiểm tra và thay đổi trạng thái.
            // PHẢI viết bằng JPQL + setLockMode: em.find(..., PESSIMISTIC_WRITE) có thể đọc dòng trước khi khoá nên không đủ an toàn.
            // Nếu 4 Admin cùng bấm "Huỷ" một đơn: người đầu giữ khoá và huỷ xong; 3 người sau đợi, rồi thấy đơn đã CANCELLED
            // nên canMoveTo trả false và bị từ chối → kho chỉ được cộng lại ĐÚNG MỘT LẦN.
            Order order = em.createQuery(
                    "SELECT o FROM Order o WHERE o.id = :id",
                    Order.class)
                    .setParameter("id", orderId)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getSingleResult();

            changeStatusInTransaction(em, order, next, notificationText);

            tx.commit(); // ghi đồng thời: trạng thái, kho, voucher, thanh toán, thông báo
        } catch (RuntimeException e) {
            // Bất kỳ lỗi nào (đơn đã đổi trạng thái, thiếu hàng...) → rollback, DB giữ nguyên như trước khi bấm.
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Dùng transaction và khoá đơn mà bên gọi đã chuẩn bị.
     * Không tự mở transaction mới, không tự commit hoặc đóng EntityManager.
     */
    void changeStatusInTransaction(
            EntityManager em, Order order,
            OrderStatus next, String notificationText) {
        Integer orderId = order.getId();

        // Bước 1 - Đổi trạng thái. Luật chuyển trạng thái vẫn nằm trong Order; sai luật thì ném IllegalStateException và rollback.
        order.updateStatus(next);

        // Bước 2 - Huỷ đơn hoặc duyệt hoàn hàng: cộng lại kho cho từng dòng hàng (dễ quên nhất, quên là bán vượt tồn khi demo).
        if (next == OrderStatus.CANCELLED
                || next == OrderStatus.RETURNED) {
            List<Integer> productIds = em.createQuery(
                    "SELECT DISTINCT i.product.id FROM OrderItem i "
                    + "WHERE i.order.id = :orderId",
                    Integer.class)
                    .setParameter("orderId", orderId)
                    .getResultList();

            if (!productIds.isEmpty()) {
                // Cùng thứ tự khoá sản phẩm với checkout.
                List<Product> products = em.createQuery(
                        "SELECT p FROM Product p WHERE p.id IN :ids "
                        + "ORDER BY p.id ASC",
                        Product.class)
                        .setParameter("ids", productIds)
                        .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                        .getResultList();

                for (Product product : products) {
                    em.refresh(product);
                }
            }

            // Product.updateStock(+qty): cộng số lượng của dòng hàng vào tồn kho (sản phẩm đã được khoá + refresh ở trên).
            for (OrderItem item : order.getItems()) {
                item.getProduct().updateStock(item.getQuantity());
            }
        }

        // Bước 3 - Huỷ đơn có voucher: trả lại 1 lượt dùng (quantityUsed - 1) để voucher không bị "hết lượt" oan.
        if (next == OrderStatus.CANCELLED
                && order.getVoucher() != null) {
            // Khoá voucher sau sản phẩm, rồi đọc lại số lượt đã dùng.
            Voucher voucher = em.createQuery(
                    "SELECT v FROM Voucher v WHERE v.id = :id",
                    Voucher.class)
                    .setParameter("id", order.getVoucher().getId())
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getSingleResult();

            em.refresh(voucher);

            if (voucher.getQuantityUsed() > 0) {
                voucher.setQuantityUsed(voucher.getQuantityUsed() - 1);
            }
        }

        // Bước 4 - Thanh toán COD: giao xong (COMPLETED) = đã thu tiền → SUCCESS; huỷ khi còn chờ → FAILED.
        // VNPay thì KHÔNG đụng vào ở đây vì kết quả do IPN đã xác thực chữ ký ghi nhận.
        Payment payment = order.getPayment();

        if (payment instanceof CODPayment) {
            if (next == OrderStatus.COMPLETED
                    && payment.getStatus() != PaymentStatus.SUCCESS) {
                payment.setStatus(PaymentStatus.SUCCESS);
                payment.setPaymentDate(LocalDateTime.now());

            } else if (next == OrderStatus.CANCELLED
                    && payment.getStatus() == PaymentStatus.PENDING) {
                payment.setStatus(PaymentStatus.FAILED);
            }
        }

        // VNPay giữ kết quả do IPN đã xác thực ghi nhận.
        // Bước 5 - Tạo thông báo cho khách (hiện ở chuông thông báo). Cùng transaction nên đổi trạng thái thành công thì chắc chắn có thông báo.
        Notification notification = new Notification();
        notification.setUser(order.getCustomer());
        notification.setMessage(notificationText);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        em.persist(notification);
    }
}
