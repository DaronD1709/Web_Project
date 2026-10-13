<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  MANH: ket qua "Thu cau hoi". AdminAutoReplyServlet (/test) truyen: tested (true neu Admin da go gi do), matched (AutoReply khop, hoac null).
  htmx nhet vao <div id="test-result"> cua admin-autoreply-list.jsp moi khi Admin go.
--%>
<c:choose>
  <c:when test="${not tested}"><div class="text-sm text-base-content/50">Gõ một câu khách có thể hỏi để xem chatbot sẽ trả lời thế nào.</div></c:when>
  <c:when test="${not empty matched}">
    <div class="text-sm space-y-2">
      <div class="text-base-content/60">Khớp luật <b>#${matched.id}</b> (ưu tiên ${matched.priority}):
        <c:forEach var="k" items="${matched.keywordList}"><span class="badge badge-sm badge-soft badge-primary mr-1"><c:out value="${k}"/></span></c:forEach></div>
      <div class="chat chat-start"><div class="chat-bubble"><c:out value="${matched.replyText}"/></div></div>
      <c:if test="${matched.handoff}"><div class="text-xs text-warning">→ Sau câu này cuộc trò chuyện được chuyển sang nhân viên.</div></c:if>
    </div>
  </c:when>
  <c:otherwise>
    <div class="alert alert-warning alert-soft text-sm">Không có luật nào khớp. Chatbot sẽ trả lời câu mặc định "Mình chưa trả lời được câu này…" và chuyển cho nhân viên.
      Muốn bắt mọi câu còn lại, thêm 1 luật với từ khoá <b>*</b>.</div>
  </c:otherwise>
</c:choose>
