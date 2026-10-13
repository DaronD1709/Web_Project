package com.ecommerce.service;

import com.ecommerce.dao.ProductDAO;
import com.ecommerce.dto.PageResult;
import com.ecommerce.dto.ProductFilter;
import com.ecommerce.entity.Product;

import java.util.List;

public class ProductService {

    private final ProductDAO productDAO = new ProductDAO();

    public List<Product> getAllProducts() {
        return productDAO.findAll();
    }

    public Product getProductById(Integer id) {
        return productDAO.findById(id);
    }

    public List<Product> getProductsByCategory(Integer categoryId) {
        return productDAO.findByCategoryId(categoryId);
    }

    public List<Product> getLatestProducts(int limit) {
        return productDAO.findLatest(limit);
    }

    /** Trang /products: 1 query lay san pham cua trang + 1 query dem tong de tinh so trang. */
    public PageResult<Product> search(ProductFilter filter) {
        long total = productDAO.countSearch(filter);
        List<Product> items = productDAO.search(filter);
        return new PageResult<>(items, total, filter.getPage(), filter.getPageSize());
    }
}
