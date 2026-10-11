<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%--
  MANH: bang san pham + dong tong so + phan trang. Nam trong <div id="product-list"> cua admin-product-list.jsp; khi loc/doi trang
  AdminProductServlet chi tra rieng file nay de htmx thay noi dung #product-list. Attribute: filter, result.
--%>
<%-- Khong co san pham nao khop bo loc thi hien thong bao, nguoc lai hien bang --%>
<c:choose>
  <%-- ===== Khong co ket qua ===== --%>
  <c:when test="${empty result.items}">
    <div class="p-12 text-center text-base-content/60"><div class="text-4xl">📦</div>Không có sản phẩm phù hợp.</div>
  </c:when>
  <c:otherwise>
    <%-- ===== BANG: moi san pham 1 dong ===== --%>
    <div class="overflow-x-auto">
      <table class="table">
        <thead><tr><th>Sản phẩm</th><th>Danh mục</th><th class="text-right">Giá</th><th class="text-center">Tồn kho</th><th class="text-right">Thao tác</th></tr></thead>
        <tbody>
          <c:forEach var="p" items="${result.items}">
            <tr id="product-row-${p.id}" class="hover">
              <%-- Cot san pham: anh nho + ten (bam de sua) + ma --%>
              <td>
                <div class="flex items-center gap-3">
                  <c:choose>
                    <%-- Co anh thi hien anh (duong dan /uploads/... do ImageServlet tra), chua co thi hien o gia lap --%>
                    <c:when test="${not empty p.imageUrl}"><img src="${ctx}/${p.imageUrl}" alt="" class="size-12 rounded-field object-cover shrink-0"></c:when>
                    <c:otherwise><div class="ph ph-sm size-12 rounded-field shrink-0">🌾</div></c:otherwise>
                  </c:choose>
                  <div>
                    <a href="${ctx}/admin/products/edit?id=${p.id}" class="font-medium hover:text-primary"><c:out value="${p.name}"/></a>
                    <div class="text-xs text-base-content/50">Mã SP #${p.id}</div>
                  </div>
                </div>
              </td>
              <td><c:out value="${p.category.name}"/></td>
              <td class="text-right tabular-nums"><fmt:formatNumber value="${p.price}" pattern="#,##0"/>₫</td>
              <%-- Cot ton kho: het hang / sap het (<= 5) / con binh thuong --%>
              <td class="text-center">
                <c:choose>
                  <c:when test="${p.stockQuantity == 0}"><span class="badge badge-neutral badge-sm">Hết hàng</span></c:when>
                  <c:when test="${p.stockQuantity <= 5}"><span class="badge badge-warning badge-sm">Còn ${p.stockQuantity}</span></c:when>
                  <c:otherwise><span class="tabular-nums">${p.stockQuantity}</span></c:otherwise>
                </c:choose>
              </td>
              <%-- Cot thao tac. htmx: nut Xoa -> hoi xac nhan -> POST /admin/products/delete -> server tra manh rong nen DONG BIEN MAT
                   (hx-swap outerHTML thay dong bang noi dung rong); hx-include gui kem bo loc de server tinh lai dong tong so. --%>
              <td class="text-right whitespace-nowrap">
                <a href="${ctx}/admin/products/edit?id=${p.id}" class="btn btn-ghost btn-xs">Sửa</a>
                <button type="button" class="btn btn-ghost btn-xs text-error"
                        hx-post="${ctx}/admin/products/delete" hx-vals='{"id":"${p.id}"}' hx-include="#product-filter"
                        hx-confirm="Xoá sản phẩm &quot;${fn:escapeXml(p.name)}&quot;? Các đánh giá của sản phẩm cũng bị xoá theo."
                        hx-target="#product-row-${p.id}" hx-swap="outerHTML" hx-disabled-elt="this">Xoá</button>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>
  </c:otherwise>
</c:choose>

<%-- ===== CHAN BANG: dong tong so + phan trang (nam ngoai c:choose nen luon hien) ===== --%>
<div class="flex flex-wrap items-center justify-between gap-2 p-3 border-t border-base-300 text-sm">
  <jsp:include page="/WEB-INF/views/admin/fragments/admin-product-summary.jsp" />
  <c:if test="${result.totalPages > 1}">
    <%-- Link phan trang la link thuong; vi nam trong khung hx-boost nen htmx tu bien thanh request ngam, hx-target doi vung can thay
         tu #adm-main (ke thua) thanh #product-list (chi thay bang). --%>
    <div class="join">
      <c:forEach begin="1" end="${result.totalPages}" var="i">
        <a href="${ctx}/admin/products?${filter.toQueryString()}&page=${i}" hx-target="#product-list"
           class="join-item btn btn-sm ${i == result.page ? 'btn-primary' : ''}">${i}</a>
      </c:forEach>
    </div>
  </c:if>
</div>
