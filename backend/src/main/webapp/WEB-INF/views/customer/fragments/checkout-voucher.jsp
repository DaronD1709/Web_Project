<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<jsp:include page="/WEB-INF/views/customer/fragments/checkout-voucher-message.jsp" />
<%-- Đổi tổng tiền và mã được gửi khi đặt hàng cùng với kết quả áp dụng. --%>
<div id="checkout-summary" hx-swap-oob="innerHTML">
  <jsp:include page="/WEB-INF/views/customer/fragments/checkout-summary.jsp" />
</div>
