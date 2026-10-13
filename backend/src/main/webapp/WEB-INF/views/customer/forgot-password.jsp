<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Quên mật khẩu — Nông Việt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen flex flex-col [&>footer]:mt-0">
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<%-- flex-1 + items-center: khung nam GIUA vung giua header va footer, footer luon sat day man hinh --%>
<main class="flex-1 w-full max-w-5xl mx-auto px-4 py-12 flex items-center">
  <div class="w-full card bg-base-100 border border-base-300 overflow-hidden md:grid md:grid-cols-2">
    <div class="hero-bg text-white p-10 hidden md:flex flex-col justify-center">
      <div class="text-7xl" aria-hidden="true">🔑</div>
      <h2 class="text-3xl font-extrabold mt-4">Quên mật khẩu?</h2>
      <p class="text-white/80 mt-2">Đừng lo, chỉ cần email đã đăng ký — chúng tôi sẽ gửi liên kết để bạn đặt lại mật khẩu trong vài giây.</p>
    </div>
  <form method="post" action="${ctx}/forgot-password" class="p-8 space-y-4">
    <h1 class="text-2xl font-bold">Quên mật khẩu</h1>
    <p class="text-sm text-base-content/70">Nhập email đã đăng ký, chúng tôi sẽ gửi liên kết để bạn đặt lại mật khẩu.</p>

    <%-- Thong bao nay hien nhu nhau du email co ton tai hay khong (chong do email) --%>
    <c:if test="${param.sent == '1'}">
      <div class="alert alert-success alert-soft text-sm">Nếu email này đã được đăng ký, chúng tôi vừa gửi hướng dẫn đặt lại mật khẩu. Liên kết có hiệu lực 30 phút.</div>
    </c:if>

    <label class="form-control w-full"><div class="label"><span class="label-text">Email</span></div>
      <input type="email" name="email" class="input w-full" placeholder="ban@email.com" required></label>
    <button class="btn btn-primary btn-block">Gửi liên kết đặt lại</button>
    <a href="${ctx}/login" class="btn btn-ghost btn-sm btn-block">← Quay lại đăng nhập</a>
  </form>
  </div>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
