<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%--
  MANH: danh sach hoi thoai (moi khach 1 dong, tin cuoi cung). Attr: conversations (List<Message> = tin cuoi moi khach), selectedId, filter, q, botEmail.
  Tu hoi lai moi 5 giay (polling) va khi go o tim kiem. QUAN TRONG hx-target="this": khung hx-boost ben ngoai dat hx-target="#adm-main"
  va thuoc tinh nay duoc ke thua -> neu khong chi dinh, ket qua polling se thay vao ca #adm-main va xoa mat trang.
--%>
<%-- c:url tu them context path va ma hoa tham so (tu khoa co dau cach, dau & ...) --%>
<c:url var="listUrl" value="/admin/chat/list"><c:param name="c" value="${selectedId}" /><c:param name="filter" value="${filter}" /><c:param name="q" value="${q}" /></c:url>
<ul id="chat-list" class="overflow-y-auto flex-1" hx-get="${listUrl}"
    hx-trigger="every 5s" hx-target="this" hx-swap="outerHTML">
  <c:if test="${empty conversations}"><li class="p-8 text-center text-sm text-base-content/50">Chưa có cuộc trò chuyện nào.</li></c:if>
  <c:forEach var="m" items="${conversations}">
    <c:set var="cu" value="${m.customer}" />
    <%-- Tin cuoi la cua KHACH nghia la shop chua tra loi -> danh dau "cho phan hoi" --%>
    <c:set var="waiting" value="${m.sender.id == cu.id}" />
    <li>
      <c:url var="openUrl" value="/admin/chat"><c:param name="c" value="${cu.id}" /><c:param name="filter" value="${filter}" /><c:param name="q" value="${q}" /></c:url>
      <%-- Link mo hoi thoai nam TRONG <ul> co hx-target="this" + hx-swap="outerHTML" (de polling thay chinh danh sach) nen se KE THUA 2 thuoc tinh do:
           neu khong khai lai, bam vao se nhet CA TRANG chat vao trong danh sach (trang long trang). Vi vay phai tu dat lai dich la #adm-main. --%>
      <a href="${openUrl}" hx-target="#adm-main" hx-swap="innerHTML show:window:top"
         class="w-full text-left px-5 py-4 flex gap-3 items-start border-b border-base-300/70 hover:bg-base-200/70 transition ${cu.id == selectedId ? 'bg-primary/10 shadow-[inset_3px_0_0_var(--color-primary)]' : ''}">
        <div class="avatar avatar-placeholder shrink-0"><div class="bg-primary/15 text-primary w-10 rounded-full text-sm font-semibold"><span>${fn:toUpperCase(fn:substring(cu.fullName, 0, 1))}</span></div></div>
        <div class="min-w-0 flex-1 space-y-1">
          <div class="flex items-center justify-between gap-2">
            <span class="font-medium truncate ${waiting ? '' : 'text-base-content/80'}"><c:out value="${cu.fullName}"/></span>
            <span class="text-[11px] text-base-content/50 shrink-0"><fmt:formatNumber value="${m.sentAt.hour}" minIntegerDigits="2"/>:<fmt:formatNumber value="${m.sentAt.minute}" minIntegerDigits="2"/></span>
          </div>
          <div class="flex items-center justify-between gap-2">
            <span class="text-xs truncate ${waiting ? 'text-base-content font-medium' : 'text-base-content/60'}"><c:out value="${m.content}"/></span>
            <c:if test="${waiting}"><span class="badge badge-primary badge-xs rounded-full shrink-0">Chờ</span></c:if>
          </div>
          <div><c:choose>
            <c:when test="${cu.handledByHuman}"><span class="badge badge-soft badge-primary badge-sm">👤 Nhân viên</span></c:when>
            <c:otherwise><span class="badge badge-soft badge-accent badge-sm">🤖 AI</span></c:otherwise>
          </c:choose></div>
        </div>
      </a>
    </li>
  </c:forEach>
</ul>
