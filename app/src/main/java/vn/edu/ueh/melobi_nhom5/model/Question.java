package vn.edu.ueh.melobi_nhom5.model;

import com.google.firebase.firestore.Exclude;

import java.io.Serializable;

/**
 * Entity Question - collection "questions" trên Firestore (bản chung của nhóm).
 * Các field gốc: id, title, content, category, userId, userName, userAvatarUrl, createdAt, answerCount.
 * Các field THÊM MỚI (chức năng đăng/like/ẩn câu hỏi): majorId, titleLower, imageUrl,
 * status, likeCount, updatedAt. Dữ liệu cũ thiếu các field này vẫn đọc được bình thường.
 */
public class Question implements Serializable {
    public static final String STATUS_VISIBLE = "VISIBLE";
    public static final String STATUS_HIDDEN = "HIDDEN";
    public static final String STATUS_DELETED = "DELETED";   // xóa mềm

    private String id;
    private String title;
    private String content;
    private String category;          // tên ngành hiển thị (MainActivity lọc theo field này)
    private String userId;
    private String userName;
    private String userAvatarUrl;
    private long createdAt;           // mili giây
    private long answerCount;

    // ---- field thêm mới ----
    private String majorId;
    private String titleLower;        // phục vụ searchQuestions (QuestionService tự điền)
    private String imageUrl;          // có thể null
    private String status = STATUS_VISIBLE;
    private long likeCount;
    private long updatedAt;           // mili giây

    public Question() {
    }

    public Question(String id, String title, String content, String category, String userId, String userName, String userAvatarUrl, long createdAt, long answerCount) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.category = category;
        this.userId = userId;
        this.userName = userName;
        this.userAvatarUrl = userAvatarUrl;
        this.createdAt = createdAt;
        this.answerCount = answerCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    /** Bí danh của getId() cho code cũ; @Exclude để Firestore không ghi thêm field "questionId". */
    @Exclude
    public String getQuestionId() { return id; }
    @Exclude
    public void setQuestionId(String questionId) { this.id = questionId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserAvatarUrl() { return userAvatarUrl; }
    public void setUserAvatarUrl(String userAvatarUrl) { this.userAvatarUrl = userAvatarUrl; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getAnswerCount() { return answerCount; }
    public void setAnswerCount(long answerCount) { this.answerCount = answerCount; }

    public String getMajorId() { return majorId; }
    public void setMajorId(String majorId) { this.majorId = majorId; }

    public String getTitleLower() { return titleLower; }
    public void setTitleLower(String titleLower) { this.titleLower = titleLower; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getLikeCount() { return likeCount; }
    public void setLikeCount(long likeCount) { this.likeCount = likeCount; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}