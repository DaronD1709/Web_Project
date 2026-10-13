<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" />
<%--
  NOI DUNG trang Admin > Chat voi khach. AdminChatServlet.showPage truyen:
    conversations : tin cuoi cung cua moi khach co chat (List<Message>)     selected : Customer dang mo (null neu chua co chat nao)     selectedId
    filter : all | human | ai     q : tu khoa tim khach     messages / lastId : tin cua khach dang mo va id tin cuoi     orders : 3 don gan nhat cua khach
  Chuyen tu mockup/admin/chat.html. Khong WebSocket: danh sach tu hoi moi 5 giay, khung chat tu hoi moi 3 giay (htmx polling).
--%>
<div class="max-w-7xl mx-auto">

  <%-- ===== VUNG 1: TIEU DE TRANG ===== --%>
  <div class="mb-4">
    <h1 class="text-2xl font-bold">Chat với khách</h1>
    <p class="text-sm text-base-content/60">Trợ lý AI trả lời trước; bạn tiếp quản khi khách cần nhân viên</p>
  </div>

  <div class="card bg-base-100 border border-base-300 overflow-hidden grid grid-cols-1 lg:grid-cols-[20rem_minmax(0,1fr)] 2xl:grid-cols-[20rem_minmax(0,1fr)_17rem] lg:h-[calc(100vh-11rem)] lg:min-h-[34rem]">

    <%-- ===== VUNG 2 (cot 1): DANH SACH HOI THOAI + TIM KIEM + LOC AI / NHAN VIEN ===== --%>
    <div class="border-b lg:border-b-0 lg:border-r border-base-300 flex flex-col min-h-0 min-w-0 max-h-72 lg:max-h-none">
      <div class="px-5 pt-5 pb-4 space-y-3 border-b border-base-300">
        <div class="flex items-center justify-between"><h2 class="font-semibold">Hội thoại</h2><span class="text-xs text-base-content/50">${fn:length(conversations)} cuộc</span></div>
        <%-- Tim khach: go xong cho 0,3s thi chi thay lai danh sach #chat-list (khong tai lai trang nen o tim kiem giu nguyen con tro) --%>
        <form id="chat-search" method="get" action="${ctx}/admin/chat" onsubmit="return false">
          <input type="hidden" name="c" value="${selectedId}"><input type="hidden" name="filter" value="${filter}">
          <label class="input input-sm w-full">
            <input type="search" name="q" value="<c:out value='${q}'/>" placeholder="Tìm khách hàng…" autocomplete="off"
                   hx-get="${ctx}/admin/chat/list" hx-include="#chat-search" hx-trigger="input delay:300ms" hx-target="#chat-list" hx-swap="outerHTML">
          </label>
        </form>
        <%-- Loc theo che do: link thuong (hx-boost doi trang khong nhay) --%>
        <div class="join w-full">
          <a href="${ctx}/admin/chat?filter=all" class="join-item btn btn-sm flex-1 ${filter == 'all' ? 'btn-primary' : ''}">Tất cả</a>
          <a href="${ctx}/admin/chat?filter=human" class="join-item btn btn-sm flex-1 ${filter == 'human' ? 'btn-primary' : ''}">Nhân viên</a>
          <a href="${ctx}/admin/chat?filter=ai" class="join-item btn btn-sm flex-1 ${filter == 'ai' ? 'btn-primary' : ''}">AI</a>
        </div>
      </div>
      <jsp:include page="/WEB-INF/views/admin/fragments/admin-chat-list.jsp" />
    </div>

    <c:choose>
      <%-- ===== Chua co cuoc tro chuyen nao (hoac khong khop bo loc) ===== --%>
      <c:when test="${empty selected}">
        <div class="lg:col-span-1 2xl:col-span-2 p-12 text-center text-base-content/60 flex flex-col items-center justify-center">
          <div class="text-5xl">💬</div><div class="mt-2">Chưa có cuộc trò chuyện nào để hiển thị.</div>
        </div>
      </c:when>
      <c:otherwise>

        <%-- ===== VUNG 3 (cot 2): KHUNG CHAT ===== --%>
        <div class="flex flex-col min-h-[34rem] lg:min-h-0 min-w-0 border-b lg:border-b-0 2xl:border-r border-base-300">
          <%-- Dau khung: ten khach + che do + nut AI / Nhan vien (fragment, duoc thay khi bam nut hoac khi vua gui tin) --%>
          <jsp:include page="/WEB-INF/views/admin/fragments/admin-chat-head.jsp" />

          <%-- Cac bong bong chat + the #poll (hoi tin moi moi 3 giay). id chat-msgs de admin.js cuon xuong cuoi khi co tin moi --%>
          <div id="chat-msgs" class="flex-1 overflow-y-auto px-6 py-6 space-y-5 bg-base-200/60">
            <jsp:include page="/WEB-INF/views/admin/fragments/admin-chat-messages.jsp" />
          </div>

          <%-- Cau tra loi nhanh: bam de dien vao o nhap (admin.js: nvChatChip) --%>
          <div class="px-6 pt-3 flex gap-2 overflow-x-auto [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
            <c:forEach var="chip" items="${['Chào bạn, mình hỗ trợ gì được ạ?', 'Shop kiểm tra giúp bạn nhé.', 'Đơn của bạn đang được giao.', 'Cảm ơn bạn đã liên hệ!']}">
              <button type="button" class="btn btn-sm btn-outline rounded-full font-normal whitespace-nowrap" onclick="nvChatChip(this)"><c:out value="${chip}"/></button>
            </c:forEach>
          </div>

          <%-- O nhap + nut gui. htmx: POST /admin/chat/send kem id khach va id tin cuoi dang co (after); server tra tin moi thay vao #poll --%>
          <form class="px-6 py-4 flex items-center gap-3" hx-post="${ctx}/admin/chat/send" hx-target="#poll" hx-swap="outerHTML"
                hx-vals='js:{after: document.getElementById("poll").dataset.after}'
                hx-on::after-request="if (event.detail.successful) this.reset()">
            <input type="hidden" name="c" value="${selected.id}">
            <input id="chat-input" name="content" class="input flex-1 rounded-full px-5" maxlength="1000" autocomplete="off" placeholder="Nhập tin nhắn cho khách…" required>
            <button class="btn btn-primary btn-circle" aria-label="Gửi tin nhắn">
              <svg class="size-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M6 12L3.27 3.13A59.8 59.8 0 0121.5 12a59.8 59.8 0 01-18.23 8.87L6 12zm0 0h7.5"/></svg>
            </button>
          </form>
        </div>

        <%-- ===== VUNG 4 (cot 3, chi man hinh rong): THONG TIN KHACH + DON GAN DAY ===== --%>
        <aside class="hidden 2xl:block p-6 text-sm overflow-y-auto">
          <div class="text-center">
            <div class="avatar avatar-placeholder"><div class="bg-primary/15 text-primary w-16 rounded-full text-xl font-semibold"><span>${fn:toUpperCase(fn:substring(selected.fullName, 0, 1))}</span></div></div>
            <div class="font-semibold mt-3"><c:out value="${selected.fullName}"/></div>
            <div class="text-base-content/60 text-xs mt-1"><c:out value="${selected.email}"/><br><c:out value="${selected.phone}"/></div>
          </div>
          <div class="divider my-5"></div>
          <div class="font-semibold mb-3">Đơn gần đây</div>
          <ul class="space-y-3">
            <c:forEach var="o" items="${orders}">
              <li class="flex items-center justify-between gap-2">
                <a class="link link-primary font-mono" href="${ctx}/admin/orders/detail?id=${o.id}">#${o.id}</a>
                <c:set var="st" value="${o.status}" /><%@ include file="/WEB-INF/views/admin/common/admin-order-badge.jspf" %>
              </li>
            </c:forEach>
            <c:if test="${empty orders}"><li class="text-base-content/50">Chưa có đơn.</li></c:if>
          </ul>
          <a class="btn btn-outline btn-sm btn-block mt-6" href="${ctx}/admin/users/detail?id=${selected.id}">Xem hồ sơ khách</a>
        </aside>
      </c:otherwise>
    </c:choose>
  </div>
</div>
