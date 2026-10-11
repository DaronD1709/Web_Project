/* Phan dung chung cua cac trang mockup: header, footer, product card, du lieu mau.
   Trong JSP that: header/footer -> file include (common/header.jspf), card -> <c:forEach>. */

const CATEGORIES = [
  { id: 1, name: 'Hạt giống', icon: '🌱' },
  { id: 2, name: 'Phân bón', icon: '🌿' },
  { id: 3, name: 'Thuốc BVTV', icon: '🧴' },
  { id: 4, name: 'Dụng cụ cầm tay', icon: '⛏️' },
  { id: 5, name: 'Máy móc & tưới tiêu', icon: '🚜' },
  { id: 6, name: 'Nông sản', icon: '🥬' },
];

const PRODUCTS = [
  { id: 1, name: 'Hạt giống cà chua Beef F1 (gói 50 hạt)', cat: 1, price: 25000, stock: 200, icon: '🍅', rating: 4.8, sold: 812 },
  { id: 2, name: 'Hạt giống rau cải xanh (combo 3 gói)', cat: 1, price: 30000, stock: 150, icon: '🥬', rating: 4.6, sold: 540 },
  { id: 3, name: 'Phân bón NPK 20-20-15 (bao 5kg)', cat: 2, price: 185000, stock: 80, icon: '🌾', rating: 4.7, sold: 410 },
  { id: 4, name: 'Phân hữu cơ vi sinh (bao 10kg)', cat: 2, price: 120000, stock: 100, icon: '🌿', rating: 4.5, sold: 287 },
  { id: 5, name: 'Máy phun thuốc đeo vai chạy điện 16L', cat: 5, price: 980000, oldPrice: 1150000, stock: 15, icon: '💦', rating: 4.9, sold: 96 },
  { id: 6, name: 'Bộ tưới nhỏ giọt 50m', cat: 5, price: 250000, stock: 40, icon: '💧', rating: 4.7, sold: 174 },
  { id: 7, name: 'Cuốc chim thép cán gỗ', cat: 4, price: 165000, stock: 30, icon: '⛏️', rating: 4.8, sold: 133 },
  { id: 8, name: 'Máy cắt cỏ cầm tay chạy xăng', cat: 5, price: 3200000, oldPrice: 3600000, stock: 3, icon: '🌾', rating: 4.4, sold: 41 },
  { id: 9, name: 'Thuốc trừ sâu sinh học Abamectin 100ml', cat: 3, price: 95000, stock: 60, icon: '🧴', rating: 4.9, sold: 402 },
  { id: 10, name: 'Kéo cắt tỉa cành cao cấp', cat: 4, price: 120000, stock: 45, icon: '✂️', rating: 4.6, sold: 158 },
  { id: 11, name: 'Máy xới đất mini 7HP', cat: 5, price: 8500000, stock: 0, icon: '🚜', rating: 4.8, sold: 27 },
  { id: 12, name: 'Gạo hữu cơ ST25 (túi 5kg)', cat: 6, price: 160000, stock: 70, icon: '🍚', rating: 4.5, sold: 260 },
];

const fmt = n => n.toLocaleString('vi-VN') + '₫';
const catName = id => (CATEGORIES.find(c => c.id === id) || {}).name || '';
const stars = r => '★'.repeat(Math.round(r)) + '☆'.repeat(5 - Math.round(r));

const ICON = {
  search: '<svg class="size-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M21 21l-4.35-4.35M17 10.5a6.5 6.5 0 11-13 0 6.5 6.5 0 0113 0z"/></svg>',
  cart: '<svg class="size-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M3 3h2l.4 2M7 13h10l4-8H5.4M7 13L5.4 5M7 13l-2.3 2.3c-.6.6-.2 1.7.7 1.7H17m0 0a2 2 0 100 4 2 2 0 000-4zm-8 2a2 2 0 11-4 0 2 2 0 014 0z"/></svg>',
  bell: '<svg class="size-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M15 17h5l-1.4-1.4A2 2 0 0118 14.2V11a6 6 0 10-12 0v3.2c0 .5-.2 1-.6 1.4L4 17h5m6 0a3 3 0 11-6 0"/></svg>',
  chat: '<svg class="size-6" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2"><path stroke-linecap="round" stroke-linejoin="round" d="M8 10h8M8 14h5m-9 6l2.5-3H18a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v14z"/></svg>',
};

