<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="vi" data-theme="nongnghiep">
<head>
  <title>Địa chỉ giao hàng — Nông Việt</title>
  <%@ include file="/WEB-INF/views/common/head.jspf" %>
</head>

<body class="bg-base-200 min-h-screen">
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<main class="max-w-7xl mx-auto px-4 py-6 grid grid-cols-1 lg:grid-cols-[15rem_minmax(0,1fr)] gap-6">

  <aside class="min-w-0">
    <c:set var="activeAccount" value="addresses"/>
    <%@ include file="/WEB-INF/views/common/account-nav.jspf" %>
  </aside>

  <section class="space-y-5 max-w-3xl min-w-0 w-full">

    <div>
      <h1 class="text-2xl font-bold">Địa chỉ giao hàng</h1>
      <p class="text-sm text-base-content/60 mt-1">
        Quản lý các địa chỉ được sử dụng khi đặt hàng.
      </p>
    </div>

    <%-- Thông báo sau khi thao tác thành công --%>
    <c:if test="${param.created eq '1'}">
      <div class="alert alert-success">
        <span>Đã thêm địa chỉ giao hàng.</span>
      </div>
    </c:if>

    <c:if test="${param.updated eq '1'}">
      <div class="alert alert-success">
        <span>Đã cập nhật địa chỉ giao hàng.</span>
      </div>
    </c:if>

    <c:if test="${param.deleted eq '1'}">
      <div class="alert alert-success">
        <span>Đã xóa địa chỉ giao hàng.</span>
      </div>
    </c:if>

    <c:if test="${param.defaultChanged eq '1'}">
      <div class="alert alert-success">
        <span>Đã thay đổi địa chỉ mặc định.</span>
      </div>
    </c:if>

    <c:if test="${not empty addressError}">
      <div class="alert alert-error">
        <span><c:out value="${addressError}"/></span>
      </div>
    </c:if>

    <%-- Danh sách địa chỉ --%>
    <div class="space-y-3">

      <c:choose>
        <c:when test="${empty addresses}">
          <div class="card bg-base-100 border border-dashed border-base-300">
            <div class="card-body items-center text-center py-10">
              <div class="text-5xl" aria-hidden="true">📍</div>
              <h2 class="card-title mt-2">
                Bạn chưa có địa chỉ giao hàng
              </h2>
              <p class="text-sm text-base-content/60">
                Hãy thêm địa chỉ đầu tiên để sử dụng khi đặt hàng.
              </p>
            </div>
          </div>
        </c:when>

        <c:otherwise>
          <c:forEach var="address" items="${addresses}">
            <c:set var="editingThis"
                   value="${formAction eq 'update' and formId == address.id}"/>

            <article class="card bg-base-100 border ${address['default'] ? 'border-primary' : 'border-base-300'}">
              <div class="card-body p-5 gap-3">

                <div class="flex flex-wrap items-start justify-between gap-3">

                  <div class="min-w-0">
                    <div class="flex flex-wrap items-center gap-2">
                      <h2 class="font-bold text-lg">
                        <c:out value="${address.recipientName}"/>
                      </h2>

                      <c:if test="${address['default']}">
                        <span class="badge badge-primary">
                          Mặc định
                        </span>
                      </c:if>
                    </div>

                    <p class="text-sm mt-1">
                      <c:out value="${address.phone}"/>
                    </p>

                    <p class="text-sm text-base-content/70 mt-1 break-words">
                      <c:out value="${address.street}"/>,
                      <c:out value="${address.city}"/>
                    </p>
                  </div>

                  <div class="flex flex-wrap gap-2">

                    <c:if test="${not address['default']}">
                      <form method="post"
                            action="${ctx}/account/addresses">
                        <input type="hidden"
                               name="csrfToken"
                               value="<c:out value='${csrfToken}'/>">
                        <input type="hidden"
                               name="action"
                               value="setDefault">
                        <input type="hidden"
                               name="id"
                               value="${address.id}">

                        <button type="submit"
                                class="btn btn-outline btn-primary btn-sm">
                          Đặt mặc định
                        </button>
                      </form>
                    </c:if>

                    <form method="post"
                          action="${ctx}/account/addresses"
                          onsubmit="return confirm('Bạn có chắc muốn xóa địa chỉ này?');">

                      <input type="hidden"
                             name="csrfToken"
                             value="<c:out value='${csrfToken}'/>">
                      <input type="hidden"
                             name="action"
                             value="delete">
                      <input type="hidden"
                             name="id"
                             value="${address.id}">

                      <button type="submit"
                              class="btn btn-outline btn-error btn-sm">
                        Xóa
                      </button>
                    </form>

                  </div>
                </div>

                <%-- Form sửa của từng địa chỉ --%>
                <details class="collapse collapse-arrow border border-base-300 bg-base-200/50"
                         ${editingThis ? 'open' : ''}>

                  <summary class="collapse-title font-medium min-h-0 py-3">
                    Chỉnh sửa địa chỉ
                  </summary>

                  <div class="collapse-content">
                    <form method="post"
                          action="${ctx}/account/addresses"
                          class="grid grid-cols-1 md:grid-cols-2 gap-3 pt-2">

                      <input type="hidden"
                             name="csrfToken"
                             value="<c:out value='${csrfToken}'/>">
                      <input type="hidden"
                             name="action"
                             value="update">
                      <input type="hidden"
                             name="id"
                             value="${address.id}">

                      <label class="form-control w-full">
                        <div class="label">
                          <span class="label-text">Tên người nhận</span>
                        </div>

                        <input type="text"
                               name="recipientName"
                               maxlength="100"
                               required
                               value="<c:out value='${editingThis ? recipientName : address.recipientName}'/>"
                               class="input input-bordered w-full ${editingThis and not empty errors.recipientName ? 'input-error' : ''}">

                        <c:if test="${editingThis and not empty errors.recipientName}">
                          <div class="label">
                            <span class="label-text-alt text-error">
                              <c:out value="${errors.recipientName}"/>
                            </span>
                          </div>
                        </c:if>
                      </label>

                      <label class="form-control w-full">
                        <div class="label">
                          <span class="label-text">Số điện thoại</span>
                        </div>

                        <input type="tel"
                               name="phone"
                               maxlength="20"
                               required
                               value="<c:out value='${editingThis ? phone : address.phone}'/>"
                               class="input input-bordered w-full ${editingThis and not empty errors.phone ? 'input-error' : ''}">

                        <c:if test="${editingThis and not empty errors.phone}">
                          <div class="label">
                            <span class="label-text-alt text-error">
                              <c:out value="${errors.phone}"/>
                            </span>
                          </div>
                        </c:if>
                      </label>

                      <label class="form-control w-full md:col-span-2">
                        <div class="label">
                          <span class="label-text">
                            Số nhà, tên đường, phường/xã, quận/huyện
                          </span>
                        </div>

                        <input type="text"
                               name="street"
                               maxlength="255"
                               required
                               value="<c:out value='${editingThis ? street : address.street}'/>"
                               class="input input-bordered w-full ${editingThis and not empty errors.street ? 'input-error' : ''}">

                        <c:if test="${editingThis and not empty errors.street}">
                          <div class="label">
                            <span class="label-text-alt text-error">
                              <c:out value="${errors.street}"/>
                            </span>
                          </div>
                        </c:if>
                      </label>

                      <label class="form-control w-full md:col-span-2">
                        <div class="label">
                          <span class="label-text">Tỉnh/Thành phố</span>
                        </div>

                        <input type="text"
                               name="city"
                               maxlength="100"
                               required
                               value="<c:out value='${editingThis ? city : address.city}'/>"
                               class="input input-bordered w-full ${editingThis and not empty errors.city ? 'input-error' : ''}">

                        <c:if test="${editingThis and not empty errors.city}">
                          <div class="label">
                            <span class="label-text-alt text-error">
                              <c:out value="${errors.city}"/>
                            </span>
                          </div>
                        </c:if>
                      </label>

                      <label class="label cursor-pointer justify-start gap-3 md:col-span-2">
                        <input type="checkbox"
                               name="isDefault"
                               class="checkbox checkbox-primary"
                               ${(editingThis ? formDefault : address['default']) ? 'checked' : ''}>
                        <span class="label-text">
                          Đặt làm địa chỉ mặc định
                        </span>
                      </label>

                      <div class="md:col-span-2">
                        <button type="submit"
                                class="btn btn-primary">
                          Lưu thay đổi
                        </button>
                      </div>

                    </form>
                  </div>
                </details>

              </div>
            </article>
          </c:forEach>
        </c:otherwise>
      </c:choose>

    </div>

    <%-- Form thêm địa chỉ --%>
    <c:set var="creating" value="${formAction eq 'create'}"/>

    <div class="card bg-base-100 border border-base-300">
      <div class="card-body p-5">

        <h2 class="card-title">Thêm địa chỉ mới</h2>

        <form method="post"
              action="${ctx}/account/addresses"
              class="grid grid-cols-1 md:grid-cols-2 gap-3">

          <input type="hidden"
                 name="csrfToken"
                 value="<c:out value='${csrfToken}'/>">
          <input type="hidden"
                 name="action"
                 value="create">

          <label class="form-control w-full">
            <div class="label">
              <span class="label-text">Tên người nhận</span>
            </div>

            <input type="text"
                   name="recipientName"
                   maxlength="100"
                   required
                   value="<c:out value='${creating ? recipientName : ""}'/>"
                   class="input input-bordered w-full ${creating and not empty errors.recipientName ? 'input-error' : ''}">

            <c:if test="${creating and not empty errors.recipientName}">
              <div class="label">
                <span class="label-text-alt text-error">
                  <c:out value="${errors.recipientName}"/>
                </span>
              </div>
            </c:if>
          </label>

          <label class="form-control w-full">
            <div class="label">
              <span class="label-text">Số điện thoại</span>
            </div>

            <input type="tel"
                   name="phone"
                   maxlength="20"
                   required
                   placeholder="09xxxxxxxx"
                   value="<c:out value='${creating ? phone : ""}'/>"
                   class="input input-bordered w-full ${creating and not empty errors.phone ? 'input-error' : ''}">

            <c:if test="${creating and not empty errors.phone}">
              <div class="label">
                <span class="label-text-alt text-error">
                  <c:out value="${errors.phone}"/>
                </span>
              </div>
            </c:if>
          </label>

          <label class="form-control w-full md:col-span-2">
            <div class="label">
              <span class="label-text">
                Số nhà, tên đường, phường/xã, quận/huyện
              </span>
            </div>

            <input type="text"
                   name="street"
                   maxlength="255"
                   required
                   value="<c:out value='${creating ? street : ""}'/>"
                   class="input input-bordered w-full ${creating and not empty errors.street ? 'input-error' : ''}">

            <c:if test="${creating and not empty errors.street}">
              <div class="label">
                <span class="label-text-alt text-error">
                  <c:out value="${errors.street}"/>
                </span>
              </div>
            </c:if>
          </label>

          <label class="form-control w-full md:col-span-2">
            <div class="label">
              <span class="label-text">Tỉnh/Thành phố</span>
            </div>

            <input type="text"
                   name="city"
                   maxlength="100"
                   required
                   value="<c:out value='${creating ? city : ""}'/>"
                   class="input input-bordered w-full ${creating and not empty errors.city ? 'input-error' : ''}">

            <c:if test="${creating and not empty errors.city}">
              <div class="label">
                <span class="label-text-alt text-error">
                  <c:out value="${errors.city}"/>
                </span>
              </div>
            </c:if>
          </label>

          <label class="label cursor-pointer justify-start gap-3 md:col-span-2">
            <input type="checkbox"
                   name="isDefault"
                   class="checkbox checkbox-primary"
                   ${creating and formDefault ? 'checked' : ''}>
            <span class="label-text">
              Đặt làm địa chỉ mặc định
            </span>
          </label>

          <div class="md:col-span-2">
            <button type="submit"
                    class="btn btn-primary">
              Thêm địa chỉ
            </button>
          </div>

        </form>
      </div>
    </div>

  </section>
</main>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
</body>
</html>