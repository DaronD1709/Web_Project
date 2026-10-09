<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<%--
  NOI DUNG trang Admin > Thong ke doanh thu (GET /admin/statistics?days=). AdminStatisticsServlet truyen:
    days (7|14|30), rangeText     revenue, orders, avgOrder, cancelled, cancelRate : so lieu ky nay     revenueDelta, ordersDelta, avgDelta : % so voi ky truoc (null neu ky truoc = 0)
    revenueChart (theo ngay), categoryChart (theo danh muc) : ChartData     topProducts : List<TopProduct> (10 dong)
  Chuyen tu mockup/admin/statistics.html. Doanh thu = don dang xu ly + hoan tat, khong tinh don huy / hoan hang.
--%>
<div class="max-w-7xl mx-auto space-y-4">

  <%-- ===== VUNG 1: TIEU DE + CHON KHOANG THOI GIAN + XUAT CSV ===== --%>
  <div class="flex flex-wrap items-end justify-between gap-3">
    <div>
      <h1 class="text-2xl font-bold">Thống kê doanh thu</h1>
      <p class="text-sm text-base-content/60"><c:out value="${rangeText}"/></p>
    </div>
    <div class="flex flex-wrap items-center gap-2">
      <%-- Chon khoang: doi o chon la gui form GET (hx-boost doi trang khong nhay); requestSubmit() de form boost bat duoc su kien submit --%>
      <form method="get" action="${ctx}/admin/statistics">
        <select name="days" class="select select-sm w-44" onchange="this.form.requestSubmit()">
          <option value="7" ${days == 7 ? 'selected' : ''}>7 ngày qua</option>
          <option value="14" ${days == 14 ? 'selected' : ''}>14 ngày qua</option>
          <option value="30" ${days == 30 ? 'selected' : ''}>30 ngày qua</option>
        </select>
      </form>
      <%-- Tai file: link thuong, tat hx-boost de trinh duyet tai file that su thay vi nhet noi dung vao trang --%>
      <a href="${ctx}/admin/statistics/export?days=${days}" hx-boost="false" class="btn btn-outline btn-sm">Xuất CSV</a>
    </div>
  </div>

  <%-- ===== VUNG 2: 4 THE SO LIEU (kem % so voi ky truoc co do dai bang nhau) ===== --%>
  <div class="grid grid-cols-2 xl:grid-cols-4 gap-4">
    <div class="card bg-base-100 border border-base-300 p-4">
      <div class="text-sm text-base-content/60">Tổng doanh thu</div>
      <div class="mt-1 text-2xl font-bold tabular-nums"><fmt:formatNumber value="${revenue}" pattern="#,##0"/>₫</div>
      <div class="mt-2 text-xs"><c:set var="delta" value="${revenueDelta}" /><%@ include file="/WEB-INF/views/admin/common/admin-delta.jspf" %></div>
    </div>
    <div class="card bg-base-100 border border-base-300 p-4">
      <div class="text-sm text-base-content/60">Số đơn hàng</div>
      <div class="mt-1 text-2xl font-bold tabular-nums">${orders}</div>
      <div class="mt-2 text-xs"><c:set var="delta" value="${ordersDelta}" /><%@ include file="/WEB-INF/views/admin/common/admin-delta.jspf" %></div>
    </div>
    <div class="card bg-base-100 border border-base-300 p-4">
      <div class="text-sm text-base-content/60">Giá trị trung bình / đơn</div>
      <div class="mt-1 text-2xl font-bold tabular-nums"><fmt:formatNumber value="${avgOrder}" pattern="#,##0"/>₫</div>
      <div class="mt-2 text-xs"><c:set var="delta" value="${avgDelta}" /><%@ include file="/WEB-INF/views/admin/common/admin-delta.jspf" %></div>
    </div>
    <div class="card bg-base-100 border border-base-300 p-4">
      <div class="text-sm text-base-content/60">Tỷ lệ huỷ đơn</div>
      <div class="mt-1 text-2xl font-bold tabular-nums"><fmt:formatNumber value="${cancelRate}" pattern="0.0"/>%</div>
      <div class="mt-2 text-xs text-base-content/50">${cancelled} đơn bị huỷ</div>
    </div>
  </div>

  <%-- ===== VUNG 3: BIEU DO DOANH THU THEO NGAY ===== --%>
  <section class="card bg-base-100 border border-base-300 p-5" data-chart-card>
    <div class="flex items-start justify-between gap-2">
      <div><h2 class="font-semibold">Doanh thu theo ngày</h2><p class="text-xs text-base-content/60">Đơn đang xử lý và hoàn tất, không tính đơn huỷ/hoàn hàng</p></div>
      <button class="btn btn-ghost btn-xs" onclick="nvToggleChartTable(this)" aria-pressed="false">Xem dạng bảng</button>
    </div>
    <div class="mt-3"><c:set var="chart" value="${revenueChart}" /><%@ include file="/WEB-INF/views/admin/common/admin-chart-columns.jspf" %></div>
  </section>

  <div class="grid xl:grid-cols-2 gap-4">
    <%-- ===== VUNG 4: DOANH THU THEO DANH MUC ===== --%>
    <section class="card bg-base-100 border border-base-300 p-5" data-chart-card>
      <div class="flex items-start justify-between gap-2">
        <div><h2 class="font-semibold">Doanh thu theo danh mục</h2><p class="text-xs text-base-content/60">Sắp xếp từ cao xuống thấp</p></div>
        <button class="btn btn-ghost btn-xs" onclick="nvToggleChartTable(this)" aria-pressed="false">Xem dạng bảng</button>
      </div>
      <div class="mt-4"><c:set var="chart" value="${categoryChart}" /><c:set var="money" value="${true}" /><c:set var="unit" value="" /><%@ include file="/WEB-INF/views/admin/common/admin-chart-bars.jspf" %></div>
    </section>

    <%-- ===== VUNG 5: TOP 10 SAN PHAM BAN CHAY ===== --%>
    <section class="card bg-base-100 border border-base-300 overflow-hidden">
      <div class="px-5 py-4"><h2 class="font-semibold">Top 10 sản phẩm bán chạy</h2></div>
      <div class="overflow-x-auto">
        <table class="table table-sm">
          <thead><tr><th>#</th><th>Sản phẩm</th><th class="text-right">Đã bán</th><th class="text-right">Doanh thu</th></tr></thead>
          <tbody>
            <c:forEach var="t" items="${topProducts}" varStatus="vs">
              <tr class="hover">
                <td class="text-base-content/50">${vs.count}</td>
                <td><a class="link link-hover" href="${ctx}/admin/products/edit?id=${t.id}"><c:out value="${t.name}"/></a></td>
                <td class="text-right tabular-nums">${t.quantity}</td>
                <td class="text-right tabular-nums"><fmt:formatNumber value="${t.revenue}" pattern="#,##0"/>₫</td>
              </tr>
            </c:forEach>
            <c:if test="${empty topProducts}"><tr><td colspan="4" class="text-center text-base-content/50 py-6">Chưa có đơn hàng trong khoảng này.</td></tr></c:if>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</div>
