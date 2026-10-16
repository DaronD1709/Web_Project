/* HOP THOAI XAC NHAN rieng, dung chung cho CA khach hang LAN Admin (nap o common/head.jspf, defer).
   Thay cho confirm()/alert() mac dinh cua trinh duyet (xau, khong dong bo giao dien, khong tuy bien duoc).

   CACH DUNG TRONG JSP (hai cach, tuy form co dung htmx hay khong):
   (1) Form/nut dung htmx (hx-boost, hx-post, hx-get ...):  hx-confirm="Xoá voucher ABC? Không thể hoàn tác."
         htmx truoc khi gui request phat su kien "htmx:confirm" (kem cau hoi trong evt.detail.question). Ta chan lai
         (preventDefault), hien hop thoai; dong y thi goi evt.detail.issueRequest(true) de gui tiep (true = "da xac nhan roi,
         dung hoi lai").
   (2) Form thuong (khong htmx, gui POST that):  data-confirm="Xoá địa chỉ này?"  tren the <form>
         Ta bat su kien "submit" o giai doan capture (chay truoc moi xu ly khac), chan lai, hoi; dong y thi form.submit().
   Bam "Quay lai" / Esc / bam ra ngoai hop thoai = khong gui gi ca.

   Tuy chon tren chinh phan tu co hx-confirm hoac data-confirm:
       data-confirm-title="..."   tieu de hop thoai;      data-confirm-ok="..."   chu tren nut dong y.
   Cau hoi bat dau bang Xoa / Huy / Tu choi / Khoa (ke ca kieu bo dau "xóa", "hủy") duoc coi la thao tac NGUY HIEM:
   icon + nut dong y mau do, va nut "Quay lai" duoc chon san de lo bam Enter cung khong xoa nham. */

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

// Hien hop thoai, tra ve Promise<boolean> (true = nguoi dung dong y). elt = phan tu co hx-confirm/data-confirm (de doc data-confirm-*).
function nvConfirm(question, elt) {
  var d = nvConfirmBox || (nvConfirmBox = nvConfirmBuild());
  var danger = /^(xoá|xóa|huỷ|hủy|từ chối|khoá|khóa)/.test(String(question).normalize('NFC').toLowerCase());
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
    (danger ? d.querySelector('#cf-no') : yes).focus(); // nguy hiem thi mac dinh chon "Quay lai"
  });
}

// (1) htmx phat su kien nay truoc MOI request; chi chen vao khi phan tu co hx-confirm (evt.detail.question co gia tri)
document.body.addEventListener('htmx:confirm', function (evt) {
  if (!evt.detail.question) return;               // khong co hx-confirm -> de htmx gui binh thuong
  evt.preventDefault();                           // chan confirm() mac dinh va chan request cho den khi nguoi dung chon
  nvConfirm(evt.detail.question, evt.detail.elt).then(function (ok) {
    if (ok) evt.detail.issueRequest(true);        // true = bo qua buoc hoi lai
  });
});

// (2) Form thuong khong dung htmx: <form data-confirm="Cau hoi?">. capture=true de chay truoc cac xu ly "submit" khac.
document.addEventListener('submit', function (evt) {
  var form = evt.target;
  var question = form.dataset && form.dataset.confirm;
  if (!question) return;                          // form khong can xac nhan
  evt.preventDefault();                           // chan viec gui form cho den khi nguoi dung chon
  nvConfirm(question, form).then(function (ok) {
    if (ok) form.submit();                        // submit() gui that va KHONG phat lai su kien "submit" -> khong hoi lap vo han
  });
}, true);
