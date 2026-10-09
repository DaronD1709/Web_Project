<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%--
  MANH: dau khung chat (ten khach + che do dang tra loi + 2 nut AI / Nhan vien). Attr: selected (Customer), oob (true khi tra kem sau khi gui tin
  de htmx tu thay #chat-head o noi khac bang hx-swap-oob). Bam nut -> POST /admin/chat/mode, server tra lai chinh manh nay thay vao #chat-head.
--%>
<div id="chat-head" ${oob ? 'hx-swap-oob="true"' : ''} class="px-6 py-4 border-b border-base-300 flex items-center gap-4">
  <div class="avatar avatar-online avatar-placeholder"><div class="bg-primary/15 text-primary w-11 rounded-full font-semibold"><span>${fn:toUpperCase(fn:substring(selected.fullName, 0, 1))}</span></div></div>
  <div class="flex-1 min-w-0">
    <div class="font-semibold truncate"><c:out value="${selected.fullName}"/></div>
    <div class="text-xs text-base-content/60 flex items-center gap-1.5 mt-0.5 min-w-0">
      <span class="size-2 rounded-full inline-block shrink-0 ${selected.handledByHuman ? 'bg-success' : 'bg-warning'}"></span>
      <span class="truncate">${selected.handledByHuman ? 'Nhân viên đang trả lời' : 'AI đang tự động trả lời'}</span>
    </div>
  </div>
  <%-- 2 nut chuyen che do: gui "human" la gia tri MONG MUON; hx-target="#chat-head" + outerHTML = thay chinh khung dau nay --%>
  <div class="join shrink-0" role="group" aria-label="Chế độ trả lời">
    <button type="button" class="join-item btn btn-sm ${selected.handledByHuman ? 'btn-ghost border border-base-300' : 'btn-primary'}"
            hx-post="${ctx}/admin/chat/mode" hx-vals='{"c":"${selected.id}","human":"false"}' hx-target="#chat-head" hx-swap="outerHTML">🤖 AI</button>
    <button type="button" class="join-item btn btn-sm ${selected.handledByHuman ? 'btn-primary' : 'btn-ghost border border-base-300'}"
            hx-post="${ctx}/admin/chat/mode" hx-vals='{"c":"${selected.id}","human":"true"}' hx-target="#chat-head" hx-swap="outerHTML">👤 Nhân viên</button>
  </div>
</div>
