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
  <%-- GIAI THICH CAC THUOC TINH HTMX CUA FORM LOC:
       - method="get" action=...    : cach gui truyen thong, chi dung khi trinh duyet khong co JavaScript (phuong an du phong).
       - hx-get="/admin/orders"     : khi duoc kich hoat, gui GET kem TOAN BO cac o cua form lam tham so (?q=..&pay=..&from=..&to=..&status=..).
       - hx-target="#order-list"    : ket qua (chi phan tab + bang) duoc nhet vao <div id="order-list"> chu khong thay ca trang.
       - hx-push-url="true"         : dong thoi doi thanh dia chi + lich su trinh duyet thanh URL vua gui -> bam F5, Back hoac gui link van ra dung bo loc.
       - hx-trigger="..."           : GOM 4 DIEU KIEN kich hoat, cach nhau dau phay (dieu kien nao xay ra cung gui):
           1. input[target.type=='search'] delay:300ms  -> go vao O TIM KIEM (target = o vua go); cho 300ms sau ky tu cuoi roi moi gui (debounce: go "nguyen" chi gui 1 lan, khong gui 6 lan)
           2. change[target.tagName=='SELECT']          -> chon xong O CHON (thanh toan COD/VNPay) thi gui ngay
           3. change[target.type=='date']               -> chon xong O NGAY (tu ngay / den ngay) thi gui ngay
           4. submit                                    -> bam Enter trong o tim kiem
         Phan trong [...] la bo loc JavaScript: chi cho su kien di tiep neu dung loai o, de moi loai o chi kich hoat dung 1 lan (neu khong, o ngay vua phat "input" vua phat "change" se gui trung). --%>
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
    <span class="text-base-content/50 hidden sm:inline" aria-hidden="true"><i data-lucide="arrow-right" class="size-4"></i></span>
    <input type="date" name="to" value="${filter.to}" class="input input-sm w-full sm:w-40" aria-label="Đến ngày">
  </form>

  <%-- ===== VUNG 3: TAB TRANG THAI + BANG DON HANG + PHAN TRANG (phan nay duoc htmx thay khi loc/doi tab/doi trang) ===== --%>
  <div id="order-list" class="space-y-4">
    <jsp:include page="/WEB-INF/views/admin/fragments/admin-order-table.jsp" />
  </div>
</div>
