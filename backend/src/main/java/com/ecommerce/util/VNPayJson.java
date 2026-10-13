package com.ecommerce.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** JSON phẳng của QueryDr; không thêm thư viện hay thay cách trả JSP của app. */
public final class VNPayJson {

    private VNPayJson() {
    }

    public static String write(Map<String, String> fields) {
        StringBuilder json = new StringBuilder("{");
        for (var entry : fields.entrySet()) {
            if (json.length() > 1) {
                json.append(',');
            }
            quote(json, entry.getKey());
            json.append(':');
            quote(json, entry.getValue());
        }
        return json.append('}').toString();
    }

    private static void quote(StringBuilder out, String text) {
        out.append('"');
        for (char ch : text.toCharArray()) {
            if (ch == '"' || ch == '\\') {
                out.append('\\').append(ch);
            } else if (ch < 0x20) {
                out.append(String.format("\\u%04x", (int) ch));
            } else {
                out.append(ch);
            }
        }
        out.append('"');
    }

    public static Map<String, String> read(String json) {
        if (json == null || json.length() > 16_384) {
            throw invalid();
        }
        return new Reader(json).readObject();
    }

    private static IllegalArgumentException invalid() {
        // Không đưa nội dung gateway/chữ ký vào exception hoặc log.
        return new IllegalArgumentException("JSON VNPay không hợp lệ.");
    }

    private static final class Reader {
        private final String text;
        private int position;

        private Reader(String text) {
            this.text = text;
        }

        private Map<String, String> readObject() {
            Map<String, String> fields = new LinkedHashMap<>();
            expect('{');
            if (!take('}')) {
                do {
                    String name = string();
                    expect(':');
                    String value = scalar();
                    if (fields.size() >= 64 || fields.containsKey(name)) {
                        throw invalid();
                    }
                    fields.put(name, value);
                } while (take(','));
                expect('}');
            }
            whitespace();
            if (position != text.length()) {
                throw invalid();
            }
            return fields;
        }

        private String scalar() {
            whitespace();
            if (position < text.length() && text.charAt(position) == '"') {
                return string();
            }
            int start = position;
            while (position < text.length()
                    && ",} \t\r\n".indexOf(text.charAt(position)) < 0) {
                position++;
            }
            String value = text.substring(start, position);
            if ("null".equals(value)) {
                return "";
            }
            // Một số gateway trả số tiền dưới dạng JSON number.
            // Giữ nguyên chữ số để kiểm tra kiểu và chữ ký ở Service.
            if (!value.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")) {
                throw invalid();
            }
            return value;
        }

        private String string() {
            expect('"');
            StringBuilder value = new StringBuilder();
            while (position < text.length()) {
                char ch = text.charAt(position++);
                if (ch == '"') {
                    return value.toString();
                }
                if (ch < 0x20) {
                    throw invalid();
                }
                if (ch == '\\') {
                    if (position == text.length()) {
                        throw invalid();
                    }
                    char escape = text.charAt(position++);
                    switch (escape) {
                        case '"', '\\', '/' -> value.append(escape);
                        case 'b' -> value.append('\b');
                        case 'f' -> value.append('\f');
                        case 'n' -> value.append('\n');
                        case 'r' -> value.append('\r');
                        case 't' -> value.append('\t');
                        case 'u' -> {
                            if (position + 4 > text.length()) {
                                throw invalid();
                            }
                            String hex = text.substring(position, position + 4);
                            if (!hex.matches("[0-9a-fA-F]{4}")) {
                                throw invalid();
                            }
                            value.append((char) Integer.parseInt(hex, 16));
                            position += 4;
                        }
                        default -> throw invalid();
                    }
                } else {
                    value.append(ch);
                }
            }
            throw invalid();
        }

        private boolean take(char wanted) {
            whitespace();
            if (position < text.length() && text.charAt(position) == wanted) {
                position++;
                return true;
            }
            return false;
        }

        private void expect(char wanted) {
            if (!take(wanted)) {
                throw invalid();
            }
        }

        private void whitespace() {
            while (position < text.length()
                    && " \t\r\n".indexOf(text.charAt(position)) >= 0) {
                position++;
            }
        }
    }
}
