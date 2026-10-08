<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%-- Menu tai khoan tu mockup/layout.js; co the thay qua OOB sau khi luu ho so. --%>
<c:set var="ctx" value="${pageContext.request.contextPath}" />
<c:set var="accountUser" value="${sessionScope.currentUser}" />
<c:set var="accountWords" value="${fn:split(accountUser.fullName, ' ')}" />
<c:set var="firstWord" value="${accountWords[0]}" />
<c:set var="lastWord" value="${accountWords[fn:length(accountWords) - 1]}" />
<c:set var="accountInitials" value="${empty firstWord ? 'KH' : fn:toUpperCase(firstWord.substring(0, firstWord.offsetByCodePoints(0, 1)))}${fn:length(accountWords) gt 1 ? fn:toUpperCase(lastWord.substring(0, lastWord.offsetByCodePoints(0, 1))) : ''}" />
<div id="customer-account-menu" class="dropdown dropdown-end" <c:if test="${updateHeader}">hx-swap-oob="outerHTML"</c:if>>
  <div tabindex="0" role="button" aria-label="Mở menu tài khoản" aria-haspopup="menu" class="btn btn-ghost gap-2 px-2">
    <div class="avatar avatar-placeholder"><div class="bg-primary text-primary-content w-8 rounded-full"><span><c:out value="${empty accountInitials ? 'KH' : accountInitials}"/></span></div></div>
    <span id="current-user-name" class="hidden md:inline-block text-sm max-w-32 lg:max-w-48 truncate" title="<c:out value='${accountUser.fullName}'/>"><c:out value="${accountUser.fullName}"/></span>
  </div>
  <ul tabindex="0" class="dropdown-content menu bg-base-100 rounded-box z-10 w-52 p-2 shadow-lg border border-base-300 mt-2">
    <li><a href="${ctx}/account/profile">Hồ sơ của tôi</a></li>
    <li><a href="${ctx}/account/addresses">Địa chỉ giao hàng</a></li>
    <li><a href="${ctx}/orders">Đơn hàng của tôi</a></li>
    <li><form action="${ctx}/logout" method="post" class="p-0"><button type="submit" class="text-error px-3 py-2 cursor-pointer">Đăng xuất</button></form></li>
  </ul>
</div>
