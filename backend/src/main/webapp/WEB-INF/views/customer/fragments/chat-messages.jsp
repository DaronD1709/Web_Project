<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%-- Cac bong bong chat moi + 1 phan tu #poll o cuoi. Attr: messages (List<Message>), lastId (id tin cuoi da co), botEmail (email tai khoan he thong cua chatbot), chatOwnerId (id nguoi dang chat: tai khoan that hoac khach vang lai).
     #poll tu hoi /chat/messages?after=lastId moi 3 giay (polling); ket qua thay chinh no bang tin moi + #poll moi. --%>
<%-- Cach nhan ra nguoi gui: mine = nguoi gui la chinh nguoi dang chat (bong bong ben phai, mau chinh); isBot = tai khoan he thong "Tro ly tu dong";
     con lai la nhan vien shop (nhan "Shop"). Ca ba kieu duoc phan biet bang so sanh id/email, khong can luu them cot nao. --%>
<c:forEach var="m" items="${messages}">
  <c:set var="mine" value="${m.sender.id == chatOwnerId}" />
  <c:set var="isBot" value="${m.sender.email == botEmail}" />
  <div class="chat ${mine ? 'chat-end' : 'chat-start'}">
    <div class="chat-header text-xs">
      <c:choose>
        <c:when test="${mine}">Bạn</c:when>
        <c:when test="${isBot}">Trợ lý tự động</c:when>
        <c:otherwise>Nhân viên shop <span class="badge badge-xs badge-primary">Shop</span></c:otherwise>
      </c:choose>
      <time class="opacity-50"><fmt:formatNumber value="${m.sentAt.hour}" minIntegerDigits="2"/>:<fmt:formatNumber value="${m.sentAt.minute}" minIntegerDigits="2"/></time>
    </div>
    <%-- Noi dung do nguoi dung nhap: luon <c:out> de chong XSS --%>
    <div class="chat-bubble ${mine ? 'chat-bubble-primary' : (isBot ? '' : 'chat-bubble-secondary')}"><c:out value="${m.content}"/></div>
  </div>
</c:forEach>
<%-- THE #poll: moi 3 giay hoi /chat/messages?after=lastId (chi tin moi hon lastId); ket qua gom tin moi + #poll moi (after moi) thay chinh the nay, nen vong lap tiep tuc.
     data-after de form gui tin doc duoc id tin cuoi hien tai. --%>
<div id="poll" data-after="${lastId}" hx-get="${ctx}/chat/messages?after=${lastId}" hx-trigger="every 3s" hx-swap="outerHTML"></div>
