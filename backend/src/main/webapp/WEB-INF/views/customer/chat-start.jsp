<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%--
  Buoc dau tien cua chat khi khach CHUA dang nhap: chon dang nhap (dung tai khoan, luu lich su) hoac nhap ten de chat nhanh voi tu cach khach vang lai.
  ChatServlet truyen: error (loi nhap ten, co the null), guestName (ten da go, de dien lai khi loi).
--%>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Chat với shop — Nông Việt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>
<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main class="max-w-3xl mx-auto px-4 py-10">
  <div class="text-center mb-6">
    <div class="inline-flex size-14 items-center justify-center rounded-full bg-primary/15 text-primary"><i data-lucide="message-circle" class="size-7"></i></div>
    <h1 class="text-2xl font-bold mt-3">Chat với shop</h1>
    <p class="text-base-content/70 mt-1">Hỏi tư vấn về hạt giống, phân bón, máy móc… Nhân viên sẽ phản hồi trong giờ làm việc.</p>
  </div>

  <div class="grid gap-4 md:grid-cols-2">
    <%-- Co tai khoan: dang nhap roi quay lai /chat --%>
    <div class="card bg-base-100 border border-base-300 p-6 flex flex-col">
      <div class="flex items-center gap-2 font-semibold"><i data-lucide="user-check" class="size-5 text-primary"></i> Tôi đã có tài khoản</div>
      <p class="text-sm text-base-content/60 mt-2 flex-1">Đăng nhập để shop biết bạn là ai, lưu lịch sử chat và tiện theo dõi đơn hàng.</p>
      <a href="${ctx}/login?next=/chat" class="btn btn-primary mt-4">Đăng nhập</a>
      <a href="${ctx}/register" class="btn btn-outline btn-sm mt-2">Tạo tài khoản mới</a>
    </div>

    <%-- Khach vang lai: chi can nhap ten --%>
    <form method="post" action="${ctx}/chat/guest" class="card bg-base-100 border border-base-300 p-6 flex flex-col">
      <div class="flex items-center gap-2 font-semibold"><i data-lucide="user-round" class="size-5 text-primary"></i> Chat nhanh, không cần tài khoản</div>
      <p class="text-sm text-base-content/60 mt-2">Cho shop biết tên để tiện xưng hô.</p>
      <label class="form-control mt-3">
        <span class="label">Tên của bạn</span>
        <input name="name" class="input w-full ${empty error ? '' : 'input-error'}" maxlength="60" required autocomplete="name" placeholder="Ví dụ: Anh Nam" value="<c:out value='${guestName}'/>">
      </label>
      <c:if test="${not empty error}"><p class="text-error text-sm mt-1" role="alert"><c:out value="${error}"/></p></c:if>
      <button class="btn btn-secondary mt-4"><i data-lucide="message-circle" class="size-4"></i> Bắt đầu chat</button>
      <p class="text-xs text-base-content/50 mt-2">Cuộc chat chỉ lưu trong phiên này; đóng trình duyệt có thể mất.</p>
    </form>
  </div>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>
