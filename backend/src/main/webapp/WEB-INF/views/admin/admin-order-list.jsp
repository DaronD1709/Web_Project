<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  NOI DUNG trang Admin > Don hang (khung header/sidebar do admin-layout.jsp lo). AdminOrderServlet.showList truyen:
    filter : AdminOrderFilter (tab, tu khoa, thanh toan, khoang ngay, trang dang chon)     result : PageResult<Order>
    tabs   : cac tab {khoa, nhan}     tabCounts : so don moi tab     itemCounts : id don -> so mat hang
  Chuyen tu mockup/admin/orders.html.
--%>
<div class="max-w-7xl mx-auto space-y-4">

  <%-- ===== VUNG 1: TIEU DE TRANG ===== --%>
  <div>
    <h1 class="text-2xl font-bold">Đơn hàng</h1>
    <p class="text-sm text-base-content/60">Xác nhận, cập nhật trạng thái giao hàng, huỷ đơn và duyệt hoàn hàng</p>
  </div>

  <%-- ===== VUNG 2: THANH LOC (tu tim khi go / doi o chon, khong tai lai trang) =====
       htmx: form tu gui GET /admin/orders khi go o tim kiem (cho 0,3s), doi o chon/ngay, hoac nhan Enter. Servlet thay HX-Target = order-list
       nen chi tra phan tab + bang. Tab dang chon khong nam o day ma o the <input hidden form="order-filter"> trong vung 3 (xem fragment). --%>
  <form id="order-filter" method="get" action="${ctx}/admin/orders"
        hx-get="${ctx}/admin/orders" hx-target="#order-list" hx-push-url="true"
        hx-trigger="input[target.type=='search'] delay:300ms, change[target.tagName=='SELECT'], change[target.type=='date'], submit"
        class="card bg-base-100 border border-base-300 p-3 flex flex-row flex-wrap items-center gap-2">
    <label class="input input-sm w-full sm:w-72">
      <input type="search" name="q" value="<c:out value='${filter.keyword}'/>" placeholder="Tìm theo mã đơn, tên hoặc email khách…" autocomplete="off">
    </label>
    <select name="pay" class="select select-sm w-full sm:w-44">
      <option value="" ${filter.pay == '' ? 'selected' : ''}>Mọi thanh toán</option>
      <option value="cod" ${filter.pay == 'cod' ? 'selected' : ''}>COD</option>
      <option value="vnpay" ${filter.pay == 'vnpay' ? 'selected' : ''}>VNPay</option>
    </select>
    <input type="date" name="from" value="${filter.from}" class="input input-sm w-full sm:w-40" aria-label="Từ ngày">
    <span class="text-base-content/50 hidden sm:inline">→</span>
    <input type="date" name="to" value="${filter.to}" class="input input-sm w-full sm:w-40" aria-label="Đến ngày">
  </form>

  <%-- ===== VUNG 3: TAB TRANG THAI + BANG DON HANG + PHAN TRANG (phan nay duoc htmx thay khi loc/doi tab/doi trang) ===== --%>
  <div id="order-list" class="space-y-4">
    <jsp:include page="/WEB-INF/views/admin/fragments/admin-order-table.jsp" />
  </div>
</div>
