<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<fmt:setLocale value="vi_VN" />
<%-- Đơn hàng là dữ liệu riêng: htmx phải hỏi lại server khi Back, không lưu HTML vào localStorage. --%>
<div id="order-history" hx-history="false">
  <div role="tablist" aria-label="Trạng thái đơn hàng" class="tabs tabs-border overflow-x-auto flex-nowrap bg-base-100 rounded-box border border-base-300 px-2 mb-4">
    <c:forTokens items="ALL,PENDING,SHIPPING,COMPLETED,CANCELLED,RETURN_REQUESTED" delims="," var="tab">
      <c:set var="tabLabel" value="${tab == 'ALL' ? 'Tất cả' : tab == 'PENDING' ? 'Chờ xác nhận' : tab == 'SHIPPING' ? 'Đang giao' : tab == 'COMPLETED' ? 'Hoàn tất' : tab == 'CANCELLED' ? 'Đã huỷ' : 'Hoàn hàng'}" />
      <c:set var="tabActive" value="${selectedStatus == tab or (tab == 'PENDING' and selectedStatus == 'CONFIRMED') or (tab == 'RETURN_REQUESTED' and selectedStatus == 'RETURNED')}" />
      <a href="${ctx}/orders?status=${tab}" role="tab" aria-selected="${tabActive}" class="tab whitespace-nowrap ${tabActive ? 'tab-active' : ''}"
         hx-get="${ctx}/orders?status=${tab}" hx-target="#order-history" hx-swap="outerHTML" hx-push-url="true"><c:out value="${tabLabel}"/></a>
    </c:forTokens>
  </div>
  <div class="space-y-4">
    <c:choose>
      <c:when test="${empty orders}">
        <div class="card bg-base-100 border border-dashed border-base-300 p-12 text-center"><div class="text-5xl" aria-hidden="true">📦</div><div class="mt-2 font-semibold">Chưa có đơn hàng nào</div><a href="${ctx}/products" class="link link-primary mt-2">Tiếp tục mua sắm</a></div>
      </c:when>
      <c:otherwise>
        <c:forEach var="order" items="${orders}">
          <article class="card bg-base-100 border border-base-300">
            <div class="flex flex-wrap items-center justify-between gap-2 px-5 py-3 border-b border-base-300 text-sm">
              <div><b class="font-mono">#${order.id}</b> <span class="text-base-content/60">· <c:out value="${order.orderDateText}"/></span></div>
              <%@ include file="/WEB-INF/views/customer/fragments/order-badge.jspf" %>
            </div>
            <div class="px-5 py-3 space-y-2">
              <c:forEach var="item" items="${order.items}">
                <div class="flex items-center gap-3 text-sm">
                  <c:choose><c:when test="${not empty item.product.imageUrl}"><img src="${ctx}/<c:out value='${item.product.imageUrl}'/>" alt="" class="size-12 shrink-0 rounded-field object-cover"></c:when><c:otherwise><div class="ph ph-sm size-12 shrink-0 rounded-field" aria-hidden="true">🌾</div></c:otherwise></c:choose>
                  <div class="flex-1 min-w-0 break-words"><c:out value="${item.product.name}"/></div><div class="text-base-content/60 shrink-0">×${item.quantity}</div>
                </div>
              </c:forEach>
            </div>
            <div class="flex flex-wrap items-center justify-between gap-2 px-5 py-3 border-t border-base-300">
              <div class="text-sm">Tổng tiền: <b class="text-primary text-lg"><fmt:formatNumber value="${order.totalAmount}" pattern="#,##0"/>₫</b></div>
              <%-- Link chi tiết không kế thừa hx-target của vùng danh sách. --%>
              <a href="${ctx}/orders/detail?id=${order.id}" hx-boost="false" class="btn btn-sm btn-primary">Xem chi tiết</a>
            </div>
          </article>
        </c:forEach>
      </c:otherwise>
    </c:choose>
  </div>
  <c:if test="${result.totalPages > 1}">
    <nav aria-label="Phân trang đơn hàng" class="flex flex-wrap items-center justify-center gap-2 mt-6">
      <c:if test="${result.page > 1}"><a class="btn btn-sm btn-outline" href="${ctx}/orders?status=${selectedStatus}&amp;page=${result.page - 1}" hx-get="${ctx}/orders?status=${selectedStatus}&amp;page=${result.page - 1}" hx-target="#order-history" hx-swap="outerHTML" hx-push-url="true">← Trước</a></c:if>
      <span class="text-sm">Trang ${result.page}/${result.totalPages}</span>
      <c:if test="${result.page < result.totalPages}"><a class="btn btn-sm btn-outline" href="${ctx}/orders?status=${selectedStatus}&amp;page=${result.page + 1}" hx-get="${ctx}/orders?status=${selectedStatus}&amp;page=${result.page + 1}" hx-target="#order-history" hx-swap="outerHTML" hx-push-url="true">Sau →</a></c:if>
    </nav>
  </c:if>
</div>
