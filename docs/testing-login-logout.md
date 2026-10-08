# Kiểm thử Login/Logout — feat/login-logout

Phạm vi: phần Login/Logout của Registered Customer theo CLAUDE.md mục 9. Kế thừa auth có sẵn trên dev;
không đổi schema, thư viện, Java 17, pom.xml, web.xml, cấu hình NetBeans hoặc Tomcat.
Form lấy từ mockup/login.html: giữ màu sắc, bố cục hai cột, nội dung, nút và ô Ghi nhớ đăng nhập.
Header dùng chung chỉ điều chỉnh khoảng cách và bố cục ở màn hình nhỏ để không tràn ngang.

## Chạy trên NetBeans

1. Mở project backend/. PostgreSQL đang chạy; db.properties local đã điền đúng; không commit file này.
2. Chọn Clean and Build rồi Run với Tomcat 10.1 đã đăng ký. Nếu port 8080 đang được một instance khác dùng,
   dừng instance đó trước khi Run. Không tạo lại DB chỉ để kiểm thử auth.
3. Mở /ecommerce/login. Tài khoản local mẫu xem comment đầu DataSeeder.java.
4. Thực hiện các trường hợp dưới đây. Xem tab Network/Application của DevTools để kiểm tra cookie và redirect.

| Trường hợp | Kết quả mong đợi |
|---|---|
| Email đúng, mật khẩu đúng | POST trả 302 về trang chủ; header hiện tên và nút Đăng xuất |
| Email viết hoa/có khoảng trắng đầu cuối | Vẫn đăng nhập được; không trim hoặc đổi mật khẩu |
| Mật khẩu sai, email không tồn tại, thiếu dữ liệu | Cùng thông báo Email hoặc mật khẩu không đúng; chưa có quyền vào /cart |
| Form lỗi khi tích Ghi nhớ và có next | Giữ email, checkbox và next; không giữ mật khẩu; dữ liệu hiển thị được escape |
| Chưa login mở /cart, rồi login | Quay lại /cart; htmx dùng HX-Redirect tới login |
| next là URL ngoài, //, dấu backslash hoặc ký tự điều khiển | Về trang chủ, không redirect tới URL ngoài |
| Login sau khi đã có session | ID session thay đổi; session cũ không vào /cart được |
| Không tích Ghi nhớ | Cookie session không có Max-Age 7 ngày; dùng timeout mặc định Tomcat |
| Tích Ghi nhớ | Cookie có Max-Age=604800, HttpOnly, SameSite=Lax; session timeout không hoạt động là 7 ngày |
| Đăng xuất | POST /logout trả 302 về /; xoá cookie; session cũ không dùng lại được |
| GET /logout | Trả 405 và không đăng xuất phiên đang có |
| Login Admin sau Customer trong cùng trình duyệt | Session mới; không giữ badge giỏ của Customer; /cart trả 403 |
| UI desktop/mobile | Form giữ thiết kế mockup, không tràn ngang; liên kết Quên mật khẩu/Đăng ký hoạt động |

Ghi nhớ dựa trên session Tomcat: cookie tối đa 7 ngày kể từ lúc login. Nếu server mất session khi restart/redeploy,
người dùng cần login lại. Không lưu mật khẩu trong cookie. Trên HTTPS cookie có Secure.
Các ý nghĩa Max-Age được quy định trong [Cookie API của Servlet 6.0](https://jakarta.ee/specifications/servlet/6.0/apidocs/jakarta.servlet/jakarta/servlet/http/cookie).

## Regression HTTP tự động (tuỳ chọn, không đổi Maven)

Sau khi app đang chạy, từ thư mục gốc repo:

```powershell
python backend/tests/test_login_logout.py
```

Script dùng Python standard library, không cần pip hoặc dependency trong pom.xml. Có 11 bài kiểm thử HTTP thực;
không sửa tài khoản, sản phẩm hoặc giỏ hàng. Mặc định dùng tài khoản dev của DataSeeder và URL localhost:8080/ecommerce.
Có thể truyền AUTH_BASE_URL, AUTH_CUSTOMER_EMAIL, AUTH_CUSTOMER_PASSWORD, AUTH_ADMIN_EMAIL, AUTH_ADMIN_PASSWORD
qua biến môi trường nếu máy dùng URL/tài khoản khác; không ghi mật khẩu thật vào script hoặc commit.

## Kết quả đã xác minh ngày 08/10/2026

- Maven đi kèm NetBeans: clean package thành công, biên dịch target 17 trên JDK 21.
- WAR chạy trên Tomcat 10.1.57 + PostgreSQL 18 local: 11/11 regression HTTP qua.
- Chrome headless: đối chiếu phần form desktop với mockup và kiểm tra bố cục responsive.
- Kiểm thử này dùng runtime Tomcat tạm dưới backend/target (Git bỏ qua), không sửa cấu hình Tomcat gốc.
- Thao tác Clean and Build/Run bằng giao diện NetBeans cần thực hiện theo hướng dẫn ở trên trên máy từng thành viên.
