package vn.edu.ueh.melobi_nhom5.entity;

public class Like {
    public static final String TARGET_QUESTION = "QUESTION";
    public static final String TARGET_ANSWER = "ANSWER";

    private String likeId;
    private String userId;
    private String targetId;
    private String targetType;
    private long createdAt;

    public Like() {
        this.createdAt = System.currentTimeMillis();
    }

    public Like(String likeId, String userId, String targetId, String targetType, long createdAt) {
        this.likeId = likeId;
        this.userId = userId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.createdAt = createdAt;
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
