# Backend (NetBeans / Java Servlet)

Di chuyển project NetBeans hiện tại (servlet/JSP + PostgreSQL) vào thư mục này.

## Cấu trúc đề xuất

- Giữ nguyên toàn bộ phần Admin bằng JSP (CRUD sản phẩm, đơn hàng, danh mục...) — không cần sửa gì, 2 bạn chưa biết React vẫn code JSP bình thường cho Admin.
- Thêm 1 nhóm servlet mới đóng vai trò REST API (ví dụ package `api/`), trả JSON thay vì forward sang JSP — dùng cho phần Customer-facing mà frontend React gọi vào (`/api/products`, `/api/cart`, `/api/orders`...).
- DAO và business logic (theo class diagram đã thiết kế trong `docs/class-diagram.md`) dùng chung cho cả JSP lẫn REST — không viết lại logic 2 lần.

## Thư viện cần thêm

- 1 JSON library, ví dụ `org.json` hoặc `Gson`, để serialize entity sang JSON trong các servlet REST.
- CORS: nếu frontend chạy `localhost:5173` (Vite dev server) còn backend chạy `localhost:8080` (Tomcat) thì cần set header `Access-Control-Allow-Origin` khi dev local.
