<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Trang /cart (chi cho khach da dang nhap). Attr tu CartServlet: cart, subtotal, shippingFee, total, error (neu co).
     Cac nut +/-/Xoa dung htmx: goi POST /cart roi chi doi #cart-content (khong reload trang). --%>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Giỏ hàng — Nông Việt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main class="max-w-7xl mx-auto px-4 py-6">
  <c:if test="${not empty error}"><div class="alert alert-error alert-soft text-sm mb-4"><c:out value="${error}"/></div></c:if>
  <div id="cart-content">
    <jsp:include page="/WEB-INF/views/customer/fragments/cart-content.jsp" />
  </div>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
