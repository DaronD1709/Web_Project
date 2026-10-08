<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<form id="profile-form" method="post" action="${ctx}/account/profile"
      hx-post="${ctx}/account/profile" hx-target="#profile-form" hx-swap="outerHTML"
      class="card bg-base-100 border border-base-300 p-6 space-y-4">
  <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>" />
  <div class="flex items-center gap-4">
    <div class="avatar avatar-placeholder shrink-0"><div class="bg-primary text-primary-content w-16 rounded-full text-xl"><span><c:out value="${initials}"/></span></div></div>
    <div class="min-w-0"><div class="font-semibold break-words"><c:out value="${user.fullName}"/></div><div class="text-sm text-base-content/60">Khách hàng<c:if test="${not empty joinedMonth}"> · Tham gia <c:out value="${joinedMonth}"/></c:if></div></div>
  </div>
  <c:if test="${saved or param.saved eq '1'}"><div class="alert alert-success alert-soft text-sm" role="status">Đã lưu thay đổi.</div></c:if>
  <c:if test="${not empty profileError}"><div class="alert alert-error text-sm" role="alert"><c:out value="${profileError}"/></div></c:if>
  <label class="form-control w-full"><div class="label"><span class="label-text">Họ và tên</span></div>
    <input name="fullName" autocomplete="name" maxlength="255" required value="<c:out value='${fullName}'/>" class="input w-full ${not empty profileErrors.fullName ? 'input-error' : ''}" aria-invalid="${not empty profileErrors.fullName}">
    <c:if test="${not empty profileErrors.fullName}"><div class="label"><span class="label-text-alt text-error" role="alert"><c:out value="${profileErrors.fullName}"/></span></div></c:if>
  </label>
  <label class="form-control w-full"><div class="label"><span class="label-text">Email</span></div>
    <input type="email" class="input w-full" value="<c:out value='${user.email}'/>" disabled>
    <div class="label"><span class="label-text-alt whitespace-normal">Email dùng để đăng nhập, không thể thay đổi.</span></div>
  </label>
  <label class="form-control w-full"><div class="label"><span class="label-text">Số điện thoại</span></div>
    <input type="tel" name="phone" autocomplete="tel" maxlength="255" value="<c:out value='${phone}'/>" class="input w-full ${not empty profileErrors.phone ? 'input-error' : ''}" aria-invalid="${not empty profileErrors.phone}">
    <c:if test="${not empty profileErrors.phone}"><div class="label"><span class="label-text-alt text-error whitespace-normal" role="alert"><c:out value="${profileErrors.phone}"/></span></div></c:if>
  </label>
  <div class="flex justify-end gap-2"><button type="reset" class="btn btn-ghost">Huỷ</button><button type="submit" class="btn btn-primary">Lưu thay đổi</button></div>
</form>
<c:if test="${updateHeader}">
  <a id="current-user-name" hx-swap-oob="outerHTML" href="${ctx}/account/profile" class="text-sm hidden md:inline-block max-w-32 lg:max-w-48 truncate" title="<c:out value='${user.fullName}'/>"><c:out value="${user.fullName}"/></a>
</c:if>
