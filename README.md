# Web Ecommerce — Đồ án

Đồ án web ecommerce: Java Servlet/JSP (NetBeans) + Tomcat + PostgreSQL.

## Tech stack (chốt)

| Layer | Công nghệ |
|---|---|
| Backend / View | Java Servlet + JSP (NetBeans), Tomcat 10.1, PostgreSQL |
| CSS | Tailwind CSS (CDN) |
| Component có sẵn | daisyUI (CDN) |
| Tương tác không reload trang | htmx (CDN) |

Không tách frontend riêng, không REST API, không build pipeline (npm/Vite) — 1 codebase JSP duy nhất, cả team code chung, không ai cần học framework mới. Xem `webapp/sample-product-card.jsp` để biết cách nhúng 3 thư viện CDN trên vào 1 trang thật.

## Cấu trúc repo

```
.
├── backend/     Project NetBeans (servlet/JSP) — move project hiện tại vào đây
└── docs/        Tài liệu thiết kế: feature list, class diagram
```

## Chạy thử

Mở `backend/` bằng NetBeans, deploy lên Tomcat như bình thường.

## Tài liệu
- [Feature list](docs/feature-list.md)
- [Class diagram & entity design](docs/class-diagram.md)
