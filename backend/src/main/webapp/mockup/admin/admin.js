/* Phan dung chung cua mockup ADMIN (CMS): khung trang (sidebar + header), du lieu mau, bieu do, toast, hop thoai xac nhan.
   Can nap SAU ../layout.js (dung lai PRODUCTS, CATEGORIES, fmt, catName, stars).
   Trong JSP that: khung -> WEB-INF/views/admin/common/{sidebar,header}.jspf; du lieu mau -> Servlet truyen qua request attribute. */

const esc = s => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
const TODAY = '2026-10-02';
const ADM = { name: 'Hữu Danh', email: 'admin@nongviet.vn' };

/* ------------------------------------------------------------------ du lieu mau */
// Khop OrderStatus trong entity: PENDING, CONFIRMED, SHIPPING, COMPLETED, CANCELLED, RETURN_REQUESTED, RETURNED
const ORDER_STATUS = {
  pending: ['Chờ xác nhận', 'badge-warning'], confirmed: ['Đã xác nhận', 'badge-info'], shipping: ['Đang giao', 'badge-info'],
  completed: ['Hoàn tất', 'badge-success'], cancelled: ['Đã huỷ', 'badge-neutral'],
  return: ['Yêu cầu hoàn hàng', 'badge-error'], returned: ['Đã hoàn hàng', 'badge-neutral'],
};
const statusBadge = s => `<span class="badge ${ORDER_STATUS[s][1]} badge-sm whitespace-nowrap">${ORDER_STATUS[s][0]}</span>`;

const CUSTOMERS = [
  { id: 1, name: 'Nguyễn Văn Khách', email: 'khach@nongviet.vn', phone: '0901 234 567', joined: '01/10/2026', status: 'active' },
  { id: 2, name: 'Trần Thị Mai', email: 'mai.tran@gmail.com', phone: '0912 345 678', joined: '28/09/2026', status: 'active' },
  { id: 3, name: 'Lê Hoàng Nam', email: 'nam.le@gmail.com', phone: '0987 654 321', joined: '25/09/2026', status: 'active' },
  { id: 4, name: 'Phạm Quốc Bảo', email: 'bao.pq@outlook.com', phone: '0933 111 222', joined: '20/09/2026', status: 'locked' },
  { id: 5, name: 'Võ Thị Lan', email: 'lan.vo@gmail.com', phone: '0977 888 999', joined: '18/09/2026', status: 'active' },
  { id: 6, name: 'Đặng Minh Tuấn', email: 'tuan.dm@gmail.com', phone: '0944 555 666', joined: '15/09/2026', status: 'active' },
  { id: 7, name: 'Hoàng Thị Hạnh', email: 'hanh.ht@gmail.com', phone: '0966 777 000', joined: '10/09/2026', status: 'active' },
  { id: 8, name: 'Bùi Văn Long', email: 'long.bv@yahoo.com', phone: '0922 333 444', joined: '05/09/2026', status: 'locked' },
];
const custById = id => CUSTOMERS.find(c => c.id === id);

const ORDERS = [
  { no: 1042, date: '02/10/2026 14:30', cid: 1, items: [[5, 1], [3, 4], [9, 2]], status: 'pending', pay: 'COD', paid: false, voucher: 'NONG10' },
  { no: 1041, date: '02/10/2026 11:05', cid: 2, items: [[1, 10], [2, 3]], status: 'pending', pay: 'VNPay', paid: true },
  { no: 1040, date: '01/10/2026 16:20', cid: 3, items: [[8, 1]], status: 'confirmed', pay: 'COD', paid: false },
  { no: 1038, date: '30/09/2026 09:10', cid: 1, items: [[6, 1]], status: 'shipping', pay: 'COD', paid: false },
  { no: 1037, date: '30/09/2026 08:45', cid: 5, items: [[4, 5], [3, 2]], status: 'shipping', pay: 'VNPay', paid: true },
  { no: 1035, date: '29/09/2026 19:00', cid: 2, items: [[7, 2]], status: 'completed', pay: 'COD', paid: true },
  { no: 1031, date: '28/09/2026 10:30', cid: 7, items: [[1, 1], [2, 1]], status: 'completed', pay: 'VNPay', paid: true },
  { no: 1030, date: '27/09/2026 15:15', cid: 6, items: [[10, 2], [7, 1]], status: 'completed', pay: 'COD', paid: true },
  { no: 1027, date: '25/09/2026 13:40', cid: 3, items: [[8, 1]], status: 'return', pay: 'COD', paid: true, reason: 'Máy chạy không ổn định, động cơ phát tiếng ồn lớn ngay khi khởi động.' },
  { no: 1024, date: '22/09/2026 09:00', cid: 4, items: [[12, 4]], status: 'returned', pay: 'COD', paid: false },
  { no: 1019, date: '20/09/2026 17:25', cid: 5, items: [[9, 3]], status: 'cancelled', pay: 'COD', paid: false },
  { no: 1015, date: '18/09/2026 12:00', cid: 8, items: [[3, 10]], status: 'cancelled', pay: 'VNPay', paid: false },
];
function orderTotals(o) {
  const sub = o.items.reduce((s, [pid, q]) => s + PRODUCTS.find(p => p.id === pid).price * q, 0);
  const discount = o.voucher ? Math.round(sub * 0.1) : 0;
  const ship = sub >= 500000 ? 0 : 30000;
  return { sub, discount, ship, total: sub - discount + ship };
}

