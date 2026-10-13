package com.ecommerce.service;

import com.ecommerce.dao.CategoryDAO;
import com.ecommerce.dao.ProductDAO;
import com.ecommerce.entity.Category;
import com.ecommerce.entity.Product;
import com.ecommerce.util.UploadUtil;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nghiep vu QUAN LY SAN PHAM cua Admin (them / sua / xoa). Phan doc/tim kiem san pham cho khach hang nam o ProductService,
 * tach rieng de 2 ben khong dam chan len nhau.
 */
public class AdminProductService {

    private final ProductDAO productDAO = new ProductDAO();
    private final CategoryDAO categoryDAO = new CategoryDAO();

    /**
     * Them (id == null) hoac sua san pham. Nhan tham so dang CHUOI nguyen ban tu form de tu kiem tra va bao loi tung o
     * (BusinessException chua Map ten-o -> loi). image = byte file tai len (null neu khong chon); removeImage = tick "xoa anh".
     */
    public Product save(Integer id, String name, String description, String price, String stock,
                        Integer categoryId, byte[] image, boolean removeImage) {
        // Gom MOI loi vao 1 Map<ten o, thong bao> roi nem 1 lan, de form hien loi o tat ca cac o sai cung luc
        Map<String, String> errors = new LinkedHashMap<>();

        // (1) Kiem tra tung o. Tham so la chuoi nguyen ban tu form nen tu doi sang so, sai dinh dang thi bao loi o do.
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

        // Danh muc phai co that trong DB (chong nguoi dung sua tay gia tri o chon)
        Category category = categoryId == null ? null : categoryDAO.findById(categoryId);
        if (category == null) errors.put("category", "Vui lòng chọn danh mục");

        // Anh (neu co chon): toi da 2MB va phai la JPG/PNG/WEBP (nhan dien bang byte dau file, khong tin ten file)
        String imageExt = null;
        if (image != null) {
            if (image.length > UploadUtil.MAX_BYTES) errors.put("image", "Ảnh tối đa 2MB");
            else if ((imageExt = UploadUtil.detectExtension(image)) == null) errors.put("image", "Chỉ nhận ảnh JPG, PNG hoặc WEBP");
        }

        // (2) Them moi: san pham rong. Sua: lay san pham cu tu DB roi ghi de cac o.
        Product product = new Product();
        if (id != null) {
            product = productDAO.findById(id);
            if (product == null) throw new BusinessException("Sản phẩm không tồn tại.");
        }
        if (!errors.isEmpty()) throw new BusinessException(errors); // co loi -> dung o day, chua ghi gi xuong DB hay o dia

        // (3) Het loi validate moi bat dau ghi: luu anh truoc de co duong dan, roi luu san pham.
        // oldImage de biet file nao can xoa sau khi luu thanh cong (khi doi anh hoac tick "xoa anh").
        String oldImage = product.getImageUrl();
        String newImage = oldImage;
        try {
            if (image != null) newImage = UploadUtil.store(image, imageExt);
            else if (removeImage) newImage = null;
        } catch (IOException e) {
            throw new BusinessException("Không lưu được ảnh, vui lòng thử lại.");
        }

        // (4) Gan gia tri da kiem tra vao san pham
        product.setName(name);
        product.setDescription(description == null || description.isBlank() ? null : description.trim());
        product.setPrice(priceValue);
        product.setStockQuantity(stockValue);
        product.setCategory(category);
        product.setImageUrl(newImage);

        // (5) Ghi DB. Neu that bai thi xoa file anh moi vua luu (de khong de file mo coi), roi nem loi tiep.
        Product saved;
        try {
            saved = id == null ? productDAO.save(product) : productDAO.update(product);
        } catch (RuntimeException e) {
            if (newImage != null && !newImage.equals(oldImage)) UploadUtil.delete(newImage); // khong de file mo coi
            throw e;
        }
        // (6) Luu DB thanh cong roi moi xoa anh cu (lam nguoc lai ma luu loi thi mat ca anh cu)
        if (oldImage != null && !oldImage.equals(newImage)) UploadUtil.delete(oldImage);
        return saved;
    }

    /** Xoa san pham. Da nam trong don hang thi tu choi (don hang can giu lai lich su), thay vao do dat ton kho = 0. */
    public void delete(Integer id) {
        Product product = productDAO.findById(id);
        if (product == null) throw new BusinessException("Sản phẩm không tồn tại hoặc đã bị xoá.");
        // Don hang da dat phai giu lai lich su (OrderItem tro toi san pham nay) -> khong cho xoa
        long inOrders = productDAO.countOrderItems(id);
        if (inOrders > 0) {
            throw new BusinessException("Không thể xoá: sản phẩm đã có trong " + inOrders
                    + " đơn hàng. Hãy đặt tồn kho = 0 để ngừng bán.");
        }
        productDAO.deleteWithCartItems(id); // go khoi cac gio hang truoc roi xoa san pham (danh gia xoa theo)
        UploadUtil.delete(product.getImageUrl());
    }
}
