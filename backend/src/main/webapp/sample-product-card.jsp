<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!doctype html>
<html lang="vi">
<head>
  <meta charset="UTF-8">
  <title>Sản phẩm</title>
  <script src="https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4"></script>
  <link href="https://cdn.jsdelivr.net/npm/daisyui@5" rel="stylesheet" type="text/css" />
  <script src="https://unpkg.com/htmx.org"></script>
</head>
<body class="p-8">

  <h1 class="text-2xl font-semibold mb-6">Sản phẩm</h1>

  <div class="grid grid-cols-3 gap-4">
    <c:forEach var="p" items="${products}">
      <div class="card bg-base-100 shadow-md">
        <div class="card-body">
          <h3 class="card-title">${p.name}</h3>
          <p>${p.price}đ</p>
          <!-- htmx: bấm nút gọi servlet /cart/add, chỉ thay nội dung #cart-count, không reload trang -->
          <button class="btn btn-neutral"
                  hx-post="cart/add?productId=${p.id}"
                  hx-target="#cart-count"
                  hx-swap="innerHTML">
            Thêm vào giỏ
          </button>
        </div>
      </div>
    </c:forEach>
  </div>

  <div class="mt-6">
    Giỏ hàng: <span id="cart-count" class="font-semibold">${cartCount}</span> sản phẩm
  </div>

</body>
</html>