const VOUCHERS = [
  { id: 1, code: 'NONG10', type: 'percent', value: 10, min: 300000, issued: 100, used: 37, perUser: 1, start: '2026-10-01', end: '2026-10-31', active: true },
  { id: 2, code: 'FREESHIP', type: 'fixed', value: 30000, min: 200000, issued: 200, used: 148, perUser: 3, start: '2026-09-15', end: '2026-12-31', active: true },
  { id: 3, code: 'KHAITRUONG50', type: 'fixed', value: 50000, min: 500000, issued: 50, used: 50, perUser: 1, start: '2026-09-01', end: '2026-10-15', active: true },
  { id: 4, code: 'THU15', type: 'percent', value: 15, min: 400000, issued: 80, used: 21, perUser: 1, start: '2026-08-01', end: '2026-09-15', active: true },
  { id: 5, code: 'VIP20', type: 'percent', value: 20, min: 1000000, issued: 30, used: 4, perUser: 1, start: '2026-10-01', end: '2026-12-31', active: false },
];
// Khop Voucher.isValid(): con han + dang bat + con luot
function voucherState(v) {
  if (!v.active) return 'off';
  if (v.end < TODAY || v.start > TODAY) return 'expired';
  if (v.used >= v.issued) return 'soldout';
  return 'active';
}
const VOUCHER_STATE = { active: ['Đang hoạt động', 'badge-success'], expired: ['Hết hạn', 'badge-neutral'], soldout: ['Hết lượt', 'badge-warning'], off: ['Đã tắt', 'badge-ghost'] };

const REVIEWS = [
  { id: 1, pid: 5, customer: 'Trần Thị Mai', rating: 5, comment: 'Máy phun nhẹ, pin dùng cả buổi sáng không hết. Rất đáng tiền!', date: '01/10/2026', hidden: false },
  { id: 2, pid: 1, customer: 'Lê Hoàng Nam', rating: 5, comment: 'Hạt nảy mầm đều, 95% sau 3 ngày. Sẽ mua lại.', date: '30/09/2026', hidden: false },
  { id: 3, pid: 8, customer: 'Phạm Quốc Bảo', rating: 1, comment: 'Hàng lỗi, shop phản hồi chậm. <script>alert("xss")</script>', date: '28/09/2026', hidden: false, flagged: true },
  { id: 4, pid: 3, customer: 'Võ Thị Lan', rating: 4, comment: 'Phân tốt, lúa lên xanh. Đóng gói hơi sơ sài.', date: '27/09/2026', hidden: false },
  { id: 5, pid: 9, customer: 'Đặng Minh Tuấn', rating: 2, comment: 'Liên hệ số 0900 000 000 để mua rẻ hơn shop này nhé!!!', date: '26/09/2026', hidden: true, flagged: true },
  { id: 6, pid: 7, customer: 'Hoàng Thị Hạnh', rating: 5, comment: 'Cuốc chắc tay, cán gỗ đẹp.', date: '25/09/2026', hidden: false },
];

