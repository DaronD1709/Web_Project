package com.ecommerce.service;

import com.ecommerce.dao.CategoryDAO;
import com.ecommerce.entity.Category;

import java.util.List;

public class CategoryService {

    private final CategoryDAO categoryDAO = new CategoryDAO();

    public List<Category> getAllCategories() {
        return categoryDAO.findAll();
    }

    /**
     * Lấy danh mục theo id để hiển thị tên danh mục đang được chọn.
     * Không có id hoặc id không tồn tại thì trả về null.
     */
    public Category getCategoryById(Integer categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryDAO.findById(categoryId);
    }
}