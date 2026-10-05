<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<%--
  NOI DUNG trang Admin > Them / Sua san pham (1 file cho ca 2 che do). AdminProductServlet truyen:
    draft      : Product dang hien tren form (rong khi them, san pham that khi sua, gia tri vua go khi bi loi)
    errors     : Map<ten o, loi> (chi co khi POST bi tu choi)      formError : loi chung khong gan voi o nao
    categories : List<Category>
  Chuyen tu mockup/admin/product-form.html. Them: draft.id rong.
--%>
<c:set var="editing" value="${not empty draft.id}" />
<c:set var="action" value="${ctx}/admin/products/${editing ? 'edit?id=' : 'new'}${draft.id}" />
<fmt:formatNumber value="${draft.price}" pattern="0" var="priceText" />

<div class="max-w-5xl mx-auto space-y-4">

  <%-- ===== VUNG 1: LIEN KET QUAY LAI + TIEU DE ===== --%>
  <div>
    <a href="${ctx}/admin/products" class="text-sm link link-primary">← Danh sách sản phẩm</a>
    <h1 class="text-2xl font-bold">${editing ? 'Sửa sản phẩm #' : 'Thêm sản phẩm'}${draft.id}</h1>
  </div>

  <c:if test="${not empty formError}"><div class="alert alert-error text-sm"><c:out value="${formError}"/></div></c:if>

  <%-- Form multipart (co upload anh). hx-boost (ke thua tu khung): gui bang htmx; thanh cong -> server redirect ve danh sach,
       loi validate -> server tra lai form kem thong bao, htmx chi thay #adm-main. oninput/onchange cap nhat the "Xem truoc" (admin.js). --%>
  <form id="product-form" method="post" action="${action}" enctype="multipart/form-data" novalidate
        oninput="nvProductPreview()" onchange="nvProductPreview()" class="grid lg:grid-cols-[1fr_20rem] gap-4">

    <div class="space-y-4">

      <%-- ===== VUNG 2: THONG TIN CO BAN (ten, mo ta, gia, ton kho, danh muc) ===== --%>
      <section class="card bg-base-100 border border-base-300 p-5 space-y-4">
        <h2 class="font-semibold">Thông tin cơ bản</h2>

        <label class="form-control w-full">
          <div class="label"><span class="label-text">Tên sản phẩm <span class="text-error">*</span></span></div>
          <input name="name" value="<c:out value='${draft.name}'/>" class="input w-full ${not empty errors.name ? 'input-error' : ''}"
                 placeholder="VD: Phân bón NPK 20-20-15 (bao 5kg)" maxlength="255">
          <c:if test="${not empty errors.name}"><div class="label"><span class="label-text-alt text-error">${errors.name}</span></div></c:if>
        </label>

        <label class="form-control w-full">
          <div class="label"><span class="label-text">Mô tả</span></div>
          <textarea name="description" class="textarea w-full" rows="4" placeholder="Công dụng, thành phần, hướng dẫn sử dụng…"><c:out value="${draft.description}"/></textarea>
        </label>

        <div class="grid sm:grid-cols-3 gap-3">
          <label class="form-control">
            <div class="label"><span class="label-text">Giá bán (₫) <span class="text-error">*</span></span></div>
            <input name="price" type="number" min="0" step="any" value="${draft.price > 0 ? priceText : ''}"
                   class="input w-full ${not empty errors.price ? 'input-error' : ''}" placeholder="185000">
            <c:if test="${not empty errors.price}"><div class="label"><span class="label-text-alt text-error">${errors.price}</span></div></c:if>
          </label>
          <label class="form-control">
            <div class="label"><span class="label-text">Tồn kho <span class="text-error">*</span></span></div>
            <input name="stock" type="number" min="0" value="${draft.stockQuantity}"
                   class="input w-full ${not empty errors.stock ? 'input-error' : ''}" placeholder="0">
            <c:if test="${not empty errors.stock}"><div class="label"><span class="label-text-alt text-error">${errors.stock}</span></div></c:if>
          </label>
          <label class="form-control">
            <div class="label"><span class="label-text">Danh mục <span class="text-error">*</span></span></div>
            <select name="category" class="select w-full ${not empty errors.category ? 'select-error' : ''}">
              <option value="">Chọn danh mục…</option>
              <c:forEach var="cat" items="${categories}">
                <option value="${cat.id}" ${draft.category.id == cat.id ? 'selected' : ''}><c:out value="${cat.name}"/></option>
              </c:forEach>
            </select>
            <c:if test="${not empty errors.category}"><div class="label"><span class="label-text-alt text-error">${errors.category}</span></div></c:if>
          </label>
        </div>
      </section>

      <%-- ===== VUNG 3: HINH ANH (1 anh dai dien: Product co 1 cot imageUrl) ===== --%>
      <section class="card bg-base-100 border border-base-300 p-5 space-y-3">
        <div class="flex items-center justify-between">
          <h2 class="font-semibold">Hình ảnh sản phẩm</h2>
          <span class="text-xs text-base-content/50">JPG / PNG / WEBP · tối đa 2MB</span>
        </div>
        <%-- Anh dang dung (khi sua) + tuy chon xoa --%>
        <c:if test="${not empty draft.imageUrl}">
          <div id="current-image" class="flex items-center gap-4">
            <img src="${ctx}/${draft.imageUrl}" alt="" class="size-20 rounded-field object-cover">
            <label class="flex items-center gap-2 text-sm cursor-pointer">
              <input type="checkbox" name="removeImage" class="checkbox checkbox-sm"> Xoá ảnh hiện tại
            </label>
          </div>
        </c:if>
        <input type="file" name="image" accept="image/png,image/jpeg,image/webp" class="file-input w-full ${not empty errors.image ? 'file-input-error' : ''}">
        <c:if test="${not empty errors.image}"><div class="text-xs text-error">${errors.image}</div></c:if>
        <c:if test="${editing}"><p class="text-xs text-base-content/50">Chọn ảnh mới để thay ảnh hiện tại.</p></c:if>
      </section>
    </div>

    <aside class="space-y-4">

      <%-- ===== VUNG 4: XEM TRUOC TREN CUA HANG (JS cap nhat khi go; gia tri ban dau do server in san) ===== --%>
      <section class="card bg-base-100 border border-base-300 p-5">
        <h2 class="font-semibold mb-3">Xem trước trên cửa hàng</h2>
        <div class="card bg-base-100 border border-base-300">
          <div id="pv-img">
            <c:choose>
              <c:when test="${not empty draft.imageUrl}"><img src="${ctx}/${draft.imageUrl}" alt="" class="h-36 w-full object-cover rounded-t-[var(--radius-box)]"></c:when>
              <c:otherwise><div class="ph h-36 rounded-t-[var(--radius-box)]">🌾</div></c:otherwise>
            </c:choose>
          </div>
          <div class="p-4 space-y-1">
            <div class="text-xs text-base-content/60" id="pv-cat"><c:out value="${empty draft.category.name ? 'Danh mục' : draft.category.name}"/></div>
            <div class="font-semibold leading-snug" id="pv-name"><c:out value="${empty draft.name ? 'Tên sản phẩm' : draft.name}"/></div>
            <div class="text-lg font-bold text-primary" id="pv-price"><fmt:formatNumber value="${draft.price}" pattern="#,##0"/>₫</div>
            <div class="text-xs" id="pv-stock"></div>
          </div>
        </div>
      </section>

      <%-- ===== VUNG 5: NUT HANH DONG (Luu / Huy / Xoa) ===== --%>
      <section class="card bg-base-100 border border-base-300 p-5 space-y-2">
        <button class="btn btn-primary btn-block">Lưu sản phẩm</button>
        <a href="${ctx}/admin/products" class="btn btn-ghost btn-block">Huỷ</a>
        <%-- Nut Xoa (chi khi sua): htmx POST /admin/products/delete?redirect -> server tra HX-Redirect ve danh sach, hoac toast loi
             neu san pham da co trong don hang. hx-swap="none": khong thay noi dung nao tren trang. --%>
        <c:if test="${editing}">
          <button type="button" class="btn btn-outline btn-error btn-block"
                  hx-post="${ctx}/admin/products/delete" hx-vals='{"id":"${draft.id}","redirect":"1"}'
                  hx-confirm="Xoá sản phẩm này? Không thể hoàn tác." hx-target="this" hx-swap="none">Xoá sản phẩm</button>
        </c:if>
      </section>
    </aside>
  </form>
</div>