const CONVOS = [
  { id: 1, cid: 3, human: true, unread: 2, last: 'Cho mình hỏi máy cắt cỏ còn bảo hành bao lâu?', time: '14:35', msgs: [
    ['bot', 'Xin chào! Mình là trợ lý AI của NôngViệt. Bạn cần tư vấn gì?', '14:20'],
    ['me', 'Mình muốn mua máy cắt cỏ, còn hàng không?', '14:22'],
    ['bot', 'Máy cắt cỏ cầm tay chạy xăng hiện còn 3 máy. Bạn muốn mình chuyển nhân viên hỗ trợ không?', '14:22'],
    ['me', 'Có, gọi nhân viên giúp mình', '14:23'],
    ['admin', 'Chào bạn, mình là Danh bên NôngViệt. Bạn cần hỗ trợ gì về máy cắt cỏ ạ?', '14:30'],
    ['me', 'Cho mình hỏi máy cắt cỏ còn bảo hành bao lâu?', '14:35']] },
  { id: 2, cid: 2, human: false, unread: 0, last: 'Trợ lý AI: Phân NPK 20-20-15 phù hợp bón thúc cho rau màu…', time: '13:10', msgs: [
    ['me', 'Rau màu nên bón phân gì?', '13:09'],
    ['bot', 'Phân NPK 20-20-15 phù hợp bón thúc cho rau màu, kết hợp phân hữu cơ vi sinh bón lót.', '13:10']] },
  { id: 3, cid: 5, human: false, unread: 1, last: 'Đơn #1037 bao giờ giao vậy shop?', time: '11:02', msgs: [
    ['me', 'Đơn #1037 bao giờ giao vậy shop?', '11:02']] },
  { id: 4, cid: 7, human: false, unread: 0, last: 'Cảm ơn shop!', time: 'Hôm qua', msgs: [
    ['me', 'Mình nhận được hàng rồi.', '10:00'], ['bot', 'Cảm ơn bạn! Chúc bạn một mùa vụ bội thu.', '10:00'], ['me', 'Cảm ơn shop!', '10:01']] },
];

