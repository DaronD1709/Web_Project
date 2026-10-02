<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<%--
  Trang /products. Du lieu do ProductServlet truyen qua request attribute:
    filter      : ProductFilter (bo loc dang chon)
    result      : PageResult<Product> (result.items, result.totalCount, result.page, result.totalPages)
    categories  : List<Category>
  Chuyen tu mockup/products.html: du lieu JS gia -> vong <c:forEach> doc du lieu that.
  QUAN TRONG: du lieu nguoi dung/admin nhap (ten SP, tu khoa) luon di qua <c:out> de chong XSS.
--%>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Sản phẩm — NôngViệt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main class="max-w-7xl mx-auto px-4 py-6">
  <div class="grid grid-cols-1 lg:grid-cols-[16rem_minmax(0,1fr)] gap-6">

    <%-- Bo loc: gui GET /products, cac o input trung ten tham so ma ProductServlet doc --%>
    <aside>
      <form id="filter" method="get" action="${ctx}/products" class="card bg-base-100 border border-base-300 p-4 space-y-5">
        <input type="hidden" name="q" value="<c:out value='${filter.keyword}'/>">
        <div>
          <div class="font-semibold mb-2">Danh mục</div>
          <label class="flex items-center gap-2 py-1 cursor-pointer">
            <input type="radio" name="cat" value="" class="radio radio-primary radio-sm" ${empty filter.categoryId ? 'checked' : ''}> Tất cả
          </label>
          <c:forEach var="cat" items="${categories}">
            <label class="flex items-center gap-2 py-1 cursor-pointer">
              <input type="radio" name="cat" value="${cat.id}" class="radio radio-primary radio-sm" ${filter.categoryId == cat.id ? 'checked' : ''}>
              <c:out value="${cat.name}"/>
            </label>
          </c:forEach>
        </div>
        <div>
          <div class="font-semibold mb-2">Khoảng giá (₫)</div>
          <div class="flex items-center gap-2">
            <input name="min" type="number" min="0" placeholder="Từ" value="${filter.minPrice != null ? filter.minPrice.longValue() : ''}" class="input input-sm w-full">
            <span>–</span>
            <input name="max" type="number" min="0" placeholder="Đến" value="${filter.maxPrice != null ? filter.maxPrice.longValue() : ''}" class="input input-sm w-full">
          </div>
        </div>
        <label class="flex items-center gap-2 cursor-pointer">
          <input type="checkbox" name="instock" value="on" class="checkbox checkbox-primary checkbox-sm" ${filter.inStockOnly ? 'checked' : ''}> Chỉ hiện còn hàng
        </label>
        <button class="btn btn-primary btn-block btn-sm">Áp dụng bộ lọc</button>
        <a href="${ctx}/products" class="btn btn-ghost btn-block btn-xs">Xoá bộ lọc</a>
      </form>
    </aside>

    <section>
      <div class="flex flex-wrap items-center justify-between gap-3 mb-4">
        <div>
          <h1 class="text-xl font-bold">
            <c:choose>
              <c:when test="${not empty filter.keyword}">Kết quả cho “<c:out value="${filter.keyword}"/>”</c:when>
              <c:otherwise>Tất cả sản phẩm</c:otherwise>
            </c:choose>
          </h1>
          <div class="text-sm text-base-content/60">${result.totalCount} sản phẩm</div>
        </div>
        <%-- Select nam ngoai <form> nhung thuoc ve form#filter (thuoc tinh form=) va tu gui khi doi gia tri --%>
        <label class="select select-sm w-56"><span class="label">Sắp xếp</span>
          <select name="sort" form="filter" onchange="this.form.submit()">
            <option value="new" ${filter.sort == 'new' ? 'selected' : ''}>Mới nhất</option>
            <option value="asc" ${filter.sort == 'asc' ? 'selected' : ''}>Giá: thấp → cao</option>
            <option value="desc" ${filter.sort == 'desc' ? 'selected' : ''}>Giá: cao → thấp</option>
          </select>
        </label>
      </div>

      <c:choose>
        <c:when test="${empty result.items}">
          <div class="card bg-base-100 border border-dashed border-base-300 p-12 text-center">
            <div class="text-5xl">🔍</div>
            <div class="font-semibold mt-2">Không tìm thấy sản phẩm phù hợp</div>
            <p class="text-sm text-base-content/60">Thử bỏ bớt bộ lọc hoặc đổi từ khoá khác.</p>
          </div>
        </c:when>
        <c:otherwise>
          <div class="grid grid-cols-2 md:grid-cols-3 gap-4">
            <c:forEach var="p" items="${result.items}">
              <div class="card bg-base-100 border border-base-300 hover:shadow-lg transition">
                <div class="relative">
                  <c:choose>
                    <c:when test="${not empty p.imageUrl}">
                      <img src="${ctx}/${p.imageUrl}" alt="<c:out value='${p.name}'/>" class="h-44 w-full object-cover rounded-t-[var(--radius-box)]">
                    </c:when>
                    <c:otherwise>
                      <div class="ph rounded-t-[var(--radius-box)] h-44">🌾</div>
                    </c:otherwise>
                  </c:choose>
                  <c:if test="${p.stockQuantity == 0}"><span class="badge badge-neutral absolute top-2 right-2">Hết hàng</span></c:if>
                  <c:if test="${p.stockQuantity > 0 && p.stockQuantity <= 5}"><span class="badge badge-warning absolute top-2 right-2">Chỉ còn ${p.stockQuantity}</span></c:if>
                </div>
                <div class="card-body p-4 gap-1">
                  <span class="text-xs text-base-content/60"><c:out value="${p.category.name}"/></span>
                  <span class="font-semibold leading-snug line-clamp-2 min-h-10"><c:out value="${p.name}"/></span>
                  <span class="text-lg font-bold text-primary"><fmt:formatNumber value="${p.price}" pattern="#,##0"/>₫</span>
                  <%-- TODO: khi co CartServlet -> hx-post="${ctx}/cart" hx-vals='{"action":"add","productId":"${p.id}","qty":"1"}' --%>
                  <button type="button" class="btn btn-primary btn-sm mt-2" ${p.stockQuantity == 0 ? 'disabled' : ''}>
                    ${p.stockQuantity == 0 ? 'Hết hàng' : 'Thêm vào giỏ'}
                  </button>
                </div>
              </div>
            </c:forEach>
          </div>

          <%-- Phan trang: giu nguyen bo loc hien tai nho filter.toQueryString() --%>
          <c:if test="${result.totalPages > 1}">
            <div class="join flex justify-center mt-8">
              <c:forEach begin="1" end="${result.totalPages}" var="i">
                <a href="${ctx}/products?${filter.toQueryString()}&page=${i}"
                   class="join-item btn btn-sm ${i == result.page ? 'btn-primary' : ''}">${i}</a>
              </c:forEach>
            </div>
          </c:if>
        </c:otherwise>
      </c:choose>
    </section>
  </div>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
