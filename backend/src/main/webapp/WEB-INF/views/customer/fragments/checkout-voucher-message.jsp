<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:choose>
  <c:when test="${not empty voucherError}">
    <div class="alert alert-error alert-soft text-sm" role="alert"><c:out value="${voucherError}"/></div>
  </c:when>
  <c:when test="${not empty voucher}">
    <div class="alert alert-success alert-soft text-sm">✔ Đã áp dụng <b><c:out value="${voucher.code}"/></b>: giảm <fmt:formatNumber value="${discountAmount}" pattern="#,##0"/>₫.</div>
  </c:when>
</c:choose>
