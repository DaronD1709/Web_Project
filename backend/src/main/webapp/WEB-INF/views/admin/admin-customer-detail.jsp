<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" />
<%--
  NOI DUNG trang Admin > Chi tiet khach hang. AdminCustomerServlet.showDetail truyen:
    customer : Customer     stat : {so don, tong chi tieu} (null neu chua co don)     orders : 5 don gan nhat (Order)
  Thay cho hop thoai "Xem" cua mockup/admin/customers.html (dung 1 trang rieng de co URL, bam Back duoc).
--%>
<c:set var="back" value="/admin/users/detail?id=${customer.id}" />
<div class="max-w-3xl mx-auto space-y-4">

  <%-- ===== VUNG 1: LIEN KET QUAY LAI ===== --%>
  <a href="${ctx}/admin/users" class="text-sm link link-primary">← Danh sách khách hàng</a>

  <%-- ===== VUNG 2: THE KHACH HANG (ten, lien he, trang thai, nut khoa/mo khoa) ===== --%>
  <section class="card bg-base-100 border border-base-300 p-5 flex flex-row flex-wrap items-center gap-4">
    <div class="avatar avatar-placeholder"><div class="bg-primary text-primary-content w-14 rounded-full"><span>${fn:toUpperCase(fn:substring(customer.fullName, 0, 1))}</span></div></div>
    <div class="flex-1 min-w-0">
      <h1 class="text-xl font-bold"><c:out value="${customer.fullName}"/>
        <c:if test="${not customer.active}"><span class="badge badge-error badge-sm align-middle ml-1">🔒 Đã khoá</span></c:if></h1>
      <div class="text-sm text-base-content/60"><c:out value="${customer.email}"/> · <c:out value="${customer.phone}"/></div>
    </div>
    <form method="post" action="${ctx}/admin/users/lock"
          hx-confirm="${customer.active ? 'Khoá' : 'Mở khoá'} tài khoản này? ${customer.active ? 'Khách sẽ không thể đăng nhập hay đặt hàng; đơn đang xử lý vẫn tiếp tục.' : 'Khách sẽ đăng nhập và mua hàng bình thường trở lại.'}">
      <input type="hidden" name="id" value="${customer.id}"><input type="hidden" name="locked" value="${customer.active ? 'true' : 'false'}"><input type="hidden" name="back" value="${back}">
      <button class="btn ${customer.active ? 'btn-outline btn-error' : 'btn-success'}">${customer.active ? 'Khoá tài khoản' : 'Mở khoá'}</button>
    </form>
  </section>

  <%-- ===== VUNG 3: 3 SO LIEU (so don, tong chi tieu, ngay tham gia) ===== --%>
  <div class="grid grid-cols-3 gap-3 text-center">
    <div class="card bg-base-100 border border-base-300 p-4"><div class="text-2xl font-bold">${empty stat ? 0 : stat[0].intValue()}</div><div class="text-xs text-base-content/60">Đơn hàng</div></div>
    <div class="card bg-base-100 border border-base-300 p-4"><div class="text-2xl font-bold tabular-nums"><fmt:formatNumber value="${empty stat ? 0 : stat[1]}" pattern="#,##0"/>₫</div><div class="text-xs text-base-content/60">Tổng chi tiêu (không tính đơn huỷ)</div></div>
    <div class="card bg-base-100 border border-base-300 p-4"><div class="text-lg font-bold pt-1">${customer.createdAtText}</div><div class="text-xs text-base-content/60">Tham gia</div></div>
  </div>

  <%-- ===== VUNG 4: DON HANG GAN DAY (bam ma don de sang chi tiet don) ===== --%>
  <section class="card bg-base-100 border border-base-300 p-5">
    <h2 class="font-semibold mb-2">Đơn hàng gần đây</h2>
    <c:choose>
      <c:when test="${empty orders}"><div class="text-sm text-base-content/60 py-3">Chưa có đơn hàng.</div></c:when>
      <c:otherwise>
        <ul class="divide-y divide-base-300 text-sm">
          <c:forEach var="o" items="${orders}">
            <li class="py-2 flex items-center justify-between gap-2">
              <a class="link link-primary font-mono" href="${ctx}/admin/orders/detail?id=${o.id}">#${o.id}</a>
              <span class="text-base-content/50">${o.orderDateText}</span>
              <span><c:set var="st" value="${o.status}" /><%@ include file="/WEB-INF/views/admin/common/admin-order-badge.jspf" %></span>
              <span class="tabular-nums"><fmt:formatNumber value="${o.totalAmount}" pattern="#,##0"/>₫</span>
            </li>
          </c:forEach>
        </ul>
      </c:otherwise>
    </c:choose>
    <p class="text-xs text-base-content/50 mt-3">Quản trị viên không xem được mật khẩu của khách hàng.</p>
  </section>
</div>
