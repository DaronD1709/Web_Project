<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Thanh toán — Nông Việt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <script src="${ctx}/static/js/checkout.js?v=${applicationScope.assetVersion}" defer></script>
</head>
<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<main class="max-w-6xl mx-auto px-4 py-6">
  <ul class="steps w-full mb-6 text-sm">
    <li class="step step-primary"><a href="${ctx}/cart">Giỏ hàng</a></li>
    <li class="step step-primary">Thanh toán</li><li class="step">Hoàn tất</li>
  </ul>
  <c:if test="${not empty error}"><div class="alert alert-error alert-soft text-sm mb-4" role="alert"><c:out value="${error}"/></div></c:if>

  <%-- Form riêng để áp dụng voucher không kích hoạt validate địa chỉ của form đặt hàng. --%>
  <form id="voucher-preview" method="post" action="${ctx}/checkout/voucher"
        hx-post="${ctx}/checkout/voucher" hx-target="#voucher-result" hx-swap="innerHTML"
        hx-disabled-elt="#apply-voucher"></form>

  <form id="checkout-form" method="post" action="${ctx}/checkout" hx-boost="false"
        class="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_24rem] gap-6">
    <input type="hidden" name="checkoutToken" value="<c:out value='${checkoutToken}'/>">
    <div class="space-y-5 min-w-0">
      <section class="card bg-base-100 border border-base-300 p-5">
        <div class="flex flex-wrap items-center justify-between gap-2 mb-3">
          <h2 class="font-bold">1. Địa chỉ giao hàng</h2>
          <a href="${ctx}/account/addresses" class="link link-primary text-sm">+ Thêm / sửa địa chỉ</a>
        </div>
        <c:choose>
          <c:when test="${empty addresses}"><p class="text-sm text-base-content/70">Bạn chưa có địa chỉ giao hàng.</p></c:when>
          <c:otherwise>
            <div class="grid sm:grid-cols-2 gap-3">
              <c:forEach var="a" items="${addresses}">
                <label data-address-option class="border ${selectedAddressId == a.id ? 'border-primary bg-primary/5' : 'border-base-300'} rounded-box p-4 cursor-pointer flex gap-3 min-w-0">
                  <input type="radio" name="addressId" value="${a.id}" class="radio radio-primary radio-sm mt-1 shrink-0" ${selectedAddressId == a.id ? 'checked' : ''} required>
                  <div class="text-sm min-w-0 break-words">
                    <div class="font-semibold"><c:out value="${a.recipientName}"/> <c:if test="${a.isDefault()}"><span class="badge badge-primary badge-sm">Mặc định</span></c:if></div>
                    <div class="text-base-content/70"><c:out value="${a.phone}"/></div>
                    <div class="text-base-content/70"><c:out value="${a.street}"/>, <c:out value="${a.city}"/></div>
                  </div>
                </label>
              </c:forEach>
            </div>
          </c:otherwise>
        </c:choose>
      </section>

      <section class="card bg-base-100 border border-base-300 p-5">
        <h2 class="font-bold mb-3">2. Phương thức thanh toán</h2>
        <div class="space-y-2">
          <label class="border border-primary bg-primary/5 rounded-box p-4 cursor-pointer flex items-center gap-3">
            <input type="radio" name="paymentMethod" value="COD" class="radio radio-primary radio-sm shrink-0" checked required>
            <span class="text-2xl" aria-hidden="true">💵</span><div class="text-sm"><div class="font-semibold">Thanh toán khi nhận hàng (COD)</div><div class="text-base-content/60">Trả tiền mặt cho shipper</div></div>
          </label>
          <label class="border border-base-300 rounded-box p-4 flex items-center gap-3 opacity-60" title="Chưa hỗ trợ thanh toán VNPay">
            <input type="radio" name="paymentMethod" value="VNPAY" class="radio radio-primary radio-sm shrink-0" disabled>
            <span class="text-2xl" aria-hidden="true">💳</span><div class="text-sm"><div class="font-semibold">VNPay (sandbox)</div><div class="text-base-content/60">Thẻ ATM / QR / Internet Banking</div></div>
          </label>
        </div>
      </section>

      <section class="card bg-base-100 border border-base-300 p-5">
        <h2 class="font-bold mb-3">3. Mã giảm giá</h2>
        <div class="join w-full">
          <input id="voucher-code" name="code" form="voucher-preview" class="input join-item w-full min-w-0 font-mono" value="<c:out value='${voucherCode}'/>" placeholder="Nhập mã giảm giá" maxlength="20" aria-label="Mã giảm giá">
          <button id="apply-voucher" type="submit" form="voucher-preview" class="btn btn-primary join-item">Áp dụng</button>
        </div>
        <div id="voucher-result" class="mt-3 space-y-2" aria-live="polite">
          <jsp:include page="/WEB-INF/views/customer/fragments/checkout-voucher-message.jsp" />
        </div>
      </section>
    </div>

    <aside id="checkout-summary" class="min-w-0">
      <jsp:include page="/WEB-INF/views/customer/fragments/checkout-summary.jsp" />
    </aside>
  </form>
</main>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
