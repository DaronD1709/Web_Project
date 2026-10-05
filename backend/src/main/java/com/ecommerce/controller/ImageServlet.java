package com.ecommerce.controller;

import com.ecommerce.util.UploadUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

// GET /uploads/<ten-file> -> TRA ANH san pham ve cho trinh duyet (anh do Admin tai len, luu ngoai webapp, xem UploadUtil).
// Chi phuc vu (doc) anh; viec nhan file tai len nam o AdminProductServlet/AdminProductService.
@WebServlet("/uploads/*")
public class ImageServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String info = req.getPathInfo(); // "/abc.png"
        Path file = (info == null || info.length() < 2) ? null : UploadUtil.resolve(info.substring(1));
        if (file == null || !Files.isRegularFile(file)) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        String name = file.getFileName().toString();
        resp.setContentType(name.endsWith(".png") ? "image/png" : name.endsWith(".webp") ? "image/webp" : "image/jpeg");
        resp.setContentLengthLong(Files.size(file));
        resp.setHeader("Cache-Control", "public, max-age=86400"); // ten file la UUID, doi anh = ten moi nen cache an toan
        resp.setHeader("X-Content-Type-Options", "nosniff");
        Files.copy(file, resp.getOutputStream());
    }
}
