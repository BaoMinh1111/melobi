package vn.edu.ueh.melobi_nhom5.entity;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Entity Notification - collection "notifications".
 * Lưu ý: tên trùng android.app.Notification. Khi dùng cả hai trong cùng 1 file,
 * hãy viết đầy đủ android.app.Notification cho lớp của Android.
 */
public class Notification {
    public static final String TYPE_FOLLOW = "FOLLOW";
    public static final String TYPE_NEW_ANSWER = "NEW_ANSWER";
    public static final String TYPE_NEW_COMMENT = "NEW_COMMENT";
    public static final String TYPE_LIKE = "LIKE";

    @DocumentId
    private String notificationId;
    private String receiverId;   // người nhận thông báo
    private String actorId;      // người gây ra hành động
    private String type;         // FOLLOW / NEW_ANSWER / NEW_COMMENT / LIKE
    private String targetId;
    private String targetType;   // QUESTION / ANSWER / COMMENT / USER
    private boolean isRead;
    @ServerTimestamp
    private Date createdAt;

    public Notification() { }

    public Notification(String receiverId, String actorId, String type,
                        String targetId, String targetType) {
        this.receiverId = receiverId;
        this.actorId = actorId;
        this.type = type;
        this.targetId = targetId;
        this.targetType = targetType;
        this.isRead = false;
    }

    public String getNotificationId() { return notificationId; }
    public void setNotificationId(String notificationId) { this.notificationId = notificationId; }
    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }
    public String getActorId() { return actorId; }
    public void setActorId(String actorId) { this.actorId = actorId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    // Bắt buộc dùng @PropertyName: nếu không, Firestore sẽ lưu field tên "read" thay vì "isRead"
    @PropertyName("isRead")
    public boolean isRead() { return isRead; }
    @PropertyName("isRead")
    public void setRead(boolean read) { isRead = read; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}