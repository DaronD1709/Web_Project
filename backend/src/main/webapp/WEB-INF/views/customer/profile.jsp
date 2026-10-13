<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Hồ sơ cá nhân — NôngViệt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main class="max-w-7xl mx-auto px-4 py-6 grid grid-cols-1 lg:grid-cols-[15rem_minmax(0,1fr)] gap-6">
  <aside class="min-w-0"><c:set var="activeAccount" value="profile" /><%@ include file="/WEB-INF/views/common/account-nav.jspf" %></aside>
  <section class="space-y-5 max-w-2xl min-w-0 w-full">
    <h1 class="text-2xl font-bold">Hồ sơ cá nhân</h1>
    <jsp:include page="/WEB-INF/views/customer/fragments/profile-info.jsp" />
    <jsp:include page="/WEB-INF/views/customer/fragments/profile-password.jsp" />
  </section>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
<script>
  // htmx mac dinh khong swap 422; hai form nay can hien loi theo tung o.
  document.body.addEventListener('htmx:beforeSwap', function (event) {
    var detail = event.detail;
    if (detail.xhr.status === 422 && (detail.target.id === 'profile-form' || detail.target.id === 'password-form')) {
      detail.shouldSwap = true;
      detail.isError = false;
    }
  });
</script>
</body>
</html>
