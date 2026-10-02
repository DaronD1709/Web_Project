package com.ecommerce.service;

import com.ecommerce.dao.ProductDAO;
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

    public Product createProduct(Product product) {
        return productDAO.save(product);
    }
}
