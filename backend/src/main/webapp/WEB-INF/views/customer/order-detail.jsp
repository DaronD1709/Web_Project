<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:set var="st" value="${order.status}" />
<c:set var="stepNo" value="${st == 'PENDING' ? 1 : st == 'CONFIRMED' ? 2 : st == 'SHIPPING' ? 3 : 4}" />
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Đơn hàng #${order.id} — Nông Việt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<main class="max-w-7xl mx-auto px-4 py-6 grid grid-cols-1 lg:grid-cols-[15rem_minmax(0,1fr)] gap-6">
  <aside class="min-w-0"><%@ include file="/WEB-INF/views/common/account-nav.jspf" %></aside>
  <section class="space-y-5 min-w-0">
    <div class="flex flex-wrap items-center justify-between gap-2">
      <div><a href="${ctx}/orders" class="text-sm link link-primary">← Đơn hàng của tôi</a><h1 class="text-2xl font-bold">Đơn hàng <span class="font-mono text-primary">#${order.id}</span></h1><div class="text-sm text-base-content/60">Đặt lúc <c:out value="${order.orderDateText}"/></div></div>
      <%@ include file="/WEB-INF/views/customer/fragments/order-badge.jspf" %>
    </div>
    <div class="card bg-base-100 border border-base-300 p-5 overflow-x-auto">
      <ul class="steps w-full text-sm">
        <c:choose><c:when test="${st == 'CANCELLED'}"><li class="step step-primary">Đặt hàng</li><li class="step step-error" data-content="✕">Đã huỷ</li></c:when><c:otherwise><li class="step step-primary">Đặt hàng</li><li class="step ${stepNo >= 2 ? 'step-primary' : ''}">Xác nhận</li><li class="step ${stepNo >= 3 ? 'step-primary' : ''}">Đang giao</li><li class="step ${stepNo >= 4 ? 'step-primary' : ''}">Hoàn tất</li></c:otherwise></c:choose>
      </ul>
      <c:if test="${st == 'RETURN_REQUESTED' or st == 'RETURNED'}"><div class="alert alert-error alert-soft mt-4 text-sm"><div><c:out value="${st.label}"/><c:if test="${not empty order.returnReason}"><br>Lý do: <c:out value="${order.returnReason}"/></c:if></div></div></c:if>
      <c:if test="${st == 'CANCELLED' and order.payment.method == 'VNPay' and order.payment.status == 'SUCCESS'}"><div class="alert alert-warning alert-soft mt-4 text-sm">VNPay đã ghi nhận thanh toán cho đơn đã huỷ. Vui lòng liên hệ cửa hàng để đối soát/hoàn tiền.</div></c:if>
    </div>
    <div class="grid md:grid-cols-2 gap-5">
      <div class="card bg-base-100 border border-base-300 p-5 text-sm space-y-1 min-w-0 break-words"><h2 class="font-bold mb-1">Địa chỉ nhận hàng</h2><c:choose><c:when test="${not empty order.shippingAddress}"><div class="font-semibold"><c:out value="${order.shippingAddress.recipientName}"/></div><div><c:out value="${order.shippingAddress.phone}"/></div><div class="text-base-content/70"><c:out value="${order.shippingAddress.street}"/>, <c:out value="${order.shippingAddress.city}"/></div></c:when><c:otherwise><p>Chưa có thông tin địa chỉ.</p></c:otherwise></c:choose></div>
      <div class="card bg-base-100 border border-base-300 p-5 text-sm space-y-1 min-w-0"><h2 class="font-bold mb-1">Thanh toán</h2><div>Phương thức: <b><c:out value="${order.payment.method}" default="Chưa có thông tin"/></b></div><div>Trạng thái: <span class="badge badge-sm ${order.payment.status == 'SUCCESS' ? 'badge-success' : 'badge-warning'}"><c:choose><c:when test="${order.payment.status == 'SUCCESS'}">Đã thanh toán</c:when><c:when test="${order.payment.status == 'FAILED'}">Không thành công</c:when><c:when test="${order.payment.method == 'COD'}">Chưa thanh toán</c:when><c:otherwise>Chờ xác nhận</c:otherwise></c:choose></span></div></div>
    </div>
    <div class="card bg-base-100 border border-base-300 overflow-hidden">
      <div class="overflow-x-auto"><table class="table"><thead><tr><th>Sản phẩm</th><th>Đơn giá</th><th class="text-center">SL</th><th class="text-right">Thành tiền</th></tr></thead><tbody><c:forEach var="item" items="${order.items}"><tr><td><div class="font-medium min-w-40 break-words"><c:out value="${item.product.name}"/></div></td><td class="tabular-nums whitespace-nowrap"><fmt:formatNumber value="${item.priceAtOrder}" pattern="#,##0"/>₫</td><td class="text-center">${item.quantity}</td><td class="text-right tabular-nums whitespace-nowrap"><fmt:formatNumber value="${item.priceAtOrder * item.quantity}" pattern="#,##0"/>₫</td></tr></c:forEach></tbody></table></div>
      <div class="p-5 border-t border-base-300 ml-auto w-full sm:w-80 text-sm space-y-1">
        <div class="flex justify-between gap-2"><span>Tạm tính</span><span><fmt:formatNumber value="${order.calculateTotal()}" pattern="#,##0"/>₫</span></div>
        <div class="flex justify-between gap-2"><span>Phí vận chuyển</span><span><c:choose><c:when test="${order.shippingFee == 0}">Miễn phí</c:when><c:otherwise><fmt:formatNumber value="${order.shippingFee}" pattern="#,##0"/>₫</c:otherwise></c:choose></span></div>
        <c:if test="${order.discountAmount > 0}"><div class="flex justify-between gap-2 text-success"><span class="min-w-0 break-words">Voucher <c:out value="${order.voucher.code}"/></span><span class="shrink-0">−<fmt:formatNumber value="${order.discountAmount}" pattern="#,##0"/>₫</span></div></c:if>
        <div class="flex justify-between gap-2 font-bold text-lg pt-1"><span>Tổng cộng</span><span class="text-primary"><fmt:formatNumber value="${order.totalAmount}" pattern="#,##0"/>₫</span></div>
      </div>
    </div>
    <div class="flex flex-wrap gap-2 justify-end"><a href="${ctx}/checkout/success?orderId=${order.id}" class="btn btn-outline">Xem kết quả đặt hàng</a><a href="${ctx}/chat" class="btn btn-ghost">Chat với shop</a></div>
  </section>
</main>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
