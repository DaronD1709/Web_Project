<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Dong "Hien thi 1-10 / 23 san pham". attribute: result, filter, oob (true khi tra kem sau khi xoa: thay vao #product-summary bang hx-swap-oob). --%>
<span id="product-summary" class="text-base-content/60"${oob ? ' hx-swap-oob="true"' : ''}>
  <c:choose>
    <c:when test="${result.totalCount == 0}">0 sản phẩm</c:when>
    <c:otherwise>Hiển thị ${(result.page - 1) * filter.pageSize + 1}–${result.page * filter.pageSize < result.totalCount ? result.page * filter.pageSize : result.totalCount}
      / ${result.totalCount} sản phẩm</c:otherwise>
  </c:choose>
</span>
