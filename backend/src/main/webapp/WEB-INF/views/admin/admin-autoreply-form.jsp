<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  NOI DUNG trang Admin > Them / Sua cau tra loi tu dong (1 file cho ca 2 che do). AdminAutoReplyServlet truyen:
    draft : AutoReply dang hien tren form (mac dinh khi them, luat that khi sua, gia tri vua go khi bi loi)
    errors : Map<ten o, loi> (chi co khi POST bi tu choi)     formError : loi chung khong gan voi o nao
--%>
<c:set var="editing" value="${not empty draft.id}" />
<c:set var="action" value="${ctx}/admin/auto-replies/${editing ? 'edit?id=' : 'new'}${draft.id}" />

<div class="max-w-3xl mx-auto space-y-4">

  <%-- ===== VUNG 1: LIEN KET QUAY LAI + TIEU DE ===== --%>
  <div>
    <a href="${ctx}/admin/auto-replies" class="text-sm link link-primary">← Danh sách câu trả lời tự động</a>
    <h1 class="text-2xl font-bold">${editing ? 'Sửa câu trả lời tự động' : 'Thêm câu trả lời tự động'}</h1>
  </div>

  <c:if test="${not empty formError}"><div class="alert alert-error text-sm"><c:out value="${formError}"/></div></c:if>

  <%-- Form gui bang htmx (hx-boost ke thua tu khung): thanh cong -> redirect ve danh sach; loi -> tra lai form kem loi --%>
  <form method="post" action="${action}" novalidate class="card bg-base-100 border border-base-300 p-5 space-y-4">

    <%-- ===== VUNG 2: TU KHOA ===== --%>
    <label class="form-control w-full">
      <div class="label"><span class="label-text">Từ khoá <span class="text-error">*</span></span></div>
      <textarea name="keywords" rows="2" class="textarea w-full ${not empty errors.keywords ? 'textarea-error' : ''}"
                placeholder="Vd: ship, giao hàng, vận chuyển"><c:out value="${draft.keywords}"/></textarea>
      <div class="label"><span class="label-text-alt text-base-content/60">Cách nhau bằng dấu phẩy. Khớp theo từ/cụm từ, không phân biệt hoa thường và dấu. Dùng <b>*</b> (một mình) để trả lời mọi câu còn lại.</span></div>
      <c:if test="${not empty errors.keywords}"><div class="label"><span class="label-text-alt text-error">${errors.keywords}</span></div></c:if>
    </label>

    <%-- ===== VUNG 3: CAU TRA LOI ===== --%>
    <label class="form-control w-full">
      <div class="label"><span class="label-text">Câu trả lời <span class="text-error">*</span></span></div>
      <textarea name="replyText" rows="4" maxlength="1000" class="textarea w-full ${not empty errors.replyText ? 'textarea-error' : ''}"
                placeholder="Nội dung chatbot gửi cho khách"><c:out value="${draft.replyText}"/></textarea>
      <c:if test="${not empty errors.replyText}"><div class="label"><span class="label-text-alt text-error">${errors.replyText}</span></div></c:if>
    </label>

    <%-- ===== VUNG 4: UU TIEN + TUY CHON ===== --%>
    <div class="grid sm:grid-cols-2 gap-3">
      <label class="form-control">
        <div class="label"><span class="label-text">Mức ưu tiên (1–9999)</span></div>
        <input name="priority" type="number" min="1" max="9999" value="${draft.priority > 0 ? draft.priority : ''}" class="input w-full ${not empty errors.priority ? 'input-error' : ''}">
        <div class="label"><span class="label-text-alt text-base-content/60">Số nhỏ được xét trước.</span></div>
        <c:if test="${not empty errors.priority}"><div class="label"><span class="label-text-alt text-error">${errors.priority}</span></div></c:if>
      </label>
    </div>
    <label class="flex items-center gap-3"><input name="handoff" type="checkbox" class="toggle toggle-primary" ${draft.handoff ? 'checked' : ''}>
      <span class="text-sm">Sau khi trả lời, <b>chuyển cuộc trò chuyện sang nhân viên</b> (chatbot ngừng trả lời)</span></label>
    <label class="flex items-center gap-3"><input name="active" type="checkbox" class="toggle toggle-primary" ${draft.active ? 'checked' : ''}>
      <span class="text-sm">Bật (chatbot dùng luật này)</span></label>

    <%-- ===== VUNG 5: NUT LUU / HUY ===== --%>
    <div class="flex justify-end gap-2 pt-2">
      <a href="${ctx}/admin/auto-replies" class="btn btn-ghost">Huỷ</a>
      <button class="btn btn-primary">Lưu</button>
    </div>
  </form>
</div>
