<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Attr tu RegisterServlet khi loi: errors (Map ten_o -> thong bao), fullName/email/phone (gia tri da nhap) --%>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Đăng ký — Nông Việt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen flex flex-col [&>footer]:mt-0">
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<%-- flex-1 + items-center: khung nam GIUA vung giua header va footer, footer luon sat day man hinh --%>
<main class="flex-1 w-full max-w-5xl mx-auto px-4 py-12 flex items-center">
  <div class="w-full card bg-base-100 border border-base-300 overflow-hidden md:grid md:grid-cols-2">
    <div class="hero-bg text-white p-10 hidden md:flex flex-col justify-center">
      <div class="text-7xl" aria-hidden="true"><i data-lucide="sprout" class="size-20"></i></div>
      <h2 class="text-3xl font-extrabold mt-4">Tạo tài khoản miễn phí</h2>
      <ul class="text-white/80 mt-3 space-y-1 text-sm"><li class="flex items-center gap-2"><i data-lucide="check" class="size-4 shrink-0"></i> Theo dõi trạng thái đơn hàng</li><li class="flex items-center gap-2"><i data-lucide="check" class="size-4 shrink-0"></i> Lưu nhiều địa chỉ giao hàng</li><li class="flex items-center gap-2"><i data-lucide="check" class="size-4 shrink-0"></i> Nhận mã giảm giá và thông báo</li></ul>
    </div>
  <form method="post" action="${ctx}/register" class="p-8 space-y-3">
    <h1 class="text-2xl font-bold">Đăng ký</h1>

    <label class="form-control w-full"><div class="label"><span class="label-text">Họ và tên</span></div>
      <input name="fullName" value="<c:out value='${fullName}'/>" maxlength="255" autocomplete="name"
             class="input w-full ${not empty errors.fullName ? 'input-error' : ''}" required>
      <c:if test="${not empty errors.fullName}"><div class="label"><span class="label-text-alt text-error"><c:out value="${errors.fullName}"/></span></div></c:if></label>

    <label class="form-control w-full"><div class="label"><span class="label-text">Email</span></div>
      <input type="email" name="email" value="<c:out value='${email}'/>" maxlength="255" autocomplete="email"
             class="input w-full ${not empty errors.email ? 'input-error' : ''}" required>
      <c:if test="${not empty errors.email}"><div class="label"><span class="label-text-alt text-error"><c:out value="${errors.email}"/></span></div></c:if></label>

    <label class="form-control w-full"><div class="label"><span class="label-text">Số điện thoại</span></div>
      <input type="tel" name="phone" value="<c:out value='${phone}'/>" maxlength="255" autocomplete="tel"
             class="input w-full ${not empty errors.phone ? 'input-error' : ''}" placeholder="09xx xxx xxx">
      <c:if test="${not empty errors.phone}"><div class="label"><span class="label-text-alt text-error"><c:out value="${errors.phone}"/></span></div></c:if></label>

    <label class="form-control w-full"><div class="label"><span class="label-text">Mật khẩu</span></div>
      <input type="password" name="password" autocomplete="new-password"
             class="input w-full ${not empty errors.password ? 'input-error' : ''}" placeholder="Tối thiểu 6 ký tự" required>
      <c:if test="${not empty errors.password}"><div class="label"><span class="label-text-alt text-error"><c:out value="${errors.password}"/></span></div></c:if></label>

    <label class="form-control w-full"><div class="label"><span class="label-text">Nhập lại mật khẩu</span></div>
      <input type="password" name="confirmPassword" autocomplete="new-password"
             class="input w-full ${not empty errors.confirmPassword ? 'input-error' : ''}" required>
      <c:if test="${not empty errors.confirmPassword}"><div class="label"><span class="label-text-alt text-error"><c:out value="${errors.confirmPassword}"/></span></div></c:if></label>

    <button class="btn btn-primary btn-block">Tạo tài khoản</button>
    <div class="text-center text-sm">Đã có tài khoản? <a href="${ctx}/login" class="link link-primary font-medium">Đăng nhập</a></div>
  </form>
  </div>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>