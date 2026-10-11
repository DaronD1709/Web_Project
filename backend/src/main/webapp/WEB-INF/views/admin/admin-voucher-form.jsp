<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<%--
  NOI DUNG trang Admin > Tao / Sua voucher (1 file cho ca 2 che do). AdminVoucherServlet truyen:
    draft : Voucher dang hien tren form (mac dinh khi tao, voucher that khi sua, gia tri vua go khi bi loi)
    errors : Map<ten o, loi> (chi co khi POST bi tu choi)     formError : loi chung khong gan voi o nao
  Chuyen tu mockup/admin/voucher-form.html. Entity chua co "gioi han moi khach" nen khong co o do.
--%>
<c:set var="editing" value="${not empty draft.id}" />
<c:set var="action" value="${ctx}/admin/vouchers/${editing ? 'edit?id=' : 'new'}${draft.id}" />
<c:set var="isPercent" value="${draft.discountType == 'PERCENTAGE'}" />
<%-- So dang nguyen khong dau phan cach de dien vao <input type="number"> (in thang double se ra 1.0E7 voi so lon). Phai dung locale en_US:
     voi vi_VN dau thap phan la dau phay ("12,5") ma o type="number" chi nhan dau cham. Xong lai tra ve vi_VN cho cac so hien thi (1.000.000). --%>
<fmt:setLocale value="en_US" />
<fmt:formatNumber value="${draft.discountValue}" pattern="0.##" var="valueText" groupingUsed="false" />
<fmt:formatNumber value="${draft.minOrderValue}" pattern="0" var="minText" groupingUsed="false" />
<fmt:setLocale value="vi_VN" />

