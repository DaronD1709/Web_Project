<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%--
  MANH: cac bong bong chat moi + 1 phan tu #poll o cuoi. Attr: messages (List<Message>), lastId (id tin cuoi da co), selected (Customer dang mo),
  botEmail (email tai khoan he thong cua chatbot), withHead (true khi vua gui tin: tra kem dau khung chat da cap nhat).
  #poll tu hoi /admin/chat/messages?c=..&after=lastId moi 3 giay (polling); ket qua thay chinh no bang tin moi + #poll moi.
  3 kieu nguoi gui: khach (trang, ben trai), tro ly AI (vang nhat, ben phai), nhan vien (xanh dam, ben phai).
--%>
<c:forEach var="m" items="${messages}">
  <c:set var="fromCustomer" value="${m.sender.id == m.customer.id}" />
  <c:set var="fromBot" value="${m.sender.email == botEmail}" />
  <div class="flex items-end gap-2.5 chat-bubble-row ${fromCustomer ? '' : 'flex-row-reverse'}">
    <c:choose>
      <c:when test="${fromCustomer}"><div class="size-8 rounded-full bg-base-300 text-base-content/70 grid place-items-center text-xs font-semibold shrink-0">${fn:toUpperCase(fn:substring(selected.fullName, 0, 1))}</div></c:when>
      <c:when test="${fromBot}"><div class="size-8 rounded-full bg-accent/40 grid place-items-center text-sm shrink-0">🤖</div></c:when>
      <c:otherwise><div class="size-8 rounded-full bg-primary text-primary-content grid place-items-center text-xs font-semibold shrink-0">NV</div></c:otherwise>
    </c:choose>
    <div class="flex flex-col gap-1 min-w-0 max-w-[78%] ${fromCustomer ? 'items-start' : 'items-end'}">
      <div class="text-[11px] text-base-content/50 px-1">
        <c:choose>
          <c:when test="${fromCustomer}"><c:out value="${selected.fullName}"/></c:when>
          <c:when test="${fromBot}">Trợ lý AI</c:when>
          <c:when test="${m.sender.id == sessionScope.currentUser.id}">Bạn</c:when>
          <c:otherwise><c:out value="${m.sender.fullName}"/></c:otherwise>
        </c:choose>
        · <fmt:formatNumber value="${m.sentAt.hour}" minIntegerDigits="2"/>:<fmt:formatNumber value="${m.sentAt.minute}" minIntegerDigits="2"/>
      </div>
      <%-- Noi dung do nguoi dung nhap: luon <c:out> de chong XSS --%>
      <div class="px-4 py-2.5 rounded-2xl text-sm leading-relaxed break-words ${fromCustomer ? 'bg-base-100 border border-base-300 rounded-bl-md' : fromBot ? 'bg-accent/35 border border-accent/50 rounded-br-md' : 'bg-primary text-primary-content rounded-br-md'}"><c:out value="${m.content}"/></div>
    </div>
  </div>
</c:forEach>
<div id="poll" data-after="${lastId}" hx-get="${ctx}/admin/chat/messages?c=${selected.id}&after=${lastId}" hx-trigger="every 3s" hx-target="this" hx-swap="outerHTML"></div>
<c:if test="${withHead}"><c:set var="oob" value="${true}" scope="request" /><jsp:include page="/WEB-INF/views/admin/fragments/admin-chat-head.jsp" /></c:if>
