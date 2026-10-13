<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head><title>Thông báo — Nông Việt</title><%@ include file="/WEB-INF/views/common/head.jspf" %></head>
<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>
<main class="max-w-7xl mx-auto px-4 py-6 grid grid-cols-1 lg:grid-cols-[15rem_minmax(0,1fr)] gap-6">
  <aside class="min-w-0"><%@ include file="/WEB-INF/views/common/account-nav.jspf" %></aside>
  <section class="max-w-3xl min-w-0"><%@ include file="/WEB-INF/views/customer/fragments/notification-list.jsp" %></section>
</main>
<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body></html>
