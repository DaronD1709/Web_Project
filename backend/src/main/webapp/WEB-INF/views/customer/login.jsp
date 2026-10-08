<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Attr: error (thong bao sai mat khau), email (gia tri da nhap), next (trang can quay lai sau khi dang nhap).
     Query: ?registered=1 (vua dang ky), ?reset=1 (vua dat lai mat khau) --%>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Đăng nhập — NôngViệt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main class="max-w-5xl mx-auto px-4 py-12">
  <div class="card bg-base-100 border border-base-300 overflow-hidden md:grid md:grid-cols-2">
    <div class="hero-bg text-white p-10 hidden md:flex flex-col justify-center">
      <div class="text-7xl" aria-hidden="true">🌾</div>
      <h2 class="text-3xl font-extrabold mt-4">Chào mừng trở lại!</h2>
      <p class="text-white/80 mt-2">Đăng nhập để theo dõi đơn hàng, lưu địa chỉ giao hàng và chat với shop.</p>
    </div>
  <form method="post" action="${ctx}/login" class="p-8 space-y-4">
    <h1 class="text-2xl font-bold">Đăng nhập</h1>

    <c:if test="${param.registered == '1'}"><div class="alert alert-success alert-soft text-sm">Đăng ký thành công! Hãy đăng nhập. Chúng tôi đã gửi email chào mừng tới hộp thư của bạn.</div></c:if>
    <c:if test="${param.reset == '1'}"><div class="alert alert-success alert-soft text-sm">Đã đặt lại mật khẩu. Hãy đăng nhập bằng mật khẩu mới.</div></c:if>
    <c:if test="${not empty error}"><div class="alert alert-error alert-soft text-sm"><c:out value="${error}"/></div></c:if>

    <input type="hidden" name="next" value="<c:out value='${next}'/>">
    <label class="form-control w-full"><div class="label"><span class="label-text">Email</span></div>
      <input type="email" name="email" value="<c:out value='${email}'/>" class="input w-full" placeholder="ban@email.com" autocomplete="username" required></label>
    <label class="form-control w-full"><div class="label"><span class="label-text">Mật khẩu</span></div>
      <input type="password" name="password" class="input w-full" placeholder="••••••••" autocomplete="current-password" required></label>
    <div class="flex items-center justify-between text-sm">
      <label class="flex items-center gap-2"><input type="checkbox" name="remember" class="checkbox checkbox-sm checkbox-primary" ${remember ? 'checked' : ''}> Ghi nhớ đăng nhập</label>
      <a href="${ctx}/forgot-password" class="link link-primary">Quên mật khẩu?</a>
    </div>

    <button class="btn btn-primary btn-block">Đăng nhập</button>
    <div class="text-center text-sm">Chưa có tài khoản? <a href="${ctx}/register" class="link link-primary font-medium">Đăng ký</a></div>
  </form>
  </div>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
