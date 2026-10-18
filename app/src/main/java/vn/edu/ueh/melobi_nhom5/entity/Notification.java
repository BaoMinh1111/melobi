package vn.edu.ueh.melobi_nhom5.entity;

public class Notification {
    public static final String TYPE_FOLLOW = "FOLLOW";
    public static final String TYPE_NEW_ANSWER = "NEW_ANSWER";
    public static final String TYPE_NEW_COMMENT = "NEW_COMMENT";
    public static final String TYPE_LIKE = "LIKE";

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
/*1 số lỗi sau:
* isRead bị ghi thành hai trường read và isRead
* Thiếu @DocumentId nên không có ID để đánh dấu đã đọc
* Constructor rỗng cũng gán createdAt = System.currentTimeMillis(). Có hai tác động:
- Máy lệch giờ thì thứ tự thông báo sai, vì getNotifications sắp xếp theo createdAt.
- Constructor rỗng gán giờ hiện tại trước khi Firestore điền dữ liệu. Nếu document thiếu createdAt, thông báo cũ sẽ hiện như vừa mới tạo mà không có dấu hiệu gì. 
Nên để constructor rỗng không gán gì, chỉ constructor 5 tham số gán giờ.*/
