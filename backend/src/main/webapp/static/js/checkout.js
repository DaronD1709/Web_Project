// Chỉ điều khiển giao diện; giá và quyền đặt hàng vẫn được server kiểm tra.
(function () {
  let previewPending = false;
  function updateSubmit() {
    const button = document.getElementById('place-order');
    const draft = document.getElementById('voucher-code');
    const applied = document.getElementById('applied-voucher');
    if (!button || !draft || !applied) return;
    const changed = draft.value.trim().toUpperCase() !== applied.value.toUpperCase();
    button.disabled = previewPending || button.dataset.unavailable === 'true' || changed;
  }
  document.addEventListener('input', function (event) {
    if (event.target.id === 'voucher-code') updateSubmit();
  });
  document.addEventListener('change', function (event) {
    if (event.target.name !== 'addressId') return;
    document.querySelectorAll('[data-address-option]').forEach(function (label) {
      const checked = label.querySelector('input').checked;
      label.classList.toggle('border-primary', checked);
      label.classList.toggle('bg-primary/5', checked);
      label.classList.toggle('border-base-300', !checked);
    });
  });
  document.addEventListener('htmx:beforeSwap', function (event) {
    // 422 của voucher vẫn cần thay thông báo và tổng tiền OOB.
    if (event.detail.xhr.status === 422 && event.detail.target.id === 'voucher-result') {
      event.detail.shouldSwap = true;
      event.detail.isError = false;
    }
  });
  document.addEventListener('htmx:beforeRequest', function (event) {
    if (event.detail.elt.id === 'voucher-preview') {
      previewPending = true;
      updateSubmit();
    }
  });
  document.addEventListener('htmx:afterRequest', function (event) {
    if (event.detail.elt.id === 'voucher-preview') {
      previewPending = false;
      updateSubmit();
    }
  });
  document.addEventListener('htmx:afterSwap', updateSubmit);
  const form = document.getElementById('checkout-form');
  if (form) form.addEventListener('submit', function () {
    const button = document.getElementById('place-order');
    if (button) button.disabled = true;
  });
  window.addEventListener('pageshow', updateSubmit);
  updateSubmit();
}());
