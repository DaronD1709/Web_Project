<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Trang /chat: khach co tai khoan HOAC khach vang lai da nhap ten (xem ChatServlet). Chuyen tu mockup/chat.html. Khong WebSocket: #poll hoi tin moi moi 3 giay (htmx). --%>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Chat với shop — Nông Việt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<%-- ChatServlet.forwardMessages truyen: messages (tin cua cuoc chat), lastId (id tin cuoi, cho polling), botEmail (nhan ra tin cua tro ly tu dong),
     chatOwnerId (id nguoi dang chat, de biet tin nao la "cua minh"), isGuestChat + chatOwner (khach vang lai hay khong, va khach nao). --%>
<main class="max-w-3xl mx-auto px-4 py-6">
  <div class="card bg-base-100 border border-base-300 overflow-hidden">
    <%-- ===== VUNG 1: DAU KHUNG CHAT (ten shop + dong gioi thieu) ===== --%>
    <div class="flex items-center gap-3 px-5 py-3 border-b border-base-300">
      <div class="avatar avatar-online avatar-placeholder"><div class="bg-primary text-primary-content w-10 rounded-full"><i data-lucide="wheat" class="size-5"></i></div></div>
      <div class="flex-1"><div class="font-semibold">Nông Việt</div><div class="text-xs text-success">Gửi tin nhắn cho shop — nhân viên sẽ phản hồi trong giờ làm việc</div></div>
    </div>

    <%-- ===== VUNG 2: BANNER KHACH VANG LAI (chi hien khi chua dang nhap): nhac ten dang dung + goi y dang nhap de luu lich su ===== --%>
    <c:if test="${isGuestChat}">
      <div class="px-5 py-3 border-b border-base-300 bg-info/10 text-sm flex flex-wrap items-center justify-between gap-2">
        <span class="flex items-center gap-2"><i data-lucide="user-round" class="size-4"></i> Bạn đang chat với tên <b><c:out value="${chatOwner.fullName}"/></b> (khách vãng lai)</span>
        <a href="${ctx}/login?next=/chat" class="btn btn-outline btn-xs">Đăng nhập để lưu lịch sử chat</a>
      </div>
    </c:if>
    <%-- ===== VUNG 3: KHUNG TIN NHAN (cuon duoc). Noi dung la fragment chat-messages.jsp, cung fragment nay duoc server tra lai moi 3 giay (polling) ===== --%>
    <div id="msgs" class="p-5 space-y-1 h-[26rem] overflow-y-auto bg-base-200/50">
      <jsp:include page="/WEB-INF/views/customer/fragments/chat-messages.jsp" />
    </div>

    <%-- ===== VUNG 4: CAU HOI GOI Y (bam de dien san vao o nhap, chua gui; ham useChip o cuoi trang). Cau thu 3 dung de thu chuyen sang nhan vien ===== --%>
    <div class="px-5 pt-3 flex flex-wrap gap-2">
      <button type="button" class="badge badge-outline cursor-pointer" onclick="useChip(this)">Rau màu nên bón phân gì?</button>
      <button type="button" class="badge badge-outline cursor-pointer" onclick="useChip(this)">Tư vấn hệ thống tưới 1000m²</button>
      <button type="button" class="badge badge-outline cursor-pointer" onclick="useChip(this)">Gặp nhân viên</button>
    </div>
    <%-- ===== VUNG 5: O NHAP + NUT GUI. htmx: POST /chat/send kem "after" = id tin cuoi dang co (tinh bang JS luc bam gui, doc data-after cua #poll hien tai).
         Server luu tin + chatbot tra loi, roi tra ve cac tin moi thay vao #poll (outerHTML). Gui thanh cong thi xoa o nhap, loi thi giu chu de sua. ===== --%>
    <form class="p-4 flex gap-2" hx-post="${ctx}/chat/send" hx-target="#poll" hx-swap="outerHTML"
          hx-vals='js:{after: document.getElementById("poll").dataset.after}'
          hx-on::after-request="if (event.detail.successful) this.reset()">
      <input id="chat-input" name="content" class="input flex-1" maxlength="1000" autocomplete="off" placeholder="Nhập tin nhắn…" required>
      <button class="btn btn-primary">Gửi</button>
    </form>
  </div>
</main>

<%-- Dang o trang chat day du thi khong hien bong bong chat (footer.jspf doc bien nay) --%>
<c:set var="hideChatWidget" value="true" scope="request" />
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
<%-- ===== SCRIPT NHO: cuon khung chat xuong cuoi luc mo trang va khi co tin moi; ham dien cau goi y vao o nhap ===== --%>
<script>
  var box = document.getElementById('msgs'), bubbles = box.querySelectorAll('.chat').length;
  box.scrollTop = box.scrollHeight;
  // Chi cuon xuong cuoi khi co tin MOI (polling 3s khong lam nhay khung neu dang doc tin cu)
  document.body.addEventListener('htmx:afterSwap', function () {
    var n = box.querySelectorAll('.chat').length;
    if (n !== bubbles) { bubbles = n; box.scrollTop = box.scrollHeight; }
  });
  function useChip(b) { var i = document.getElementById('chat-input'); i.value = b.textContent; i.focus(); }
</script>
</body>
</html>