function productCard(p) {
  const out = p.stock === 0;
  const low = p.stock > 0 && p.stock <= 5;
  const sale = p.oldPrice ? Math.round((1 - p.price / p.oldPrice) * 100) : 0;
  return `
  <div class="card bg-base-100 border border-base-300 hover:shadow-lg transition">
    <a href="product-detail.html?id=${p.id}" class="relative block">
      <div class="ph rounded-t-[var(--radius-box)] h-44">${p.icon}</div>
      ${sale ? `<span class="badge badge-accent absolute top-2 left-2">-${sale}%</span>` : ''}
      ${out ? `<span class="badge badge-neutral absolute top-2 right-2">Hết hàng</span>` : ''}
      ${low ? `<span class="badge badge-warning absolute top-2 right-2">Chỉ còn ${p.stock}</span>` : ''}
    </a>
    <div class="card-body p-4 gap-1">
      <span class="text-xs text-base-content/60">${catName(p.cat)}</span>
      <a href="product-detail.html?id=${p.id}" class="font-semibold leading-snug line-clamp-2 min-h-10 hover:text-primary">${p.name}</a>
      <div class="text-xs text-warning">${stars(p.rating)} <span class="text-base-content/60">(${p.rating}) · Đã bán ${p.sold}</span></div>
      <div class="flex items-baseline gap-2 mt-1">
        <span class="text-lg font-bold text-primary">${fmt(p.price)}</span>
        ${p.oldPrice ? `<span class="text-xs line-through text-base-content/50">${fmt(p.oldPrice)}</span>` : ''}
      </div>
      <!-- htmx that: hx-post="/cart/add" hx-vals='{"productId":${p.id}}' hx-target="#cart-count" -->
      <button class="btn btn-primary btn-sm mt-2" ${out ? 'disabled' : ''}>${out ? 'Hết hàng' : 'Thêm vào giỏ'}</button>
    </div>
  </div>`;
}

function renderHeader(auth, active) {
  const catLinks = CATEGORIES.map(c => `<li><a href="products.html?cat=${c.id}">${c.icon} ${c.name}</a></li>`).join('');
  const right = auth === 'guest'
    ? `<a href="login.html" class="btn btn-ghost btn-sm">Đăng nhập</a>
       <a href="register.html" class="btn btn-primary btn-sm">Đăng ký</a>`
    : `<a href="chat.html" class="btn btn-ghost btn-circle" title="Chat với shop">${ICON.chat}</a>
       <a href="notifications.html" class="btn btn-ghost btn-circle" title="Thông báo">
         <div class="indicator">${ICON.bell}<span class="badge badge-error badge-xs indicator-item">2</span></div></a>
       <div class="dropdown dropdown-end">
         <div tabindex="0" role="button" class="btn btn-ghost gap-2 px-2">
           <div class="avatar avatar-placeholder"><div class="bg-primary text-primary-content w-8 rounded-full"><span>ND</span></div></div>
           <span class="hidden md:inline text-sm">Nguyễn Danh</span>
         </div>
         <ul tabindex="0" class="dropdown-content menu bg-base-100 rounded-box z-10 w-52 p-2 shadow-lg border border-base-300 mt-2">
           <li><a href="profile.html">Hồ sơ của tôi</a></li>
           <li><a href="addresses.html">Địa chỉ giao hàng</a></li>
           <li><a href="orders.html">Đơn hàng của tôi</a></li>
           <li><a href="login.html" class="text-error">Đăng xuất</a></li>
         </ul>
       </div>`;
  return `
  <header class="bg-base-100 border-b border-base-300 sticky top-0 z-30">
    <div class="max-w-7xl mx-auto px-4 h-16 flex items-center gap-3">
      <a href="home.html" class="text-lg sm:text-xl font-extrabold text-primary whitespace-nowrap">🌾 Nông Việt</a>
      <div class="dropdown hidden md:block">
        <div tabindex="0" role="button" class="btn btn-ghost btn-sm">Danh mục ▾</div>
        <ul tabindex="0" class="dropdown-content menu bg-base-100 rounded-box z-10 w-56 p-2 shadow-lg border border-base-300 mt-2">${catLinks}</ul>
      </div>
      <form action="products.html" class="hidden sm:block flex-1 max-w-xl mx-auto">
        <label class="input w-full">${ICON.search}<input type="search" name="q" placeholder="Tìm hạt giống, phân bón, máy móc…" /></label>
      </form>
      <div class="flex items-center gap-1 ml-auto sm:ml-0">
        ${right}
        <a href="cart.html" class="btn btn-ghost btn-circle" title="Giỏ hàng">
          <div class="indicator">${ICON.cart}<span id="cart-count" class="badge badge-primary badge-xs indicator-item">3</span></div></a>
      </div>
    </div>
    <form action="products.html" class="sm:hidden px-4 pb-2">
      <label class="input input-sm w-full">${ICON.search}<input type="search" name="q" placeholder="Tìm hạt giống, phân bón, máy móc…" /></label>
    </form>
  </header>`;
}

