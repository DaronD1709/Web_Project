package com.ecommerce.service;

import com.ecommerce.dao.AutoReplyDAO;
import com.ecommerce.entity.AutoReply;

import java.text.Normalizer;
import java.util.List;

/**
 * Chatbot tra loi theo cac LUAT DO ADMIN SOAN (bang auto_replies, quan ly o Admin > Tra loi tu dong) - khong goi AI ben ngoai.
 * Cach hoat dong: chuan hoa cau cua khach (bo dau, chu thuong, bo dau cau) roi xet cac luat dang bat theo thu tu uu tien;
 * luat dau tien co 1 tu khoa khop (khop NGUYEN TU/CUM TU) thi dung cau tra loi cua luat do. Khong luat nao khop thi dung cau mac dinh
 * va chuyen cho nhan vien. Luat chuyen cho nhan vien (handoff) lam ChatService chuyen cuoc tro chuyen sang NHAN VIEN, chatbot ngung tra loi.
 * Muon thay bang AI that (OpenAI/Anthropic...) chi can sua ham reply().
 */
public class ChatBotService {

    /** text = cau chatbot gui cho khach; handoff = true neu can nhan vien tiep quan. */
    public record Reply(String text, boolean handoff) { }

    // Dung khi chua co luat nao khop (hoac Admin da tat het cac luat): khong doan bua, chuyen cho nguoi that
    private static final Reply FALLBACK = new Reply(
            "Mình chưa trả lời được câu này nên đã chuyển cho nhân viên shop, bạn vui lòng chờ phản hồi nhé!", true);

    private final AutoReplyDAO autoReplyDAO = new AutoReplyDAO();

    public Reply reply(String customerMessage) {
        AutoReply rule = match(customerMessage);
        return rule == null ? FALLBACK : new Reply(rule.getReplyText(), rule.isHandoff());
    }

    /** Luat dau tien (theo uu tien) khop cau cua khach, hoac null neu khong co. Admin cung dung ham nay de "thu cau hoi" ngay tren trang quan ly. */
    public AutoReply match(String customerMessage) {
        String text = normalize(customerMessage);
        List<AutoReply> rules = autoReplyDAO.findActiveOrdered();
        for (AutoReply rule : rules) {
            for (String keyword : rule.getKeywordList()) {
                if (keyword.equals("*")) return rule; // dau * = khop moi cau
                String k = normalize(keyword);
                if (k.length() > 2 && text.contains(k)) return rule; // k da co dau cach 2 dau -> khop nguyen tu (chu "ship" khong khop "relationship")
            }
        }
        return null;
    }

    /**
     * Chuan hoa de so khop: bo dau tieng Viet, chu thuong, moi ky tu khong phai chu/so doi thanh dau cach, gop khoang trang,
     * them 1 dau cach o moi dau. Vd "Phí ship, bao nhiêu?" -> " phi ship bao nhieu ". "đ" khong tach duoc bang NFD nen doi rieng.
     */
    public static String normalize(String s) {
        String noMarks = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String plain = noMarks.toLowerCase().replace('đ', 'd').replaceAll("[^a-z0-9]+", " ").trim();
        return " " + plain + " ";
    }
}
