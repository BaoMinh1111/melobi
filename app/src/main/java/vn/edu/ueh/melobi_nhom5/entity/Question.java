package vn.edu.ueh.melobi_nhom5.entity;

public class Question {
    public static final String STATUS_VISIBLE = "VISIBLE";
    public static final String STATUS_HIDDEN = "HIDDEN";
    private String questionId;
    private String userId;
    private String majorId;
    private String title;
    private String content;
    private String imageUrl;
    private String status;
    private int likeCount;
    private int answerCount;
    private long createdAt;
    private long updatedAt;
    private String syncStatus = "SYNCED";
    private String authorName;
    private String authorAvatarUrl;

    // Constructor rỗng (Bắt buộc cho Firebase/Firestore toObject)
    public Question() {
    }

    // Constructor đầy đủ tham số
    public Question(String questionId, String userId, String majorId, String title, String content, String imageUrl, String status, int likeCount, int answerCount, long createdAt, long updatedAt) {
        this.questionId = questionId;
        this.userId = userId;
        this.majorId = majorId;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
        this.status = status;
        this.likeCount = likeCount;
        this.answerCount = answerCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getQuestionId() { return questionId; }
    public void setQuestionId(String questionId) { this.questionId = questionId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getMajorId() { return majorId; }
    public void setMajorId(String majorId) { this.majorId = majorId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getAnswerCount() { return answerCount; }
    public void setAnswerCount(int answerCount) { this.answerCount = answerCount; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public String getSyncStatus() {return syncStatus; }
    public void setSyncStatus(String syncStatus) {this.syncStatus = syncStatus; }

    public String getAuthorName() {return authorName; }
    public void setAuthorName(String authorName) {this.authorName = authorName; }

    public String getAuthorAvatarUrl() {return authorAvatarUrl; }
    public void setAuthorAvatarUrl(String authorAvatarUrl) {this.authorAvatarUrl = authorAvatarUrl; }
}