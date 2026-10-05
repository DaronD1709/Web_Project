<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  Tra loi cho nut Xoa trong bang. Phan chinh de TRONG -> htmx thay dong san pham bang "khong co gi" (dong bien mat).
  Phan con lai la dong tong so da cap nhat, danh dau hx-swap-oob de htmx tu tim #product-summary ngoai dong do va thay the.
--%>
<%-- oob phai o request scope: jsp:include ben duoi co page scope rieng --%>
<c:set var="oob" value="${true}" scope="request" />
<jsp:include page="/WEB-INF/views/admin/fragments/product-summary.jsp" />
