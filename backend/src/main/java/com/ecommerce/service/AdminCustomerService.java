package com.ecommerce.service;

import com.ecommerce.dao.CustomerDAO;
import com.ecommerce.dao.OrderDAO;
import com.ecommerce.dao.UserDAO;
import com.ecommerce.dto.AdminCustomerFilter;
import com.ecommerce.dto.PageResult;
import com.ecommerce.entity.Customer;
import com.ecommerce.entity.Order;
import com.ecommerce.entity.User;

import java.util.List;
import java.util.Map;

/** Nghiep vu QUAN LY TAI KHOAN KHACH HANG cua Admin: xem danh sach/chi tiet, khoa va mo khoa. Admin khong xem duoc mat khau cua khach. */
public class AdminCustomerService {

    private final CustomerDAO customerDAO = new CustomerDAO();
    private final OrderDAO orderDAO = new OrderDAO();
    private final UserDAO userDAO = new UserDAO();

    /** 1 trang khach hang khop bo loc: 1 query lay khach cua trang + 1 query dem tong de tinh so trang. */
    public PageResult<Customer> search(AdminCustomerFilter filter) {
        long total = customerDAO.countSearch(filter);
        List<Customer> items = customerDAO.search(filter);
        return new PageResult<>(items, total, filter.getPage(), filter.getPageSize());
    }

    /** id khach -> {so don, tong chi tieu} cho cac khach dang hien trong danh sach. */
    public Map<Integer, double[]> stats(List<Customer> customers) {
        return customerDAO.orderStats(customers.stream().map(Customer::getId).toList());
    }

    public Map<String, Long> counts() {
        return customerDAO.countLocked();
    }

    public Customer getById(Integer id) {
        Customer c = id == null ? null : customerDAO.findById(id);
        if (c == null) throw new BusinessException("Khách hàng không tồn tại.");
        return c;
    }

    public List<Order> recentOrders(Integer customerId) {
        return orderDAO.findRecentByCustomer(customerId, 5);
    }

    /**
     * Khoa (locked = true) hoac mo khoa tai khoan khach. Chi khoa duoc KHACH HANG, khong khoa duoc Admin (tranh Admin tu khoa minh
     * hoac khoa tai khoan he thong cua chatbot). Dat theo trang thai MONG MUON (khong dao nguoc) nen bam nhanh 2 lan khong bi lech.
     * Don dang xu ly van tiep tuc binh thuong; khach bi khoa khong dang nhap duoc (AuthService.login) va dang dung thi bi dang xuat.
     */
    public String setLocked(Integer id, boolean locked) {
        User user = id == null ? null : userDAO.findById(id);
        if (!(user instanceof Customer customer)) throw new BusinessException("Khách hàng không tồn tại.");
        customer.setActive(!locked);
        userDAO.update(customer);
        return (locked ? "Đã khoá tài khoản " : "Đã mở khoá tài khoản ") + customer.getFullName();
    }
}
