<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<form id="password-form" method="post" action="${ctx}/account/password"
      hx-post="${ctx}/account/password" hx-target="#password-form" hx-swap="outerHTML"
      class="card bg-base-100 border border-base-300 p-6 space-y-4">
  <input type="hidden" name="csrfToken" value="<c:out value='${csrfToken}'/>" />
  <h2 class="font-bold">Đổi mật khẩu</h2>
  <c:if test="${passwordChanged or param.passwordChanged eq '1'}"><div class="alert alert-success alert-soft text-sm" role="status">Đã cập nhật mật khẩu.</div></c:if>
  <c:if test="${not empty passwordError}"><div class="alert alert-error text-sm" role="alert"><c:out value="${passwordError}"/></div></c:if>
  <label class="form-control w-full"><div class="label"><span class="label-text">Mật khẩu hiện tại</span></div>
    <input type="password" name="current" autocomplete="current-password" required class="input w-full ${not empty passwordErrors.current ? 'input-error' : ''}" aria-invalid="${not empty passwordErrors.current}">
    <c:if test="${not empty passwordErrors.current}"><div class="label"><span class="label-text-alt text-error whitespace-normal" role="alert"><c:out value="${passwordErrors.current}"/></span></div></c:if>
  </label>
  <div class="grid sm:grid-cols-2 gap-3">
    <label class="form-control min-w-0"><div class="label"><span class="label-text">Mật khẩu mới</span></div>
      <input type="password" name="newPassword" autocomplete="new-password" minlength="6" required class="input w-full ${not empty passwordErrors.newPassword ? 'input-error' : ''}" aria-invalid="${not empty passwordErrors.newPassword}">
      <c:if test="${not empty passwordErrors.newPassword}"><div class="label"><span class="label-text-alt text-error whitespace-normal" role="alert"><c:out value="${passwordErrors.newPassword}"/></span></div></c:if>
    </label>
    <label class="form-control min-w-0"><div class="label"><span class="label-text">Nhập lại mật khẩu mới</span></div>
      <input type="password" name="confirm" autocomplete="new-password" required class="input w-full ${not empty passwordErrors.confirm ? 'input-error' : ''}" aria-invalid="${not empty passwordErrors.confirm}">
      <c:if test="${not empty passwordErrors.confirm}"><div class="label"><span class="label-text-alt text-error whitespace-normal" role="alert"><c:out value="${passwordErrors.confirm}"/></span></div></c:if>
    </label>
  </div>
  <div class="flex justify-end"><button type="submit" class="btn btn-primary">Cập nhật mật khẩu</button></div>
</form>