/* ------------------------------------------------------------------ khung trang */
const NAV = [
  ['dashboard', 'Tổng quan', 'dashboard.html', 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6'],
  ['orders', 'Đơn hàng', 'orders.html', 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01'],
  ['products', 'Sản phẩm', 'products.html', 'M20 7l-8-4-8 4m16 0l-8 4m8-4v10l-8 4m0-10L4 7m8 4v10M4 7v10l8 4'],
  ['categories', 'Danh mục', 'categories.html', 'M7 7h.01M7 3h5c.512 0 1.024.195 1.414.586l7 7a2 2 0 010 2.828l-7 7a2 2 0 01-2.828 0l-7-7A1.994 1.994 0 013 12V7a4 4 0 014-4z'],
  ['vouchers', 'Voucher', 'vouchers.html', 'M15 5v2m0 4v2m0 4v2M5 5a2 2 0 00-2 2v3a2 2 0 110 4v3a2 2 0 002 2h14a2 2 0 002-2v-3a2 2 0 110-4V7a2 2 0 00-2-2H5z'],
  ['customers', 'Khách hàng', 'customers.html', 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z'],
  ['reviews', 'Đánh giá', 'reviews.html', 'M11.049 2.927c.3-.921 1.603-.921 1.902 0l1.519 4.674a1 1 0 00.95.69h4.915c.969 0 1.371 1.24.588 1.81l-3.976 2.888a1 1 0 00-.363 1.118l1.518 4.674c.3.922-.755 1.688-1.538 1.118l-3.976-2.888a1 1 0 00-1.176 0l-3.976 2.888c-.783.57-1.838-.197-1.538-1.118l1.518-4.674a1 1 0 00-.363-1.118l-3.976-2.888c-.784-.57-.38-1.81.588-1.81h4.914a1 1 0 00.951-.69l1.519-4.674z'],
  ['chat', 'Chat với khách', 'chat.html', 'M8 10h8M8 14h5m-9 6l2.5-3H18a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v14z'],
  ['statistics', 'Thống kê doanh thu', 'statistics.html', 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z'],
];
const NAV_BADGE = {
  orders: ORDERS.filter(o => o.status === 'pending' || o.status === 'return').length,
  reviews: REVIEWS.filter(r => r.flagged && !r.hidden).length,
  chat: CONVOS.reduce((s, c) => s + c.unread, 0),
};
const svgIcon = (d, cls = 'size-5') => `<svg class="${cls}" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8"><path stroke-linecap="round" stroke-linejoin="round" d="${d}"/></svg>`;

const NAV_ACTIVE = 'bg-primary text-primary-content font-medium', NAV_IDLE = 'text-neutral-content/80 hover:bg-white/10';
function navHtml(active) {
  return NAV.map(([k, t, href, d]) => `
    <li><a href="${href}" class="flex items-center gap-3 rounded-field px-3 py-2 text-sm ${k === active ? NAV_ACTIVE : NAV_IDLE}">
      ${svgIcon(d)}<span class="flex-1">${t}</span>${NAV_BADGE[k] ? `<span class="badge badge-sm ${k === active ? 'bg-white/25 border-0 text-primary-content' : 'badge-accent'}">${NAV_BADGE[k]}</span>` : ''}</a></li>`).join('');
}

/* Khung (sidebar + header) chi dung 1 lan; chuyen trang thi htmx chi doi noi dung #adm-main (hx-boost + hx-select="#page")
   nen khong tai lai trang, khong phai bien dich lai Tailwind, khong nhay. Trong JSP that: <body hx-boost> tuong tu. */
function mountAdminShell() {
  const body = document.body, page = document.getElementById('page');
  if (!page || body.dataset.shell === 'none') return;
  const shell = document.createElement('div');
  shell.className = 'drawer lg:drawer-open';
  shell.setAttribute('hx-boost', 'true');
  shell.setAttribute('hx-target', '#adm-main');
  shell.setAttribute('hx-select', '#page');
  shell.setAttribute('hx-swap', 'innerHTML show:window:top');
  shell.innerHTML = `
    <input id="adm-drawer" type="checkbox" class="drawer-toggle" />
    <div class="drawer-content flex flex-col min-w-0 min-h-screen">
      <header class="sticky top-0 z-30 bg-base-100 border-b border-base-300 h-14 px-4 flex items-center gap-3">
        <label for="adm-drawer" class="btn btn-ghost btn-sm btn-square lg:hidden" aria-label="Mở menu">${svgIcon('M4 6h16M4 12h16M4 18h16')}</label>
        <div class="text-sm flex items-center gap-2 min-w-0"><span class="text-base-content/50 hidden sm:inline">Quản trị /</span><span id="adm-title" class="font-semibold truncate"></span></div>
        <div class="ml-auto flex items-center gap-1">
          <a href="chat.html" class="btn btn-ghost btn-sm btn-circle" title="Tin nhắn">
            <div class="indicator">${svgIcon('M8 10h8M8 14h5m-9 6l2.5-3H18a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v14z')}${NAV_BADGE.chat ? `<span class="badge badge-error badge-xs indicator-item">${NAV_BADGE.chat}</span>` : ''}</div></a>
          <div class="dropdown dropdown-end">
            <div tabindex="0" role="button" class="btn btn-ghost btn-sm gap-2 px-2">
              <div class="avatar avatar-placeholder"><div class="bg-neutral text-neutral-content w-7 rounded-full text-xs"><span>HD</span></div></div>
              <span class="hidden sm:inline text-sm">${esc(ADM.name)}</span><span class="badge badge-primary badge-sm">Admin</span>
            </div>
            <ul tabindex="0" class="dropdown-content menu bg-base-100 rounded-box z-10 w-52 p-2 shadow-lg border border-base-300 mt-2">
              <li class="menu-title">${esc(ADM.email)}</li>
              <li><a href="../home.html" target="_blank" hx-boost="false">Xem cửa hàng ↗</a></li>
              <li><a href="login.html" class="text-error" hx-boost="false">Đăng xuất</a></li>
            </ul>
          </div>
        </div>
      </header>
      <main class="flex-1 p-4 md:p-6 min-w-0" id="adm-main"></main>
    </div>
    <div class="drawer-side z-40">
      <label for="adm-drawer" class="drawer-overlay" aria-label="Đóng menu"></label>
      <aside class="w-64 min-h-full bg-neutral text-neutral-content flex flex-col">
        <a href="dashboard.html" class="h-14 px-5 flex items-center gap-2 text-lg font-extrabold border-b border-white/10">🌾 NôngViệt <span class="badge badge-sm badge-outline border-white/40 text-white/70">CMS</span></a>
        <ul id="adm-nav" class="p-3 space-y-1 flex-1"></ul>
        <div class="p-4 text-xs text-neutral-content/50 border-t border-white/10">Đăng nhập với vai trò <b>ADMIN</b><br>Mockup giao diện — chưa kết nối dữ liệu</div>
      </aside>
    </div>`;
  body.insertBefore(shell, page);
  shell.querySelector('#adm-main').appendChild(page);
  if (window.htmx) htmx.process(shell);
  refreshShell();
}

// Cap nhat tieu de + muc sidebar dang chon theo trang vua hien (doc tu data-* tren #page)
function refreshShell() {
  const p = document.getElementById('page'); if (!p) return;
  const nav = document.getElementById('adm-nav'), t = document.getElementById('adm-title'), d = document.getElementById('adm-drawer');
  if (nav) { nav.innerHTML = navHtml(p.dataset.active || ''); if (window.htmx) htmx.process(nav); }
  if (t) t.textContent = p.dataset.title || '';
  if (d) d.checked = false; // man hinh nho: dong menu sau khi chon
}

// Chuyen trang bang code (vd sau khi luu form) ma van qua htmx, khong tai lai trang
function go(url) {
  const a = document.createElement('a'); a.href = url; a.hidden = true;
  document.querySelector('.drawer').appendChild(a); htmx.process(a); a.click(); a.remove();
}

document.addEventListener('DOMContentLoaded', mountAdminShell);
document.addEventListener('htmx:afterSwap', e => { if (e.detail.target && e.detail.target.id === 'adm-main') refreshShell(); });
// Nut Back/Forward: htmx khoi phuc HTML da luu nhung KHONG chay lai script cua trang -> chay lai de nut/bieu do hoat dong
document.addEventListener('htmx:historyRestore', () => {
  refreshShell();
  document.querySelectorAll('#page script').forEach(old => { const s = document.createElement('script'); s.textContent = old.textContent; old.replaceWith(s); });
});

/* ------------------------------------------------------------------ toast & hop thoai xac nhan */
function toast(msg, kind = 'success') {
  let box = document.getElementById('adm-toast');
  if (!box) { box = document.createElement('div'); box.id = 'adm-toast'; box.className = 'toast toast-end toast-bottom z-50'; document.body.appendChild(box); }
  const el = document.createElement('div');
  el.className = `alert alert-${kind} shadow-lg text-sm`; el.innerHTML = `<span>${esc(msg)}</span>`;
  box.appendChild(el); setTimeout(() => el.remove(), 3200);
}
function confirmBox({ title, message, okText = 'Xác nhận', okClass = 'btn-primary', onOk }) {
  const d = document.createElement('dialog'); d.className = 'modal';
  d.innerHTML = `<div class="modal-box"><h3 class="font-bold text-lg">${esc(title)}</h3><p class="py-3 text-sm text-base-content/70">${message}</p>
    <div class="modal-action"><button class="btn" data-no>Huỷ</button><button class="btn ${okClass}" data-ok>${esc(okText)}</button></div></div>
    <form method="dialog" class="modal-backdrop"><button>close</button></form>`;
  document.body.appendChild(d); d.showModal();
  const close = () => { d.close(); d.remove(); };
  d.querySelector('[data-no]').onclick = close;
  d.querySelector('[data-ok]').onclick = () => { close(); onOk && onOk(); };
  d.addEventListener('close', () => d.remove());
}

/* ------------------------------------------------------------------ bieu do (SVG/HTML thuan, khong thu vien)
   Quy tac: 1 mau (primary), cot <= 24px bo 4px o dau cot, luoi 1px nhat, 1 truc, chi ghi nhan o cot cao nhat,
   tooltip khi re chuot, co nut "Xem dang bang" cho nguoi doc man hinh / in an. */
const Chart = (() => {
  let tip;
  const showTip = (html, x, y) => {
    if (!tip) { tip = document.createElement('div'); tip.className = 'fixed z-50 pointer-events-none hidden bg-base-100 border border-base-300 shadow-lg rounded-field px-3 py-2 text-xs'; document.body.appendChild(tip); }
    tip.innerHTML = html; tip.classList.remove('hidden');
    const r = tip.getBoundingClientRect();
    tip.style.left = Math.min(x + 12, innerWidth - r.width - 8) + 'px';
    tip.style.top = Math.max(8, y - r.height - 12) + 'px';
  };
  const hideTip = () => tip && tip.classList.add('hidden');
  const niceStep = (max, n = 4) => { const raw = (max || 1) / n, p = Math.pow(10, Math.floor(Math.log10(raw))), f = raw / p; return (f <= 1 ? 1 : f <= 2 ? 2 : f <= 5 ? 5 : 10) * p; };
  const compact = v => v >= 1e6 ? (v / 1e6).toLocaleString('vi-VN') + 'tr' : v >= 1e3 ? (v / 1e3) + 'k' : String(v);

  // Khung chung: vung ve + bang an; nut [data-toggle-table] trong the .chart-card chuyen qua lai.
  function frame(el, data, o) {
    el.innerHTML = '<div class="chart-plot"></div><div class="chart-table hidden overflow-x-auto"></div>';
    el.querySelector('.chart-table').innerHTML = `<table class="table table-sm"><thead><tr><th>${o.colLabel || 'Mục'}</th><th class="text-right">${o.colValue || 'Giá trị'}</th></tr></thead><tbody>${
      data.map(d => `<tr><td>${esc(d.label)}</td><td class="text-right tabular-nums">${(o.fmtValue || fmt)(d.value)}</td></tr>`).join('')}</tbody></table>`;
    const btn = (el.closest('[data-chart-card]') || document).querySelector('[data-toggle-table]');
    if (btn && !btn.dataset.wired) {
      btn.dataset.wired = '1';
      btn.addEventListener('click', () => {
        const showTable = el.querySelector('.chart-table').classList.toggle('hidden') === false;
        el.querySelector('.chart-plot').classList.toggle('hidden', showTable);
        btn.textContent = showTable ? 'Xem biểu đồ' : 'Xem dạng bảng'; btn.setAttribute('aria-pressed', showTable);
      });
    }
    return el.querySelector('.chart-plot');
  }

  function columns(el, data, o = {}) {
    const plot = frame(el, data, o);
    const fmtVal = o.fmtValue || fmt;
    const draw = () => {
      const W = el.clientWidth || 600, H = o.height || 220, m = { l: 44, r: 8, t: 18, b: 26 };
      const step = niceStep(Math.max(...data.map(d => d.value)), 4), max = step * 4;
      const iw = W - m.l - m.r, ih = H - m.t - m.b, slot = iw / data.length, bw = Math.min(24, slot * 0.6);
      const y = v => m.t + ih - v / max * ih, base = m.t + ih;
      const maxI = data.reduce((b, d, i) => d.value > data[b].value ? i : b, 0);
      let s = `<svg class="block w-full" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}" role="img" aria-label="${esc(o.label || 'Biểu đồ cột')}">`;
      for (let i = 0; i <= 4; i++) {
        const yy = y(i * step);
        s += `<line x1="${m.l}" x2="${W - m.r}" y1="${yy}" y2="${yy}" stroke="var(--color-base-300)" stroke-width="1"/>`;
        s += `<text x="${m.l - 8}" y="${yy + 4}" text-anchor="end" font-size="11" fill="currentColor" opacity=".6">${compact(i * step)}</text>`;
      }
      const every = slot < 34 ? 2 : 1;
      data.forEach((d, i) => {
        const x = m.l + slot * i + (slot - bw) / 2, yy = y(d.value), h = base - yy, r = Math.max(0, Math.min(4, bw / 2, h));
        s += `<path class="bar" data-i="${i}" fill="var(--color-primary)" d="M${x},${base} V${yy + r} Q${x},${yy} ${x + r},${yy} H${x + bw - r} Q${x + bw},${yy} ${x + bw},${yy + r} V${base} Z"/>`;
        if (i === maxI) s += `<text x="${x + bw / 2}" y="${yy - 6}" text-anchor="middle" font-size="11" font-weight="600" fill="currentColor">${compact(d.value)}</text>`;
        if (i % every === 0) s += `<text x="${x + bw / 2}" y="${H - 8}" text-anchor="middle" font-size="11" fill="currentColor" opacity=".6">${esc(d.label)}</text>`;
        s += `<rect class="hit" data-i="${i}" x="${m.l + slot * i}" y="${m.t}" width="${slot}" height="${ih}" fill="transparent"/>`;
      });
      plot.innerHTML = s + '</svg>';
      const bars = plot.querySelectorAll('.bar');
      plot.querySelectorAll('.hit').forEach(r => {
        r.addEventListener('mouseenter', e => { const i = +r.dataset.i; bars.forEach(b => b.style.opacity = +b.dataset.i === i ? 1 : .4); });
        r.addEventListener('mousemove', e => { const d = data[+r.dataset.i]; showTip(o.tip ? o.tip(d) : `<div class="font-semibold">${esc(d.label)}</div><div class="tabular-nums">${fmtVal(d.value)}</div>`, e.clientX, e.clientY); });
        r.addEventListener('mouseleave', () => { bars.forEach(b => b.style.opacity = 1); hideTip(); });
      });
    };
    draw();
    if (window.ResizeObserver) { let w = el.clientWidth; new ResizeObserver(() => { if (el.clientWidth !== w) { w = el.clientWidth; draw(); } }).observe(el); }
  }

  function hbars(el, rows, o = {}) {
    const plot = frame(el, rows, o);
    const fmtVal = o.fmtValue || fmt, max = Math.max(...rows.map(r => r.value)) || 1;
    plot.innerHTML = '<div class="space-y-3">' + rows.map((r, i) => `
      <div class="grid grid-cols-[minmax(0,9rem)_1fr_auto] items-center gap-3 text-sm" data-i="${i}">
        <div class="truncate" title="${esc(r.label)}">${esc(r.label)}</div>
        <div class="h-5 flex items-center"><div class="h-3 rounded-r-[4px]" style="width:${Math.max(2, r.value / max * 100)}%;background:var(--color-primary)"></div></div>
        <div class="tabular-nums text-base-content/70 text-right min-w-12">${fmtVal(r.value)}</div>
      </div>`).join('') + '</div>';
    plot.querySelectorAll('[data-i]').forEach(row => {
      row.addEventListener('mousemove', e => { const d = rows[+row.dataset.i]; showTip(`<div class="font-semibold">${esc(d.label)}</div><div class="tabular-nums">${fmtVal(d.value)}</div>${d.sub ? `<div class="text-base-content/60">${esc(d.sub)}</div>` : ''}`, e.clientX, e.clientY); });
      row.addEventListener('mouseleave', hideTip);
    });
  }

  // Sparkline 12 diem: duong 2px, cham cuoi r=4 co vong 2px mau nen
  function spark(values, w = 96, h = 28) {
    const mn = Math.min(...values), mx = Math.max(...values), pad = 5;
    const pts = values.map((v, i) => [pad + i * (w - 2 * pad) / (values.length - 1), h - pad - (v - mn) / ((mx - mn) || 1) * (h - 2 * pad)]);
    const [lx, ly] = pts[pts.length - 1];
    return `<svg width="${w}" height="${h}" viewBox="0 0 ${w} ${h}" aria-hidden="true"><polyline points="${pts.map(p => p.join(',')).join(' ')}" fill="none" stroke="var(--color-primary)" stroke-opacity=".55" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/><circle cx="${lx}" cy="${ly}" r="4" fill="var(--color-primary)" stroke="var(--color-base-100)" stroke-width="2"/></svg>`;
  }
  return { columns, hbars, spark };
})();

// Chuoi doanh thu gia lap co dinh (khong ngau nhien that su de moi lan mo ra giong nhau)
function genSeries(n, seed, base, spread) {
  let a = seed >>> 0;
  const rnd = () => { a |= 0; a = a + 0x6D2B79F5 | 0; let t = Math.imul(a ^ a >>> 15, 1 | a); t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t; return ((t ^ t >>> 14) >>> 0) / 4294967296; };
  return Array.from({ length: n }, (_, i) => Math.round((base + (rnd() - .35) * spread + Math.sin(i / 2) * spread * .15) / 10000) * 10000);
}
const dayLabels = (n, endDay = 2, endMonth = 10) => Array.from({ length: n }, (_, i) => { const d = new Date(2026, endMonth - 1, endDay - (n - 1 - i)); return d.getDate() + '/' + (d.getMonth() + 1); });
