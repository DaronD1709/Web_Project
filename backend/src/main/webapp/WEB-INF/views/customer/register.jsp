<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Attr tu RegisterServlet khi loi: errors (Map ten_o -> thong bao), fullName/email/phone (gia tri da nhap) --%>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Đăng ký — NôngViệt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main class="max-w-md mx-auto px-4 py-12">
  <form method="post" action="${ctx}/register" class="card bg-base-100 border border-base-300 p-8 space-y-3">
    <h1 class="text-2xl font-bold">Đăng ký</h1>

    <label class="form-control w-full"><div class="label"><span class="label-text">Họ và tên</span></div>
      <input name="fullName" value="<c:out value='${fullName}'/>" class="input w-full ${not empty errors.fullName ? 'input-error' : ''}" required>
      <c:if test="${not empty errors.fullName}"><div class="label"><span class="label-text-alt text-error"><c:out value="${errors.fullName}"/></span></div></c:if></label>

    <label class="form-control w-full"><div class="label"><span class="label-text">Email</span></div>
      <input type="email" name="email" value="<c:out value='${email}'/>" class="input w-full ${not empty errors.email ? 'input-error' : ''}" required>
      <c:if test="${not empty errors.email}"><div class="label"><span class="label-text-alt text-error"><c:out value="${errors.email}"/></span></div></c:if></label>

    <label class="form-control w-full"><div class="label"><span class="label-text">Số điện thoại</span></div>
      <input name="phone" value="<c:out value='${phone}'/>" class="input w-full" placeholder="09xx xxx xxx"></label>

    <label class="form-control w-full"><div class="label"><span class="label-text">Mật khẩu</span></div>
      <input type="password" name="password" class="input w-full ${not empty errors.password ? 'input-error' : ''}" placeholder="Tối thiểu 6 ký tự" required>
      <c:if test="${not empty errors.password}"><div class="label"><span class="label-text-alt text-error"><c:out value="${errors.password}"/></span></div></c:if></label>

    <label class="form-control w-full"><div class="label"><span class="label-text">Nhập lại mật khẩu</span></div>
      <input type="password" name="confirmPassword" class="input w-full ${not empty errors.confirmPassword ? 'input-error' : ''}" required>
      <c:if test="${not empty errors.confirmPassword}"><div class="label"><span class="label-text-alt text-error"><c:out value="${errors.confirmPassword}"/></span></div></c:if></label>

    <button class="btn btn-primary btn-block">Tạo tài khoản</button>
    <div class="text-center text-sm">Đã có tài khoản? <a href="${ctx}/login" class="link link-primary font-medium">Đăng nhập</a></div>
  </form>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
