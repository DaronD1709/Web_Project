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

        <%-- Tom tat danh gia (bam de nhay toi muc Danh gia) --%>
        <a href="#reviews" class="flex items-center gap-2 mt-3 no-underline">
          <c:choose>
            <c:when test="${reviewCount > 0}">
              <span class="text-warning">★</span>
              <span class="font-semibold"><fmt:formatNumber value="${avgRating}" minFractionDigits="1" maxFractionDigits="1"/></span>
              <span class="text-sm text-base-content/60">(${reviewCount} đánh giá)</span>
            </c:when>
            <c:otherwise><span class="text-sm text-base-content/60">Chưa có đánh giá</span></c:otherwise>
          </c:choose>
        </a>

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

  <%-- ===== VUNG: DANH GIA SAN PHAM (xem: ai cung xem duoc; viet: chi khach da mua) ===== --%>
  <section id="reviews" class="mt-8">
    <div class="flex flex-wrap items-end justify-between gap-2 mb-4">
      <h2 class="text-2xl font-bold">Đánh giá sản phẩm</h2>
      <c:if test="${reviewCount > 0}">
        <div class="flex items-center gap-2">
          <span class="text-3xl font-extrabold text-primary"><fmt:formatNumber value="${avgRating}" minFractionDigits="1" maxFractionDigits="1"/></span>
          <span class="text-warning text-xl" aria-hidden="true">★</span>
          <span class="text-sm text-base-content/60">${reviewCount} đánh giá</span>
        </div>
      </c:if>
    </div>

    <%-- Thong bao sau khi gui (ok / loi) --%>
    <c:if test="${not empty reviewFlash}">
      <div role="alert" class="alert ${reviewFlashType eq 'success' ? 'alert-success' : 'alert-error'} alert-soft mb-4 text-sm"><c:out value="${reviewFlash}"/></div>
    </c:if>

    <%-- Form viet danh gia: 3 trang thai (chua dang nhap / dang nhap nhung chua mua / da mua) --%>
    <c:choose>
      <c:when test="${canReview}">
        <form method="post" action="${ctx}/product/review" class="card bg-base-100 border border-base-300 p-5 mb-4">
          <input type="hidden" name="cartCsrfToken" value="<c:out value='${cartCsrfToken}'/>">
          <input type="hidden" name="productId" value="${product.id}">
          <h3 class="font-semibold">${empty myReview ? 'Viết đánh giá của bạn' : 'Đánh giá của bạn (gửi lại để cập nhật)'}</h3>
          <div class="rating rating-lg mt-2" role="radiogroup" aria-label="Số sao">
            <c:forEach var="i" begin="1" end="5">
              <input type="radio" name="rating" value="${i}" class="mask mask-star-2 bg-orange-400" aria-label="${i} sao" ${myReview.rating == i ? 'checked' : ''} required>
            </c:forEach>
          </div>
          <textarea name="comment" rows="3" maxlength="1000" class="textarea textarea-bordered w-full mt-3" placeholder="Chia sẻ trải nghiệm của bạn với sản phẩm này (không bắt buộc)"><c:out value="${myReview.comment}"/></textarea>
          <div class="mt-3"><button type="submit" class="btn btn-primary">${empty myReview ? 'Gửi đánh giá' : 'Cập nhật đánh giá'}</button></div>
        </form>
      </c:when>
      <c:when test="${not isCustomer and empty sessionScope.currentUser}">
        <div class="alert alert-soft mb-4 text-sm">
          <span><a class="link link-primary" href="${ctx}/login?next=${pageContext.request.servletPath}%3Fid%3D${product.id}">Đăng nhập</a> để viết đánh giá (chỉ dành cho khách đã mua sản phẩm này).</span>
        </div>
      </c:when>
      <c:when test="${isCustomer}">
        <div class="alert alert-soft mb-4 text-sm">Chỉ khách đã mua và nhận sản phẩm này mới viết được đánh giá.</div>
      </c:when>
    </c:choose>

    <%-- Danh sach danh gia --%>
    <c:choose>
      <c:when test="${empty reviews}">
        <div class="card bg-base-100 border border-base-300 p-8 text-center">
          <div class="text-4xl">⭐</div>
          <p class="font-semibold mt-2">Chưa có đánh giá</p>
          <p class="text-sm text-base-content/60">Sản phẩm này chưa nhận được đánh giá nào.</p>
        </div>
      </c:when>
      <c:otherwise>
        <div class="space-y-3">
          <c:forEach var="review" items="${reviews}">
            <article class="card bg-base-100 border border-base-300 p-5">
              <div class="flex flex-wrap items-center justify-between gap-2">
                <div class="font-semibold"><c:out value="${review.customer.fullName}"/></div>
                <div class="text-xs text-base-content/50">${review.createdAtText}</div>
              </div>
              <div class="text-warning mt-1" aria-label="${review.rating} trên 5 sao">
                <c:forEach var="i" begin="1" end="5">${i <= review.rating ? '★' : '☆'}</c:forEach>
              </div>
              <%-- Nhan xet do khach nhap: luon <c:out> de chong XSS --%>
              <c:if test="${not empty review.comment}"><p class="mt-2 whitespace-pre-line break-words"><c:out value="${review.comment}"/></p></c:if>
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