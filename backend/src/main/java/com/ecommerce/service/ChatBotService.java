package com.ecommerce.service;

import java.text.Normalizer;

/**
 * Chatbot tra loi theo LUAT tu khoa (khong goi AI ben ngoai): bo dau + chu thuong cau cua khach roi tim tu khoa quen thuoc.
 * Khong hieu / khach xin gap nhan vien thi handoff = true: ChatService chuyen cuoc tro chuyen sang NHAN VIEN va chatbot ngung tra loi.
 * Muon thay bang AI that (OpenAI/Anthropic...) chi can sua ham reply() nay, cac cho goi khong doi.
 */
public class ChatBotService {

    /** text = cau chatbot gui cho khach; handoff = true neu can nhan vien tiep quan. */
    public record Reply(String text, boolean handoff) { }

    public Reply reply(String customerMessage) {
        String t = normalize(customerMessage);

        if (has(t, "nhan vien", "gap nguoi", "tu van vien", "gap shop", "goi cho")) {
            return new Reply("Mình đã chuyển cuộc trò chuyện cho nhân viên shop. Bạn vui lòng chờ trong giây lát nhé!", true);
        }
        if (has(t, "ship", "giao hang", "van chuyen", "phi giao")) {
            return new Reply("Shop miễn phí vận chuyển cho đơn từ 500.000đ, đơn dưới mức này phí 30.000đ. Nội thành giao 1–2 ngày, tỉnh khác 2–4 ngày.", false);
        }
        if (has(t, "thanh toan", "tra tien", "cod", "vnpay", "chuyen khoan")) {
            return new Reply("Shop hỗ trợ thanh toán khi nhận hàng (COD) và thanh toán online qua VNPay. Bạn chọn khi đặt hàng nhé.", false);
        }
        if (has(t, "hoan hang", "doi tra", "tra hang", "bao hanh")) {
            return new Reply("Sau khi nhận hàng bạn có thể gửi yêu cầu hoàn hàng ở mục Đơn hàng, nhân viên sẽ xem xét và phản hồi. "
                    + "Về bảo hành, bạn cho mình biết tên sản phẩm để nhân viên hỗ trợ chính xác nhé.", false);
        }
        if (has(t, "ma giam", "voucher", "khuyen mai", "giam gia")) {
            return new Reply("Bạn nhập mã giảm giá ở bước thanh toán, hệ thống sẽ kiểm tra mã còn hạn, còn lượt và đạt giá trị đơn tối thiểu hay không.", false);
        }
        if (has(t, "phan bon", "bon phan", "npk")) {
            return new Reply("Với rau màu, bạn có thể dùng phân NPK 20-20-15 để bón thúc kết hợp phân hữu cơ vi sinh bón lót. "
                    + "Bạn xem nhóm Phân bón trong cửa hàng, hoặc nói rõ loại cây để nhân viên tư vấn kỹ hơn.", false);
        }
        if (has(t, "hat giong", "gieo", "giong")) {
            return new Reply("Shop có nhiều hạt giống rau củ (cà chua, cải xanh…) tỷ lệ nảy mầm cao. Bạn xem nhóm Hạt giống trong cửa hàng nhé.", false);
        }
        if (has(t, "tuoi", "may ", "dung cu", "phun thuoc", "cat co")) {
            return new Reply("Shop có máy móc, dụng cụ và hệ thống tưới tiêu cho nhà nông. Bạn cho mình biết diện tích hoặc nhu cầu để nhân viên tư vấn chi tiết nhé.", false);
        }
        if (has(t, "xin chao", "hello", "chao ban", "alo")) {
            return new Reply("Chào bạn! Bạn cần tư vấn về hạt giống, phân bón, thuốc BVTV hay máy móc nông nghiệp ạ?", false);
        }
        // Khong hieu -> khong doan bua, chuyen cho nguoi that
        return new Reply("Mình chưa trả lời được câu này nên đã chuyển cho nhân viên shop, bạn vui lòng chờ phản hồi nhé!", true);
    }

    // Bo dau tieng Viet, chu thuong, them dau cach 2 dau (de khop tu khoa ngan nhu "may "); "đ" khong tach duoc bang NFD nen doi rieng
    private static String normalize(String s) {
        String noMarks = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return " " + noMarks.toLowerCase().replace('đ', 'd') + " ";
    }

    private static boolean has(String text, String... keywords) {
        for (String k : keywords) if (text.contains(k)) return true;
        return false;
    }
}
