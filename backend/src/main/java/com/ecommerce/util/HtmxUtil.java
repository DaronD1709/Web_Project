package com.ecommerce.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/** Cac tien ich nho khi lam viec voi htmx: nhan biet request tu htmx, bao toast, chuyen huong. */
public class HtmxUtil {

    /** htmx luon gui header "HX-Request: true". */
    public static boolean isHtmx(HttpServletRequest req) {
        return "true".equals(req.getHeader("HX-Request"));
    }

    /**
     * Bao trinh duyet hien 1 toast (listener "showToast" o footer.jspf). Header HTTP chi an toan voi ASCII,
     * nen tieng Viet duoc ma hoa \\uXXXX trong JSON.
     */
    public static void toast(HttpServletResponse resp, String message, String type) {
        resp.setHeader("HX-Trigger", "{\"showToast\":{\"message\":\"" + jsonAscii(message) + "\",\"type\":\"" + type + "\"}}");
    }

    /** Yeu cau htmx chuyen sang trang khac (vd /login) thay vi doi noi dung 1 phan. */
    public static void redirect(HttpServletResponse resp, String url) {
        resp.setHeader("HX-Redirect", url);
    }

    private static String jsonAscii(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') sb.append('\\').append(c);
            else if (c < 0x20 || c > 0x7e) sb.append(String.format("\\u%04x", (int) c));
            else sb.append(c);
        }
        return sb.toString();
    }
}
