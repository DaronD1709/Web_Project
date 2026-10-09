<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  Trang dang nhap quan tri (CA TRANG, khong qua admin-layout.jsp vi chua co header/sidebar de hien). AdminLoginServlet truyen:
    error : thong bao sai mat khau / khong phai Admin     email : email da go (de dien lai)     next : trang CMS can quay lai sau khi dang nhap
  Chuyen tu mockup/admin/login.html.
--%>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Đăng nhập quản trị — Nông Việt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen">
<div class="min-h-screen grid lg:grid-cols-2">

  <%-- ===== VUNG 1: GIOI THIEU BEN TRAI (chi hien tren man hinh rong) ===== --%>
  <div class="hero-bg text-white hidden lg:flex flex-col justify-center p-14">
    <div class="text-7xl" aria-hidden="true">🌾</div>
    <h1 class="text-4xl font-extrabold mt-4">Nông Việt CMS</h1>
    <p class="text-white/80 mt-3 max-w-md">Quản lý sản phẩm, đơn hàng, voucher và khách hàng của cửa hàng nông nghiệp Nông Việt.</p>
  </div>

  <%-- ===== VUNG 2: FORM DANG NHAP (canh giua) ===== --%>
  <div class="flex items-center justify-center p-6">
    <form method="post" action="${ctx}/admin/login" class="w-full max-w-sm space-y-4">
      <div>
        <div class="lg:hidden text-2xl font-extrabold text-primary mb-4">🌾 Nông Việt CMS</div>
        <h2 class="text-2xl font-bold">Đăng nhập quản trị</h2>
        <p class="text-sm text-base-content/60">Chỉ dành cho tài khoản có vai trò Admin.</p>
      </div>

      <%-- Thong bao loi (chi hien khi servlet gan attribute error); c:out chong XSS --%>
      <c:if test="${not empty error}"><div class="alert alert-error alert-soft text-sm"><c:out value="${error}"/></div></c:if>

      <%-- "next": trang CMS nguoi dung dang muon vao, de dang nhap xong quay lai dung cho do --%>
      <input type="hidden" name="next" value="<c:out value='${next}'/>">

      <label class="form-control w-full"><div class="label"><span class="label-text">Email</span></div>
        <input type="email" name="email" value="<c:out value='${email}'/>" class="input w-full" required autofocus></label>
      <label class="form-control w-full"><div class="label"><span class="label-text">Mật khẩu</span></div>
        <input type="password" name="password" class="input w-full" required></label>
      <button class="btn btn-primary btn-block">Đăng nhập</button>
      <a href="${ctx}/" class="btn btn-ghost btn-sm btn-block">← Về cửa hàng</a>
    </form>
  </div>
</div>
</body>
</html>