function renderFooter() {
  return `
  <footer class="bg-neutral text-neutral-content mt-16">
    <div class="max-w-7xl mx-auto px-4 py-10 grid gap-8 md:grid-cols-4 text-sm">
      <div><div class="text-lg font-bold mb-2">🌾 Nông Việt</div><p class="opacity-70">Hạt giống, phân bón, dụng cụ và máy móc cho nhà nông.</p></div>
      <div><div class="font-semibold mb-2">Mua sắm</div><ul class="space-y-1 opacity-70"><li><a href="products.html">Tất cả sản phẩm</a></li><li><a href="cart.html">Giỏ hàng</a></li><li><a href="orders.html">Tra cứu đơn hàng</a></li></ul></div>
      <div><div class="font-semibold mb-2">Hỗ trợ</div><ul class="space-y-1 opacity-70"><li><a href="chat.html">Chat với shop</a></li><li>Chính sách đổi trả</li><li>Hướng dẫn thanh toán</li></ul></div>
      <div><div class="font-semibold mb-2">Liên hệ</div><ul class="space-y-1 opacity-70"><li>0900 000 000</li><li>support@nongviet.vn</li></ul></div>
    </div>
    <div class="text-center text-xs opacity-50 pb-6">© 2026 Nông Việt — đồ án môn học</div>
  </footer>`;
}

/* Menu ben trai cho nhom trang "Tai khoan" */
function renderAccountNav(active) {
  const items = [
    ['profile', 'profile.html', 'Hồ sơ cá nhân'],
    ['addresses', 'addresses.html', 'Địa chỉ giao hàng'],
    ['orders', 'orders.html', 'Đơn hàng của tôi'],
    ['notifications', 'notifications.html', 'Thông báo'],
  ];
  return `<ul class="menu menu-horizontal lg:menu-vertical flex-nowrap overflow-x-auto bg-base-100 rounded-box border border-base-300 w-full p-2">
    ${items.map(([k, h, t]) => `<li><a href="${h}" class="${k === active ? 'menu-active' : ''}">${t}</a></li>`).join('')}
    <li><a href="login.html" class="text-error">Đăng xuất</a></li></ul>`;
}

document.documentElement.setAttribute('data-theme', 'nongnghiep');
document.addEventListener('DOMContentLoaded', () => {
  const body = document.body;
  const h = document.getElementById('site-header');
  const f = document.getElementById('site-footer');
  if (h) h.innerHTML = renderHeader(body.dataset.auth || 'user', body.dataset.active || '');
  if (f) f.innerHTML = renderFooter();
  const an = document.getElementById('account-nav');
  if (an) an.innerHTML = renderAccountNav(body.dataset.active || '');
});
