package com.ecommerce.service;

import com.ecommerce.dao.AutoReplyDAO;
import com.ecommerce.entity.AutoReply;
import com.ecommerce.util.ParamUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Nghiep vu QUAN LY CAU TRA LOI TU DONG cua chatbot (Admin): xem, tao, sua, bat/tat, xoa, thu cau hoi. Luat khop nam o ChatBotService. */
public class AdminAutoReplyService {

    private static final int MAX_KEYWORDS = 20;
    private static final int MAX_KEYWORD_LENGTH = 40;
    private static final int MAX_REPLY_LENGTH = 1000;

    private final AutoReplyDAO autoReplyDAO = new AutoReplyDAO();
    private final ChatBotService chatBot = new ChatBotService();

    /** Tat ca luat theo thu tu uu tien (so nho truoc). */
    public List<AutoReply> list() {
        return autoReplyDAO.findAllOrdered();
    }

    public AutoReply getById(Integer id) {
        AutoReply r = id == null ? null : autoReplyDAO.findById(id);
        if (r == null) throw new BusinessException("Câu trả lời tự động không tồn tại.");
        return r;
    }

    /** Muc uu tien goi y cho luat moi: lon hon luat thuong lon nhat (nhung nho hon luat "*" bat moi cau, neu co). */
    public int suggestPriority() {
        int max = 0;
        for (AutoReply r : autoReplyDAO.findAllOrdered()) {
            if (!r.getKeywordList().contains("*")) max = Math.max(max, r.getPriority());
        }
        return max + 10;
    }

    /**
     * Tao (id == null) hoac sua 1 luat. Nhan tham so dang CHUOI nguyen ban tu form de tu kiem tra va bao loi tung o
     * (BusinessException chua Map ten-o -> loi). Cac buoc: (1) kiem tra tung o (2) neu sua thi lay ban cu (3) gan gia tri (4) luu.
     */
    public AutoReply save(Integer id, String keywords, String replyText, String priority, boolean handoff, boolean active) {
        Map<String, String> errors = new LinkedHashMap<>();

        // Tu khoa: it nhat 1, toi da 20, moi tu khoa <= 40 ky tu; "*" (bat moi cau) phai dung 1 minh
        AutoReply probe = new AutoReply();
        probe.setKeywords(keywords);
        List<String> list = probe.getKeywordList();
        if (list.isEmpty()) errors.put("keywords", "Vui lòng nhập ít nhất 1 từ khoá");
        else if (list.size() > MAX_KEYWORDS) errors.put("keywords", "Tối đa " + MAX_KEYWORDS + " từ khoá");
        else if (list.stream().anyMatch(k -> k.length() > MAX_KEYWORD_LENGTH)) errors.put("keywords", "Mỗi từ khoá tối đa " + MAX_KEYWORD_LENGTH + " ký tự");
        else if (list.contains("*") && list.size() > 1) errors.put("keywords", "Dấu * (khớp mọi câu) phải đứng một mình");
        else if (list.stream().anyMatch(k -> !k.equals("*") && ChatBotService.normalize(k).trim().length() < 3)) {
            errors.put("keywords", "Mỗi từ khoá cần ít nhất 3 chữ cái hoặc số (để tránh khớp nhầm)");
        }

        String reply = replyText == null ? "" : replyText.trim();
        if (reply.isEmpty()) errors.put("replyText", "Vui lòng nhập câu trả lời");
        else if (reply.length() > MAX_REPLY_LENGTH) errors.put("replyText", "Câu trả lời tối đa " + MAX_REPLY_LENGTH + " ký tự");

        Integer priorityValue = ParamUtil.intOrNull(priority);
        if (priorityValue == null || priorityValue < 1 || priorityValue > 9999) errors.put("priority", "Mức ưu tiên từ 1 đến 9999");

        AutoReply rule = new AutoReply();
        if (id != null) rule = getById(id);
        if (!errors.isEmpty()) throw new BusinessException(errors);

        rule.setKeywords(String.join(", ", list)); // luu gon: tu khoa cach nhau ", "
        rule.setReplyText(reply);
        rule.setPriority(priorityValue);
        rule.setHandoff(handoff);
        rule.setActive(active);
        return id == null ? autoReplyDAO.save(rule) : autoReplyDAO.update(rule);
    }

    /** Dat bat/tat theo trang thai MONG MUON (khong dao nguoc): bam nhanh 2 lan khong bi lech. Tra ve cau thong bao. */
    public String setActive(Integer id, boolean active) {
        AutoReply r = getById(id);
        r.setActive(active);
        autoReplyDAO.update(r);
        return active ? "Đã bật câu trả lời tự động" : "Đã tắt câu trả lời tự động";
    }

    public void delete(Integer id) {
        getById(id); // khong ton tai thi bao loi
        autoReplyDAO.deleteById(id);
    }

    /** "Thu cau hoi": luat nao se tra loi cau nay (null neu khong luat nao khop -> chatbot dung cau mac dinh va chuyen nhan vien). */
    public AutoReply test(String message) {
        return chatBot.match(message);
    }
}
