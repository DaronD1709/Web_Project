<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  MANH: dau khung chat (avatar chu cai dau + ten khach, kem nhan "Khach vang lai" neu chua co tai khoan). Attr: selected (Customer dang mo).
  Duoc admin-chat.jsp include 1 lan khi ve trang; khong thay doi khi polling.
--%>
<div id="chat-head" class="px-6 py-4 border-b border-base-300 flex items-center gap-4">
  <div class="avatar avatar-online avatar-placeholder"><div class="bg-primary/15 text-primary w-11 rounded-full font-semibold"><span>${fn:toUpperCase(fn:substring(selected.fullName, 0, 1))}</span></div></div>
  <div class="flex-1 min-w-0">
    <div class="font-semibold truncate"><c:out value="${selected.fullName}"/><c:if test="${selected.guest}"> <span class="badge badge-soft badge-neutral badge-sm align-middle">Khách vãng lai</span></c:if></div>
    <div class="text-xs text-base-content/60 mt-0.5">Trả lời bằng tay trong khung bên dưới</div>
  </div>
</div>
