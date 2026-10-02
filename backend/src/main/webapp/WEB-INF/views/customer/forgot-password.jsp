<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Quên mật khẩu — NôngViệt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main class="max-w-md mx-auto px-4 py-12">
  <form method="post" action="${ctx}/forgot-password" class="card bg-base-100 border border-base-300 p-8 space-y-4">
    <h1 class="text-2xl font-bold">Quên mật khẩu</h1>
    <p class="text-sm text-base-content/70">Nhập email đã đăng ký, chúng tôi sẽ gửi liên kết để bạn đặt lại mật khẩu.</p>

    <%-- Thong bao nay hien nhu nhau du email co ton tai hay khong (chong do email) --%>
    <c:if test="${param.sent == '1'}">
      <div class="alert alert-success alert-soft text-sm">Nếu email này đã được đăng ký, chúng tôi vừa gửi hướng dẫn đặt lại mật khẩu. Liên kết có hiệu lực 30 phút.</div>
    </c:if>

    <label class="form-control w-full"><div class="label"><span class="label-text">Email</span></div>
      <input type="email" name="email" class="input w-full" placeholder="ban@email.com" required></label>
    <button class="btn btn-primary btn-block">Gửi liên kết đặt lại</button>
    <div class="text-center text-sm"><a href="${ctx}/login" class="link link-primary">← Quay lại đăng nhập</a></div>
  </form>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
