package com.ecommerce.service;

import com.ecommerce.dao.CategoryDAO;
import com.ecommerce.dao.ProductDAO;
import com.ecommerce.dto.PageResult;
import com.ecommerce.dto.ProductFilter;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.util.UploadUtil;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProductService {

    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

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

    // ------------------------------------------------------------------ Admin: them / sua / xoa

    /**
     * Them (id == null) hoac sua san pham. Nhan tham so dang CHUOI nguyen ban tu form de tu kiem tra va bao loi tung o
     * (BusinessException chua Map ten-o -> loi). image = byte file tai len (null neu khong chon); removeImage = tick "xoa anh".
     */
    public Product save(Integer id, String name, String description, String price, String stock,
                        Integer categoryId, byte[] image, boolean removeImage) {
        Map<String, String> errors = new LinkedHashMap<>();

        name = name == null ? "" : name.trim();
        if (name.isEmpty()) errors.put("name", "Vui lòng nhập tên sản phẩm");
        else if (name.length() > 255) errors.put("name", "Tên sản phẩm tối đa 255 ký tự");

        double priceValue = 0;
        try {
            priceValue = Double.parseDouble(price == null ? "" : price.trim());
            if (!(priceValue > 0) || priceValue > 1_000_000_000) errors.put("price", "Giá phải lớn hơn 0 và không quá 1 tỷ");
        } catch (NumberFormatException e) {
            errors.put("price", "Giá không hợp lệ");
        }

        int stockValue = 0;
        try {
            stockValue = Integer.parseInt(stock == null ? "" : stock.trim());
            if (stockValue < 0) errors.put("stock", "Tồn kho không được âm");
        } catch (NumberFormatException e) {
            errors.put("stock", "Tồn kho phải là số nguyên");
        }

        Category category = categoryId == null ? null : categoryDAO.findById(categoryId);
        if (category == null) errors.put("category", "Vui lòng chọn danh mục");

        String imageExt = null;
        if (image != null) {
            if (image.length > UploadUtil.MAX_BYTES) errors.put("image", "Ảnh tối đa 2MB");
            else if ((imageExt = UploadUtil.detectExtension(image)) == null) errors.put("image", "Chỉ nhận ảnh JPG, PNG hoặc WEBP");
        }

        Product product = new Product();
        if (id != null) {
            product = productDAO.findById(id);
            if (product == null) throw new BusinessException("Sản phẩm không tồn tại.");
        }
        if (!errors.isEmpty()) throw new BusinessException(errors);

        // Het loi validate moi bat dau ghi: luu anh truoc de co duong dan, roi luu san pham.
        String oldImage = product.getImageUrl();
        String newImage = oldImage;
        try {
            if (image != null) newImage = UploadUtil.store(image, imageExt);
            else if (removeImage) newImage = null;
        } catch (IOException e) {
            throw new BusinessException("Không lưu được ảnh, vui lòng thử lại.");
        }

        product.setName(name);
        product.setDescription(description == null || description.isBlank() ? null : description.trim());
        product.setPrice(priceValue);
        product.setStockQuantity(stockValue);
        product.setCategory(category);
        product.setImageUrl(newImage);

        Product saved;
        try {
            saved = id == null ? productDAO.save(product) : productDAO.update(product);
        } catch (RuntimeException e) {
            if (newImage != null && !newImage.equals(oldImage)) UploadUtil.delete(newImage); // khong de file mo coi
            throw e;
        }
        if (oldImage != null && !oldImage.equals(newImage)) UploadUtil.delete(oldImage);
        return saved;
    }

    /** Xoa san pham. Da nam trong don hang thi tu choi (don hang can giu lai lich su), thay vao do dat ton kho = 0. */
    public void delete(Integer id) {
        Product product = productDAO.findById(id);
        if (product == null) throw new BusinessException("Sản phẩm không tồn tại hoặc đã bị xoá.");
        long inOrders = productDAO.countOrderItems(id);
        if (inOrders > 0) {
            throw new BusinessException("Không thể xoá: sản phẩm đã có trong " + inOrders
                    + " đơn hàng. Hãy đặt tồn kho = 0 để ngừng bán.");
        }
        productDAO.deleteWithCartItems(id);
        UploadUtil.delete(product.getImageUrl());
    }
}
