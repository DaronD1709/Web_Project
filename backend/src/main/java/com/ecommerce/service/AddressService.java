package com.ecommerce.service;

import com.ecommerce.dao.AddressDAO;
import com.ecommerce.entity.Address;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class AddressService {

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^0\\d{9}$");

    private final AddressDAO addressDAO = new AddressDAO();

    public List<Address> getAddresses(Integer customerId) {
        requireCustomerId(customerId);
        return addressDAO.findByCustomerId(customerId);
    }

    public Address create(Integer customerId,
                          String recipientName,
                          String phone,
                          String street,
                          String city,
                          boolean isDefault) {
        requireCustomerId(customerId);

        Address address = buildAndValidate(
                recipientName,
                phone,
                street,
                city
        );

        return addressDAO.createForCustomer(
                customerId,
                address,
                isDefault
        );
    }

    private static final String IN_USE_MESSAGE =
            "Địa chỉ này đã được dùng trong đơn hàng nên không thể sửa. Hãy thêm địa chỉ mới.";

    /** Id các địa chỉ của khách đã dùng trong đơn (không sửa được). */
    public java.util.Set<Integer> getUsedAddressIds(Integer customerId) {
        return addressDAO.findUsedAddressIds(customerId);
    }

    public Address update(Integer customerId,
                          Integer addressId,
                          String recipientName,
                          String phone,
                          String street,
                          String city,
                          boolean isDefault) {
        requireCustomerId(customerId);
        requireAddressId(addressId);

        requireOwnedAddress(customerId, addressId);

        Address values = buildAndValidate(
                recipientName,
                phone,
                street,
                city
        );

        // Dia chi da nam trong don hang thi khong sua (se doi noi giao cua don cu): khach them dia chi moi
        if (addressDAO.countOrderReferences(addressId) > 0) {
            throw new BusinessException(IN_USE_MESSAGE);
        }

        Address updated;
        try {
            updated = addressDAO.updateOwned(customerId, addressId, values, isDefault);
        } catch (IllegalStateException e) { // vua co don dung dia chi nay giua luc kiem tra va luc luu
            if (AddressDAO.ADDRESS_IN_USE.equals(e.getMessage())) throw new BusinessException(IN_USE_MESSAGE);
            throw e;
        }

        if (updated == null) {
            throw new BusinessException(
                    "Địa chỉ không tồn tại hoặc không thuộc tài khoản của bạn."
            );
        }

        return updated;
    }

    public void setDefault(Integer customerId, Integer addressId) {
        requireCustomerId(customerId);
        requireAddressId(addressId);

        requireOwnedAddress(customerId, addressId);

        if (!addressDAO.setDefaultOwned(customerId, addressId)) {
            throw new BusinessException(
                    "Không thể đặt địa chỉ này làm mặc định."
            );
        }
    }

    public void delete(Integer customerId, Integer addressId) {
        requireCustomerId(customerId);
        requireAddressId(addressId);

        requireOwnedAddress(customerId, addressId);

        if (addressDAO.countOrderReferences(addressId) > 0) {
            throw new BusinessException(
                    "Không thể xóa địa chỉ đã được sử dụng trong đơn hàng."
            );
        }

        if (!addressDAO.deleteOwned(customerId, addressId)) {
            throw new BusinessException(
                    "Không thể xóa địa chỉ này."
            );
        }
    }

    private Address requireOwnedAddress(Integer customerId,
                                        Integer addressId) {
        Address address =
                addressDAO.findOwnedById(addressId, customerId);

        if (address == null) {
            throw new BusinessException(
                    "Địa chỉ không tồn tại hoặc không thuộc tài khoản của bạn."
            );
        }

        return address;
    }

    private Address buildAndValidate(String recipientName,
                                     String phone,
                                     String street,
                                     String city) {
        recipientName = normalize(recipientName);
        phone = normalizePhone(phone);
        street = normalize(street);
        city = normalize(city);

        Map<String, String> errors = new LinkedHashMap<>();

        if (recipientName.isEmpty()) {
            errors.put(
                    "recipientName",
                    "Vui lòng nhập tên người nhận"
            );
        } else if (recipientName.length() > 100) {
            errors.put(
                    "recipientName",
                    "Tên người nhận không được vượt quá 100 ký tự"
            );
        }

        if (phone.isEmpty()) {
            errors.put(
                    "phone",
                    "Vui lòng nhập số điện thoại"
            );
        } else if (!PHONE_PATTERN.matcher(phone).matches()) {
            errors.put(
                    "phone",
                    "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0"
            );
        }

        if (street.isEmpty()) {
            errors.put(
                    "street",
                    "Vui lòng nhập địa chỉ cụ thể"
            );
        } else if (street.length() > 255) {
            errors.put(
                    "street",
                    "Địa chỉ cụ thể không được vượt quá 255 ký tự"
            );
        }

        if (city.isEmpty()) {
            errors.put(
                    "city",
                    "Vui lòng nhập tỉnh hoặc thành phố"
            );
        } else if (city.length() > 100) {
            errors.put(
                    "city",
                    "Tỉnh hoặc thành phố không được vượt quá 100 ký tự"
            );
        }

        if (!errors.isEmpty()) {
            throw new BusinessException(errors);
        }

        Address address = new Address();
        address.setRecipientName(recipientName);
        address.setPhone(phone);
        address.setStreet(street);
        address.setCity(city);

        return address;
    }

    private void requireCustomerId(Integer customerId) {
        if (customerId == null || customerId <= 0) {
            throw new BusinessException(
                    "Tài khoản khách hàng không hợp lệ."
            );
        }
    }

    private void requireAddressId(Integer addressId) {
        if (addressId == null || addressId <= 0) {
            throw new BusinessException(
                    "Địa chỉ không hợp lệ."
            );
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizePhone(String value) {
        if (value == null) {
            return "";
        }

        return value.trim()
                .replace(" ", "")
                .replace(".", "")
                .replace("-", "");
    }
}