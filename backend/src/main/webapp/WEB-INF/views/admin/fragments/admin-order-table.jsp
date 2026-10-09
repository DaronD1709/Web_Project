<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%--
  MANH: tab trang thai + bang don hang + phan trang. Nam trong <div id="order-list"> cua admin-order-list.jsp; khi loc/doi tab/doi trang
  AdminOrderServlet chi tra rieng file nay de htmx thay noi dung #order-list. Attribute: filter, result, tabs, tabCounts, itemCounts.
--%>

<%-- Tab dang chon gui kem moi lan loc: the hidden nay thuoc form#order-filter (thuoc tinh form=) du nam ngoai the <form> --%>
<input type="hidden" name="status" value="${filter.status}" form="order-filter">

<%-- ===== TAB TRANG THAI: bam tab -> GET kem toan bo o loc (hx-include), rieng "status" bi ghi de bang gia tri cua tab (hx-vals) ===== --%>
<div role="tablist" class="tabs tabs-border bg-base-100 rounded-box border border-base-300 px-2 overflow-x-auto">
  <c:forEach var="t" items="${tabs}">
    <button type="button" role="tab" class="tab gap-2 ${filter.status == t[0] ? 'tab-active' : ''}"
            hx-get="${ctx}/admin/orders" hx-include="#order-filter" hx-vals='{"status":"${t[0]}"}' hx-target="#order-list" hx-push-url="true">
      <c:out value="${t[1]}"/>
      <span class="badge badge-sm ${t[0] == 'return' and tabCounts['return_requested'] > 0 ? 'badge-error' : 'badge-ghost'}">${empty tabCounts[t[0]] ? 0 : tabCounts[t[0]]}</span>
    </button>
  </c:forEach>
</div>

<div class="card bg-base-100 border border-base-300 overflow-hidden">
  <c:choose>
    <%-- ===== Khong co don nao khop ===== --%>
    <c:when test="${empty result.items}">
      <div class="p-12 text-center text-base-content/60"><div class="text-4xl">📭</div>Không có đơn hàng phù hợp.</div>
    </c:when>
    <c:otherwise>
      <%-- ===== BANG: moi don 1 dong ===== --%>
      <div class="overflow-x-auto">
        <table class="table">
          <thead><tr><th>Mã đơn</th><th>Khách hàng</th><th class="text-right">Tổng tiền</th><th>Thanh toán</th><th>Trạng thái</th><th class="text-right">Thao tác</th></tr></thead>
          <tbody>
            <c:forEach var="o" items="${result.items}">
              <%-- Don dang cho duyet hoan hang duoc to nen do nhat de Admin de thay --%>
              <tr class="hover ${o.status == 'RETURN_REQUESTED' ? 'bg-error/5' : ''}">
                <td>
                  <a class="link link-primary font-mono font-medium" href="${ctx}/admin/orders/detail?id=${o.id}">#${o.id}</a>
                  <div class="text-xs text-base-content/50">${o.orderDateText} · ${empty itemCounts[o.id] ? 0 : itemCounts[o.id]} mặt hàng</div>
                </td>
                <td><c:out value="${o.customer.fullName}"/><div class="text-xs text-base-content/50"><c:out value="${o.customer.email}"/></div></td>
                <td class="text-right tabular-nums font-medium"><fmt:formatNumber value="${o.totalAmount}" pattern="#,##0"/>₫</td>
                <td>
                  <span class="badge badge-outline badge-sm"><c:out value="${o.payment.method}"/></span>
                  <span class="text-xs whitespace-nowrap ${o.payment.status == 'SUCCESS' ? 'text-success' : 'text-base-content/50'}">${o.payment.status == 'SUCCESS' ? '✓ Đã thanh toán' : 'Chưa thanh toán'}</span>
                </td>
                <td><c:set var="st" value="${o.status}" /><%@ include file="/WEB-INF/views/admin/common/admin-order-badge.jspf" %></td>

                <%-- Cot thao tac. Moi nut la 1 form nho POST /admin/orders/status (hx-boost gui bang htmx, hx-confirm hoi truoc khi gui);
                     server doi trang thai roi redirect ve "back" = dung bo loc va trang hien tai, kem toast. --%>
                <td class="text-right whitespace-nowrap">
                  <c:set var="back" value="/admin/orders?${filter.toQueryString()}&page=${result.page}" />
                  <c:choose>
                    <c:when test="${o.status == 'PENDING'}"><c:set var="nextStatus" value="CONFIRMED" /><c:set var="nextLabel" value="Xác nhận" /></c:when>
                    <c:when test="${o.status == 'CONFIRMED'}"><c:set var="nextStatus" value="SHIPPING" /><c:set var="nextLabel" value="Giao hàng" /></c:when>
                    <c:when test="${o.status == 'SHIPPING'}"><c:set var="nextStatus" value="COMPLETED" /><c:set var="nextLabel" value="Hoàn tất" /></c:when>
                    <c:otherwise><c:set var="nextStatus" value="" /></c:otherwise>
                  </c:choose>
                  <c:if test="${not empty nextStatus}">
                    <form method="post" action="${ctx}/admin/orders/status" class="inline"
                          hx-confirm="${nextLabel} đơn #${o.id}? Khách hàng sẽ nhận thông báo.">
                      <input type="hidden" name="id" value="${o.id}"><input type="hidden" name="status" value="${nextStatus}">
                      <input type="hidden" name="back" value="<c:out value='${back}'/>">
                      <button class="btn btn-primary btn-xs">${nextLabel}</button>
                    </form>
                  </c:if>
                  <c:if test="${o.status == 'RETURN_REQUESTED'}"><a class="btn btn-error btn-xs" href="${ctx}/admin/orders/detail?id=${o.id}">Duyệt hoàn hàng</a></c:if>
                  <a class="btn btn-ghost btn-xs" href="${ctx}/admin/orders/detail?id=${o.id}">Chi tiết</a>
                </td>
              </tr>
            </c:forEach>
          </tbody>
        </table>
      </div>
    </c:otherwise>
  </c:choose>

  <%-- ===== CHAN BANG: dong tong so + phan trang ===== --%>
  <div class="flex flex-wrap items-center justify-between gap-2 p-3 border-t border-base-300 text-sm">
    <span class="text-base-content/60">
      <c:choose>
        <c:when test="${result.totalCount == 0}">0 đơn hàng</c:when>
        <c:otherwise>Hiển thị ${(result.page - 1) * filter.pageSize + 1}–${result.page * filter.pageSize < result.totalCount ? result.page * filter.pageSize : result.totalCount} / ${result.totalCount} đơn hàng</c:otherwise>
      </c:choose>
    </span>
    <c:if test="${result.totalPages > 1}">
      <%-- Link phan trang la link thuong; nam trong khung hx-boost nen htmx tu bien thanh request ngam, hx-target doi vung can thay thanh #order-list --%>
      <div class="join">
        <c:forEach begin="1" end="${result.totalPages}" var="i">
          <a href="${ctx}/admin/orders?${filter.toQueryString()}&page=${i}" hx-target="#order-list"
             class="join-item btn btn-sm ${i == result.page ? 'btn-primary' : ''}">${i}</a>
        </c:forEach>
      </div>
    </c:if>
  </div>
</div>
