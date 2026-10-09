<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" />
<%--
  NOI DUNG trang Admin > Voucher. AdminVoucherServlet.showList truyen:
    vouchers : List<Voucher> theo tab dang chon     tab : khoa tab dang chon     tabs : cac tab {khoa, nhan}     tabCounts : so voucher moi tab
  Chuyen tu mockup/admin/vouchers.html. Trang thai cua voucher do Voucher.getState() tinh (active | soldout | expired | off).
--%>
<c:set var="back" value="/admin/vouchers?tab=${tab}" />

<div class="max-w-7xl mx-auto space-y-4">

  <%-- ===== VUNG 1: TIEU DE TRANG + NUT "TAO VOUCHER" ===== --%>
  <div class="flex flex-wrap items-center justify-between gap-2">
    <div>
      <h1 class="text-2xl font-bold">Voucher</h1>
      <p class="text-sm text-base-content/60">Mã giảm giá áp dụng ở bước thanh toán</p>
    </div>
    <a href="${ctx}/admin/vouchers/new" class="btn btn-primary btn-sm">+ Tạo voucher</a>
  </div>

  <%-- ===== VUNG 2: TAB TRANG THAI. Moi tab la link thuong ?tab=...; nam trong khung hx-boost nen htmx doi trang khong tai lai ===== --%>
  <div role="tablist" class="tabs tabs-border bg-base-100 rounded-box border border-base-300 px-2 overflow-x-auto">
    <c:forEach var="t" items="${tabs}">
      <a role="tab" href="${ctx}/admin/vouchers?tab=${t[0]}" class="tab gap-2 ${tab == t[0] ? 'tab-active' : ''}">
        <c:out value="${t[1]}"/><span class="badge badge-sm badge-ghost">${empty tabCounts[t[0]] ? 0 : tabCounts[t[0]]}</span>
      </a>
    </c:forEach>
  </div>

  <%-- ===== VUNG 3: BANG VOUCHER ===== --%>
  <div class="card bg-base-100 border border-base-300 overflow-hidden">
    <c:choose>
      <c:when test="${empty vouchers}">
        <div class="p-12 text-center text-base-content/60"><div class="text-4xl">🎟️</div>Không có voucher.</div>
      </c:when>
      <c:otherwise>
        <div class="overflow-x-auto">
          <table class="table">
            <thead><tr><th>Mã</th><th>Giảm</th><th class="text-right">Đơn tối thiểu</th><th>Đã dùng</th><th>Thời hạn</th><th>Trạng thái</th><th class="text-center">Bật/tắt</th><th class="text-right">Thao tác</th></tr></thead>
            <tbody>
              <c:forEach var="v" items="${vouchers}">
                <tr class="hover">
                  <td><span class="font-mono font-semibold"><c:out value="${v.code}"/></span></td>
                  <td>
                    <c:choose>
                      <c:when test="${v.discountType == 'PERCENTAGE'}"><fmt:formatNumber value="${v.discountValue}" pattern="#,##0.##"/>%</c:when>
                      <c:otherwise><fmt:formatNumber value="${v.discountValue}" pattern="#,##0"/>₫</c:otherwise>
                    </c:choose>
                  </td>
                  <td class="text-right tabular-nums"><fmt:formatNumber value="${v.minOrderValue}" pattern="#,##0"/>₫</td>
                  <%-- Da dung / phat hanh + thanh tien do (vang khi het luot) --%>
                  <td class="min-w-36">
                    <c:set var="pct" value="${v.quantityIssued > 0 ? v.quantityUsed * 100 / v.quantityIssued : 0}" />
                    <div class="text-xs tabular-nums mb-1">${v.quantityUsed} / ${v.quantityIssued} <span class="text-base-content/50">(<fmt:formatNumber value="${pct}" pattern="0"/>%)</span></div>
                    <progress class="progress ${pct >= 100 ? 'progress-warning' : 'progress-primary'} w-full h-1.5" value="${v.quantityUsed}" max="${v.quantityIssued}"></progress>
                  </td>
                  <td class="text-sm whitespace-nowrap">
                    ${fn:substring(v.startDateText, 8, 10)}/${fn:substring(v.startDateText, 5, 7)}/${fn:substring(v.startDateText, 0, 4)} →
                    ${fn:substring(v.endDateText, 8, 10)}/${fn:substring(v.endDateText, 5, 7)}/${fn:substring(v.endDateText, 0, 4)}
                  </td>
                  <td>
                    <c:set var="state" value="${v.state}" />
                    <span class="badge badge-sm whitespace-nowrap ${state == 'active' ? 'badge-success' : state == 'soldout' ? 'badge-warning' : state == 'expired' ? 'badge-neutral' : 'badge-ghost'}">
                      ${state == 'active' ? 'Đang hoạt động' : state == 'soldout' ? 'Hết lượt' : state == 'expired' ? 'Hết hạn' : 'Đã tắt'}</span>
                  </td>
                  <%-- Cong tac bat/tat. htmx: gat la form tu POST /admin/vouchers/toggle (hx-trigger="change"); "active" la gia tri MONG MUON
                       (nguoc voi hien tai) nen bam nhanh nhieu lan khong bi lech. Server redirect ve "back" (cung tab) kem toast. --%>
                  <td class="text-center">
                    <form method="post" action="${ctx}/admin/vouchers/toggle" hx-post="${ctx}/admin/vouchers/toggle" hx-trigger="change">
                      <input type="hidden" name="id" value="${v.id}">
                      <input type="hidden" name="active" value="${v.active ? 'false' : 'true'}">
                      <input type="hidden" name="back" value="${back}">
                      <input type="checkbox" class="toggle toggle-primary toggle-sm" ${v.active ? 'checked' : ''} aria-label="Bật/tắt <c:out value='${v.code}'/>">
                    </form>
                  </td>
                  <%-- Xoa: form POST + hx-confirm; voucher da co don dung thi server tu choi va bao loi --%>
                  <td class="text-right whitespace-nowrap">
                    <a class="btn btn-ghost btn-xs" href="${ctx}/admin/vouchers/edit?id=${v.id}">Sửa</a>
                    <form method="post" action="${ctx}/admin/vouchers/delete" class="inline"
                          hx-confirm="Xoá voucher &quot;${fn:escapeXml(v.code)}&quot;? Voucher đã được dùng trong đơn hàng sẽ không xoá được.">
                      <input type="hidden" name="id" value="${v.id}"><input type="hidden" name="back" value="${back}">
                      <button class="btn btn-ghost btn-xs text-error">Xoá</button>
                    </form>
                  </td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
      </c:otherwise>
    </c:choose>
  </div>
</div>
