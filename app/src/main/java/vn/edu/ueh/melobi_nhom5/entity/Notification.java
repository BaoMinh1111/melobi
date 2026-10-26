package vn.edu.ueh.melobi_nhom5.entity;

public class Notification {
    public static final String TYPE_FOLLOW = "FOLLOW";
    public static final String TYPE_NEW_ANSWER = "NEW_ANSWER";
    public static final String TYPE_NEW_COMMENT = "NEW_COMMENT";
    public static final String TYPE_LIKE = "LIKE";
    public static final String TYPE_MESSAGE = "MESSAGE";

    private String notificationId;
    private String receiverId;
    private String actorId;
    private String type;
    private String targetId;
    private String targetType;
    private boolean isRead;
    private long createdAt;

    public Notification() {
        this.createdAt = System.currentTimeMillis();
    }

    /** Constructor đơn giản: NotificationService đang dùng. Tự điền isRead = false và createdAt. */
    public Notification(String receiverId, String actorId, String type, String targetId, String targetType) {
        this.receiverId = receiverId;
        this.actorId = actorId;
        this.type = type;
        this.targetId = targetId;
        this.targetType = targetType;
        this.isRead = false;
        this.createdAt = System.currentTimeMillis();
    }

    public Notification(String notificationId, String receiverId, String actorId, String type, String targetId, String targetType, boolean isRead, long createdAt) {
        this.notificationId = notificationId;
        this.receiverId = receiverId;
        this.actorId = actorId;
        this.type = type;
        this.targetId = targetId;
        this.targetType = targetType;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }

    public String getActorId() {
        return actorId;
    }

    public void setActorId(String actorId) {
        this.actorId = actorId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getTargetId() {
        return targetId;
    }

    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public boolean isRead() {
        return isRead;
    }

    public boolean getIsRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public void setIsRead(boolean isRead) {
        this.isRead = isRead;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}