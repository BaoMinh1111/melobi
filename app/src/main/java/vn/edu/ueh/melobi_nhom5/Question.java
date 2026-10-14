package vn.edu.ueh.melobi_nhom5;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Entity Question - collection "questions" trên Firestore.
 * Firestore yêu cầu: có constructor rỗng + getter/setter cho mọi field.
 */
public class Question {
    public static final String STATUS_VISIBLE = "VISIBLE";
    public static final String STATUS_HIDDEN = "HIDDEN";

    @DocumentId
    private String questionId;   // tự lấy từ ID của document, không lưu thành field
    private String userId;
    private String majorId;
    private String title;
    private String content;
    private String imageUrl;     // có thể null
    private String status = STATUS_VISIBLE;
    private int likeCount;
    private int answerCount;
    @ServerTimestamp
    private Date createdAt;
    @ServerTimestamp
    private Date updatedAt;

    public Question() { }        // bắt buộc cho Firestore

    public Question(String userId, String majorId, String title, String content, String imageUrl) {
        this.userId = userId;
        this.majorId = majorId;
        this.title = title;
        this.content = content;
        this.imageUrl = imageUrl;
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
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
