<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%--
  NOI DUNG trang Admin > Danh muc. AdminCategoryServlet truyen: categories (List<Category>), counts (id danh muc -> so san pham).
  Chuyen tu mockup/admin/categories.html (mockup dung hop thoai them/sua; ban that dung trang form rieng de co URL va bao loi tung o).
--%>
<div class="max-w-4xl mx-auto space-y-4">

  <%-- ===== VUNG 1: TIEU DE TRANG + NUT "THEM DANH MUC" ===== --%>
  <div class="flex flex-wrap items-center justify-between gap-2">
    <div>
      <h1 class="text-2xl font-bold">Danh mục sản phẩm</h1>
      <p class="text-sm text-base-content/60">Phân loại sản phẩm để khách dễ tìm kiếm</p>
    </div>
    <a href="${ctx}/admin/categories/new" class="btn btn-primary btn-sm">+ Thêm danh mục</a>
  </div>

  <%-- ===== VUNG 2: BANG DANH MUC ===== --%>
  <div class="card bg-base-100 border border-base-300 overflow-hidden">
    <c:choose>
      <c:when test="${empty categories}"><div class="p-12 text-center text-base-content/60"><div class="text-4xl">🏷️</div>Chưa có danh mục nào.</div></c:when>
      <c:otherwise>
        <div class="overflow-x-auto">
          <table class="table">
            <thead><tr><th>Danh mục</th><th>Mô tả</th><th class="text-center">Sản phẩm</th><th class="text-right">Thao tác</th></tr></thead>
            <tbody>
              <c:forEach var="cat" items="${categories}">
                <%-- counts[cat.id] = so san pham cua danh muc; danh muc rong khong co trong counts nen coi la 0 --%>
                <c:set var="n" value="${empty counts[cat.id] ? 0 : counts[cat.id]}" />
                <tr class="hover">
                  <td class="font-semibold"><c:out value="${cat.name}"/></td>
                  <td class="text-base-content/70"><c:out value="${cat.description}"/></td>
                  <%-- Bam so san pham de sang danh sach san pham da loc theo danh muc nay --%>
                  <td class="text-center"><a class="link" href="${ctx}/admin/products?cat=${cat.id}">${n}</a></td>
                  <td class="text-right whitespace-nowrap">
                    <a class="btn btn-ghost btn-xs" href="${ctx}/admin/categories/edit?id=${cat.id}">Sửa</a>
                    <c:choose>
                      <%-- Con san pham thi khong cho xoa (nut mo di + goi y); server cung tu choi neu ai co tinh gui yeu cau xoa --%>
                      <c:when test="${n > 0}">
                        <span class="tooltip tooltip-left" data-tip="Không xoá được: danh mục còn ${n} sản phẩm"><button class="btn btn-ghost btn-xs" disabled>Xoá</button></span>
                      </c:when>
                      <c:otherwise>
                        <%-- Xoa: form POST + hx-confirm (hx-boost gui bang htmx), xong server redirect ve danh sach kem toast --%>
                        <form method="post" action="${ctx}/admin/categories/delete" class="inline"
                              hx-confirm="Xoá danh mục &quot;${fn:escapeXml(cat.name)}&quot;? Danh mục chưa có sản phẩm nào nên xoá an toàn.">
                          <input type="hidden" name="id" value="${cat.id}">
                          <button class="btn btn-ghost btn-xs text-error">Xoá</button>
                        </form>
                      </c:otherwise>
                    </c:choose>
                  </td>
                </tr>
              </c:forEach>
            </tbody>
          </table>
        </div>
      </c:otherwise>
    </c:choose>
  </div>
</div>
