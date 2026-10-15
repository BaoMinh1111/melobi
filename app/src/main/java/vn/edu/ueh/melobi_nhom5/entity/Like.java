package vn.edu.ueh.melobi_nhom5.entity;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Entity Like - collection "likes". Dùng chung cho Question và Answer (đa hình).
 *
 * Mẹo: đặt ID document = userId_targetType_targetId (xem buildId).
 * Nhờ vậy 1 người không thể like trùng 1 nội dung (tương đương UNIQUE trong ERD)
 * và kiểm tra "đã like chưa" chỉ cần đọc đúng 1 document.
 */
public class Like {
    public static final String TYPE_QUESTION = "QUESTION";
    public static final String TYPE_ANSWER = "ANSWER";

    @DocumentId
    private String likeId;
    private String userId;
    private String targetId;
    private String targetType;   // QUESTION / ANSWER
    @ServerTimestamp
    private Date createdAt;

    public Like() { }

    public Like(String userId, String targetId, String targetType) {
        this.userId = userId;
        this.targetId = targetId;
        this.targetType = targetType;
    }

    public static String buildId(String userId, String targetType, String targetId) {
        return userId + "_" + targetType + "_" + targetId;
    }

    public String getLikeId() { return likeId; }
    public void setLikeId(String likeId) { this.likeId = likeId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}