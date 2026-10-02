# Backend — Java Servlet/JSP + JPA (Hibernate) + PostgreSQL

Maven Web Application. Mo truc tiep thu muc nay bang NetBeans ("Open Project").

## Stack

- Jakarta Servlet 6.0 / JSP (chay tren Tomcat 10.1)
- JPA (Jakarta Persistence 3.1) — Hibernate 6.5 lam provider
- PostgreSQL

## Kien truc 3 lop

```
src/main/java/com/ecommerce/
├── entity/        Model — 22 JPA entity + 4 enum, dung chung cho ca JSP lan service
├── dao/           Data Access — AbstractDAO<T,ID> (CRUD chung) + DAO cu the
├── service/       Business logic — goi DAO, xu ly rule (vd: check voucher con han)
├── controller/    Servlet — nhan request, goi Service, forward sang JSP
│   ├── admin/
│   └── customer/
└── util/
    └── JPAUtil.java   lay EntityManagerFactory (tuong duong DBUtil trong slide)
```

**Luong 1 request:** `Servlet (controller)` → `Service` → `DAO` → `EntityManager` (JPA) → PostgreSQL.
Servlet KHONG duoc goi thang DAO — luon qua Service, de sau nay them validate/business rule
(vd: check Voucher.isValid() truoc khi cho ap dung) ma khong phai sua Servlet.

## Cau hinh ket noi DB

Sua `src/main/resources/META-INF/persistence.xml` — doi `jakarta.persistence.jdbc.user`
va `jakarta.persistence.jdbc.password` theo PostgreSQL cua may ban. Co `hibernate.hbm2ddl.auto=update`
nen KHONG can tu tao bang bang tay — chay app 1 lan, Hibernate tu doc 22 entity va tao bang.

## Cach hoan thien phan con lai (theo dung mau da co)

Moi nhom entity da co it nhat 1 vi du hoan chinh (Product → ProductDAO → ProductService → ProductServlet).
Voi 21 entity con lai, lam dung mau nay:

1. **DAO**: `public class XxxDAO extends AbstractDAO<Xxx, Integer> { public XxxDAO() { super(Xxx.class); } }`
   — da co du CRUD, chi them method rieng neu can query dac thu (xem `ProductDAO.findByCategoryId` lam vi du).
2. **Service**: 1 class goi DAO tuong ung, chua business rule (vd `OrderService.cancelOrder()` phai
   goi ca `OrderDAO.update()` lan `ProductDAO.update()` de hoan kho — xem ghi chu trong `Order.cancelOrder()`).
3. **Servlet**: `@WebServlet("/duong-dan")`, goi Service, `forward` sang JSP tuong ung.

## Ghi chu JPA quan trong (khac voi slide mon hoc)

- Dung `jakarta.persistence.*`, KHONG dung `javax.persistence.*` — bat buoc vi Tomcat 10.1.
- `User` va `Payment` dung `@Inheritance(strategy = InheritanceType.SINGLE_TABLE)` —
  Customer/Admin/AIBot gop chung bang `users`, CODPayment/VNPayPayment gop chung bang `payments`,
  phan biet qua cot discriminator (`user_type`, `payment_type`). Chi dung trong slide neu co
  phan ke thua — neu mon hoc chua day toi InheritanceType thi day la phan can tu doc them
  (tu khoa de tra: "JPA single table inheritance").
- Composition trong class diagram (vd `Order *-- OrderItem`) map thanh `cascade = CascadeType.ALL,
  orphanRemoval = true`. Aggregation (vd `OrderItem o-- Product`) thi KHONG co cascade — xoa
  OrderItem khong duoc dung lam Product bien mat.
