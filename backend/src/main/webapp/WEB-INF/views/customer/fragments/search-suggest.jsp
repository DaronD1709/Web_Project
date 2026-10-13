<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%--
  MANH: danh sach goi y tim kiem (nhet vao #search-suggest o header). SearchSuggestServlet truyen: q (tu khoa), result (PageResult<Product>, toi da 6 dong).
  Moi dong la 1 nut co anh + ten + danh muc + gia; cuoi cung la nut "Xem tat ca" sang trang /products?q=...
  Du lieu (ten san pham, tu khoa) luon qua <c:out> / <c:param> de chong XSS.
--%>
<c:url var="allUrl" value="/products"><c:param name="q" value="${q}"/></c:url>
<c:choose>
  <c:when test="${empty result.items}">
    <div class="p-4 text-sm text-base-content/60 flex items-center gap-2"><i data-lucide="search-x" class="size-4"></i> Không có sản phẩm khớp “<c:out value="${q}"/>”</div>
  </c:when>
  <c:otherwise>
    <div class="p-2 flex flex-col gap-1">
      <c:forEach var="p" items="${result.items}">
        <a href="${ctx}/product?id=${p.id}" class="btn btn-ghost h-auto min-h-0 py-2 px-2 justify-start gap-3 w-full font-normal text-left flex-nowrap">
          <c:choose>
            <c:when test="${not empty p.imageUrl}"><img src="${ctx}/<c:out value='${p.imageUrl}'/>" alt="" class="size-12 shrink-0 rounded-field object-cover bg-base-200"></c:when>
            <c:otherwise><span class="ph ph-sm size-12 shrink-0 rounded-field"><i data-lucide="wheat" class="size-6 text-primary/50"></i></span></c:otherwise>
          </c:choose>
          <span class="flex-1 min-w-0">
            <span class="block truncate font-medium"><c:out value="${p.name}"/></span>
            <span class="block text-xs text-base-content/60"><c:out value="${p.category.name}"/><c:if test="${p.stockQuantity <= 0}"> · Hết hàng</c:if></span>
          </span>
          <span class="font-bold text-primary whitespace-nowrap"><fmt:formatNumber value="${p.price}" pattern="#,##0"/>₫</span>
        </a>
      </c:forEach>
      <a href="${allUrl}" class="btn btn-primary btn-sm btn-block mt-1">Xem tất cả ${result.totalCount} kết quả</a>
    </div>
  </c:otherwise>
</c:choose>
