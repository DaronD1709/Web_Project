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
