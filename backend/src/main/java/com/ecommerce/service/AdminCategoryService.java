package com.ecommerce.service;

import com.ecommerce.dao.CategoryDAO;
import com.ecommerce.entity.Category;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Nghiep vu QUAN LY DANH MUC SAN PHAM cua Admin: xem (kem so san pham), them, sua, xoa. Danh muc con san pham thi khong xoa duoc. */
public class AdminCategoryService {

    private static final int MAX_NAME = 100;
    private static final int MAX_DESCRIPTION = 255; // khop do dai cot description (varchar 255)

    private final CategoryDAO categoryDAO = new CategoryDAO();

    public List<Category> list() {
        return categoryDAO.findAllOrdered();
    }

    /** id danh muc -> so san pham, cho cot "San pham" cua bang. */
    public Map<Integer, Long> productCounts() {
        return categoryDAO.countProducts();
    }

    public Category getById(Integer id) {
        Category c = id == null ? null : categoryDAO.findById(id);
        if (c == null) throw new BusinessException("Danh mục không tồn tại.");
        return c;
    }

    /**
     * Them (id == null) hoac sua danh muc. Nhan tham so dang CHUOI nguyen ban tu form de tu kiem tra va bao loi tung o
     * (BusinessException chua Map ten-o -> loi). Cac buoc: (1) kiem tra tung o (2) neu sua thi lay ban cu (3) gan gia tri (4) luu.
     */
    public Category save(Integer id, String name, String description) {
        Map<String, String> errors = new LinkedHashMap<>();

        // Ten: bat buoc, toi da 100 ky tu, khong trung voi danh muc khac (khong phan biet hoa thuong)
        name = name == null ? "" : name.trim().replaceAll("\\s+", " ");
        if (name.isEmpty()) errors.put("name", "Vui lòng nhập tên danh mục");
        else if (name.length() > MAX_NAME) errors.put("name", "Tên danh mục tối đa " + MAX_NAME + " ký tự");
        else {
            Category same = categoryDAO.findByName(name);
            if (same != null && !same.getId().equals(id)) errors.put("name", "Đã có danh mục tên này");
        }

        String desc = description == null ? "" : description.trim();
        if (desc.length() > MAX_DESCRIPTION) errors.put("description", "Mô tả tối đa " + MAX_DESCRIPTION + " ký tự");

        Category category = new Category();
        if (id != null) category = getById(id);
        if (!errors.isEmpty()) throw new BusinessException(errors);

        category.setName(name);
        category.setDescription(desc.isEmpty() ? null : desc);
        return id == null ? categoryDAO.save(category) : categoryDAO.update(category);
    }

    /** Xoa danh muc. Con san pham thi tu choi (san pham khong duoc mat danh muc): Admin phai chuyen hoac xoa san pham truoc. */
    public void delete(Integer id) {
        Category c = getById(id);
        long products = categoryDAO.countProducts(id);
        if (products > 0) {
            throw new BusinessException("Không thể xoá: danh mục \"" + c.getName() + "\" còn " + products
                    + " sản phẩm. Hãy chuyển hoặc xoá các sản phẩm đó trước.");
        }
        categoryDAO.deleteById(id);
    }
}
