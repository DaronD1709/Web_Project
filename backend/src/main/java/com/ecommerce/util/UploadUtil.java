package com.ecommerce.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * Luu anh san pham do Admin tai len. Anh nam NGOAI thu muc webapp (mac dinh ~/nongviet-uploads, doi bang bien moi truong
 * UPLOAD_DIR) vi webapp bi xoa sach moi lan build/deploy lai; ImageServlet phuc vu anh qua URL /uploads/<ten-file>.
 * Chi nhan JPG/PNG/WEBP, nhan dien bang BYTE DAU cua file (khong tin ten file/Content-Type nguoi dung gui), ten file luu
 * la UUID ngau nhien -> khong ghi de, khong lo duong dan.
 */
public class UploadUtil {

    public static final long MAX_BYTES = 2L * 1024 * 1024;
    public static final String URL_PREFIX = "uploads/";

    public static Path dir() {
        String env = System.getenv("UPLOAD_DIR");
        return (env != null && !env.isBlank()) ? Paths.get(env) : Paths.get(System.getProperty("user.home"), "nongviet-uploads");
    }

    /** "jpg" | "png" | "webp" theo byte dau, hoac null neu khong phai anh duoc phep. */
    public static String detectExtension(byte[] d) {
        if (d.length >= 3 && (d[0] & 0xFF) == 0xFF && (d[1] & 0xFF) == 0xD8 && (d[2] & 0xFF) == 0xFF) return "jpg";
        if (d.length >= 8 && (d[0] & 0xFF) == 0x89 && d[1] == 'P' && d[2] == 'N' && d[3] == 'G') return "png";
        if (d.length >= 12 && d[0] == 'R' && d[1] == 'I' && d[2] == 'F' && d[3] == 'F'
                && d[8] == 'W' && d[9] == 'E' && d[10] == 'B' && d[11] == 'P') return "webp";
        return null;
    }

    /** Ghi file, tra ve gia tri luu vao Product.imageUrl, vd "uploads/3f2a....png". */
    public static String store(byte[] data, String extension) throws IOException {
        Files.createDirectories(dir());
        String name = UUID.randomUUID() + "." + extension;
        Files.write(dir().resolve(name), data);
        return URL_PREFIX + name;
    }

    /** Xoa file cua 1 imageUrl cu (khong bao loi neu khong con); bo qua gia tri khong thuoc thu muc uploads. */
    public static void delete(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(URL_PREFIX)) return;
        Path file = resolve(imageUrl.substring(URL_PREFIX.length()));
        if (file == null) return;
        try {
            Files.deleteIfExists(file);
        } catch (IOException ignored) {
            // file mo coi con lai khong anh huong gi, khong dang de lam hong thao tac xoa san pham
        }
    }

    /** Duong dan that cua 1 ten file trong thu muc uploads; null neu ten khong an toan (chan "../"). */
    public static Path resolve(String fileName) {
        if (fileName == null || fileName.isEmpty() || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) return null;
        Path base = dir().toAbsolutePath().normalize();
        Path file = base.resolve(fileName).normalize();
        return file.startsWith(base) ? file : null;
    }
}
