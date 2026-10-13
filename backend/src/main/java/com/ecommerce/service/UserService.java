package com.ecommerce.service;

import com.ecommerce.dao.UserDAO;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.User;
import com.ecommerce.util.PasswordUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** Ho so cua Customer. ID phai lay tu currentUser trong session, khong lay tu form. */
public class UserService {

    private static final Pattern PHONE = Pattern.compile("\\+?[0-9][0-9 .()-]*");
    private final UserDAO userDAO = new UserDAO();

    public Customer getProfile(Integer customerId) {
        User user = customerId == null ? null : userDAO.findById(customerId);
        if (!(user instanceof Customer customer)) {
            throw new BusinessException("Tài khoản khách hàng không tồn tại.");
        }
        return customer;
    }

    public Customer updateProfile(Integer customerId, String fullName, String phone) {
        getProfile(customerId); // kiem tra lai loai tai khoan o Service
        Map<String, String> errors = new LinkedHashMap<>();
        fullName = fullName == null ? "" : fullName.trim();
        phone = phone == null ? "" : phone.trim();
        if (fullName.isBlank()) errors.put("fullName", "Vui lòng nhập họ tên");
        else if (fullName.length() > 255) errors.put("fullName", "Họ tên tối đa 255 ký tự");
        if (!phone.isEmpty()) {
            long digits = phone.chars().filter(c -> c >= '0' && c <= '9').count();
            if (phone.length() > 255 || !PHONE.matcher(phone).matches() || digits < 8 || digits > 15) {
                errors.put("phone", "Số điện thoại cần có từ 8 đến 15 chữ số");
            }
        }
        if (!errors.isEmpty()) throw new BusinessException(errors);
        if (!userDAO.updateProfile(customerId, fullName, phone.isEmpty() ? null : phone)) {
            throw new BusinessException("Tài khoản khách hàng không tồn tại.");
        }
        return getProfile(customerId);
    }

    public Customer changePassword(Integer customerId, String current, String newPassword, String confirm) {
        Customer customer = getProfile(customerId); // doc hash moi nhat tu DB, khong dung hash trong session
        Map<String, String> errors = new LinkedHashMap<>();
        if (current == null || current.isEmpty()) errors.put("current", "Vui lòng nhập mật khẩu hiện tại");
        else if (!PasswordUtil.verify(current, customer.getPasswordHash())) {
            errors.put("current", "Mật khẩu hiện tại không đúng");
        }
        if (newPassword == null || newPassword.length() < 6) {
            errors.put("newPassword", "Mật khẩu tối thiểu 6 ký tự");
        } else if (!newPassword.equals(confirm)) {
            errors.put("confirm", "Mật khẩu nhập lại không khớp");
        }
        if (!errors.isEmpty()) throw new BusinessException(errors);
        if (!userDAO.changePassword(customerId, customer.getPasswordHash(), PasswordUtil.hash(newPassword))) {
            throw new BusinessException(Map.of("current", "Mật khẩu đã thay đổi. Vui lòng nhập lại mật khẩu hiện tại."));
        }
        return getProfile(customerId);
    }
}
