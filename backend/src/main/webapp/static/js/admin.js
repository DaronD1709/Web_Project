/* JS dung chung cua CMS Admin. Nap 1 lan trong <head> (admin-layout.jsp). Dat o day thay vi <script> trong trang vi
   script ben trong noi dung htmx swap vao khong chay lai khi bam Back (lich su htmx chi luu HTML).

   TONG QUAN FILE (moi nhom ham dung o dau)
     1. Toast (thong bao nho goc duoi-phai)      : nvToast  <- header HX-Trigger do server gui, hoac thong bao "flash" sau redirect
     2. Xem truoc the san pham                  : nvProductPreview   (admin-product-form.jsp)
     3. Xem truoc the voucher                   : nvVoucherPreview   (admin-voucher-form.jsp)
     4. Chat voi khach                          : nvChatChip, nvChatScroll   (admin-chat.jsp)
     5. Bieu do tong quan                       : nvToggleChartTable (admin-dashboard.jsp)
   Hop thoai xac nhan (hx-confirm) KHONG nam o day ma o static/js/confirm.js (dung chung voi trang khach). Ve lai icon
   Lucide sau moi lan htmx doi noi dung nam o static/js/icons.js (ham nvIcons).

   QUY UOC CHUNG
     - Ham ten nv... khai bao o muc ngoai cung de goi duoc tu thuoc tinh onclick/oninput trong JSP (vd oninput="nvProductPreview()").
     - Moi ham doc/ghi DOM deu kiem tra "phan tu co ton tai khong" truoc (if (!f) return) vi file nay duoc nap o MOI trang admin,
       con form/khung chat chi co o mot vai trang.
     - Hai su kien can nho: DOMContentLoaded = trang vua tai xong (mo truc tiep, F5);  htmx:afterSettle = htmx vua thay noi dung xong
       (bam link/loc/phan trang bang htmx, KHONG tai lai trang) -> muon chay cho CA hai truong hop phai gan vao ca hai. */

// Toast goc duoi-phai
// type = "error" -> hop do; con lai (vd "success") -> hop xanh. Tu bien mat sau 3,5 giay.
// #toast-box la o trong dat san trong admin-layout.jsp (lop daisyUI "toast toast-end toast-bottom" ghim vao goc man hinh).
function nvToast(message, type) {
  var el = document.createElement('div');
  el.className = 'alert shadow-lg text-sm ' + (type === 'error' ? 'alert-error' : 'alert-success');
  el.textContent = message; // textContent (khong phai innerHTML): thong bao co ten nguoi dung/san pham van khong chen duoc HTML
  document.getElementById('toast-box').appendChild(el);
  setTimeout(function () { el.remove(); }, 3500);
}

// (1) Server gui header HX-Trigger {"showToast":{...}} (util/HtmxUtil.toast) -> htmx phat su kien nay
// htmx doi JSON trong header thanh su kien tren trang, ten su kien = khoa ngoai cung ("showToast"), e.detail = {message, type}.
document.body.addEventListener('showToast', function (e) {
  var d = e.detail || {};
  nvToast(d.message, d.type);
});

// (2) Thong bao "flash" sau khi redirect, in san trong HTML cua trang day du (AdminView.flash)
// Sau khi them/sua/xoa thanh cong server redirect (khong the gui header cho request moi), nen luu thong bao vao session roi in vao
// the an <div id="flash" data-type="..."> trong admin-layout.jsp; luc trang tai xong ta doc ra va hien thanh toast.
document.addEventListener('DOMContentLoaded', function () {
  var f = document.getElementById('flash');
  if (f) nvToast(f.textContent, f.dataset.type);
});

// Xem truoc the san pham trong form them/sua san pham: goi tu oninput/onchange cua form (admin-product-form.jsp)
// Chi doc gia tri dang nhap trong form roi chep sang the "Xem truoc" ben phai (id pv-*), khong gui gi len server.
function nvProductPreview() {
  var f = document.getElementById('product-form');
  if (!f) return; // trang hien tai khong co form san pham (vd danh sach) thi bo qua
  // money: 45000 -> "45.000₫" (dinh dang kieu Viet)
  var money = function (n) { return (n > 0 ? Number(n).toLocaleString('vi-VN') : '0') + '₫'; };
  var cat = f.elements.category; // f.elements.<name> = o nhap co thuoc tinh name="..." trong form
  // Chep gia tri dang go o form sang the xem truoc (id pv-* nam trong vung 4 cua admin-product-form.jsp)
  document.getElementById('pv-name').textContent = f.elements.name.value.trim() || 'Tên sản phẩm';
  document.getElementById('pv-price').textContent = money(Number(f.elements.price.value));
  document.getElementById('pv-cat').textContent = cat.value ? cat.options[cat.selectedIndex].text : 'Danh mục';
  // Badge ton kho giong the san pham that: 0 = Het hang, <= 5 = Chi con N, con lai = Con hang
  // (nguong 5 khop voi the san pham cua khach va muc "sap het hang" cua trang Tong quan). Chua nhap ton kho ('') thi de trong.
  var s = f.elements.stock.value, box = document.getElementById('pv-stock');
  box.innerHTML = s === '' ? '' : Number(s) === 0 ? '<span class="badge badge-neutral badge-sm">Hết hàng</span>'
    : Number(s) <= 5 ? '<span class="badge badge-warning badge-sm">Chỉ còn ' + Number(s) + '</span>' : '<span class="text-success inline-flex items-center gap-1"><i data-lucide="circle-check" class="size-3.5"></i> Còn hàng</span>';
  if (window.nvIcons) nvIcons(); // vua gan HTML moi co the chua the <i data-lucide> -> ve lai icon

  // Neu vua chon anh moi: hien anh do (URL.createObjectURL = duong dan tam trong trinh duyet, chua gui len server) va an anh cu
  var file = f.elements.image.files[0], img = document.getElementById('pv-img');
  if (file) {
    img.innerHTML = '<img class="h-36 w-full object-cover rounded-t-[var(--radius-box)]" alt="">';
    img.firstChild.src = URL.createObjectURL(file);
    var cur = document.getElementById('current-image'); if (cur) cur.classList.add('hidden'); // an dong "anh hien tai" vi sap bi thay
  }
}

