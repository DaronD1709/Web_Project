# Backend — Java Servlet/JSP + JPA (Hibernate) + PostgreSQL

Maven Web Application. Mo truc tiep thu muc nay bang NetBeans ("Open Project").

## Stack

- Jakarta Servlet 6.0 / JSP (chay tren Tomcat 10.1)
- JPA (Jakarta Persistence 3.1) — Hibernate 6.5 lam provider
- PostgreSQL
- Jakarta Mail (Angus Mail) — gui email qua SMTP

## Kien truc 3 lop

```
src/main/java/com/ecommerce/
├── entity/        Model — 20 JPA entity + 4 enum, dung chung cho ca JSP lan service
├── dto/           Goi tham so/ket qua di chuyen giua cac lop (ProductFilter, PageResult) — chi tao khi can
├── dao/           Data Access — AbstractDAO<T,ID> (CRUD chung) + DAO cu the
├── service/       Business logic — AuthService, ProductService, EmailService (gui mail), BusinessException
├── controller/    Servlet — nhan request, goi Service, forward sang JSP
│   ├── admin/
│   └── customer/  Register/Login/Logout/ForgotPassword/ResetPassword/Product Servlet
├── listener/      AppInitListener — nap du lieu mau luc khoi dong, dong thread gui mail luc dung
└── util/          JPAUtil (EntityManagerFactory), DataSeeder (du lieu mau), PasswordUtil (PBKDF2), ParamUtil

src/main/webapp/
├── WEB-INF/views/{customer,admin,common}/   JSP that (Servlet forward toi, khong vao thang bang URL)
├── static/                                   CSS/JS/anh tinh (NGOAI WEB-INF)
└── mockup/                                   Mockup HTML tinh de tham khao giao dien
```

**Luong 1 request:** `Servlet (controller)` → `Service` → `DAO` → `EntityManager` (JPA) → PostgreSQL.
Servlet KHONG duoc goi thang DAO — luon qua Service, de sau nay them validate/business rule
(vd: check Voucher.isValid() truoc khi cho ap dung) ma khong phai sua Servlet.

## Cau hinh ket noi DB

Moi may 1 DB rieng: copy `src/main/resources/db.properties.example` thanh `db.properties` (da .gitignore, KHONG commit)
va sua `db.url`, `db.user`, `db.password` theo PostgreSQL cua may ban (`JPAUtil` doc file nay, ghi de gia tri mac dinh trong
`persistence.xml`). Co `hibernate.hbm2ddl.auto=update` nen KHONG can tu tao bang — chay app 1 lan, Hibernate tu doc entity va tao bang,
`DataSeeder` tu nap du lieu mau (danh muc, san pham, tai khoan dev; xem comment trong `DataSeeder.java`).

## Cau hinh gui email

Copy `src/main/resources/mail.properties.example` thanh `mail.properties` (da .gitignore). `mail.enabled=false` (hoac khong co file)
thi KHONG gui that, noi dung email (ke ca link dat lai mat khau) duoc in ra log Tomcat — du de dev. Gui that qua Gmail: bat xac thuc
2 buoc, tao App password va dien vao `mail.password`. Thu nhanh khong can tai khoan: chay `python3 -m smtpd -n -c DebuggingServer localhost:1025`
roi dat `mail.host=localhost`, `mail.port=1025`, `mail.auth=false`, `mail.starttls=false`.
Ba email Core: chao mung khi dang ky, dat lai mat khau, xac nhan don hang sau khi thanh toan (`EmailService`).

## Cach hoan thien phan con lai (theo dung mau da co)

Da co 2 vi du hoan chinh: danh sach san pham (ProductServlet → ProductService → ProductDAO → products.jsp) va Auth
(Register/Login/ForgotPassword/ResetPassword → AuthService → UserDAO → JSP). Voi cac chuc nang con lai, lam dung mau nay:

1. **DAO**: `public class XxxDAO extends AbstractDAO<Xxx, Integer> { public XxxDAO() { super(Xxx.class); } }`
   — da co du CRUD, chi them method rieng neu can query dac thu (xem `ProductDAO.findByCategoryId` lam vi du).
2. **Service**: 1 class goi DAO tuong ung, chua business rule (vd `OrderService.cancelOrder()` phai
   goi ca `OrderDAO.update()` lan `ProductDAO.update()` de hoan kho — xem ghi chu trong `Order.cancelOrder()`).
3. **Servlet**: `@WebServlet("/duong-dan")`, goi Service, `forward` sang JSP tuong ung.

## Ghi chu JPA quan trong (khac voi slide mon hoc)

- Dung `jakarta.persistence.*`, KHONG dung `javax.persistence.*` — bat buoc vi Tomcat 10.1.
- `User` va `Payment` dung `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` —
  Customer/Admin gop chung bang `users`, CODPayment/VNPayPayment gop chung bang `payments`,
  phan biet qua cot discriminator (`user_type`, `payment_type`). Chi dung trong slide neu co
  phan ke thua — neu mon hoc chua day toi InheritanceType thi day la phan can tu doc them
  (tu khoa de tra: "JPA single table inheritance").
- Composition trong class diagram (vd `Order *-- OrderItem`) map thanh `cascade = CascadeType.ALL,
  orphanRemoval = true`. Aggregation (vd `OrderItem o-- Product`) thi KHONG co cascade — xoa
  OrderItem khong duoc dung lam Product bien mat.
