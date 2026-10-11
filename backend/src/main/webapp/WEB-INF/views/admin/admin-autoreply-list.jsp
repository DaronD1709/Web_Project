<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  NOI DUNG trang Admin > Tra loi tu dong. AdminAutoReplyServlet truyen: rules (List<AutoReply>, so uu tien nho truoc).
  Moi luat: tu khoa -> cau tra loi (+ co chuyen nhan vien khong). ChatBotService xet theo thu tu, luat dau tien khop thang.
--%>
<c:set var="back" value="/admin/auto-replies" />
<div class="max-w-6xl mx-auto space-y-4">

  <%-- ===== VUNG 1: TIEU DE TRANG + NUT "THEM" ===== --%>
  <div class="flex flex-wrap items-center justify-between gap-2">
    <div>
      <h1 class="text-2xl font-bold">Trả lời tự động</h1>
      <p class="text-sm text-base-content/60">Chatbot trả lời khách theo các luật bạn soạn: nếu tin của khách có từ khoá thì gửi câu trả lời tương ứng</p>
    </div>
    <a href="${ctx}/admin/auto-replies/new" class="btn btn-primary btn-sm">+ Thêm câu trả lời</a>
  </div>

  <%-- ===== VUNG 2: THU CAU HOI. htmx: go xong cho 0,4s thi GET /admin/auto-replies/test, ket qua nhet vao #test-result.
       hx-target="#test-result" tu khai vi khung ngoai da dat hx-target="#adm-main" (thuoc tinh ke thua). ===== --%>
  <section class="card bg-base-100 border border-base-300 p-5 space-y-3">
    <h2 class="font-semibold">Thử câu hỏi</h2>
    <input type="search" name="text" class="input w-full" placeholder="Vd: Phí ship bao nhiêu vậy shop?" autocomplete="off"
           hx-get="${ctx}/admin/auto-replies/test" hx-trigger="input delay:400ms" hx-target="#test-result" hx-swap="innerHTML">
    <div id="test-result"><div class="text-sm text-base-content/50">Gõ một câu khách có thể hỏi để xem chatbot sẽ trả lời thế nào.</div></div>
  </section>

  <%-- ===== VUNG 3: BANG LUAT ===== --%>
  <div class="card bg-base-100 border border-base-300 overflow-hidden">
    <c:choose>
      <c:when test="${empty rules}"><div class="p-12 text-center text-base-content/60"><div class="text-4xl">🤖</div>Chưa có câu trả lời tự động nào.</div></c:when>
      <c:otherwise>
        <div class="overflow-x-auto">
          <table class="table">
            <thead><tr><th class="w-16">Ưu tiên</th><th>Từ khoá</th><th>Câu trả lời</th><th>Chuyển NV</th><th class="text-center">Bật/tắt</th><th class="text-right">Thao tác</th></tr></thead>
            <tbody>
              <c:forEach var="r" items="${rules}">
                <tr class="hover ${r.active ? '' : 'opacity-60'}">
                  <td class="tabular-nums">${r.priority}</td>
                  <td class="max-w-xs">
                    <c:forEach var="k" items="${r.keywordList}"><span class="badge badge-sm badge-soft ${k == '*' ? 'badge-warning' : 'badge-primary'} mr-1 mb-1"><c:out value="${k == '*' ? '* (mọi câu)' : k}"/></span></c:forEach>
                  </td>
                  <td class="max-w-md text-sm"><span class="line-clamp-2"><c:out value="${r.replyText}"/></span></td>
                  <td><c:if test="${r.handoff}"><span class="badge badge-sm badge-warning whitespace-nowrap">👤 Có</span></c:if></td>
                  <%-- Cong tac bat/tat. htmx: gat la form tu POST /admin/auto-replies/toggle (hx-trigger="change"); "active" la gia tri MONG MUON
                       (nguoc voi hien tai) nen bam nhanh nhieu lan khong bi lech. Server redirect ve "back" kem toast. --%>
                  <td class="text-center">
                    <form method="post" action="${ctx}/admin/auto-replies/toggle" hx-post="${ctx}/admin/auto-replies/toggle" hx-trigger="change">
                      <input type="hidden" name="id" value="${r.id}"><input type="hidden" name="active" value="${r.active ? 'false' : 'true'}"><input type="hidden" name="back" value="${back}">
                      <input type="checkbox" class="toggle toggle-primary toggle-sm" ${r.active ? 'checked' : ''} aria-label="Bật/tắt luật ${r.id}">
                    </form>
                  </td>
                  <td class="text-right whitespace-nowrap">
                    <a class="btn btn-ghost btn-xs" href="${ctx}/admin/auto-replies/edit?id=${r.id}">Sửa</a>
                    <form method="post" action="${ctx}/admin/auto-replies/delete" class="inline" hx-confirm="Xoá câu trả lời tự động này?">
                      <input type="hidden" name="id" value="${r.id}"><input type="hidden" name="back" value="${back}">
                      <button class="btn btn-ghost btn-xs text-error">Xoá</button>
                    </form>
                  </td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
      </c:otherwise>
    </c:choose>
  </div>

  <p class="text-xs text-base-content/50">Luật có số ưu tiên nhỏ được xét trước, luật đầu tiên khớp sẽ trả lời. Từ khoá khớp theo từ/cụm từ, không phân biệt hoa thường và dấu tiếng Việt.
    Từ khoá <b>*</b> khớp mọi câu, nên đặt ưu tiên lớn nhất để làm câu trả lời mặc định.</p>
</div>
