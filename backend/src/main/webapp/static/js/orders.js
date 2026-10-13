// Event delegation keeps the confirmation buttons working after a fragment swap.
document.addEventListener('click', function (event) {
  var button = event.target.closest('[data-order-dialog], [data-close-order-dialog]');
  if (!button) return;
  var id = button.getAttribute('data-order-dialog') || button.getAttribute('data-close-order-dialog');
  var dialog = document.getElementById(id);
  if (!dialog) return;
  if (button.hasAttribute('data-order-dialog')) dialog.showModal();
  else dialog.close();
});

// Server validation retains the reason and reopens the native modal with its backdrop.
function reopenOrderDialog(root) {
  var dialog = root.querySelector('dialog[data-order-dialog-reopen]');
  if (!dialog) return;
  dialog.removeAttribute('data-order-dialog-reopen');
  if (dialog.open) dialog.close();
  dialog.showModal();
}
document.addEventListener('DOMContentLoaded', function () { reopenOrderDialog(document); });
document.body.addEventListener('htmx:afterSwap', function (event) {
  if (event.detail.target.id === 'order-detail-body') reopenOrderDialog(event.detail.target);
});

document.body.addEventListener('htmx:beforeSwap', function (event) {
  // Only validation for the tracking form may replace the fragment on HTTP 422.
  if (event.detail.xhr.status === 422 && event.detail.target.id === 'order-detail-body') {
    event.detail.shouldSwap = true;
    event.detail.isError = false;
  }
});
