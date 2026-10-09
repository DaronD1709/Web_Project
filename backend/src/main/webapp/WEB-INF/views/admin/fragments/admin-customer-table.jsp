<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<fmt:setLocale value="vi_VN" />
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<%--
  MANH: bang khach hang + dong tong so + phan trang. Nam trong <div id="customer-list"> cua admin-customer-list.jsp; khi loc/doi trang
  AdminCustomerServlet chi tra rieng file nay de htmx thay noi dung #customer-list. Attribute: filter, result, stats, counts.
--%>
<c:choose>
  <%-- ===== Khong co khach nao khop ===== --%>
  <c:when test="${empty result.items}">
    <div class="p-12 text-center text-base-content/60"><div class="text-4xl">👥</div>Không có khách hàng phù hợp.</div>
  </c:when>
  <c:otherwise>
    <%-- ===== BANG: moi khach 1 dong (tai khoan bi khoa hien mo di) ===== --%>
    <c:set var="back" value="/admin/users?${filter.toQueryString()}&page=${result.page}" />
    <div class="overflow-x-auto">
      <table class="table">
        <thead><tr><th>Khách hàng</th><th>Liên hệ</th><th>Tham gia</th><th class="text-center">Đơn</th><th class="text-right">Tổng chi tiêu</th><th>Trạng thái</th><th class="text-right">Thao tác</th></tr></thead>
        <tbody>
          <c:forEach var="u" items="${result.items}">
            <tr class="hover ${u.active ? '' : 'opacity-70'}">
              <td>
                <div class="flex items-center gap-3">
                  <div class="avatar avatar-placeholder"><div class="bg-primary/15 text-primary w-9 rounded-full text-xs"><span>${fn:toUpperCase(fn:substring(u.fullName, 0, 1))}</span></div></div>
                  <a class="font-medium hover:text-primary" href="${ctx}/admin/users/detail?id=${u.id}"><c:out value="${u.fullName}"/></a>
                </div>
              </td>
              <td class="text-sm"><c:out value="${u.email}"/><div class="text-xs text-base-content/50"><c:out value="${u.phone}"/></div></td>
              <td class="text-sm">${u.createdAtText}</td>
              <%-- stats[u.id] = {so don, tong chi tieu (khong tinh don huy)}; khach chua co don thi khong co trong stats -> hien 0 --%>
              <td class="text-center">${empty stats[u.id] ? 0 : stats[u.id][0].intValue()}</td>
              <td class="text-right tabular-nums"><fmt:formatNumber value="${empty stats[u.id] ? 0 : stats[u.id][1]}" pattern="#,##0"/>₫</td>
              <td>
                <c:choose>
                  <c:when test="${u.active}"><span class="badge badge-success badge-sm whitespace-nowrap">Hoạt động</span></c:when>
                  <c:otherwise><span class="badge badge-error badge-sm whitespace-nowrap">🔒 Đã khoá</span></c:otherwise>
                </c:choose>
              </td>
              <%-- Nut Khoa/Mo khoa la form POST /admin/users/lock (hx-boost gui bang htmx, hx-confirm hoi truoc). "locked" la trang thai MONG MUON
                   (nguoc voi hien tai); server xong redirect ve "back" = dung bo loc va trang hien tai, kem toast. --%>
              <td class="text-right whitespace-nowrap">
                <a class="btn btn-ghost btn-xs" href="${ctx}/admin/users/detail?id=${u.id}">Xem</a>
                <form method="post" action="${ctx}/admin/users/lock" class="inline"
                      hx-confirm="${u.active ? 'Khoá' : 'Mở khoá'} tài khoản ${fn:escapeXml(u.fullName)}? ${u.active ? 'Khách sẽ không thể đăng nhập hay đặt hàng; đơn đang xử lý vẫn tiếp tục.' : 'Khách sẽ đăng nhập và mua hàng bình thường trở lại.'}">
                  <input type="hidden" name="id" value="${u.id}"><input type="hidden" name="locked" value="${u.active ? 'true' : 'false'}">
                  <input type="hidden" name="back" value="<c:out value='${back}'/>">
                  <button class="btn btn-ghost btn-xs ${u.active ? 'text-error' : 'text-success'}">${u.active ? 'Khoá' : 'Mở khoá'}</button>
                </form>
              </td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </div>
  </c:otherwise>
</c:choose>

<%-- ===== CHAN BANG: dong tong so + phan trang ===== --%>
<div class="flex flex-wrap items-center justify-between gap-2 p-3 border-t border-base-300 text-sm">
  <span class="text-base-content/60">
    <c:choose>
      <c:when test="${result.totalCount == 0}">0 khách hàng</c:when>
      <c:otherwise>Hiển thị ${(result.page - 1) * filter.pageSize + 1}–${result.page * filter.pageSize < result.totalCount ? result.page * filter.pageSize : result.totalCount} / ${result.totalCount} khách hàng</c:otherwise>
    </c:choose>
    · ${counts['locked']} tài khoản bị khoá
  </span>
  <c:if test="${result.totalPages > 1}">
    <%-- Link phan trang la link thuong; nam trong khung hx-boost nen htmx tu bien thanh request ngam, hx-target doi vung can thay thanh #customer-list --%>
    <div class="join">
      <c:forEach begin="1" end="${result.totalPages}" var="i">
        <a href="${ctx}/admin/users?${filter.toQueryString()}&page=${i}" hx-target="#customer-list"
           class="join-item btn btn-sm ${i == result.page ? 'btn-primary' : ''}">${i}</a>
      </c:forEach>
    </div>
  </c:if>
</div>
