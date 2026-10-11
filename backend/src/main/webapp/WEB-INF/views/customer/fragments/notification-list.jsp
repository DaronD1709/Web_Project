<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<div id="notification-list" hx-history="false" hx-get="${ctx}/notifications?filter=${selectedFilter}&amp;page=${result.page}"
     hx-trigger="every 30s" hx-target="this" hx-swap="outerHTML" hx-sync="this:drop">
  <div class="flex flex-wrap items-center justify-between gap-2 mb-4">
    <h1 class="text-2xl font-bold">Thông báo</h1>
    <form method="post" action="${ctx}/notifications/read" hx-post="${ctx}/notifications/read"
          hx-target="#notification-list" hx-swap="outerHTML" hx-sync="#notification-list:replace" hx-disabled-elt="#notification-list button">
      <input type="hidden" name="all" value="true"><input type="hidden" name="filter" value="${selectedFilter}"><input type="hidden" name="page" value="${result.page}">
      <input type="hidden" name="trackingCsrfToken" value="<c:out value='${trackingCsrfToken}'/>">
      <button class="btn btn-ghost btn-sm" ${unreadCount == 0 ? 'disabled' : ''}>Đánh dấu tất cả đã đọc</button>
    </form>
  </div>
  <div class="tabs tabs-border mb-4" role="tablist" aria-label="Lọc thông báo">
    <c:forTokens items="ALL,UNREAD" delims="," var="filter">
      <a class="tab ${selectedFilter == filter ? 'tab-active' : ''}" role="tab" aria-selected="${selectedFilter == filter}"
         href="${ctx}/notifications?filter=${filter}" hx-get="${ctx}/notifications?filter=${filter}" hx-target="#notification-list"
         hx-swap="outerHTML" hx-push-url="true" hx-sync="#notification-list:replace">${filter == 'ALL' ? 'Tất cả' : 'Chưa đọc'}</a>
    </c:forTokens>
  </div>
  <ul class="card bg-base-100 border border-base-300 divide-y divide-base-300">
    <c:if test="${empty result.items}"><li class="p-8 text-center text-base-content/60">Chưa có thông báo.</li></c:if>
    <c:forEach var="notice" items="${result.items}">
      <li class="flex gap-4 p-4 ${notice.read ? '' : 'bg-primary/5'}" data-notification-id="${notice.id}">
        <div class="text-2xl shrink-0" aria-hidden="true">🔔</div>
        <div class="flex-1 min-w-0"><div class="break-words ${notice.read ? '' : 'font-semibold'}"><c:out value="${notice.message}"/></div>
          <div class="text-xs text-base-content/50 mt-1"><c:out value="${notice.createdAtText}"/></div>
          <div class="flex flex-wrap items-center gap-3 mt-2 text-xs">
            <a href="${ctx}/orders" hx-boost="false" class="link link-primary">Xem đơn hàng của tôi</a>
            <c:choose><c:when test="${notice.read}"><span class="text-base-content/50">Đã đọc</span></c:when><c:otherwise>
              <form method="post" action="${ctx}/notifications/read" hx-post="${ctx}/notifications/read" hx-target="#notification-list"
                    hx-swap="outerHTML" hx-sync="#notification-list:replace" hx-disabled-elt="#notification-list button">
                <input type="hidden" name="id" value="${notice.id}"><input type="hidden" name="filter" value="${selectedFilter}"><input type="hidden" name="page" value="${result.page}">
                <input type="hidden" name="trackingCsrfToken" value="<c:out value='${trackingCsrfToken}'/>">
                <button class="link link-primary">Đánh dấu đã đọc</button>
              </form>
            </c:otherwise></c:choose>
          </div>
        </div>
        <c:if test="${not notice.read}"><span class="size-2.5 rounded-full bg-primary mt-2 shrink-0" aria-label="Chưa đọc"></span></c:if>
      </li>
    </c:forEach>
  </ul>
  <c:if test="${result.totalPages > 1}">
    <nav aria-label="Phân trang thông báo" class="flex flex-wrap items-center justify-center gap-2 mt-6">
      <c:if test="${result.page > 1}"><a class="btn btn-sm btn-outline" href="${ctx}/notifications?filter=${selectedFilter}&amp;page=${result.page - 1}"
        hx-get="${ctx}/notifications?filter=${selectedFilter}&amp;page=${result.page - 1}" hx-target="#notification-list" hx-swap="outerHTML" hx-push-url="true" hx-sync="#notification-list:replace">← Trước</a></c:if>
      <span class="text-sm">Trang ${result.page}/${result.totalPages}</span>
      <c:if test="${result.page < result.totalPages}"><a class="btn btn-sm btn-outline" href="${ctx}/notifications?filter=${selectedFilter}&amp;page=${result.page + 1}"
        hx-get="${ctx}/notifications?filter=${selectedFilter}&amp;page=${result.page + 1}" hx-target="#notification-list" hx-swap="outerHTML" hx-push-url="true" hx-sync="#notification-list:replace">Sau →</a></c:if>
    </nav>
  </c:if>
</div>
<c:if test="${updateNotificationCount}"><%@ include file="/WEB-INF/views/customer/fragments/notification-count.jsp" %></c:if>
