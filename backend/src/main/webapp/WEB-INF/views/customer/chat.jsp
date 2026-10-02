<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Trang /chat (chi cho khach da dang nhap). Chuyen tu mockup/chat.html. Khong WebSocket: #poll hoi tin moi moi 3 giay (htmx). --%>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Chat với shop — NôngViệt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main class="max-w-3xl mx-auto px-4 py-6">
  <div class="card bg-base-100 border border-base-300 overflow-hidden">
    <div class="flex items-center gap-3 px-5 py-3 border-b border-base-300">
      <div class="avatar avatar-online avatar-placeholder"><div class="bg-primary text-primary-content w-10 rounded-full">🌾</div></div>
      <div class="flex-1"><div class="font-semibold">NôngViệt</div><div class="text-xs text-success">Gửi tin nhắn cho shop — nhân viên sẽ phản hồi trong giờ làm việc</div></div>
    </div>

    <div id="msgs" class="p-5 space-y-1 h-[26rem] overflow-y-auto bg-base-200/50">
      <jsp:include page="/WEB-INF/views/customer/fragments/chat-messages.jsp" />
    </div>

    <div class="px-5 pt-3 flex flex-wrap gap-2">
      <button type="button" class="badge badge-outline cursor-pointer" onclick="useChip(this)">Rau màu nên bón phân gì?</button>
      <button type="button" class="badge badge-outline cursor-pointer" onclick="useChip(this)">Tư vấn hệ thống tưới 1000m²</button>
      <button type="button" class="badge badge-outline cursor-pointer" onclick="useChip(this)">Gặp nhân viên</button>
    </div>
    <form class="p-4 flex gap-2" hx-post="${ctx}/chat/send" hx-target="#poll" hx-swap="outerHTML"
          hx-vals='js:{after: document.getElementById("poll").dataset.after}'
          hx-on::after-request="if (event.detail.successful) this.reset()">
      <input id="chat-input" name="content" class="input flex-1" maxlength="1000" autocomplete="off" placeholder="Nhập tin nhắn…" required>
      <button class="btn btn-primary">Gửi</button>
    </form>
  </div>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
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
