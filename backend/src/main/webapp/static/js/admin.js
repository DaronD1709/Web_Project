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

// ---------------------------------------------------------------------------------------------------------------------
// HOP THOAI XAC NHAN rieng (thay cho confirm() mac dinh cua trinh duyet, vi cai mac dinh xau va khong dong bo giao dien).
//
// Cach dung trong JSP: gan hx-confirm="Cau hoi?" len form hoac nut (nhu truoc day, khong phai doi gi them):
//     <form method="post" action="..." hx-confirm="Xoá voucher &quot;ABC&quot;? Không thể hoàn tác.">
// htmx KHONG tu hien confirm() nua: truoc khi gui request no phat su kien "htmx:confirm" (kem cau hoi trong evt.detail.question),
// ta chan lai (preventDefault), hien hop thoai rieng; nguoi dung dong y thi goi evt.detail.issueRequest(true) de gui tiep
// (tham so true = "da xac nhan roi, dung hoi lai"). Bam Huy / Esc / bam ra ngoai = khong gui gi ca.
// Tuy chon tren chinh phan tu co hx-confirm:  data-confirm-title="..."  data-confirm-ok="Chu tren nut dong y".
// Cau hoi bat dau bang Xoa / Huy / Tu choi / Khoa duoc coi la thao tac nguy hiem: icon va nut dong y mau do.
// ---------------------------------------------------------------------------------------------------------------------
var nvConfirmBox = null; // <dialog> tao 1 lan khi can, dung lai cho moi lan hoi

function nvConfirmBuild() {
  var d = document.createElement('dialog'); // <dialog> co san cua trinh duyet: showModal() tu khoa trang phia sau, Esc dong, Tab khong thoat ra ngoai
  d.id = 'confirm-dialog';
  d.className = 'modal';
  // Noi dung CO DINH (khong chen chuoi do nguoi dung nhap vao day); cau hoi duoc gan sau bang textContent nen khong the chen HTML.
  d.innerHTML =
    '<div class="modal-box max-w-md">' +
      '<div class="flex items-start gap-3">' +
        '<span id="cf-icon" class="size-10 shrink-0 rounded-full flex items-center justify-center"><i data-lucide="triangle-alert" class="size-5"></i></span>' +
        '<div class="min-w-0"><h3 id="cf-title" class="font-semibold text-lg"></h3><p id="cf-msg" class="py-2 text-sm text-base-content/70 whitespace-pre-line"></p></div>' +
      '</div>' +
      '<div class="modal-action">' +
        '<button type="button" id="cf-no" class="btn btn-ghost">Quay lại</button>' +
        '<button type="button" id="cf-yes" class="btn"></button>' +
      '</div>' +
    '</div>' +
    '<form method="dialog" class="modal-backdrop"><button aria-label="Đóng">đóng</button></form>'; // bam ra ngoai hop thoai = dong
  document.body.appendChild(d);
  // returnValue cho biet nut nao dong hop thoai: "yes" = dong y; Esc / bam ra ngoai / "Quay lai" = khong phai "yes"
  d.querySelector('#cf-yes').addEventListener('click', function () { d.close('yes'); });
  d.querySelector('#cf-no').addEventListener('click', function () { d.close('no'); });
  return d;
}

// Hien hop thoai, tra ve Promise<boolean> (true = nguoi dung dong y). elt = phan tu co hx-confirm (de doc data-confirm-*).
function nvConfirm(question, elt) {
  var d = nvConfirmBox || (nvConfirmBox = nvConfirmBuild());
  var danger = /^(xoá|huỷ|từ chối|khoá)/.test(String(question).normalize('NFC').toLowerCase());
  var data = (elt && elt.dataset) || {};
  d.querySelector('#cf-title').textContent = data.confirmTitle || (danger ? 'Bạn chắc chắn chứ?' : 'Xác nhận thao tác');
  d.querySelector('#cf-msg').textContent = question;
  var icon = d.querySelector('#cf-icon'), yes = d.querySelector('#cf-yes');
  icon.className = 'size-10 shrink-0 rounded-full flex items-center justify-center ' + (danger ? 'bg-error/10 text-error' : 'bg-primary/10 text-primary');
  yes.className = 'btn ' + (danger ? 'btn-error' : 'btn-primary');
  yes.textContent = data.confirmOk || (danger ? 'Đồng ý' : 'Xác nhận');
  if (window.nvIcons) nvIcons(); // the <i data-lucide> vua tao can duoc doi thanh SVG
  return new Promise(function (resolve) {
    d.addEventListener('close', function () { resolve(d.returnValue === 'yes'); }, { once: true }); // moi lan hoi 1 lan nghe, tu go
    d.returnValue = '';
    d.showModal();
    (danger ? d.querySelector('#cf-no') : yes).focus(); // nguy hiem thi mac dinh chon "Quay lai" de lo bam Enter khong xoa nham
  });
}

// htmx phat su kien nay truoc MOI request; chi chen vao khi phan tu co hx-confirm (evt.detail.question co gia tri)
document.body.addEventListener('htmx:confirm', function (evt) {
  if (!evt.detail.question) return;               // khong co hx-confirm -> de htmx gui binh thuong
  evt.preventDefault();                           // chan confirm() mac dinh va chan request cho den khi nguoi dung chon
  nvConfirm(evt.detail.question, evt.detail.elt).then(function (ok) {
    if (ok) evt.detail.issueRequest(true);        // true = bo qua buoc hoi lai
  });
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
    : Number(s) <= 5 ? '<span class="badge badge-warning badge-sm">Chỉ còn ' + Number(s) + '</span>' : '<span class="text-success inline-flex items-center gap-1"><i data-lucide="circle-check" class="size-3.5"></i> Còn hàng</span>';
  if (window.nvIcons) nvIcons(); // vua gan HTML moi co the chua the <i data-lucide> -> ve lai icon

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