// Dien san the "Xem truoc" theo gia tri hien co cua form (vd badge ton kho) khi mo trang, ke ca khi trang duoc htmx nhet vao
// (JSP in san ten/gia/danh muc nhung de trong badge ton kho, nen phai chay 1 lan luc mo trang de dien not phan do)
document.addEventListener('DOMContentLoaded', nvProductPreview);
document.body.addEventListener('htmx:afterSettle', nvProductPreview);

// Xem truoc the voucher trong form tao/sua voucher: goi tu oninput/onchange cua form (admin-voucher-form.jsp)
// Khac the san pham: JSP da in san toan bo the xem truoc luc mo form, nen ham nay CHI can chay khi nguoi dung go/chon (khong gan
// vao DOMContentLoaded). Cung chi doc form, khong gui gi len server.
function nvVoucherPreview() {
  var f = document.getElementById('voucher-form');
  if (!f) return; // trang hien tai khong co form voucher
  var money = function (n) { return Number(n).toLocaleString('vi-VN') + '₫'; };
  // type = nut radio "percent" (phan tram) hoac "fixed" (so tien co dinh), value = muc giam, min = don toi thieu
  var percent = f.elements.type.value === 'percent', v = f.elements.value.value, min = Number(f.elements.min.value);
  document.getElementById('unit').textContent = percent ? '%' : '₫'; // don vi o canh o "Gia tri giam" doi theo loai da chon
  document.getElementById('pv-code').textContent = f.elements.code.value.toUpperCase() || 'MÃ';
  document.getElementById('pv-val').textContent = 'Giảm ' + (v ? (percent ? v + '%' : money(v)) : '…');
  document.getElementById('pv-min').textContent = min ? 'Đơn từ ' + money(min) : 'Không yêu cầu đơn tối thiểu';
}

// ---- Chat voi khach (admin-chat.jsp) ----
// Dien cau tra loi nhanh vao o nhap
// btn = nut chip vua bam (onclick="nvChatChip(this)"); chi dien san noi dung vao o nhap, Admin bam Gui moi thuc su gui.
function nvChatChip(btn) {
  var input = document.getElementById('chat-input');
  if (input) { input.value = btn.textContent; input.focus(); }
}

// Cuon khung chat xuong tin cuoi, nhung CHI khi co tin MOI (polling 3 giay khong lam nhay khung neu dang doc tin cu)
// Cach nhan biet "co tin moi": dem so dong tin (.chat-bubble-row), so voi con so da luu o box.dataset.count lan truoc.
// Khong doi -> khong dung scrollTop, Admin keo len doc tin cu thi khung dung yen du polling van thay HTML moi moi 3 giay.
function nvChatScroll() {
  var box = document.getElementById('chat-msgs');
  if (!box) return;
  var n = box.querySelectorAll('.chat-bubble-row').length;
  if (String(n) !== box.dataset.count) { // so bong bong doi (hoac lan dau mo) -> cuon xuong cuoi
    box.dataset.count = n;
    box.scrollTop = box.scrollHeight; // scrollHeight = tong chieu cao noi dung -> dat scrollTop bang no = cuon het xuong day
  }
}
document.addEventListener('DOMContentLoaded', nvChatScroll);   // mo trang chat lan dau / chon khach khac
document.body.addEventListener('htmx:afterSettle', nvChatScroll); // sau moi lan polling hoac gui tin (htmx thay HTML)

// ---- Bieu do (Tong quan, Thong ke) ----
// Nut "Xem dang bang": an bieu do, hien bang so lieu tuong ung trong cung the (data-chart-card) va nguoc lai
// Moi the bieu do co 2 khoi: [data-chart] (bieu do ve bang HTML/CSS) va [data-chart-table] (bang so lieu an san, de nguoi
// khong nhin duoc mau/cot van doc duoc). aria-pressed luu trang thai nut: "true" = dang xem dang bang.
function nvToggleChartTable(btn) {
  var card = btn.closest('[data-chart-card]'); // the cha gan nhat chua ca bieu do lan bang cua nut nay (moi the doc lap nhau)
  var showTable = btn.getAttribute('aria-pressed') !== 'true'; // dang la bieu do -> bam se chuyen sang bang
  card.querySelector('[data-chart]').classList.toggle('hidden', showTable);
  card.querySelector('[data-chart-table]').classList.toggle('hidden', !showTable);
  btn.setAttribute('aria-pressed', showTable ? 'true' : 'false');
  btn.textContent = showTable ? 'Xem dạng biểu đồ' : 'Xem dạng bảng';
}
