package com.ecommerce.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 1 luat tra loi tu dong cua chatbot do Admin soan: "neu tin cua khach co tu khoa X thi tra loi Y".
 * ChatBotService xet cac luat dang bat theo thu tu `priority` tang dan, luat dau tien khop se duoc dung.
 */
@Entity
@Table(name = "auto_replies")
public class AutoReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Cac tu khoa/cum tu cach nhau bang dau phay hoac xuong dong, vd "ship, giao hang, van chuyen". Dau "*" (dung 1 minh) = khop moi cau.
    @Column(columnDefinition = "TEXT", nullable = false)
    private String keywords;

    @Column(name = "reply_text", columnDefinition = "TEXT", nullable = false)
    private String replyText;

    // true = sau khi tra loi, chuyen cuoc tro chuyen sang NHAN VIEN (chatbot ngung tra loi)
    private boolean handoff;

    @Column(name = "is_active")
    private boolean active = true;

    // So nho duoc xet truoc. Luat "*" (bat moi cau) nen de so lon nhat de khong "nuot" cac luat khac.
    private int priority = 100;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public String getReplyText() { return replyText; }
    public void setReplyText(String replyText) { this.replyText = replyText; }
    public boolean isHandoff() { return handoff; }
    public void setHandoff(boolean handoff) { this.handoff = handoff; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    /** Tach `keywords` thanh danh sach tu khoa (cat theo dau phay/xuong dong, bo khoang trang va muc rong). JSP doc bang ${rule.keywordList}. */
    public List<String> getKeywordList() {
        List<String> list = new ArrayList<>();
        if (keywords == null) return list;
        for (String k : keywords.split("[,\\n\\r]+")) {
            if (!k.isBlank()) list.add(k.trim());
        }
        return list;
    }
}
