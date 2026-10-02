package com.ecommerce.util;

/**
 * Doc tham so request an toan: tham so thieu/rong/sai dinh dang thi tra ve null (hoac gia tri mac dinh)
 * thay vi nem NumberFormatException lam trang bi loi 500 khi nguoi dung go bay URL.
 */
public class ParamUtil {

    public static Integer intOrNull(String s) {
        try {
            return (s == null || s.isBlank()) ? null : Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static int intOr(String s, int defaultValue) {
        Integer v = intOrNull(s);
        return v != null ? v : defaultValue;
    }

    public static Double doubleOrNull(String s) {
        try {
            return (s == null || s.isBlank()) ? null : Double.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String trimOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
