# Web Ecommerce — Đồ án

Đồ án web ecommerce: Java servlet/JSP backend (NetBeans) + React frontend.

## Cấu trúc repo

```
.
├── backend/     Java servlet/JSP (NetBeans) — Admin (JSP) + REST API cho frontend
├── frontend/    React + Vite + shadcn/ui — giao diện Customer-facing
└── docs/        Tài liệu thiết kế: feature list, class diagram
```

## Vì sao tách FE/BE như vậy

Admin giữ nguyên JSP vì team có 2 bạn chưa biết React — không ai phải học công nghệ mới để làm phần Admin. Phần Customer-facing (trang chủ, sản phẩm, giỏ hàng...) chuyển sang React để giao diện đẹp và dễ làm UI hơn; backend expose thêm REST API cho phần này dùng (xem `backend/README.md`).

## Chạy thử

### Backend
Mở thư mục `backend/` bằng NetBeans như bình thường, deploy lên Tomcat.

### Frontend
```bash
cd frontend
npm install
npm run dev
```

## Tài liệu
- [Feature list](docs/feature-list.md)
- [Class diagram & entity design](docs/class-diagram.md)
