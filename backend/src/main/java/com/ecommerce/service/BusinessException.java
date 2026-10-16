package com.ecommerce.service;

import java.util.Map;

/**
 * Loi nghiep vu/validate do Service nem ra (vd: email trung, het hang, sai mat khau).
 * Servlet bat no va hien thong bao cho nguoi dung thay vi de trang bi loi 500.
 *  - new BusinessException("...")           : 1 thong bao chung  -> e.getMessage()
 *  - new BusinessException(Map<ten_o, loi>) : loi theo tung o form -> e.getErrors()
 *
 * TAI SAO CAN LOP NAY
 *  Quy uoc cua du an: moi luat nghiep vu nam o SERVICE, Servlet chi lo chuyen yeu cau/hien ket qua. Khi mot luat bi vi pham
 *  (het hang, email trung...), Service can bao "sai luat gi" ma khong biet gi ve HTTP, JSP hay htmx. Nem BusinessException la
 *  cach do: Service chi noi NOI DUNG loi; Servlet quyet dinh HIEN THI loi ra sao (xem "Cach Servlet xu ly" ben duoi).
 *  Phan biet voi loi he thong (mat ket noi DB, NullPointerException...): do la loi 500 that, KHONG dung lop nay.
 *
 * VI SAO KE THUA RuntimeException (loi "unchecked")
 *  - Khong buoc moi ham phia tren phai khai bao "throws BusinessException" -> code gon.
 *  - Neu nem ben trong 1 giao dich (transaction) cua DAO thi DAO bat RuntimeException de ROLLBACK roi nem lai, nen du lieu
 *    do dang ghi do bi huy, khong luu nua chung.
 *
 * HAI CACH TAO
 *  1) new BusinessException("Het hang")                    -> loi chung, khong thuoc o nao: getMessage() = chuoi do, getErrors() = Map rong.
 *       Dung cho: sai mat khau, het hang, khong du quyen, don khong ton tai... (1 cau la du)
 *  2) new BusinessException(Map.of("email", "Email da ton tai"))  -> loi theo tung o cua form: getErrors() = Map (ten o -> loi),
 *       getMessage() luon la "Du lieu khong hop le". Dung khi kiem tra nhieu o cung luc (dang ky, ho so, form san pham/voucher)
 *       de nguoi dung thay HET cac loi trong 1 lan, thay vi sua mot loi roi lai gap loi tiep theo.
 *  Servlet dung 1 khoi catch cho ca 2 loai: neu getErrors() RONG thi dung getMessage() (loi chung), nguoc lai in loi duoi tung o.
 *
 * CACH SERVLET XU LY (vi du)
 *  - Form thuong:  catch (BusinessException e) { req.setAttribute("errors", e.getErrors()); forward lai trang form }  (JSP in loi duoi o)
 *  - Loi chung:    req.setAttribute("error", e.getMessage()); dung lai trang voi thong bao
 *  - htmx:         resp.setStatus(422); HtmxUtil.toast(resp, e.getMessage(), "error"); resp.setHeader("HX-Reswap", "none");
 *                  (422 = du lieu khong hop le; toast do bao loi; HX-Reswap none = htmx khong doi giao dien hien tai)
 *
 * Duoc nem o nhieu Service (CartService, OrderService, AuthService, UserService, Admin*Service...) va duoc bat o phan lon Servlet.
 */
public class BusinessException extends RuntimeException {

    // Loi theo tung o form: khoa = ten o (trung voi name cua <input>), gia tri = cau thong bao. Rong khi la loi chung.
    // final: gan 1 lan trong constructor, khong doi duoc sau do.
    private final Map<String, String> errors;

    /** Loi chung (1 thong bao). Lay lai bang getMessage(); getErrors() tra Map rong. */
    public BusinessException(String message) {
        super(message);
        this.errors = Map.of(); // Map.of() = Map rong bat bien: getErrors() khong bao gio la null, Servlet khong can kiem tra null
    }

    /**
     * Loi theo tung o form. errors: ten o -> thong bao loi (khong nen rong: neu rong thi Servlet se coi nhu loi chung
     * va hien "Du lieu khong hop le"). getMessage() duoc dat co dinh la thong bao tong quat cho truong hop hien 1 dong chung.
     */
    public BusinessException(Map<String, String> errors) {
        super("Dữ liệu không hợp lệ");
        this.errors = errors;
    }

    /** Loi theo tung o (ten o -> loi). Rong neu day la loi chung; JSP doc ${errors.ten_o} de in loi duoi dung o do. */
    public Map<String, String> getErrors() {
        return errors;
    }
}
