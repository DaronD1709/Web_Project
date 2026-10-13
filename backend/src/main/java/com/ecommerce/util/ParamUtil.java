package com.ecommerce.util;

/**
 * Doc tham so request an toan: tham so thieu/rong/sai dinh dang thi tra ve null (hoac gia tri mac dinh)
 * thay vi nem NumberFormatException lam trang bi loi 500 khi nguoi dung go bay URL.
 */
public class ParamUtil {

    // "3" -> 3 ; null, "", "abc" -> null (null nghia la "khong co gia tri", Servlet tu quyet dinh mac dinh)
    public static Integer intOrNull(String s) {
        try {
            return (s == null || s.isBlank()) ? null : Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Nhu intOrNull nhung thieu/sai thi dung gia tri mac dinh: intOr(req.getParameter("page"), 1)
    public static int intOr(String s, int defaultValue) {
        Integer v = intOrNull(s);
        return v != null ? v : defaultValue;
    }

    // Nhu intOrNull, cho so thuc (vd gia tien trong bo loc)
    public static Double doubleOrNull(String s) {
        try {
            return (s == null || s.isBlank()) ? null : Double.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // "2026-10-05" -> LocalDate; thieu/sai dinh dang -> null (o <input type="date"> luon gui dung dang yyyy-MM-dd)
    public static java.time.LocalDate dateOrNull(String s) {
        try {
            return (s == null || s.isBlank()) ? null : java.time.LocalDate.parse(s.trim());
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    // Bo khoang trang 2 dau; chuoi rong coi nhu khong nhap (null) -> khong loc theo tu khoa
    public static String trimOrNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
