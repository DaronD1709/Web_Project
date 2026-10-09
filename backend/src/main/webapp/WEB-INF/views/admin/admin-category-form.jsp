<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  NOI DUNG trang Admin > Them / Sua danh muc (1 file cho ca 2 che do). AdminCategoryServlet truyen:
    draft : Category dang hien tren form (rong khi them, danh muc that khi sua, gia tri vua go khi bi loi)
    errors : Map<ten o, loi> (chi co khi POST bi tu choi)     formError : loi chung khong gan voi o nao
--%>
<c:set var="editing" value="${not empty draft.id}" />
<c:set var="action" value="${ctx}/admin/categories/${editing ? 'edit?id=' : 'new'}${draft.id}" />

<div class="max-w-xl mx-auto space-y-4">

  <%-- ===== VUNG 1: LIEN KET QUAY LAI + TIEU DE ===== --%>
  <div>
    <a href="${ctx}/admin/categories" class="btn btn-outline btn-sm mb-2">← Danh sách danh mục</a>
    <h1 class="text-2xl font-bold">${editing ? 'Sửa danh mục' : 'Thêm danh mục'}</h1>
  </div>

  <c:if test="${not empty formError}"><div class="alert alert-error text-sm"><c:out value="${formError}"/></div></c:if>

  <%-- ===== VUNG 2: FORM (hx-boost ke thua tu khung: thanh cong -> redirect ve danh sach; loi -> tra lai form kem loi) ===== --%>
  <form method="post" action="${action}" novalidate class="card bg-base-100 border border-base-300 p-5 space-y-4">
    <label class="form-control w-full">
      <div class="label"><span class="label-text">Tên danh mục <span class="text-error">*</span></span></div>
      <input name="name" value="<c:out value='${draft.name}'/>" maxlength="100" autofocus
             class="input w-full ${not empty errors.name ? 'input-error' : ''}" placeholder="VD: Hạt giống">
      <c:if test="${not empty errors.name}"><div class="label"><span class="label-text-alt text-error">${errors.name}</span></div></c:if>
    </label>
    <label class="form-control w-full">
      <div class="label"><span class="label-text">Mô tả</span></div>
      <textarea name="description" rows="3" maxlength="255" class="textarea w-full ${not empty errors.description ? 'textarea-error' : ''}"
                placeholder="Vài chữ mô tả nhóm sản phẩm này"><c:out value="${draft.description}"/></textarea>
      <c:if test="${not empty errors.description}"><div class="label"><span class="label-text-alt text-error">${errors.description}</span></div></c:if>
    </label>

    <%-- ===== VUNG 3: NUT LUU / HUY ===== --%>
    <div class="flex justify-end gap-2 pt-2">
      <a href="${ctx}/admin/categories" class="btn btn-ghost">Huỷ</a>
      <button class="btn btn-primary">Lưu danh mục</button>
    </div>
  </form>
</div>
