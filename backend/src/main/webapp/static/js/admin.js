/* JS dung chung cua CMS Admin. Nap 1 lan trong <head> (layout.jsp). Dat o day thay vi <script> trong trang vi
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

// Xem truoc the san pham trong form them/sua san pham: goi tu oninput/onchange cua form (product-form.jsp)
function nvProductPreview() {
  var f = document.getElementById('product-form');
  if (!f) return;
  var money = function (n) { return (n > 0 ? Number(n).toLocaleString('vi-VN') : '0') + '₫'; };
  var cat = f.elements.category;
  document.getElementById('pv-name').textContent = f.elements.name.value.trim() || 'Tên sản phẩm';
  document.getElementById('pv-price').textContent = money(Number(f.elements.price.value));
  document.getElementById('pv-cat').textContent = cat.value ? cat.options[cat.selectedIndex].text : 'Danh mục';
  var s = f.elements.stock.value, box = document.getElementById('pv-stock');
  box.innerHTML = s === '' ? '' : Number(s) === 0 ? '<span class="badge badge-neutral badge-sm">Hết hàng</span>'
    : Number(s) <= 5 ? '<span class="badge badge-warning badge-sm">Chỉ còn ' + Number(s) + '</span>' : '<span class="text-success">● Còn hàng</span>';

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
