<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<%--
  MANH: noi dung trang chi tiet / theo doi don cua KHACH. OrderServlet (/orders/detail va cac thao tac huy/hoan) truyen: order (da JOIN FETCH),
  canCancel, canReturn (dua tren Order.canMoveTo), trackingCsrfToken, va tuy luc: trackingMessage / trackingError / returnError / returnReason.
  Cung la phia khach cua chuc nang "Quan ly don hang": khi Admin doi trang thai don, trang nay tu cap nhat nho polling o the <section> ben duoi.
--%>
<%-- st: trang thai hien tai (admin-order-badge cua khach: order-badge.jspf doc bien nay). stepNo: don di toi buoc may tren thanh tien trinh. --%>
<c:set var="st" value="${order.status}" />
<c:set var="stepNo" value="${st == 'PENDING' ? 1 : st == 'CONFIRMED' ? 2 : st == 'SHIPPING' ? 3 : 4}" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%-- ===== VUNG BAO NGOAI: tu lam moi moi 1 giay (polling) de khach thay trang thai moi khi Admin doi ma khong phai F5 =====
     - hx-get toi chinh trang nay; Servlet thay HX-Target = order-detail-body nen chi tra lai MANH nay (khong tra ca trang).
     - hx-trigger="every 1s [dieu kien]": 1 giay hoi 1 lan, NHUNG chi khi KHONG co hop thoai (huy don / hoan hang) dang mo; neu dang mo thi bo qua lan do,
       de khach dang go ly do hoan hang khong bi mat chu.
     - hx-swap="outerHTML": thay chinh the <section> nay bang ban moi (ban moi lai co hx-trigger nen vong polling tiep tuc).
     - hx-sync="this:drop": neu lan hoi truoc chua xong thi bo lan hoi moi, khong chong chat request.
     - hx-history="false": khong luu manh nay vao lich su htmx (bam Back khong hien ban cu). --%>
