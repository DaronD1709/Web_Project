<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%-- Noi dung gio hang (bang + tom tat). Attr: cart, subtotal, shippingFee, total. Duoc include o cart.jsp va tra rieng cho htmx. --%>
<h1 class="text-2xl font-bold mb-4">Giỏ hàng <span class="text-base font-normal text-base-content/60">(${cart.items.size()} sản phẩm)</span></h1>
<c:choose>
  <c:when test="${empty cart.items}">
    <div class="card bg-base-100 border border-dashed border-base-300 p-14 text-center">
      <div class="text-6xl" aria-hidden="true">🛒</div>
      <div class="font-semibold mt-2">Giỏ hàng của bạn đang trống</div>
      <a href="${ctx}/products" class="btn btn-primary btn-sm mt-3 mx-auto">Khám phá sản phẩm</a>
    </div>
  </c:when>
  <c:otherwise>
    <div class="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_22rem] gap-6">
      <div class="card bg-base-100 border border-base-300 overflow-x-auto min-w-0">
        <table class="table">
          <thead><tr><th>Sản phẩm</th><th>Đơn giá</th><th class="text-center">Số lượng</th><th class="text-right">Thành tiền</th><th></th></tr></thead>
          <tbody>
            <c:forEach var="i" items="${cart.items}">
              <tr>
                <td>
                  <div class="flex items-center gap-3">
                    <c:choose>
                      <c:when test="${not empty i.product.imageUrl}">
                        <img src="${ctx}/<c:out value='${i.product.imageUrl}'/>" alt="<c:out value='${i.product.name}'/>" class="size-16 rounded-field shrink-0 object-cover">
                      </c:when>
                      <c:otherwise><div class="ph ph-sm size-16 rounded-field shrink-0" aria-hidden="true">🌾</div></c:otherwise>
                    </c:choose>
                    <div>
                      <div class="font-medium"><c:out value="${i.product.name}"/></div>
                      <div class="text-xs text-base-content/60"><c:out value="${i.product.category.name}"/>
                        <c:if test="${i.product.stockQuantity <= 5}"> · <span class="text-warning">Chỉ còn ${i.product.stockQuantity}</span></c:if></div>
                    </div>
                  </div>
                </td>
                <td class="tabular-nums"><fmt:formatNumber value="${i.priceAtAdd}" pattern="#,##0"/>₫</td>
                <td>
                  <%-- Form POST chay duoc khi thieu htmx; htmx chi thay noi dung gio. --%>
                  <form method="post" action="${ctx}/cart" class="join flex justify-center"
                        hx-post="${ctx}/cart" hx-target="#cart-content" hx-swap="innerHTML" hx-disabled-elt="#cart-content button">
                    <input type="hidden" name="action" value="update">
                    <input type="hidden" name="itemId" value="${i.id}">
                    <button type="submit" name="qty" value="${i.quantity - 1}" class="join-item btn btn-xs" ${i.quantity <= 1 ? 'disabled' : ''} aria-label="Giảm số lượng">−</button>
                    <span class="join-item btn btn-xs pointer-events-none w-10">${i.quantity}</span>
                    <button type="submit" name="qty" value="${i.quantity + 1}" class="join-item btn btn-xs" ${i.quantity >= i.product.stockQuantity ? 'disabled' : ''} aria-label="Tăng số lượng">+</button>
                  </form>
                </td>
                <td class="text-right font-semibold tabular-nums"><fmt:formatNumber value="${i.priceAtAdd * i.quantity}" pattern="#,##0"/>₫</td>
                <td>
                  <form method="post" action="${ctx}/cart" hx-post="${ctx}/cart" hx-target="#cart-content" hx-swap="innerHTML" hx-disabled-elt="#cart-content button">
                    <input type="hidden" name="action" value="remove">
                    <input type="hidden" name="itemId" value="${i.id}">
                    <button type="submit" class="btn btn-ghost btn-xs text-error">Xoá</button>
                  </form>
                </td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </div>

      <aside class="min-w-0">
        <div class="card bg-base-100 border border-base-300 p-5 space-y-3 lg:sticky lg:top-20">
          <h2 class="font-bold">Tóm tắt đơn hàng</h2>
          <div class="flex justify-between text-sm"><span>Tạm tính</span><span class="tabular-nums"><fmt:formatNumber value="${subtotal}" pattern="#,##0"/>₫</span></div>
          <div class="flex justify-between text-sm"><span>Phí vận chuyển</span>
            <span class="tabular-nums"><c:choose><c:when test="${shippingFee == 0}">Miễn phí</c:when><c:otherwise><fmt:formatNumber value="${shippingFee}" pattern="#,##0"/>₫</c:otherwise></c:choose></span></div>
          <c:if test="${shippingFee > 0}">
            <div class="text-xs text-base-content/60">Mua thêm <fmt:formatNumber value="${500000 - subtotal}" pattern="#,##0"/>₫ để được miễn phí vận chuyển.</div>
          </c:if>
          <div class="text-xs text-base-content/60">Mã giảm giá nhập ở bước thanh toán.</div>
          <div class="divider my-0"></div>
          <div class="flex justify-between font-bold text-lg"><span>Tổng cộng</span><span class="text-primary tabular-nums"><fmt:formatNumber value="${total}" pattern="#,##0"/>₫</span></div>
          <%-- TODO (feat/place-order-pay): doi thanh <a href="${ctx}/checkout"> khi co CheckoutServlet --%>
          <button class="btn btn-primary btn-block" disabled>Tiến hành thanh toán</button>
          <a href="${ctx}/products" class="btn btn-ghost btn-sm btn-block">← Tiếp tục mua sắm</a>
        </div>
      </aside>
    </div>
  </c:otherwise>
</c:choose>
