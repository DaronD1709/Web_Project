<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" isErrorPage="true" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  TRANG LOI CHUNG cho ca khach hang lan Admin. Thay cho trang mac dinh cua Tomcat (trang trang, ghi "HTTP Status 403 – Forbidden",
  co the lo ten/phien ban Tomcat). Duoc khai bao trong web.xml: <error-page> cho 400, 403, 404, 405, 500 va moi Exception chua bat
  (java.lang.Throwable). Khi Servlet goi resp.sendError(403, "...") hoac nem exception, Tomcat tu forward noi bo toi file nay
  va GIU NGUYEN ma trang thai (trinh duyet van nhan 403/404/500 that, chi doi phan noi dung HTML).

  Du lieu lay tu cac attribute chuan Servlet (ten bat dau bang "jakarta.servlet.error."):
    status_code  ma loi (403, 404, 500...)      message  cau mo ta do Servlet truyen vao sendError (neu co)
    request_uri  duong dan nguoi dung da vao
  KHONG in stack trace hay thong bao exception ra man hinh (tranh lo thong tin noi bo); chi ghi trong log cua Tomcat.

  Trang tu dung rieng, KHONG include header/footer cua cua hang, vi loi co the chinh la tu header (vd 500) va de Admin cung dung duoc.
  Nut chinh: Admin dang dang nhap, hoac loi xay ra o khu /admin  -> ve trang quan tri;  con lai -> ve trang chu.
--%>
<c:set var="code" value="${requestScope['jakarta.servlet.error.status_code']}" />
<c:set var="uri" value="${requestScope['jakarta.servlet.error.request_uri']}" />
<c:set var="rawMsg" value="${requestScope['jakarta.servlet.error.message']}" />
<%-- Phan biet theo dung 1 chuoi so 403/404... de chon icon, tieu de, loi giai thich than thien (tieng Viet) --%>
<c:choose>
  <c:when test="${code == 403}">
    <c:set var="icon" value="lock" /><c:set var="title" value="Bạn không có quyền truy cập" />
    <c:set var="desc" value="Trang này dành cho tài khoản khác hoặc bạn cần đăng nhập đúng tài khoản." />
  </c:when>
  <c:when test="${code == 404}">
    <c:set var="icon" value="search-x" /><c:set var="title" value="Không tìm thấy trang" />
    <c:set var="desc" value="Đường dẫn không đúng, hoặc nội dung này đã bị xoá hay không còn tồn tại." />
  </c:when>
  <c:when test="${code == 405}">
    <c:set var="icon" value="ban" /><c:set var="title" value="Thao tác không được hỗ trợ" />
    <c:set var="desc" value="Trang này không nhận kiểu yêu cầu vừa gửi. Hãy quay lại và thử thao tác bằng nút trên trang." />
  </c:when>
  <c:when test="${code == 400}">
    <c:set var="icon" value="circle-alert" /><c:set var="title" value="Yêu cầu không hợp lệ" />
    <c:set var="desc" value="Dữ liệu gửi lên không đúng. Hãy tải lại trang rồi thử lại." />
  </c:when>
  <c:otherwise>
    <c:set var="icon" value="triangle-alert" /><c:set var="title" value="Có lỗi xảy ra" />
    <c:set var="desc" value="Hệ thống gặp sự cố ngoài ý muốn. Bạn thử lại sau ít phút; nếu vẫn lỗi, hãy báo cho cửa hàng." />
  </c:otherwise>
</c:choose>
<%-- Chi hien them cau do chinh code cua ta truyen vao sendError (tieng Viet, vd "Chuc nang nay danh cho quan tri vien") voi loi 400/403.
     Bo qua cau mac dinh cua Tomcat ("Forbidden", "Bad Request") va moi cau bat dau bang "/" (Tomcat de duong dan vao day) vi vo nghia. --%>
<c:set var="lowMsg" value="${fn:toLowerCase(rawMsg)}" />
<c:set var="showMsg" value="${(code == 400 || code == 403) && not empty rawMsg && fn:length(rawMsg) <= 200
                              && not fn:startsWith(rawMsg, '/') && lowMsg ne 'forbidden' && lowMsg ne 'bad request'}" />
<%-- Dang o khu quan tri khong? Dung de chon nut ve dau. currentUser.customer = true neu la khach hang. --%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="inAdminArea" value="${fn:startsWith(uri, ctx.concat('/admin'))}" />
<c:set var="isAdmin" value="${not empty sessionScope.currentUser && not sessionScope.currentUser.customer}" />
<!DOCTYPE html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title><c:out value="${code}" /> — Nông Việt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <meta name="robots" content="noindex">
</head>
<body class="min-h-screen bg-base-200 flex items-center justify-center p-4">
  <main class="card bg-base-100 border border-base-300 shadow-sm w-full max-w-md">
    <div class="card-body items-center text-center gap-3 py-10">
      <%-- Bieu tuong theo loai loi; ma loi in to de nguoi dung bao lai duoc ("loi 404") --%>
      <span class="size-16 rounded-full bg-primary/10 text-primary flex items-center justify-center">
        <i data-lucide="${icon}" class="size-8"></i>
      </span>
      <div class="text-5xl font-extrabold text-primary leading-none"><c:out value="${code}" /></div>
      <h1 class="text-xl font-semibold"><c:out value="${title}" /></h1>
      <p class="text-sm text-base-content/70"><c:out value="${desc}" /></p>
      <%-- Cau chi tiet do code cua ta truyen vao (neu co): c:out ma hoa HTML nen khong the chen the script --%>
      <c:if test="${showMsg}"><p class="text-sm font-medium"><c:out value="${rawMsg}" /></p></c:if>
      <div class="flex flex-wrap justify-center gap-2 mt-3">
        <%-- Quay lai trang truoc; mo thang tab nay (khong co trang truoc) thi ve trang chu --%>
        <button type="button" class="btn btn-ghost" onclick="if (history.length > 1) history.back(); else location.href='${ctx}/';">Quay lại</button>
        <c:choose>
          <c:when test="${isAdmin}"><a class="btn btn-primary" href="${ctx}/admin">Về trang quản trị</a></c:when>
          <c:otherwise>
            <a class="btn btn-primary" href="${ctx}/">Về trang chủ</a>
            <%-- Loi o khu /admin ma khong phai Admin: goi y dang nhap dung trang quan tri --%>
            <c:if test="${inAdminArea}"><a class="btn btn-outline" href="${ctx}/admin/login">Đăng nhập quản trị</a></c:if>
          </c:otherwise>
        </c:choose>
      </div>
    </div>
  </main>
</body>
</html>