<div class="max-w-4xl mx-auto space-y-4">

  <%-- ===== VUNG 1: LIEN KET QUAY LAI + TIEU DE ===== --%>
  <div>
    <a href="${ctx}/admin/vouchers" class="btn btn-outline btn-sm mb-2">← Danh sách voucher</a>
    <h1 class="text-2xl font-bold">${editing ? 'Sửa voucher ' : 'Tạo voucher'}<c:out value="${editing ? draft.code : ''}"/></h1>
  </div>

  <c:if test="${not empty formError}"><div class="alert alert-error text-sm"><c:out value="${formError}"/></div></c:if>

  <%-- Form gui bang htmx (hx-boost ke thua tu khung): thanh cong -> redirect ve danh sach; loi -> tra lai form kem loi.
       oninput/onchange cap nhat the "Xem truoc" (nvVoucherPreview trong admin.js). --%>
  <form id="voucher-form" method="post" action="${action}" novalidate oninput="nvVoucherPreview()" onchange="nvVoucherPreview()"
        class="grid lg:grid-cols-[1fr_18rem] gap-4">

    <%-- ===== VUNG 2: THONG TIN VOUCHER ===== --%>
    <section class="card bg-base-100 border border-base-300 p-5 space-y-4">
      <label class="form-control w-full">
        <div class="label"><span class="label-text">Mã voucher <span class="text-error">*</span></span></div>
        <input name="code" value="<c:out value='${draft.code}'/>" class="input w-full font-mono uppercase ${not empty errors.code ? 'input-error' : ''}" placeholder="VD: NONG10" maxlength="20">
        <c:if test="${not empty errors.code}"><div class="label"><span class="label-text-alt text-error">${errors.code}</span></div></c:if>
      </label>

      <%-- Loai giam: phan tram hoac so tien co dinh (khop DiscountType) --%>
      <div>
        <div class="label"><span class="label-text">Loại giảm giá</span></div>
        <div class="join">
          <input class="join-item btn btn-sm" type="radio" name="type" value="percent" aria-label="Phần trăm (%)" ${isPercent ? 'checked' : ''}>
          <input class="join-item btn btn-sm" type="radio" name="type" value="fixed" aria-label="Số tiền cố định (₫)" ${isPercent ? '' : 'checked'}>
        </div>
      </div>

      <div class="grid sm:grid-cols-2 gap-3">
        <label class="form-control">
          <div class="label"><span class="label-text">Giá trị giảm <span class="text-error">*</span></span></div>
          <label class="input w-full ${not empty errors.value ? 'input-error' : ''}"><input name="value" type="number" min="1" step="any" value="${draft.discountValue > 0 ? valueText : ''}" placeholder="10"><span class="label" id="unit">${isPercent ? '%' : '₫'}</span></label>
          <c:if test="${not empty errors.value}"><div class="label"><span class="label-text-alt text-error">${errors.value}</span></div></c:if>
        </label>
        <label class="form-control">
          <div class="label"><span class="label-text">Đơn tối thiểu (₫)</span></div>
          <input name="min" type="number" min="0" step="1000" value="${draft.minOrderValue > 0 ? minText : ''}" class="input w-full ${not empty errors.min ? 'input-error' : ''}" placeholder="300000">
          <c:if test="${not empty errors.min}"><div class="label"><span class="label-text-alt text-error">${errors.min}</span></div></c:if>
        </label>
        <label class="form-control">
          <div class="label"><span class="label-text">Số lượng phát hành <span class="text-error">*</span></span></div>
          <input name="issued" type="number" min="1" value="${draft.quantityIssued > 0 ? draft.quantityIssued : ''}" class="input w-full ${not empty errors.issued ? 'input-error' : ''}" placeholder="100">
          <c:if test="${not empty errors.issued}"><div class="label"><span class="label-text-alt text-error">${errors.issued}</span></div></c:if>
        </label>
        <div></div>
        <label class="form-control">
          <div class="label"><span class="label-text">Ngày bắt đầu</span></div>
          <input name="start" type="date" value="${draft.startDateText}" class="input w-full ${not empty errors.start ? 'input-error' : ''}">
          <c:if test="${not empty errors.start}"><div class="label"><span class="label-text-alt text-error">${errors.start}</span></div></c:if>
        </label>
        <label class="form-control">
          <div class="label"><span class="label-text">Ngày hết hạn</span></div>
          <input name="end" type="date" value="${draft.endDateText}" class="input w-full ${not empty errors.end ? 'input-error' : ''}">
          <c:if test="${not empty errors.end}"><div class="label"><span class="label-text-alt text-error">${errors.end}</span></div></c:if>
        </label>
      </div>

      <label class="flex items-center gap-3">
        <input name="active" type="checkbox" class="toggle toggle-primary" ${draft.active ? 'checked' : ''}>
        <span class="text-sm">Bật voucher (khách có thể dùng ngay khi còn hạn và còn lượt)</span>
      </label>

      <%-- ===== VUNG 3: NUT LUU / HUY ===== --%>
      <div class="flex justify-end gap-2 pt-2">
        <a href="${ctx}/admin/vouchers" class="btn btn-ghost">Huỷ</a>
        <button class="btn btn-primary">Lưu voucher</button>
      </div>
    </section>

    <%-- ===== VUNG 4: XEM TRUOC (JS cap nhat khi go; gia tri ban dau do server in san) ===== --%>
    <aside class="card bg-base-100 border border-base-300 p-5 h-fit space-y-3">
      <h2 class="font-semibold">Xem trước</h2>
      <div class="rounded-box border-2 border-dashed border-accent bg-accent/15 p-4 text-center">
        <div class="text-xs text-base-content/60">Mã giảm giá</div>
        <div class="font-mono text-2xl font-bold" id="pv-code"><c:out value="${empty draft.code ? 'MÃ' : draft.code}"/></div>
        <div class="text-lg font-semibold text-primary mt-1" id="pv-val">Giảm <c:choose><c:when test="${draft.discountValue <= 0}">…</c:when><c:when test="${isPercent}">${valueText}%</c:when><c:otherwise><fmt:formatNumber value="${draft.discountValue}" pattern="#,##0"/>₫</c:otherwise></c:choose></div>
        <div class="text-xs text-base-content/60 mt-1" id="pv-min">${draft.minOrderValue > 0 ? 'Đơn từ ' : 'Không yêu cầu đơn tối thiểu'}${draft.minOrderValue > 0 ? minText : ''}</div>
      </div>
      <p class="text-xs text-base-content/60">Khi checkout, hệ thống kiểm tra theo thứ tự: còn hạn → còn lượt → đạt giá trị tối thiểu.</p>
    </aside>
  </form>
</div>
