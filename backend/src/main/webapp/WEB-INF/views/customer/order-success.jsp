<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:set var="onlinePayment" value="${order.payment.method == 'VNPay'}" />
<c:set var="resultTitle" value="Đặt hàng thành công!" />
<c:set var="resultMessage" value="Shop sẽ sớm xác nhận đơn — bạn sẽ nhận thông báo khi trạng thái thay đổi." />
<c:set var="resultIcon" value="✓" />
<c:set var="resultClass" value="bg-success/20 text-success" />
<c:choose>
  <c:when test="${order.status == 'CANCELLED'}">
    <c:set var="resultTitle" value="Đơn hàng đã huỷ" />
    <c:set var="resultIcon" value="!" />
    <c:set var="resultClass" value="bg-warning/20 text-warning" />
    <c:choose>
      <c:when test="${onlinePayment and order.payment.status == 'SUCCESS'}"><c:set var="resultMessage" value="VNPay đã ghi nhận thanh toán cho đơn đã huỷ. Vui lòng liên hệ cửa hàng để đối soát/hoàn tiền." /></c:when>
      <c:when test="${onlinePayment and order.payment.status == 'PENDING'}"><c:set var="resultMessage" value="Đơn đã huỷ. Kết quả thanh toán vẫn đang chờ xác nhận; vui lòng liên hệ cửa hàng nếu tài khoản đã bị trừ tiền." /></c:when>
      <c:otherwise><c:set var="resultMessage" value="Đơn hàng đã huỷ. Bạn có thể tiếp tục mua sắm để tạo đơn mới." /></c:otherwise>
    </c:choose>
  </c:when>
  <c:when test="${onlinePayment and order.payment.status == 'SUCCESS'}">
    <c:set var="resultTitle" value="Thanh toán thành công!" />
  </c:when>
  <c:when test="${onlinePayment and order.payment.status == 'FAILED'}">
    <c:set var="resultTitle" value="Thanh toán không thành công" />
    <c:set var="resultMessage" value="Vui lòng kiểm tra trạng thái đơn hoặc liên hệ cửa hàng để được hỗ trợ." />
    <c:set var="resultIcon" value="!" />
    <c:set var="resultClass" value="bg-warning/20 text-warning" />
  </c:when>
  <c:when test="${onlinePayment}">
    <c:set var="resultTitle" value="Đang xác nhận thanh toán" />
    <c:set var="resultMessage" value="Thông tin thanh toán đang được cập nhật. Bạn có thể tải lại trang để xem kết quả; không cần đặt thêm đơn." />
    <c:set var="resultIcon" value="…" />
    <c:set var="resultClass" value="bg-info/20 text-info" />
  </c:when>
</c:choose>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title><c:out value="${resultTitle}"/> — Nông Việt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<main class="max-w-2xl mx-auto px-4 py-10">
  <ul class="steps w-full mb-8 text-sm"><li class="step step-primary">Giỏ hàng</li><li class="step step-primary">Thanh toán</li><li class="step step-primary">Hoàn tất</li></ul>
  <div class="card bg-base-100 border border-base-300 p-5 sm:p-8 text-center space-y-4">
    <div class="mx-auto size-20 rounded-full ${resultClass} flex items-center justify-center text-5xl" aria-hidden="true"><c:out value="${resultIcon}"/></div>
    <h1 class="text-2xl font-bold"><c:out value="${resultTitle}"/></h1>
    <p class="text-base-content/70"><c:if test="${resultIcon == '✓'}">Cảm ơn bạn đã mua sắm. </c:if>Mã đơn hàng của bạn là <b class="font-mono text-primary">#${order.id}</b>.<br><c:out value="${resultMessage}"/></p>
    <c:if test="${onlinePayment and order.payment.status != 'SUCCESS' and not empty paymentError}">
      <div class="alert alert-warning alert-soft text-sm text-left" role="alert"><c:out value="${paymentError}"/></div>
    </c:if>
    <c:if test="${onlinePayment and not empty paymentMessage}">
      <div class="alert alert-info alert-soft text-sm text-left" role="status"><c:out value="${paymentMessage}"/></div>
    </c:if>
    <div class="text-left bg-base-200 rounded-box p-4 text-sm space-y-2">
      <div class="flex flex-wrap justify-between gap-2"><span>Thanh toán</span><b><c:choose><c:when test="${order.payment.method == 'COD'}">COD — thanh toán khi nhận hàng</c:when><c:otherwise><c:out value="${order.payment.method}"/></c:otherwise></c:choose></b></div>
      <c:if test="${onlinePayment}">
        <div class="flex flex-wrap justify-between gap-2"><span>Kết quả thanh toán</span><b><c:choose><c:when test="${order.payment.status == 'SUCCESS'}">Đã thanh toán</c:when><c:when test="${order.payment.status == 'FAILED'}">Không thành công</c:when><c:otherwise>Chờ xác nhận</c:otherwise></c:choose></b></div>
      </c:if>
      <div class="flex flex-wrap justify-between gap-2"><span>Giao tới</span><b class="min-w-0 break-words"><c:out value="${order.shippingAddress.street}"/>, <c:out value="${order.shippingAddress.city}"/></b></div>
      <div class="flex justify-between gap-2"><span>Tổng tiền</span><b class="text-primary tabular-nums"><fmt:formatNumber value="${order.totalAmount}" pattern="#,##0"/>₫</b></div>
      <div class="flex justify-between gap-2"><span>Trạng thái</span><span class="badge badge-warning"><c:out value="${order.status.label}"/></span></div>
    </div>
    <div class="flex flex-wrap gap-3 justify-center">
      <c:if test="${onlinePayment and order.payment.status == 'PENDING'}">
        <a href="${ctx}/checkout/success?orderId=${order.id}" class="btn btn-outline">Tải lại kết quả</a>
      </c:if>
      <c:if test="${onlinePayment and order.payment.status != 'SUCCESS' and vnPayAvailable}">
        <form method="post" action="${ctx}/payment/vnpay/reconcile" hx-boost="false">
          <input type="hidden" name="orderId" value="${order.id}">
          <input type="hidden" name="vnPayReconcileToken" value="<c:out value='${vnPayReconcileToken}'/>">
          <button type="submit" class="btn btn-outline">Kiểm tra thanh toán</button>
        </form>
      </c:if>
      <a href="${ctx}/orders/detail?id=${order.id}" class="btn btn-primary">Theo dõi đơn hàng</a>
      <a href="${ctx}/" class="btn btn-ghost">Tiếp tục mua sắm</a>
    </div>
  </div>
</main>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
