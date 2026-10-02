package com.ecommerce.dao;

import com.ecommerce.entity.Category;

// DAO don gian nhat: khong query rieng, dung nguyen CRUD cua AbstractDAO.
public class CategoryDAO extends AbstractDAO<Category, Integer> {

    public CategoryDAO() {
        super(Category.class);
    }
}
