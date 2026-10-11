<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Đặt hàng thành công — Nông Việt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<main class="max-w-2xl mx-auto px-4 py-10">
  <ul class="steps w-full mb-8 text-sm"><li class="step step-primary">Giỏ hàng</li><li class="step step-primary">Thanh toán</li><li class="step step-primary">Hoàn tất</li></ul>
  <div class="card bg-base-100 border border-base-300 p-5 sm:p-8 text-center space-y-4">
    <div class="mx-auto size-20 rounded-full bg-success/20 text-success flex items-center justify-center text-5xl" aria-hidden="true">✓</div>
    <h1 class="text-2xl font-bold">Đặt hàng thành công!</h1>
    <p class="text-base-content/70">Cảm ơn bạn đã mua sắm. Mã đơn hàng của bạn là <b class="font-mono text-primary">#${order.id}</b>.<br>Shop sẽ sớm xác nhận đơn — bạn sẽ nhận thông báo khi trạng thái thay đổi.</p>
    <div class="text-left bg-base-200 rounded-box p-4 text-sm space-y-2">
      <div class="flex flex-wrap justify-between gap-2"><span>Thanh toán</span><b><c:choose><c:when test="${order.payment.method == 'COD'}">COD — thanh toán khi nhận hàng</c:when><c:otherwise><c:out value="${order.payment.method}"/></c:otherwise></c:choose></b></div>
      <div class="flex flex-wrap justify-between gap-2"><span>Giao tới</span><b class="min-w-0 break-words"><c:out value="${order.shippingAddress.street}"/>, <c:out value="${order.shippingAddress.city}"/></b></div>
      <div class="flex justify-between gap-2"><span>Tổng tiền</span><b class="text-primary tabular-nums"><fmt:formatNumber value="${order.totalAmount}" pattern="#,##0"/>₫</b></div>
      <div class="flex justify-between gap-2"><span>Trạng thái</span><span class="badge badge-warning"><c:out value="${order.status.label}"/></span></div>
    </div>
    <div class="flex flex-wrap gap-3 justify-center">
      <a href="${ctx}/orders/detail?id=${order.id}" class="btn btn-primary">Theo dõi đơn hàng</a>
      <a href="${ctx}/" class="btn btn-ghost">Tiếp tục mua sắm</a>
    </div>
  </div>
</main>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
