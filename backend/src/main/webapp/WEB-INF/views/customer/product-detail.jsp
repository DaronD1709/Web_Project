<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />

<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title><c:out value="${product.name}"/> — Nông Việt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>

<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main class="max-w-7xl mx-auto px-4 py-6">

  <%-- Breadcrumb --%>
  <div class="text-sm breadcrumbs mb-5">
    <ul>
      <li><a href="${ctx}/">Trang chủ</a></li>
      <li><a href="${ctx}/products">Sản phẩm</a></li>
      <li><c:out value="${product.name}"/></li>
    </ul>
  </div>

  <%-- Thông tin chính của sản phẩm --%>
  <section class="card bg-base-100 border border-base-300 overflow-hidden">
    <div class="grid grid-cols-1 md:grid-cols-2">

      <div class="bg-base-200 min-h-80">
        <c:choose>
          <c:when test="${not empty product.imageUrl}">
            <img src="${ctx}/${product.imageUrl}"
                 alt="<c:out value='${product.name}'/>"
                 class="w-full h-full max-h-[34rem] object-cover">
          </c:when>
          <c:otherwise>
            <div class="ph min-h-80 h-full text-8xl">🌾</div>
          </c:otherwise>
        </c:choose>
      </div>

      <div class="p-6 md:p-10 flex flex-col">
        <div class="text-sm text-base-content/60">
          <c:out value="${product.category.name}"/>
        </div>

        <h1 class="text-3xl font-bold mt-2">
          <c:out value="${product.name}"/>
        </h1>

        <div class="flex items-center gap-2 mt-3">
          <span class="text-warning">★</span>
          <span class="font-semibold">
            <fmt:formatNumber value="${avgRating}" maxFractionDigits="1"/>
          </span>
          <span class="text-sm text-base-content/60">
            (${reviews.size()} đánh giá)
          </span>
        </div>

        <div class="text-3xl font-extrabold text-primary mt-5">
          <fmt:formatNumber value="${product.price}" pattern="#,##0"/>₫
        </div>

        <div class="mt-3">
          <c:choose>
            <c:when test="${product.stockQuantity > 0}">
              <span class="badge badge-success badge-outline">
                Còn ${product.stockQuantity} sản phẩm
              </span>
            </c:when>
            <c:otherwise>
              <span class="badge badge-neutral">Hết hàng</span>
            </c:otherwise>
          </c:choose>
        </div>

        <div class="divider"></div>

        <h2 class="font-semibold">Mô tả sản phẩm</h2>
        <p class="mt-2 text-base-content/70 whitespace-pre-line">
          <c:choose>
            <c:when test="${not empty product.description}">
              <c:out value="${product.description}"/>
            </c:when>
            <c:otherwise>
              Chưa có mô tả cho sản phẩm này.
            </c:otherwise>
          </c:choose>
        </p>

        <div class="mt-auto pt-8">
          <c:choose>
            <c:when test="${product.stockQuantity == 0}">
              <button type="button" class="btn btn-disabled btn-block">
                Hết hàng
              </button>
            </c:when>

            <%-- Chỉ hiện form khi CartCsrf đã chuẩn bị token cho Customer --%>
            <c:when test="${not empty cartCsrfToken}">
              <form method="post"
                    action="${ctx}/cart"
                    class="flex flex-col sm:flex-row gap-3"
                    hx-post="${ctx}/cart"
                    hx-target="#cart-count"
                    hx-swap="outerHTML">

                <input type="hidden" name="action" value="add">
                <input type="hidden" name="productId" value="${product.id}">
                <input type="hidden"
                       name="cartCsrfToken"
                       value="<c:out value='${cartCsrfToken}'/>">

                <input type="number"
                       name="qty"
                       value="1"
                       min="1"
                       max="${product.stockQuantity}"
                       class="input input-bordered w-full sm:w-28"
                       aria-label="Số lượng">

                <button type="submit" class="btn btn-primary flex-1">
                  Thêm vào giỏ hàng
                </button>
              </form>
            </c:when>

            <c:otherwise>
              <a href="${ctx}/login?next=/product?id=${product.id}"
                 class="btn btn-primary btn-block">
                Đăng nhập để mua hàng
              </a>
            </c:otherwise>
          </c:choose>
        </div>
      </div>
    </div>
  </section>

  <%-- Đánh giá: dữ liệu sẽ được tích hợp từ nhánh Product Review --%>
  <section id="reviews" class="mt-8">
    <h2 class="text-2xl font-bold mb-4">Đánh giá sản phẩm</h2>

    <c:choose>
      <c:when test="${empty reviews}">
        <div class="card bg-base-100 border border-base-300 p-8 text-center">
          <div class="text-4xl">⭐</div>
          <p class="font-semibold mt-2">Chưa có đánh giá</p>
          <p class="text-sm text-base-content/60">
            Sản phẩm này chưa nhận được đánh giá nào.
          </p>
        </div>
      </c:when>

      <c:otherwise>
        <div class="space-y-3">
          <c:forEach var="review" items="${reviews}">
            <article class="card bg-base-100 border border-base-300 p-5">
              <div class="font-semibold">
                <c:out value="${review.customer.fullName}"/>
              </div>
              <div class="text-warning mt-1">
                <c:forEach begin="1" end="${review.rating}">★</c:forEach>
              </div>
              <p class="mt-2">
                <c:out value="${review.comment}"/>
              </p>
            </article>
          </c:forEach>
        </div>
      </c:otherwise>
    </c:choose>
  </section>

  <%-- Sản phẩm liên quan --%>
  <c:if test="${not empty relatedProducts}">
    <section class="mt-10">
      <h2 class="text-2xl font-bold mb-4">Sản phẩm liên quan</h2>

      <div class="grid grid-cols-2 md:grid-cols-4 gap-4">
        <c:forEach var="related" items="${relatedProducts}">
          <a href="${ctx}/product?id=${related.id}"
             class="card bg-base-100 border border-base-300 hover:shadow-lg transition overflow-hidden">

            <c:choose>
              <c:when test="${not empty related.imageUrl}">
                <img src="${ctx}/${related.imageUrl}"
                     alt="<c:out value='${related.name}'/>"
                     class="h-40 w-full object-cover">
              </c:when>
              <c:otherwise>
                <div class="ph h-40">🌾</div>
              </c:otherwise>
            </c:choose>

            <div class="card-body p-4 gap-1">
              <div class="font-semibold line-clamp-2">
                <c:out value="${related.name}"/>
              </div>
              <div class="font-bold text-primary">
                <fmt:formatNumber value="${related.price}" pattern="#,##0"/>₫
              </div>
            </div>
          </a>
        </c:forEach>
      </div>
    </section>
  </c:if>

</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>