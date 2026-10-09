<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<%--
  NOI DUNG trang Admin > Chi tiet don hang. AdminOrderServlet.showDetail truyen: order (Order da JOIN FETCH day du: khach, cac dong hang + san pham,
  thanh toan, dia chi, voucher). Chuyen tu mockup/admin/order-detail.html.
--%>
<c:set var="st" value="${order.status}" />
<%-- stepNo: don di toi buoc may tren thanh tien trinh (1 Dat hang, 2 Xac nhan, 3 Dang giao, 4 Hoan tat); don huy khong dung --%>
<c:set var="stepNo" value="${st == 'PENDING' ? 1 : st == 'CONFIRMED' ? 2 : st == 'SHIPPING' ? 3 : 4}" />
<c:set var="back" value="/admin/orders/detail?id=${order.id}" />

<div class="max-w-6xl mx-auto space-y-4">

  <%-- ===== VUNG 1: TIEU DE (ma don, ngay dat, nhan trang thai) ===== --%>
  <div class="flex flex-wrap items-start justify-between gap-2">
    <div>
      <a href="${ctx}/admin/orders" class="btn btn-outline btn-sm mb-2">← Danh sách đơn hàng</a>
      <h1 class="text-2xl font-bold">Đơn hàng <span class="font-mono text-primary">#${order.id}</span></h1>
      <div class="text-sm text-base-content/60">Đặt lúc ${order.orderDateText}</div>
    </div>
    <%@ include file="/WEB-INF/views/admin/common/admin-order-badge.jspf" %>
  </div>

  <%-- ===== VUNG 2: GHI CHU THEO TRANG THAI (yeu cau hoan hang / da huy / da hoan) ===== --%>
  <c:choose>
    <c:when test="${st == 'RETURN_REQUESTED'}">
      <div class="alert alert-error alert-soft"><div><div class="font-semibold">Khách yêu cầu hoàn hàng</div>
        <div class="text-sm">Lý do: “<c:out value="${order.returnReason}"/>”</div></div></div>
    </c:when>
    <c:when test="${st == 'CANCELLED'}"><div class="alert alert-soft text-sm">Đơn đã huỷ. Số lượng sản phẩm đã được cộng lại vào kho.</div></c:when>
    <c:when test="${st == 'RETURNED'}"><div class="alert alert-soft text-sm">Đã duyệt hoàn hàng. Số lượng sản phẩm đã được cộng lại vào kho.</div></c:when>
  </c:choose>

  <%-- ===== VUNG 3: THANH TIEN TRINH (don huy chi co 2 buoc: Dat hang, Da huy) ===== --%>
  <div class="card bg-base-100 border border-base-300 p-5">
    <ul class="steps w-full text-sm">
      <c:choose>
        <c:when test="${st == 'CANCELLED'}"><li class="step step-primary">Đặt hàng</li><li class="step step-error" data-content="✕">Đã huỷ</li></c:when>
        <c:otherwise>
          <li class="step ${stepNo >= 1 ? 'step-primary' : ''}">Đặt hàng</li><li class="step ${stepNo >= 2 ? 'step-primary' : ''}">Xác nhận</li>
          <li class="step ${stepNo >= 3 ? 'step-primary' : ''}">Đang giao</li><li class="step ${stepNo >= 4 ? 'step-primary' : ''}">Hoàn tất</li>
        </c:otherwise>
      </c:choose>
    </ul>
  </div>

  <div class="grid lg:grid-cols-[1fr_20rem] gap-4">
    <div class="space-y-4">

      <%-- ===== VUNG 4: CAC DONG HANG + TONG TIEN ===== --%>
      <section class="card bg-base-100 border border-base-300 overflow-hidden">
        <div class="overflow-x-auto">
          <table class="table">
            <thead><tr><th>Sản phẩm</th><th class="text-right">Đơn giá</th><th class="text-center">SL</th><th class="text-right">Thành tiền</th></tr></thead>
            <tbody>
              <c:forEach var="i" items="${order.items}">
                <tr>
                  <td>
                    <a class="font-medium hover:text-primary" href="${ctx}/admin/products/edit?id=${i.product.id}"><c:out value="${i.product.name}"/></a>
                    <div class="text-xs text-base-content/50">Tồn hiện tại: ${i.product.stockQuantity}</div>
                  </td>
                  <%-- Don gia la gia luc DAT (priceAtOrder), khong doi du san pham doi gia sau do --%>
                  <td class="text-right tabular-nums"><fmt:formatNumber value="${i.priceAtOrder}" pattern="#,##0"/>₫</td>
                  <td class="text-center">${i.quantity}</td>
                  <td class="text-right tabular-nums"><fmt:formatNumber value="${i.priceAtOrder * i.quantity}" pattern="#,##0"/>₫</td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
        <div class="p-5 border-t border-base-300 ml-auto w-full sm:w-96 text-sm space-y-1">
          <div class="flex justify-between"><span>Tạm tính</span><span class="tabular-nums"><fmt:formatNumber value="${order.calculateTotal()}" pattern="#,##0"/>₫</span></div>
          <div class="flex justify-between"><span>Phí vận chuyển</span><span>${order.shippingFee > 0 ? '' : 'Miễn phí'}<c:if test="${order.shippingFee > 0}"><fmt:formatNumber value="${order.shippingFee}" pattern="#,##0"/>₫</c:if></span></div>
          <c:if test="${order.discountAmount > 0}">
            <div class="flex justify-between text-success"><span>Voucher <c:out value="${order.voucher.code}"/></span><span class="tabular-nums">−<fmt:formatNumber value="${order.discountAmount}" pattern="#,##0"/>₫</span></div>
          </c:if>
          <div class="flex justify-between font-bold text-lg pt-1"><span>Tổng cộng</span><span class="text-primary tabular-nums"><fmt:formatNumber value="${order.totalAmount}" pattern="#,##0"/>₫</span></div>
        </div>
      </section>

      <%-- ===== VUNG 5: NUT HANH DONG (tuy trang thai). Moi nut la form POST /admin/orders/status (hx-boost + hx-confirm), xong ve lai trang nay ===== --%>
      <div class="flex flex-wrap gap-2 justify-end">
        <c:if test="${st == 'PENDING' or st == 'CONFIRMED'}">
          <form method="post" action="${ctx}/admin/orders/status" hx-confirm="Huỷ đơn #${order.id}? Số lượng sản phẩm được cộng lại vào kho, khách hàng nhận thông báo. Không thể hoàn tác.">
            <input type="hidden" name="id" value="${order.id}"><input type="hidden" name="status" value="CANCELLED"><input type="hidden" name="back" value="${back}">
            <button class="btn btn-outline btn-error">Huỷ đơn</button>
          </form>
        </c:if>
        <c:choose>
          <c:when test="${st == 'PENDING'}"><c:set var="nextStatus" value="CONFIRMED" /><c:set var="nextLabel" value="Xác nhận đơn" /></c:when>
          <c:when test="${st == 'CONFIRMED'}"><c:set var="nextStatus" value="SHIPPING" /><c:set var="nextLabel" value="Chuyển sang đang giao" /></c:when>
          <c:when test="${st == 'SHIPPING'}"><c:set var="nextStatus" value="COMPLETED" /><c:set var="nextLabel" value="Đánh dấu hoàn tất" /></c:when>
          <c:otherwise><c:set var="nextStatus" value="" /></c:otherwise>
        </c:choose>
        <c:if test="${not empty nextStatus}">
          <form method="post" action="${ctx}/admin/orders/status" hx-confirm="${nextLabel}? Hệ thống gửi thông báo cho khách hàng.">
            <input type="hidden" name="id" value="${order.id}"><input type="hidden" name="status" value="${nextStatus}"><input type="hidden" name="back" value="${back}">
            <button class="btn btn-primary">${nextLabel}</button>
          </form>
        </c:if>
        <%-- Khach da yeu cau hoan hang: Admin tu choi (quay lai Hoan tat) hoac duyet (Da hoan hang, cong lai kho) --%>
        <c:if test="${st == 'RETURN_REQUESTED'}">
          <form method="post" action="${ctx}/admin/orders/status" hx-confirm="Từ chối hoàn hàng? Đơn quay lại trạng thái Hoàn tất.">
            <input type="hidden" name="id" value="${order.id}"><input type="hidden" name="status" value="COMPLETED"><input type="hidden" name="back" value="${back}">
            <button class="btn btn-outline">Từ chối hoàn hàng</button>
          </form>
          <form method="post" action="${ctx}/admin/orders/status" hx-confirm="Duyệt hoàn hàng? Đơn chuyển sang Đã hoàn hàng và số lượng được cộng lại vào kho.">
            <input type="hidden" name="id" value="${order.id}"><input type="hidden" name="status" value="RETURNED"><input type="hidden" name="back" value="${back}">
            <button class="btn btn-error">Duyệt hoàn hàng</button>
          </form>
        </c:if>
      </div>
    </div>

    <aside class="space-y-4">
      <%-- ===== VUNG 6: KHACH HANG ===== --%>
      <section class="card bg-base-100 border border-base-300 p-5 text-sm space-y-1">
        <h2 class="font-semibold mb-1">Khách hàng</h2>
        <div class="font-medium"><c:out value="${order.customer.fullName}"/></div>
        <div><c:out value="${order.customer.email}"/></div>
        <div><c:out value="${order.customer.phone}"/></div>
      </section>

      <%-- ===== VUNG 7: DIA CHI GIAO HANG ===== --%>
      <section class="card bg-base-100 border border-base-300 p-5 text-sm space-y-1">
        <h2 class="font-semibold mb-1">Giao tới</h2>
        <c:choose>
          <c:when test="${not empty order.shippingAddress}">
            <div class="font-medium"><c:out value="${order.shippingAddress.recipientName}"/></div>
            <div><c:out value="${order.shippingAddress.phone}"/></div>
            <div class="text-base-content/70"><c:out value="${order.shippingAddress.street}"/>, <c:out value="${order.shippingAddress.city}"/></div>
          </c:when>
          <c:otherwise><div class="text-base-content/50">Không có địa chỉ.</div></c:otherwise>
        </c:choose>
      </section>

      <%-- ===== VUNG 8: THANH TOAN ===== --%>
      <section class="card bg-base-100 border border-base-300 p-5 text-sm space-y-1">
        <h2 class="font-semibold mb-1">Thanh toán</h2>
        <div>Phương thức: <b><c:out value="${order.payment.method}"/></b></div>
        <div>Trạng thái:
          <span class="badge badge-sm ${order.payment.status == 'SUCCESS' ? 'badge-success' : order.payment.status == 'FAILED' ? 'badge-neutral' : 'badge-warning'}">
            ${order.payment.status == 'SUCCESS' ? 'Đã thanh toán' : order.payment.status == 'FAILED' ? 'Thất bại' : 'Chưa thanh toán'}</span>
        </div>
      </section>
    </aside>
  </div>
</div>
