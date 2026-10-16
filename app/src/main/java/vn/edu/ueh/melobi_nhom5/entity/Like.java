package vn.edu.ueh.melobi_nhom5.entity;

public class Like {
    public static final String TYPE_QUESTION = "QUESTION";
    public static final String TYPE_ANSWER = "ANSWER";

    // Giữ lại tên cũ để code khác đang dùng TARGET_* không bị lỗi
    public static final String TARGET_QUESTION = TYPE_QUESTION;
    public static final String TARGET_ANSWER = TYPE_ANSWER;

    private String likeId;
    private String userId;
    private String targetId;
    private String targetType;
    private long createdAt;

    public Like() {
        this.createdAt = System.currentTimeMillis();
    }

    /** Constructor đơn giản: tự sinh likeId và createdAt (QuestionService đang dùng). */
    public Like(String userId, String targetId, String targetType) {
        this.userId = userId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.likeId = buildId(userId, targetType, targetId);
        this.createdAt = System.currentTimeMillis();
    }

    public Like(String likeId, String userId, String targetId, String targetType, long createdAt) {
        this.likeId = likeId;
        this.userId = userId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.createdAt = createdAt;
    }

    /** ID document cố định để mỗi user chỉ like 1 lần cho mỗi đối tượng. */
    public static String buildId(String userId, String targetType, String targetId) {
        return userId + "_" + targetType + "_" + targetId;
    }

    public String getLikeId() {
        return likeId;
    }

    public void setLikeId(String likeId) {
        this.likeId = likeId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}