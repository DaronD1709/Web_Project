package com.ecommerce.service;

import com.ecommerce.dao.ProductDAO;
import com.ecommerce.dao.ReviewDAO;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Product;
import com.ecommerce.entity.Review;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Danh gia + cham sao san pham (1-5 sao, kem nhan xet tuy chon).
 * Rule nghiep vu nam o day (khong phai entity/DB): CHI khach da mua san pham (co don COMPLETED/RETURNED chua no) moi danh gia duoc,
 * moi khach 1 danh gia cho 1 san pham - gui lai la cap nhat danh gia cu.
 */
public class ReviewService {

    public static final int MAX_COMMENT = 1000;
    public static final int LIST_LIMIT = 50; // so danh gia hien tren trang chi tiet

    private final ReviewDAO reviewDAO = new ReviewDAO();
    private final ProductDAO productDAO = new ProductDAO();

    public List<Review> listFor(Integer productId) {
        return reviewDAO.findByProduct(productId, LIST_LIMIT);
    }

    /** {so danh gia, diem trung binh}. */
    public double[] summary(Integer productId) {
        return reviewDAO.summary(productId);
    }

    public boolean canReview(Customer customer, Integer productId) {
        return reviewDAO.hasPurchased(customer.getId(), productId);
    }

    public Review myReview(Customer customer, Integer productId) {
        return reviewDAO.findByCustomerAndProduct(customer.getId(), productId);
    }

    /** Tao moi hoac cap nhat danh gia. Tra ve true neu la CAP NHAT. Nem BusinessException khi du lieu sai, ForbiddenException khi chua mua. */
    public boolean write(Customer customer, Integer productId, Integer rating, String comment) {
        // 1. Kiem tra du lieu
        if (rating == null || rating < 1 || rating > 5) throw new BusinessException("Vui lòng chọn số sao từ 1 đến 5.");
        String text = comment == null ? "" : comment.trim();
        if (text.length() > MAX_COMMENT) throw new BusinessException("Nhận xét tối đa " + MAX_COMMENT + " ký tự.");
        Product product = productId == null ? null : productDAO.findById(productId);
        if (product == null) throw new BusinessException("Sản phẩm không tồn tại.");

        // 2. Chi khach da mua moi duoc danh gia
        if (!reviewDAO.hasPurchased(customer.getId(), productId)) throw new ForbiddenException();

        // 3. Da danh gia roi thi cap nhat, chua thi tao moi
        Review review = reviewDAO.findByCustomerAndProduct(customer.getId(), productId);
        boolean update = review != null;
        if (!update) {
            review = new Review();
            review.setProduct(product);
            review.setCustomer(customer);
        }
        review.setRating(rating);
        review.setComment(text.isEmpty() ? null : text);
        review.setCreatedAt(LocalDateTime.now());
        if (update) reviewDAO.update(review); else reviewDAO.save(review);
        return update;
    }

    /** Khach chua mua san pham nen khong duoc danh gia (servlet doi thanh HTTP 403). */
    public static class ForbiddenException extends RuntimeException {
        public ForbiddenException() {
            super("Chỉ khách đã mua sản phẩm mới đánh giá được.");
        }
    }
}
