<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Attr: token, invalid (true = link sai/het han), errors (loi tung o), error (thong bao chung) --%>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Đặt lại mật khẩu — NôngViệt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main class="max-w-5xl mx-auto px-4 py-12">
  <div class="card bg-base-100 border border-base-300 overflow-hidden md:grid md:grid-cols-2">
    <div class="hero-bg text-white p-10 hidden md:flex flex-col justify-center">
      <div class="text-7xl" aria-hidden="true">🔒</div>
      <h2 class="text-3xl font-extrabold mt-4">Đặt mật khẩu mới</h2>
      <p class="text-white/80 mt-2">Chọn mật khẩu mạnh, tối thiểu 6 ký tự, để bảo vệ tài khoản của bạn.</p>
    </div>
    <div class="p-8 flex items-center justify-center"><c:choose>
    <c:when test="${invalid}">
      <div class="space-y-4 text-center">
        <div class="text-5xl">⏰</div>
        <h1 class="text-xl font-bold">Liên kết không hợp lệ hoặc đã hết hạn</h1>
        <p class="text-sm text-base-content/70">Liên kết đặt lại mật khẩu chỉ có hiệu lực 30 phút và dùng được một lần.</p>
        <a href="${ctx}/forgot-password" class="btn btn-primary">Gửi lại liên kết mới</a>
      </div>
    </c:when>
    <c:otherwise>
      <form method="post" action="${ctx}/reset-password" class="space-y-4 w-full">
        <h1 class="text-2xl font-bold">Đặt mật khẩu mới</h1>
        <input type="hidden" name="token" value="<c:out value='${token}'/>">
        <label class="form-control w-full"><div class="label"><span class="label-text">Mật khẩu mới</span></div>
          <input type="password" name="password" class="input w-full ${not empty errors.password ? 'input-error' : ''}" placeholder="Tối thiểu 6 ký tự" required>
          <c:if test="${not empty errors.password}"><div class="label"><span class="label-text-alt text-error"><c:out value="${errors.password}"/></span></div></c:if></label>
        <label class="form-control w-full"><div class="label"><span class="label-text">Nhập lại mật khẩu mới</span></div>
          <input type="password" name="confirmPassword" class="input w-full ${not empty errors.confirmPassword ? 'input-error' : ''}" required>
          <c:if test="${not empty errors.confirmPassword}"><div class="label"><span class="label-text-alt text-error"><c:out value="${errors.confirmPassword}"/></span></div></c:if></label>
        <button class="btn btn-primary btn-block">Đổi mật khẩu</button>
      </form>
    </c:otherwise>
    </c:choose></div>
  </div>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