<section id="order-detail-body" class="space-y-5 min-w-0" hx-history="false"
         hx-get="${ctx}/orders/detail?id=${order.id}"
         hx-trigger="every 1s [!document.querySelector('#order-detail-body dialog[open]')]"
         hx-target="this" hx-swap="outerHTML" hx-sync="this:drop">
    <c:if test="${not empty trackingMessage}"><div class="alert alert-success alert-soft"><c:out value="${trackingMessage}"/></div></c:if>
    <c:if test="${not empty trackingError}"><div class="alert alert-error alert-soft"><c:out value="${trackingError}"/></div></c:if>
    <div class="flex flex-wrap items-center justify-between gap-2">
      <div><a href="${ctx}/orders" class="btn btn-outline btn-sm mb-2"><i data-lucide="arrow-left" class="size-4"></i> Đơn hàng của tôi</a><h1 class="text-2xl font-bold">Đơn hàng <span class="font-mono text-primary">#${order.id}</span></h1><div class="text-sm text-base-content/60">Đặt lúc <c:out value="${order.orderDateText}"/></div></div>
      <%@ include file="/WEB-INF/views/customer/fragments/order-badge.jspf" %>
    </div>
    <%-- ===== THANH TIEN TRINH: Dat hang -> Xac nhan -> Dang giao -> Hoan tat (don huy chi co 2 buoc). Cap nhat theo stepNo moi 1 giay ===== --%>
    <div class="card bg-base-100 border border-base-300 p-5 overflow-x-auto">
      <ul class="steps w-full text-sm">
        <c:choose><c:when test="${st == 'CANCELLED'}"><li class="step step-primary">Đặt hàng</li><li class="step step-error" data-content="✕">Đã huỷ</li></c:when><c:otherwise><li class="step step-primary">Đặt hàng</li><li class="step ${stepNo >= 2 ? 'step-primary' : ''}">Xác nhận</li><li class="step ${stepNo >= 3 ? 'step-primary' : ''}">Đang giao</li><li class="step ${stepNo >= 4 ? 'step-primary' : ''}">Hoàn tất</li></c:otherwise></c:choose>
      </ul>
      <c:if test="${st == 'RETURN_REQUESTED' or st == 'RETURNED'}"><div class="alert alert-error alert-soft mt-4 text-sm"><div><c:out value="${st.label}"/><c:if test="${not empty order.returnReason}"><br>Lý do: <c:out value="${order.returnReason}"/></c:if></div></div></c:if>
      <c:if test="${st == 'CANCELLED' and order.payment.method == 'VNPay' and order.payment.status == 'SUCCESS'}"><div class="alert alert-warning alert-soft mt-4 text-sm">VNPay đã ghi nhận thanh toán cho đơn đã huỷ. Vui lòng liên hệ cửa hàng để đối soát/hoàn tiền.</div></c:if>
    </div>
    <%-- ===== DIA CHI NHAN HANG (trai) va THANH TOAN (phai): phuong thuc + trang thai thanh toan (COD chuyen sang "Da thanh toan" khi Admin danh dau Hoan tat) ===== --%>
    <div class="grid md:grid-cols-2 gap-5">
      <div class="card bg-base-100 border border-base-300 p-5 text-sm space-y-1 min-w-0 break-words"><h2 class="font-bold mb-1">Địa chỉ nhận hàng</h2><c:choose><c:when test="${not empty order.shippingAddress}"><div class="font-semibold"><c:out value="${order.shippingAddress.recipientName}"/></div><div><c:out value="${order.shippingAddress.phone}"/></div><div class="text-base-content/70"><c:out value="${order.shippingAddress.street}"/>, <c:out value="${order.shippingAddress.city}"/></div></c:when><c:otherwise><p>Chưa có thông tin địa chỉ.</p></c:otherwise></c:choose></div>
      <div class="card bg-base-100 border border-base-300 p-5 text-sm space-y-1 min-w-0"><h2 class="font-bold mb-1">Thanh toán</h2><div>Phương thức: <b><c:out value="${order.payment.method}" default="Chưa có thông tin"/></b></div><div>Trạng thái: <span class="badge badge-sm ${order.payment.status == 'SUCCESS' ? 'badge-success' : 'badge-warning'}"><c:choose><c:when test="${order.payment.status == 'SUCCESS'}">Đã thanh toán</c:when><c:when test="${order.payment.status == 'FAILED'}">Không thành công</c:when><c:when test="${order.payment.method == 'COD'}">Chưa thanh toán</c:when><c:otherwise>Chờ xác nhận</c:otherwise></c:choose></span></div></div>
    </div>
    <%-- ===== CAC DONG HANG + TONG TIEN: don gia la gia luc DAT (priceAtOrder); Tam tinh = tong hang, tru voucher, cong phi van chuyen = Tong cong ===== --%>
    <div class="card bg-base-100 border border-base-300 overflow-hidden">
      <div class="overflow-x-auto"><table class="table"><thead><tr><th>Sản phẩm</th><th>Đơn giá</th><th class="text-center">SL</th><th class="text-right">Thành tiền</th></tr></thead><tbody><c:forEach var="item" items="${order.items}"><tr><td><div class="font-medium min-w-40 break-words"><c:out value="${item.product.name}"/></div></td><td class="tabular-nums whitespace-nowrap"><fmt:formatNumber value="${item.priceAtOrder}" pattern="#,##0"/>₫</td><td class="text-center">${item.quantity}</td><td class="text-right tabular-nums whitespace-nowrap"><fmt:formatNumber value="${item.priceAtOrder * item.quantity}" pattern="#,##0"/>₫</td></tr></c:forEach></tbody></table></div>
      <div class="p-5 border-t border-base-300 ml-auto w-full sm:w-80 text-sm space-y-1">
        <div class="flex justify-between gap-2"><span>Tạm tính</span><span><fmt:formatNumber value="${order.calculateTotal()}" pattern="#,##0"/>₫</span></div>
        <div class="flex justify-between gap-2"><span>Phí vận chuyển</span><span><c:choose><c:when test="${order.shippingFee == 0}">Miễn phí</c:when><c:otherwise><fmt:formatNumber value="${order.shippingFee}" pattern="#,##0"/>₫</c:otherwise></c:choose></span></div>
        <c:if test="${order.discountAmount > 0}"><div class="flex justify-between gap-2 text-success"><span class="min-w-0 break-words">Voucher <c:out value="${order.voucher.code}"/></span><span class="shrink-0">−<fmt:formatNumber value="${order.discountAmount}" pattern="#,##0"/>₫</span></div></c:if>
        <div class="flex justify-between gap-2 font-bold text-lg pt-1"><span>Tổng cộng</span><span class="text-primary"><fmt:formatNumber value="${order.totalAmount}" pattern="#,##0"/>₫</span></div>
      </div>
    </div>
    <%-- ===== NUT HANH DONG: chi hien khi duoc phep (canCancel/canReturn do Order.canMoveTo quyet dinh). Bam nut mo hop thoai o duoi (orders.js xu ly data-order-dialog) ===== --%>
    <div class="flex flex-wrap gap-2 justify-end">
      <c:if test="${canCancel}"><button type="button" class="btn btn-outline btn-error" data-order-dialog="m-cancel">Huỷ đơn hàng</button></c:if>
      <c:if test="${canReturn}"><button type="button" class="btn btn-outline" data-order-dialog="m-return">Yêu cầu hoàn hàng</button></c:if><a href="${ctx}/checkout/success?orderId=${order.id}" class="btn btn-outline">Xem kết quả đặt hàng</a><a href="${ctx}/chat" class="btn btn-ghost">Chat với shop</a></div>

    <%-- ===== HOP THOAI HUY DON: form gui bang htmx POST /orders/cancel kem ma CSRF (trackingCsrfToken); ket qua thay lai ca vung #order-detail-body ===== --%>
    <c:if test="${canCancel}">
      <dialog id="m-cancel" class="modal">
        <div class="modal-box">
          <h2 class="font-bold text-lg">Huỷ đơn hàng #${order.id}?</h2>
          <p class="py-4">Bạn có chắc muốn huỷ đơn hàng này? Bạn có thể tạo đơn mới nếu muốn tiếp tục mua sắm.</p>
          <c:if test="${order.payment.method == 'VNPay' and order.payment.status == 'SUCCESS'}">
            <div class="alert alert-warning alert-soft text-sm">Đơn đã thanh toán VNPay. Huỷ đơn không tự hoàn tiền; vui lòng liên hệ cửa hàng để đối soát/hoàn tiền.</div>
          </c:if>
          <form action="${ctx}/orders/cancel" method="post" class="modal-action"
                hx-post="${ctx}/orders/cancel" hx-target="#order-detail-body" hx-swap="outerHTML"
                hx-sync="#order-detail-body:replace" hx-disabled-elt="find button">
            <input type="hidden" name="id" value="${order.id}">
            <input type="hidden" name="trackingCsrfToken" value="${trackingCsrfToken}">
            <button type="button" class="btn" data-close-order-dialog="m-cancel">Giữ đơn hàng</button>
            <button type="submit" class="btn btn-error">Xác nhận huỷ</button>
          </form>
        </div>
      </dialog>
    </c:if>
    <%-- ===== HOP THOAI YEU CAU HOAN HANG: bat buoc nhap ly do (toi da 255 ky tu). Neu server tu choi (422) thi hop thoai tu mo lai kem loi va giu nguyen ly do da go ===== --%>
    <c:if test="${canReturn}">
      <dialog id="m-return" class="modal" ${not empty returnError ? 'open data-order-dialog-reopen' : ''}>
        <div class="modal-box">
          <h2 class="font-bold text-lg">Yêu cầu hoàn hàng #${order.id}</h2>
          <p class="py-4">Vui lòng ghi rõ lý do. Cửa hàng sẽ xem xét yêu cầu trước khi xác nhận hoàn hàng.</p>
          <form action="${ctx}/orders/return" method="post"
                hx-post="${ctx}/orders/return" hx-target="#order-detail-body" hx-swap="outerHTML"
                hx-sync="#order-detail-body:replace" hx-disabled-elt="find button">
            <input type="hidden" name="id" value="${order.id}">
            <input type="hidden" name="trackingCsrfToken" value="${trackingCsrfToken}">
            <label class="label mb-2" for="return-reason">Lý do hoàn hàng</label><%-- mb-2: chua khoang cach duoi nhan, daisyUI 5 khong con tu cach nhan va o nhap --%>
            <textarea id="return-reason" name="reason" class="textarea textarea-bordered w-full" rows="3"
                      maxlength="255" required placeholder="Nhập lý do hoàn hàng..."><c:out value="${returnReason}"/></textarea>
            <c:if test="${not empty returnError}"><p class="text-error text-sm mt-1"><c:out value="${returnError}"/></p></c:if>
            <div class="modal-action">
              <button type="button" class="btn" data-close-order-dialog="m-return">Đóng</button>
              <button type="submit" class="btn btn-primary">Gửi yêu cầu</button>
            </div>
          </form>
        </div>
      </dialog>
    </c:if>
</section>
