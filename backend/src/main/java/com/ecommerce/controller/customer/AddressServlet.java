package com.ecommerce.controller.customer;

import com.ecommerce.entity.Customer;
import com.ecommerce.service.AddressService;
import com.ecommerce.service.BusinessException;
import com.ecommerce.util.ParamUtil;
import com.ecommerce.util.SessionUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;

@WebServlet("/account/addresses")
public class AddressServlet extends HttpServlet {

    private static final String VIEW =
            "/WEB-INF/views/customer/addresses.jsp";

    private static final String CSRF_SESSION_KEY =
            "addressCsrfToken";

    private final AddressService addressService =
            new AddressService();

    @Override
    protected void doGet(HttpServletRequest req,
                         HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setHeader("Cache-Control", "no-store");

        Customer customer =
                SessionUtil.requireCustomer(req, resp);

        if (customer == null) {
            return;
        }

        prepareView(req, customer);

        req.getRequestDispatcher(VIEW)
                .forward(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req,
                          HttpServletResponse resp)
            throws ServletException, IOException {

        req.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-store");

        Customer customer =
                SessionUtil.requireCustomer(req, resp);

        if (customer == null) {
            return;
        }

        if (!isValidCsrf(req)) {
            resp.sendError(
                    HttpServletResponse.SC_FORBIDDEN,
                    "Phiên gửi biểu mẫu không hợp lệ. Vui lòng tải lại trang."
            );
            return;
        }

        String[] actions = req.getParameterValues("action");

        if (actions == null
                || actions.length != 1
                || actions[0] == null) {
            resp.sendError(
                    HttpServletResponse.SC_BAD_REQUEST,
                    "Thao tác không hợp lệ."
            );
            return;
        }

        String action = actions[0];

        try {
            switch (action) {
                case "create" ->
                        createAddress(req, customer);

                case "update" ->
                        updateAddress(req, customer);

                case "delete" ->
                        deleteAddress(req, customer);

                case "setDefault" ->
                        setDefaultAddress(req, customer);

                default -> {
                    resp.sendError(
                            HttpServletResponse.SC_BAD_REQUEST,
                            "Thao tác không hợp lệ."
                    );
                    return;
                }
            }

            redirectAfterSuccess(req, resp, action);

        } catch (BusinessException e) {
            showBusinessError(
                    req,
                    resp,
                    customer,
                    action,
                    e
            );
        }
    }

    private void createAddress(HttpServletRequest req,
                               Customer customer) {
        addressService.create(
                customer.getId(),
                req.getParameter("recipientName"),
                req.getParameter("phone"),
                req.getParameter("street"),
                req.getParameter("city"),
                "on".equals(req.getParameter("isDefault"))
        );
    }

    private void updateAddress(HttpServletRequest req,
                               Customer customer) {
        Integer addressId = requireAddressId(req);

        addressService.update(
                customer.getId(),
                addressId,
                req.getParameter("recipientName"),
                req.getParameter("phone"),
                req.getParameter("street"),
                req.getParameter("city"),
                "on".equals(req.getParameter("isDefault"))
        );
    }

    private void deleteAddress(HttpServletRequest req,
                               Customer customer) {
        Integer addressId = requireAddressId(req);

        addressService.delete(
                customer.getId(),
                addressId
        );
    }

    private void setDefaultAddress(HttpServletRequest req,
                                   Customer customer) {
        Integer addressId = requireAddressId(req);

        addressService.setDefault(
                customer.getId(),
                addressId
        );
    }

    private Integer requireAddressId(HttpServletRequest req) {
        String[] suppliedIds = req.getParameterValues("id");

        if (suppliedIds == null
                || suppliedIds.length != 1) {
            throw new BusinessException(
                    "Địa chỉ không hợp lệ."
            );
        }

        Integer addressId =
                ParamUtil.intOrNull(suppliedIds[0]);

        if (addressId == null || addressId <= 0) {
            throw new BusinessException(
                    "Địa chỉ không hợp lệ."
            );
        }

        return addressId;
    }

    private void showBusinessError(HttpServletRequest req,
                                   HttpServletResponse resp,
                                   Customer customer,
                                   String action,
                                   BusinessException exception)
            throws ServletException, IOException {

        resp.setStatus(422);

        prepareView(req, customer);

        Map<String, String> errors =
                exception.getErrors();

        req.setAttribute("errors", errors);

        if (errors == null || errors.isEmpty()) {
            req.setAttribute(
                    "addressError",
                    exception.getMessage()
            );
        }

        if ("create".equals(action)
                || "update".equals(action)) {
            req.setAttribute("formAction", action);
            req.setAttribute(
                    "formId",
                    req.getParameter("id")
            );
            req.setAttribute(
                    "recipientName",
                    req.getParameter("recipientName")
            );
            req.setAttribute(
                    "phone",
                    req.getParameter("phone")
            );
            req.setAttribute(
                    "street",
                    req.getParameter("street")
            );
            req.setAttribute(
                    "city",
                    req.getParameter("city")
            );
            req.setAttribute(
                    "formDefault",
                    "on".equals(
                            req.getParameter("isDefault")
                    )
            );
        }

        req.getRequestDispatcher(VIEW)
                .forward(req, resp);
    }

    private void redirectAfterSuccess(HttpServletRequest req,
                                      HttpServletResponse resp,
                                      String action)
            throws IOException {

        String result = switch (action) {
            case "create" -> "created";
            case "update" -> "updated";
            case "delete" -> "deleted";
            case "setDefault" -> "defaultChanged";
            default -> "saved";
        };

        resp.sendRedirect(
                req.getContextPath()
                        + "/account/addresses?"
                        + result
                        + "=1"
        );
    }

    private void prepareView(HttpServletRequest req,
                             Customer customer) {

        req.setAttribute(
                "addresses",
                addressService.getAddresses(customer.getId())
        );

        req.setAttribute(
                "csrfToken",
                getOrCreateCsrfToken(req)
        );

        if (req.getAttribute("formAction") == null) {
            req.setAttribute("formAction", "create");
        }
    }

    private String getOrCreateCsrfToken(
            HttpServletRequest req) {

        HttpSession session = req.getSession(false);

        synchronized (session) {
            String token = (String) session.getAttribute(
                    CSRF_SESSION_KEY
            );

            if (token == null) {
                byte[] bytes = new byte[32];
                new SecureRandom().nextBytes(bytes);

                token = Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(bytes);

                session.setAttribute(
                        CSRF_SESSION_KEY,
                        token
                );
            }

            return token;
        }
    }

    private boolean isValidCsrf(HttpServletRequest req) {
        HttpSession session = req.getSession(false);

        if (session == null) {
            return false;
        }

        String expected = (String) session.getAttribute(
                CSRF_SESSION_KEY
        );

        String[] supplied =
                req.getParameterValues("csrfToken");

        if (expected == null
                || supplied == null
                || supplied.length != 1
                || supplied[0] == null) {
            return false;
        }

        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                supplied[0].getBytes(StandardCharsets.UTF_8)
        );
    }
}