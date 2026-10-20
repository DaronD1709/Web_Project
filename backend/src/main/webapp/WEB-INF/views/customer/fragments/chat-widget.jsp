<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%--
  MANH: noi dung than khung chat nho (bong bong chat o goc phai moi trang, xem common/footer.jspf). ChatServlet (/chat/widget va /chat/guest) tra ve,
  htmx nhet vao <div id="nvw-body">. Co 2 che do:
    1. widgetStart = true: khach CHUA dang nhap va chua nhap ten -> hien o nhap ten (chat nhanh nhu khach vang lai). Attr kem: error, guestName.
    2. nguoc lai: hien cac tin nhan + o nhap. Attr do ChatServlet.forwardMessages truyen: messages, lastId, chatOwnerId.
--%>
<c:choose>

  <%-- ===== CHE DO 1: NHAP TEN (khach vang lai) ===== --%>
  <c:when test="${widgetStart}">
    <%-- Gui bang htmx toi POST /chat/guest, ket qua thay luon than khung (hx-target="#nvw-body"): thanh cong -> thay bang khung tin nhan; loi -> hien lai o nhap ten kem loi --%>
    <form class="p-4 flex flex-col gap-3 overflow-y-auto" hx-post="${ctx}/chat/guest" hx-target="#nvw-body" hx-swap="innerHTML">
      <p class="text-sm text-base-content/70">Chat nhanh, không cần tài khoản. Cho shop biết tên của bạn để tiện xưng hô nhé.</p>
      <input name="name" class="input w-full ${empty error ? '' : 'input-error'}" maxlength="60" required autocomplete="name"
             placeholder="Ví dụ: Anh Nam" value="<c:out value='${guestName}'/>">
      <c:if test="${not empty error}"><p class="text-error text-sm" role="alert"><c:out value="${error}"/></p></c:if>
      <button class="btn btn-primary btn-sm">Bắt đầu chat</button>
      <p class="text-xs text-base-content/50">Đã có tài khoản? <a class="link link-primary" href="${ctx}/login?next=/chat">Đăng nhập</a> để lưu lịch sử chat.</p>
    </form>
  </c:when>

  <%-- ===== CHE DO 2: TIN NHAN + O NHAP ===== --%>
  <c:otherwise>
    <%-- Vung tin nhan (cuon duoc). Ben trong la fragment chat-messages.jsp: cac bong bong + the #poll hoi tin moi moi 3 giay --%>
    <div id="nvw-msgs" class="nv-chatbox-msgs p-4 space-y-1 bg-base-200/50">
      <jsp:include page="/WEB-INF/views/customer/fragments/chat-messages.jsp" />
    </div>

    <%-- O nhap + nut gui: giong trang /chat. POST /chat/send kem "after" = id tin cuoi dang co (tinh bang JS luc bam gui, doc data-after cua #poll hien tai);
         server tra cac tin moi thay vao #poll. Gui thanh cong thi xoa o nhap, loi (tin rong/qua dai) thi giu chu de sua. --%>
    <form class="p-3 flex gap-2 border-t border-base-300" hx-post="${ctx}/chat/send" hx-target="#poll" hx-swap="outerHTML"
          hx-vals='js:{after: document.getElementById("poll").dataset.after}'
          hx-on::after-request="if (event.detail.successful) this.reset()">
      <input name="content" class="input input-sm flex-1" maxlength="1000" autocomplete="off" placeholder="Nhập tin nhắn…" required>
      <button class="btn btn-primary btn-sm">Gửi</button>
    </form>
  </c:otherwise>
</c:choose>
