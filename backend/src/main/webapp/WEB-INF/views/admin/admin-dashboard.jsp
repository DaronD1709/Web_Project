<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<%--
  NOI DUNG trang Admin > Tong quan (GET /admin). AdminDashboardServlet truyen:
    pending, returnRequests, chatWaiting : so viec can xu ly     todayRevenue, revenueDelta (% so voi hom qua, null neu hom qua = 0), shipping, lowCount : cac the so lieu
    revenueChart (ChartData 14 ngay), statusChart, topChart : bieu do     recentOrders (6 don moi nhat), lowStock (San pham ton kho thap)
  Chuyen tu mockup/admin/dashboard.html. Bieu do ve bang HTML/CSS (common/admin-chart-*.jspf), khong dung thu vien JS.
--%>
<div class="space-y-6 max-w-7xl mx-auto">

  <%-- ===== VUNG 1: TIEU DE + LINK SANG THONG KE CHI TIET ===== --%>
  <div class="flex flex-wrap items-end justify-between gap-2">
    <div>
      <h1 class="text-2xl font-bold">Tổng quan</h1>
      <p class="text-sm text-base-content/60">Số liệu hôm nay so với hôm qua</p>
    </div>
    <a href="${ctx}/admin/statistics" class="btn btn-sm btn-outline">Xem thống kê chi tiết →</a>
  </div>

  <%-- ===== VUNG 2: VIEC CAN XU LY (chi hien muc co viec; bam de toi dung trang xu ly) ===== --%>
  <div class="grid gap-3 md:grid-cols-2">
    <c:if test="${pending > 0}"><a href="${ctx}/admin/orders?status=pending" class="alert alert-warning alert-soft text-sm justify-between hover:brightness-95"><span><b>Cần xử lý:</b> ${pending} đơn đang chờ xác nhận</span><span aria-hidden="true">→</span></a></c:if>
    <c:if test="${returnRequests > 0}"><a href="${ctx}/admin/orders?status=return" class="alert alert-error alert-soft text-sm justify-between hover:brightness-95"><span><b>Cần xử lý:</b> ${returnRequests} yêu cầu hoàn hàng chờ duyệt</span><span aria-hidden="true">→</span></a></c:if>
    <c:if test="${chatWaiting > 0}"><a href="${ctx}/admin/chat" class="alert alert-info alert-soft text-sm justify-between hover:brightness-95"><span><b>Cần xử lý:</b> ${chatWaiting} khách đang chờ trả lời chat</span><span aria-hidden="true">→</span></a></c:if>
  </div>

  <%-- ===== VUNG 3: 4 THE SO LIEU ===== --%>
  <div class="grid grid-cols-2 xl:grid-cols-4 gap-4">
    <a href="${ctx}/admin/statistics" class="card bg-base-100 border border-base-300 p-4 hover:border-primary transition">
      <div class="text-sm text-base-content/60">Doanh thu hôm nay</div>
      <div class="mt-1 text-2xl font-bold tabular-nums"><fmt:formatNumber value="${todayRevenue}" pattern="#,##0"/>₫</div>
      <div class="mt-2 text-xs">
        <c:choose>
          <c:when test="${empty revenueDelta}"><span class="text-base-content/50">Chưa có số liệu hôm qua để so sánh</span></c:when>
          <c:otherwise>
            <span class="${revenueDelta >= 0 ? 'text-success' : 'text-error'}">${revenueDelta >= 0 ? '▲ +' : '▼ −'}<c:choose><c:when test="${revenueDelta > 999 or revenueDelta < -999}">999+</c:when><c:otherwise><fmt:formatNumber value="${revenueDelta >= 0 ? revenueDelta : -revenueDelta}" pattern="0.0"/></c:otherwise></c:choose>%</span>
            <span class="text-base-content/50">so với hôm qua</span>
          </c:otherwise>
        </c:choose>
      </div>
    </a>
    <a href="${ctx}/admin/orders?status=pending" class="card bg-base-100 border border-base-300 p-4 hover:border-primary transition">
      <div class="text-sm text-base-content/60">Đơn chờ xác nhận</div><div class="mt-1 text-2xl font-bold tabular-nums">${pending}</div>
      <div class="mt-2 text-xs text-base-content/50">Cần xử lý sớm</div></a>
    <a href="${ctx}/admin/orders?status=shipping" class="card bg-base-100 border border-base-300 p-4 hover:border-primary transition">
      <div class="text-sm text-base-content/60">Đang giao</div><div class="mt-1 text-2xl font-bold tabular-nums">${shipping}</div>
      <div class="mt-2 text-xs text-base-content/50">Chưa hoàn tất</div></a>
    <a href="${ctx}/admin/products?stock=low" class="card bg-base-100 border border-base-300 p-4 hover:border-primary transition">
      <div class="text-sm text-base-content/60">Sản phẩm sắp hết</div><div class="mt-1 text-2xl font-bold tabular-nums">${lowCount}</div>
      <div class="mt-2 text-xs text-warning">⚠ Tồn kho ≤ 5</div></a>
  </div>

  <div class="grid xl:grid-cols-3 gap-4">
    <%-- ===== VUNG 4: BIEU DO DOANH THU 14 NGAY ===== --%>
    <section class="min-w-0 card bg-base-100 border border-base-300 p-5 xl:col-span-2" data-chart-card>
      <div class="flex items-start justify-between gap-2">
        <div><h2 class="font-semibold">Doanh thu ${revenueDays} ngày gần nhất</h2><p class="text-xs text-base-content/60">Không tính đơn đã huỷ và hoàn hàng</p></div>
        <button class="btn btn-ghost btn-xs" onclick="nvToggleChartTable(this)" aria-pressed="false">Xem dạng bảng</button>
      </div>
      <div class="mt-3"><c:set var="chart" value="${revenueChart}" /><%@ include file="/WEB-INF/views/admin/common/admin-chart-columns.jspf" %></div>
    </section>

    <%-- ===== VUNG 5: DON THEO TRANG THAI ===== --%>
    <section class="min-w-0 card bg-base-100 border border-base-300 p-5" data-chart-card>
      <div class="flex items-start justify-between gap-2">
        <div><h2 class="font-semibold">Đơn hàng theo trạng thái</h2><p class="text-xs text-base-content/60">Tất cả đơn trong hệ thống</p></div>
        <button class="btn btn-ghost btn-xs" onclick="nvToggleChartTable(this)" aria-pressed="false">Xem dạng bảng</button>
      </div>
      <div class="mt-4"><c:set var="chart" value="${statusChart}" /><c:set var="unit" value=" đơn" /><c:set var="money" value="${false}" /><%@ include file="/WEB-INF/views/admin/common/admin-chart-bars.jspf" %></div>
    </section>
  </div>

  <div class="grid xl:grid-cols-3 gap-4">
    <%-- ===== VUNG 6: DON HANG MOI NHAT ===== --%>
    <section class="min-w-0 card bg-base-100 border border-base-300 xl:col-span-2 overflow-hidden">
      <div class="flex items-center justify-between px-5 py-4"><h2 class="font-semibold">Đơn hàng mới nhất</h2><a href="${ctx}/admin/orders" class="link link-primary text-sm">Xem tất cả</a></div>
      <div class="overflow-x-auto">
        <table class="table table-sm">
          <thead><tr><th>Mã</th><th>Khách hàng</th><th class="text-right">Tổng tiền</th><th>Trạng thái</th></tr></thead>
          <tbody>
            <c:forEach var="o" items="${recentOrders}">
              <tr class="hover">
                <td><a class="link link-primary font-mono" href="${ctx}/admin/orders/detail?id=${o.id}">#${o.id}</a></td>
                <td><c:out value="${o.customer.fullName}"/></td>
                <td class="text-right tabular-nums"><fmt:formatNumber value="${o.totalAmount}" pattern="#,##0"/>₫</td>
                <td><c:set var="st" value="${o.status}" /><%@ include file="/WEB-INF/views/admin/common/admin-order-badge.jspf" %></td>
              </tr>
            </c:forEach>
            <c:if test="${empty recentOrders}"><tr><td colspan="4" class="text-center text-base-content/50 py-6">Chưa có đơn hàng.</td></tr></c:if>
          </tbody>
        </table>
      </div>
    </section>

    <%-- ===== VUNG 7: SAN PHAM SAP HET HANG (ton kho <= 15, it nhat truoc) ===== --%>
    <section class="min-w-0 card bg-base-100 border border-base-300 overflow-hidden">
      <div class="flex items-center justify-between px-5 py-4"><h2 class="font-semibold">Sắp hết hàng</h2><a href="${ctx}/admin/products?stock=low" class="link link-primary text-sm">Xem tất cả</a></div>
      <div class="overflow-x-auto">
        <table class="table table-sm">
          <thead><tr><th>Sản phẩm</th><th class="text-right">Tồn</th></tr></thead>
          <tbody>
            <c:forEach var="p" items="${lowStock}">
              <tr class="hover">
                <td><a class="link link-hover" href="${ctx}/admin/products/edit?id=${p.id}"><c:out value="${p.name}"/></a></td>
                <td class="text-right"><c:choose>
                  <c:when test="${p.stockQuantity == 0}"><span class="badge badge-neutral badge-sm whitespace-nowrap">Hết hàng</span></c:when>
                  <c:otherwise><span class="badge badge-sm ${p.stockQuantity <= 5 ? 'badge-warning' : 'badge-ghost'}">${p.stockQuantity}</span></c:otherwise>
                </c:choose></td>
              </tr>
            </c:forEach>
            <c:if test="${empty lowStock}"><tr><td colspan="2" class="text-center text-base-content/50 py-6">Không có sản phẩm nào sắp hết.</td></tr></c:if>
          </tbody>
        </table>
      </div>
    </section>
  </div>

  <%-- ===== VUNG 8: SAN PHAM BAN CHAY NHAT (30 ngay qua, theo so luong) ===== --%>
  <section class="min-w-0 card bg-base-100 border border-base-300 p-5" data-chart-card>
    <div class="flex items-start justify-between gap-2">
      <div><h2 class="font-semibold">Sản phẩm bán chạy nhất</h2><p class="text-xs text-base-content/60">30 ngày qua, theo số lượng đã bán</p></div>
      <button class="btn btn-ghost btn-xs" onclick="nvToggleChartTable(this)" aria-pressed="false">Xem dạng bảng</button>
    </div>
    <div class="mt-4"><c:set var="chart" value="${topChart}" /><c:set var="unit" value=" đã bán" /><c:set var="money" value="${false}" /><%@ include file="/WEB-INF/views/admin/common/admin-chart-bars.jspf" %></div>
  </section>
</div>
