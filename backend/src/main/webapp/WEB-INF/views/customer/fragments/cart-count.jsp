<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%-- Badge so dong trong gio tren header. Header include no luc tai trang; CartServlet tra no ve cho htmx doi tai cho
     (target #cart-count, swap outerHTML). request attr "oob"=true -> kem hx-swap-oob de cap nhat badge cung luc voi noi dung khac. --%>
<c:set var="n" value="${empty sessionScope.cartCount ? 0 : sessionScope.cartCount}" />
<span id="cart-count" ${oob ? 'hx-swap-oob="true"' : ''} class="badge badge-primary badge-xs indicator-item ${n == 0 ? 'hidden' : ''}">${n}</span>
