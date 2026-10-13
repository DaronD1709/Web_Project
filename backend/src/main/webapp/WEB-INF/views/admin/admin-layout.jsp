<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- scope="request": file noi dung duoc nap bang <jsp:include> co page scope rieng, nen ctx phai o request scope thi no moi dung duoc --%>
<c:set var="ctx" value="${pageContext.request.contextPath}" scope="request" />
<%--
  KHUNG CHUNG CUA CMS. AdminView.render(...) forward toi day, kem cac attribute:
    adminContent : file noi dung trong views/admin/ (vd admin-product-list.jsp)     adminTitle : tieu de trang
    adminActive  : muc sidebar duoc to sang                                 partial    : true khi la request htmx (xem AdminView)
  - partial = false (go URL, F5, Back)  -> in CA TRANG: <html>, header, sidebar, noi dung.
  - partial = true  (bam link da hx-boost) -> chi in NOI DUNG + title/nav cap nhat bang hx-swap-oob; htmx nhet vao #adm-main.
--%>
<%-- Nga re lon: partial (request htmx) -> nhanh A chi tra manh; nguoc lai -> nhanh B tra ca trang --%>
<c:choose>
<%-- ===== NHANH A: chi tra NOI DUNG (khung da co san tren man hinh) ===== --%>
<c:when test="${partial}">
<%-- Tieu de tab: htmx doc the <title> trong manh tra ve va tu doi tieu de tab. ${adminTitle} do AdminView gan; phan sau dau gach la chu co dinh --%>
<title><c:out value="${adminTitle}"/> — Quản trị Nông Việt</title>
<%-- Noi dung trang: jsp:include (dong, tinh luc chay) vi ten file do Servlet chon; htmx nhet no vao #adm-main nho hx-target o nhanh B --%>
<jsp:include page="/WEB-INF/views/admin/${adminContent}" />
<%-- Nhung phan nam NGOAI #adm-main nhung can doi theo trang: tieu de tren header + muc dang chon o sidebar (hx-swap-oob) --%>
<%@ include file="/WEB-INF/views/admin/common/admin-title.jspf" %>
<%@ include file="/WEB-INF/views/admin/common/admin-nav.jspf" %>
</c:when>
<%-- ===== NHANH B: tra CA TRANG (go URL, F5, Back) ===== --%>
<c:otherwise>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title><c:out value="${adminTitle}"/> — Quản trị Nông Việt</title>
  <%-- head.jspf dung chung: nap Tailwind, daisyUI, htmx, design.css. admin.js nap 1 lan (defer = chay sau khi trang doc xong) --%>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
  <script src="${ctx}/static/js/admin.js?v=${applicationScope.assetVersion}" defer></script>
</head>
<body class="bg-base-200 min-h-screen">
<%-- KHUNG: hx-boost bien MOI link/form ben trong thanh request htmx; ket qua chi thay vao #adm-main (khong tai lai trang).
     hx-target="#adm-main": the dich mac dinh; hx-swap: thay phan ben trong va cuon len dau trang. Cac phan tu con thua ke 2 thuoc tinh nay. --%>
<div class="drawer lg:drawer-open" hx-boost="true" hx-target="#adm-main" hx-swap="innerHTML show:window:top">
  <input id="adm-drawer" type="checkbox" class="drawer-toggle" />

  <div class="drawer-content flex flex-col min-w-0 min-h-screen">
    <%@ include file="/WEB-INF/views/admin/common/admin-header.jspf" %>

    <%-- VUNG NOI DUNG: thay doi theo tung trang (${adminContent} la file do Servlet chon qua AdminView.render) --%>
    <main id="adm-main" class="flex-1 p-4 md:p-6 min-w-0">
      <jsp:include page="/WEB-INF/views/admin/${adminContent}" />
    </main>
  </div>

  <%@ include file="/WEB-INF/views/admin/common/admin-sidebar.jspf" %>
</div>

<%-- ===== TOAST: o trong de admin.js tha thong bao vao; the #flash (an) chua thong bao sau redirect ===== --%>
<%-- Toast: hien khi server gui header HX-Trigger (showToast) hoac khi co flash sau redirect (xem static/js/admin.js) --%>
<div id="toast-box" class="toast toast-end toast-bottom z-50"></div>
<c:if test="${not empty flash}"><div id="flash" hidden data-type="${flash[1]}"><c:out value="${flash[0]}"/></div></c:if>
</body>
</html>
</c:otherwise>
</c:choose>
