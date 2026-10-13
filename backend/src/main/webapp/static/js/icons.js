// Icon Lucide dung chung cho moi trang (khach + admin). Trong JSP viet <i data-lucide="shopping-cart" class="size-5"></i>;
// thu vien lucide (static/vendor/lucide-0.469.0.min.js, giay phep ISC) doi the <i> thanh <svg> cung class (kich thuoc dat bang class Tailwind size-N).
// htmx thay doi noi dung trang (loc san pham, doi tab admin, polling chat...) tao ra the <i> moi chua duoc doi -> phai ve lai sau moi lan thay.
function nvIcons() {
  if (window.lucide) window.lucide.createIcons();
}
document.addEventListener('DOMContentLoaded', nvIcons);   // lan dau tai trang
document.addEventListener('htmx:afterSettle', nvIcons);   // sau moi lan htmx thay noi dung (ke ca hx-swap-oob)
document.addEventListener('htmx:historyRestore', nvIcons); // bam Back/Forward, htmx khoi phuc trang tu bo nho dem
