<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  NOI DUNG trang Admin > San pham (khung header/sidebar do admin-layout.jsp lo). AdminProductServlet.showList truyen:
    filter     : ProductFilter (q, cat, stock, sort, page dang chon)
    result     : PageResult<Product>
    categories : List<Category>
  Chuyen tu mockup/admin/products.html.
--%>
<div class="space-y-4 max-w-7xl mx-auto">

  <%-- ===== VUNG 1: TIEU DE TRANG + NUT "THEM SAN PHAM" ===== --%>
  <div class="flex flex-wrap items-center justify-between gap-2">
    <div>
      <h1 class="text-2xl font-bold">Sản phẩm</h1>
      <p class="text-sm text-base-content/60">Thêm, sửa, xoá hàng hoá và theo dõi tồn kho</p>
    </div>
    <a href="${ctx}/admin/products/new" class="btn btn-primary btn-sm">+ Thêm sản phẩm</a>
  </div>

  <%-- ===== VUNG 2: THANH LOC (tu tim khi go / doi o chon, khong tai lai trang) =====
       htmx: form tu gui GET /admin/products khi (a) go o tim kiem xong 0,3 giay, (b) doi 1 o chon, (c) nhan Enter.
       hx-trigger gom 3 su kien cach nhau dau phay: (1) go o tim kiem roi cho 0,3s (delay: moi lan go lai dem lai nen khong gui moi phim),
       (2) doi 1 o chon, (3) nhan Enter. Phan [...] la dieu kien loc su kien, de o tim kiem khong kich hoat 2 lan.
       Servlet thay HX-Target = product-list nen chi tra bang (fragments/admin-product-table.jsp); hx-push-url cap nhat thanh dia chi. --%>
  <form id="product-filter" method="get" action="${ctx}/admin/products"
        hx-get="${ctx}/admin/products" hx-target="#product-list" hx-push-url="true"
        hx-trigger="input[target.type=='search'] delay:300ms, change[target.tagName=='SELECT'], submit"
        class="card bg-base-100 border border-base-300 p-3 flex flex-row flex-wrap items-center gap-2">
    <label class="input input-sm w-full sm:w-64">
      <input type="search" name="q" value="<c:out value='${filter.keyword}'/>" placeholder="Tìm theo tên sản phẩm…" autocomplete="off">
    </label>
    <select name="cat" class="select select-sm w-full sm:w-48">
      <option value="">Tất cả danh mục</option>
      <c:forEach var="cat" items="${categories}">
        <option value="${cat.id}" ${filter.categoryId == cat.id ? 'selected' : ''}><c:out value="${cat.name}"/></option>
      </c:forEach>
    </select>
    <select name="stock" class="select select-sm w-full sm:w-44">
      <option value="" ${filter.stock == '' ? 'selected' : ''}>Mọi tồn kho</option>
      <option value="in" ${filter.stock == 'in' ? 'selected' : ''}>Còn hàng</option>
      <option value="low" ${filter.stock == 'low' ? 'selected' : ''}>Sắp hết (≤ 5)</option>
      <option value="out" ${filter.stock == 'out' ? 'selected' : ''}>Hết hàng</option>
    </select>
    <select name="sort" class="select select-sm w-full sm:w-44 sm:ml-auto">
      <option value="new" ${filter.sort == 'new' ? 'selected' : ''}>Mới nhất</option>
      <option value="name" ${filter.sort == 'name' ? 'selected' : ''}>Tên A → Z</option>
      <option value="asc" ${filter.sort == 'asc' ? 'selected' : ''}>Giá thấp → cao</option>
      <option value="desc" ${filter.sort == 'desc' ? 'selected' : ''}>Giá cao → thấp</option>
      <option value="stock" ${filter.sort == 'stock' ? 'selected' : ''}>Tồn kho thấp trước</option>
    </select>
  </form>

  <%-- ===== VUNG 3: BANG SAN PHAM + PHAN TRANG (phan nay duoc htmx thay khi loc/doi trang) ===== --%>
  <div id="product-list" class="card bg-base-100 border border-base-300 overflow-hidden">
    <jsp:include page="/WEB-INF/views/admin/fragments/admin-product-table.jsp" />
  </div>
</div>
