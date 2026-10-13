package com.ecommerce.service;

import java.util.Map;

/**
 * Loi nghiep vu/validate do Service nem ra (vd: email trung, het hang, sai mat khau).
 * Servlet bat no va hien thong bao cho nguoi dung thay vi de trang bi loi 500.
 *  - new BusinessException("...")           : 1 thong bao chung  -> e.getMessage()
 *  - new BusinessException(Map<ten_o, loi>) : loi theo tung o form -> e.getErrors()
 */
public class BusinessException extends RuntimeException {

    private final Map<String, String> errors;

    public BusinessException(String message) {
        super(message);
        this.errors = Map.of();
    }

    public BusinessException(Map<String, String> errors) {
        super("Dữ liệu không hợp lệ");
        this.errors = errors;
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
