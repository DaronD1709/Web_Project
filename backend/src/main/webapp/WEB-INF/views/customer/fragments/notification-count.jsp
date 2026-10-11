<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<span id="notification-count" class="badge badge-xs badge-primary ${empty unreadCount or unreadCount == 0 ? 'hidden' : ''}"
      hx-get="${ctx}/notifications/count" hx-trigger="${notificationCountLoaded ? 'every 30s' : 'load, every 30s'}"
      hx-target="this" hx-swap="outerHTML" hx-sync="this:drop"
      <c:if test="${updateNotificationCount}">hx-swap-oob="outerHTML"</c:if>>${empty unreadCount ? 0 : unreadCount}</span>
