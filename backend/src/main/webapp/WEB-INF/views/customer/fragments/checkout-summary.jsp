<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<div class="card bg-base-100 border border-base-300 p-5 space-y-3 lg:sticky lg:top-20">
  <h2 class="font-bold">Đơn hàng (${cart.items.size()} sản phẩm)</h2>
  <div class="space-y-2 text-sm">
    <c:forEach var="i" items="${cart.items}">
      <div class="flex items-center gap-2 min-w-0">
        <c:choose>
          <c:when test="${not empty i.product.imageUrl}"><img src="${ctx}/<c:out value='${i.product.imageUrl}'/>" alt="" class="size-10 rounded-field shrink-0 object-cover"></c:when>
          <c:otherwise><div class="ph ph-sm size-10 rounded-field shrink-0" aria-hidden="true">🌾</div></c:otherwise>
        </c:choose>
        <div class="flex-1 min-w-0 truncate" title="<c:out value='${i.product.name}'/>"><c:out value="${i.product.name}"/> <span class="text-base-content/60">×${i.quantity}</span></div>
        <div class="shrink-0 tabular-nums"><fmt:formatNumber value="${i.priceAtAdd * i.quantity}" pattern="#,##0"/>₫</div>
      </div>
    </c:forEach>
  </div>
  <div class="divider my-0"></div>
  <div class="flex justify-between text-sm"><span>Tạm tính</span><span class="tabular-nums"><fmt:formatNumber value="${subtotal}" pattern="#,##0"/>₫</span></div>
  <div class="flex justify-between text-sm"><span>Phí vận chuyển</span><span class="tabular-nums"><c:choose><c:when test="${shippingFee == 0}">Miễn phí</c:when><c:otherwise><fmt:formatNumber value="${shippingFee}" pattern="#,##0"/>₫</c:otherwise></c:choose></span></div>
  <c:if test="${not empty appliedVoucherCode}">
    <div class="flex justify-between gap-2 text-sm text-success"><span class="min-w-0 break-words">Giảm giá (<c:out value="${appliedVoucherCode}"/>)</span><span class="shrink-0 tabular-nums">−<fmt:formatNumber value="${discountAmount}" pattern="#,##0"/>₫</span></div>
  </c:if>
  <div class="divider my-0"></div>
  <div class="flex justify-between font-bold text-lg gap-2"><span>Tổng thanh toán</span><span id="checkout-total" class="text-primary tabular-nums"><fmt:formatNumber value="${total}" pattern="#,##0"/>₫</span></div>
  <input id="applied-voucher" type="hidden" name="voucherCode" form="checkout-form" value="<c:out value='${appliedVoucherCode}'/>">
  <button id="place-order" type="submit" form="checkout-form" class="btn btn-primary btn-block btn-lg" data-unavailable="${empty addresses or empty cart.items}" ${empty addresses or empty cart.items ? 'disabled' : ''}>Đặt hàng</button>
  <p class="text-xs text-base-content/60 text-center">Bằng việc đặt hàng, bạn đồng ý với điều khoản của Nông Việt.</p>
</div>
