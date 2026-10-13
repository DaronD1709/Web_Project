/* JS dung chung cua CMS Admin. Nap 1 lan trong <head> (admin-layout.jsp). Dat o day thay vi <script> trong trang vi
   script ben trong noi dung htmx swap vao khong chay lai khi bam Back (lich su htmx chi luu HTML). */

// Toast goc duoi-phai
function nvToast(message, type) {
  var el = document.createElement('div');
  el.className = 'alert shadow-lg text-sm ' + (type === 'error' ? 'alert-error' : 'alert-success');
  el.textContent = message;
  document.getElementById('toast-box').appendChild(el);
  setTimeout(function () { el.remove(); }, 3500);
}

// (1) Server gui header HX-Trigger {"showToast":{...}} (util/HtmxUtil.toast) -> htmx phat su kien nay
document.body.addEventListener('showToast', function (e) {
  var d = e.detail || {};
  nvToast(d.message, d.type);
});

// (2) Thong bao "flash" sau khi redirect, in san trong HTML cua trang day du (AdminView.flash)
document.addEventListener('DOMContentLoaded', function () {
  var f = document.getElementById('flash');
  if (f) nvToast(f.textContent, f.dataset.type);
});

// Xem truoc the san pham trong form them/sua san pham: goi tu oninput/onchange cua form (admin-product-form.jsp)
function nvProductPreview() {
  var f = document.getElementById('product-form');
  if (!f) return; // trang hien tai khong co form san pham (vd danh sach) thi bo qua
  // money: 45000 -> "45.000₫" (dinh dang kieu Viet)
  var money = function (n) { return (n > 0 ? Number(n).toLocaleString('vi-VN') : '0') + '₫'; };
  var cat = f.elements.category;
  // Chep gia tri dang go o form sang the xem truoc (id pv-* nam trong vung 4 cua admin-product-form.jsp)
  document.getElementById('pv-name').textContent = f.elements.name.value.trim() || 'Tên sản phẩm';
  document.getElementById('pv-price').textContent = money(Number(f.elements.price.value));
  document.getElementById('pv-cat').textContent = cat.value ? cat.options[cat.selectedIndex].text : 'Danh mục';
  // Badge ton kho giong the san pham that: 0 = Het hang, <= 5 = Chi con N, con lai = Con hang
  var s = f.elements.stock.value, box = document.getElementById('pv-stock');
  box.innerHTML = s === '' ? '' : Number(s) === 0 ? '<span class="badge badge-neutral badge-sm">Hết hàng</span>'
    : Number(s) <= 5 ? '<span class="badge badge-warning badge-sm">Chỉ còn ' + Number(s) + '</span>' : '<span class="text-success">● Còn hàng</span>';

  // Neu vua chon anh moi: hien anh do (URL.createObjectURL = duong dan tam trong trinh duyet, chua gui len server) va an anh cu
  var file = f.elements.image.files[0], img = document.getElementById('pv-img');
  if (file) {
    img.innerHTML = '<img class="h-36 w-full object-cover rounded-t-[var(--radius-box)]" alt="">';
    img.firstChild.src = URL.createObjectURL(file);
    var cur = document.getElementById('current-image'); if (cur) cur.classList.add('hidden');
  }
}

// Dien san the "Xem truoc" theo gia tri hien co cua form (vd badge ton kho) khi mo trang, ke ca khi trang duoc htmx nhet vao
document.addEventListener('DOMContentLoaded', nvProductPreview);
document.body.addEventListener('htmx:afterSettle', nvProductPreview);

// Xem truoc the voucher trong form tao/sua voucher: goi tu oninput/onchange cua form (admin-voucher-form.jsp)
function nvVoucherPreview() {
  var f = document.getElementById('voucher-form');
  if (!f) return; // trang hien tai khong co form voucher
  var money = function (n) { return Number(n).toLocaleString('vi-VN') + '₫'; };
  var percent = f.elements.type.value === 'percent', v = f.elements.value.value, min = Number(f.elements.min.value);
  document.getElementById('unit').textContent = percent ? '%' : '₫';
  document.getElementById('pv-code').textContent = f.elements.code.value.toUpperCase() || 'MÃ';
  document.getElementById('pv-val').textContent = 'Giảm ' + (v ? (percent ? v + '%' : money(v)) : '…');
  document.getElementById('pv-min').textContent = min ? 'Đơn từ ' + money(min) : 'Không yêu cầu đơn tối thiểu';
}

// ---- Chat voi khach (admin-chat.jsp) ----
// Dien cau tra loi nhanh vao o nhap
function nvChatChip(btn) {
  var input = document.getElementById('chat-input');
  if (input) { input.value = btn.textContent; input.focus(); }
}

// Cuon khung chat xuong tin cuoi, nhung CHI khi co tin MOI (polling 3 giay khong lam nhay khung neu dang doc tin cu)
function nvChatScroll() {
  var box = document.getElementById('chat-msgs');
  if (!box) return;
  var n = box.querySelectorAll('.chat-bubble-row').length;
  if (String(n) !== box.dataset.count) { // so bong bong doi (hoac lan dau mo) -> cuon xuong cuoi
    box.dataset.count = n;
    box.scrollTop = box.scrollHeight;
  }
}
document.addEventListener('DOMContentLoaded', nvChatScroll);
document.body.addEventListener('htmx:afterSettle', nvChatScroll);

// ---- Bieu do (Tong quan, Thong ke) ----
// Nut "Xem dang bang": an bieu do, hien bang so lieu tuong ung trong cung the (data-chart-card) va nguoc lai
function nvToggleChartTable(btn) {
  var card = btn.closest('[data-chart-card]');
  var showTable = btn.getAttribute('aria-pressed') !== 'true';
  card.querySelector('[data-chart]').classList.toggle('hidden', showTable);
  card.querySelector('[data-chart-table]').classList.toggle('hidden', !showTable);
  btn.setAttribute('aria-pressed', showTable ? 'true' : 'false');
  btn.textContent = showTable ? 'Xem dạng biểu đồ' : 'Xem dạng bảng';
}
