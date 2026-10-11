<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  NOI DUNG trang Admin > Khach hang. AdminCustomerServlet.showList truyen:
    filter : AdminCustomerFilter (tu khoa, trang thai, trang dang chon)     result : PageResult<Customer>
    stats  : id khach -> {so don, tong chi tieu}     counts : tong khach / so khach bi khoa
  Chuyen tu mockup/admin/customers.html.
--%>
<div class="max-w-7xl mx-auto space-y-4">

  <%-- ===== VUNG 1: TIEU DE TRANG ===== --%>
  <div>
    <h1 class="text-2xl font-bold">Tài khoản khách hàng</h1>
    <p class="text-sm text-base-content/60">Xem danh sách, khoá hoặc mở khoá tài khoản</p>
  </div>

  <%-- ===== VUNG 2: THANH LOC (tu tim khi go / doi o chon, khong tai lai trang) =====
       htmx: form tu gui GET /admin/users khi go o tim kiem (cho 0,3s), doi o chon, hoac nhan Enter. Servlet thay HX-Target = customer-list nen chi tra bang. --%>
  <form id="customer-filter" method="get" action="${ctx}/admin/users"
        hx-get="${ctx}/admin/users" hx-target="#customer-list" hx-push-url="true"
        hx-trigger="input[target.type=='search'] delay:300ms, change[target.tagName=='SELECT'], submit"
        class="card bg-base-100 border border-base-300 p-3 flex flex-row flex-wrap items-center gap-2">
    <label class="input input-sm w-full sm:w-72">
      <input type="search" name="q" value="<c:out value='${filter.keyword}'/>" placeholder="Tìm theo tên, email hoặc số điện thoại…" autocomplete="off">
    </label>
    <select name="status" class="select select-sm w-full sm:w-44">
      <option value="" ${filter.status == '' ? 'selected' : ''}>Mọi trạng thái</option>
      <option value="active" ${filter.status == 'active' ? 'selected' : ''}>Đang hoạt động</option>
      <option value="locked" ${filter.status == 'locked' ? 'selected' : ''}>Đã khoá</option>
    </select>
  </form>

  <%-- ===== VUNG 3: BANG KHACH HANG + PHAN TRANG (phan nay duoc htmx thay khi loc/doi trang) ===== --%>
  <div id="customer-list" class="card bg-base-100 border border-base-300 overflow-hidden">
    <jsp:include page="/WEB-INF/views/admin/fragments/admin-customer-table.jsp" />
  </div>
</div>
