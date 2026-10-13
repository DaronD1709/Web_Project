// Hop goi y tim kiem o header (#search-suggest). htmx nhet HTML vao hop; file nay chi lo AN/HIEN hop:
// hien khi co noi dung, an khi bam ra ngoai, bam Escape, hoac khi gui form tim kiem.
(function () {
  function box() { return document.getElementById('search-suggest'); }
  function hide() { var b = box(); if (b) b.classList.add('hidden'); }

  // Sau moi lan htmx thay noi dung hop: co noi dung thi hien, rong thi an
  document.addEventListener('htmx:afterSwap', function (e) {
    var b = box();
    if (b && e.detail && e.detail.target === b) b.classList.toggle('hidden', b.textContent.trim() === '' && !b.querySelector('a'));
  });
  document.addEventListener('click', function (e) { if (!e.target.closest('#header-search')) hide(); });
  document.addEventListener('keydown', function (e) { if (e.key === 'Escape') hide(); });
  document.addEventListener('submit', function (e) { if (e.target.id === 'header-search') hide(); });
})();
