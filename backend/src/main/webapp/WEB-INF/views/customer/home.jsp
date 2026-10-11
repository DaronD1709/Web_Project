<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<fmt:setLocale value="vi_VN" />
<%--
  Trang chu (GET / hoac /home). Du lieu do HomeServlet truyen qua request attribute:
    categories        : List<Category>   (o "Danh muc")
    featuredProducts  : List<Product>    (8 san pham moi nhat)
  Chuyen tu mockup/home.html. Phan hero, banner khuyen mai va "ly do chon" la noi dung tinh.
  Banner voucher chi la thong diep quang cao: voucher that se do Admin tao (nhanh feat/admin-manage-voucher).
--%>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Nông Việt — Hạt giống, phân bón, máy móc nông nghiệp</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<%-- Hero --%>
<section class="hero-bg text-white">
  <div class="max-w-7xl mx-auto px-4 py-16 md:py-24 grid md:grid-cols-2 gap-8 items-center">
    <div>
      <span class="badge badge-accent mb-4">Miễn phí giao hàng đơn từ 500.000₫</span>
      <h1 class="text-4xl md:text-5xl font-extrabold leading-tight">Đồng hành cùng<br>nhà nông Việt</h1>
      <p class="mt-4 text-white/80 max-w-md">Hạt giống, phân bón, thuốc BVTV, dụng cụ và máy móc nông nghiệp chính hãng — giao tận vườn, đổi trả trong 7 ngày.</p>
      <div class="mt-6 flex gap-3">
        <a href="${ctx}/products" class="btn btn-accent btn-lg">Mua sắm ngay</a>
        <a href="${ctx}/chat" class="btn btn-outline btn-lg text-white border-white/60 hover:bg-white hover:text-primary">Hỏi tư vấn</a>
      </div>
    </div>
    <div class="hidden md:flex justify-center text-[10rem] leading-none select-none" aria-hidden="true">🌾<span class="-ml-8 mt-16 text-[7rem]">🚜</span></div>
  </div>
</section>

<main class="max-w-7xl mx-auto px-4">
  <%-- Danh muc: icon la emoji gan theo thu tu (Category chua co cot icon) --%>
  <c:set var="icons" value="${['🌱','🌿','🧴','⛏️','🚜','🥬']}" />
  <section class="-mt-8 relative z-10">
    <div class="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-3">
      <c:forEach var="cat" items="${categories}" varStatus="st">
        <a href="${ctx}/products?cat=${cat.id}" class="card bg-base-100 border border-base-300 hover:border-primary hover:shadow-md transition items-center p-4 text-center">
          <div class="text-4xl" aria-hidden="true">${icons[st.index % 6]}</div>
          <div class="text-sm font-medium mt-1"><c:out value="${cat.name}"/></div>
        </a>
      </c:forEach>
    </div>
  </section>

  <%-- Banner khuyen mai (noi dung tinh) --%>
  <section class="mt-10 rounded-box bg-accent/20 border border-accent/40 p-5 flex flex-wrap items-center gap-4">
    <div class="text-4xl" aria-hidden="true">🎟️</div>
    <div class="flex-1 min-w-60">
      <div class="font-bold text-lg">Nhập mã <span class="badge badge-accent badge-lg font-mono">NONG10</span> giảm 10% cho đơn đầu tiên</div>
      <div class="text-sm text-base-content/70">Áp dụng cho đơn từ 300.000₫ · Hết hạn 31/10/2026 · Nhập ở bước thanh toán</div>
    </div>
    <a href="${ctx}/products" class="btn btn-accent">Dùng ngay</a>
  </section>

  <%-- San pham noi bat --%>
  <section class="mt-12">
    <div class="flex items-end justify-between mb-4">
      <h2 class="text-2xl font-bold">Sản phẩm mới</h2>
      <a href="${ctx}/products" class="link link-primary text-sm">Xem tất cả →</a>
    </div>
    <div class="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
      <c:forEach var="p" items="${featuredProducts}">
        <%@ include file="/WEB-INF/views/common/product-card.jspf" %>
      </c:forEach>
    </div>
  </section>

  <%-- Ly do chon --%>
  <section class="mt-12 grid gap-4 md:grid-cols-3">
    <div class="card bg-base-100 border border-base-300 p-5"><div class="text-3xl" aria-hidden="true">🚚</div><div class="font-semibold mt-2">Giao tận vườn toàn quốc</div><p class="text-sm text-base-content/70">Đóng gói chắc chắn, theo dõi đơn từng bước.</p></div>
    <div class="card bg-base-100 border border-base-300 p-5"><div class="text-3xl" aria-hidden="true">🧾</div><div class="font-semibold mt-2">Chính hãng, rõ nguồn gốc</div><p class="text-sm text-base-content/70">Có hạn sử dụng, hoá đơn; lỗi do shop được đổi trả 7 ngày.</p></div>
    <div class="card bg-base-100 border border-base-300 p-5"><div class="text-3xl" aria-hidden="true">💬</div><div class="font-semibold mt-2">Tư vấn kỹ thuật miễn phí</div><p class="text-sm text-base-content/70">Hỏi cách bón phân, phòng sâu bệnh qua chat hoặc trợ lý AI.</p></div>
  </section>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
