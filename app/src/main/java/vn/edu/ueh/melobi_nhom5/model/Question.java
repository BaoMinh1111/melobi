package vn.edu.ueh.melobi_nhom5.model;

import java.io.Serializable;
import java.util.Locale;

public class Question implements Serializable {
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_HIDDEN = "HIDDEN";

    private String questionId;
    private String userId;
    private String majorId;
    private String category;
    private String title;
    private String titleLower;
    private String content;
    private String imageUrl;
    private String status;
    private long likeCount;
    private long answerCount;
    private long createdAt;
    private long updatedAt;
    private String userName;
    private String userAvatarUrl;

    public Question() {
        this.status = STATUS_ACTIVE;
    }

    public Question(String id, String title, String content, String category, String userId, String userName, String userAvatarUrl, long createdAt, long answerCount) {
        this.questionId = id;
        this.title = title;
        this.titleLower = title != null ? title.toLowerCase(Locale.ROOT) : null;
        this.content = content;
        this.userId = userId;
        this.majorId = category;
        this.category = category;
        this.userName = userName;
        this.userAvatarUrl = userAvatarUrl;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.answerCount = answerCount;
        this.status = STATUS_ACTIVE;
    }

    public String getId() {
        return questionId;
    }

    public void setId(String id) {
        this.questionId = id;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.titleLower = title != null ? title.toLowerCase(Locale.ROOT) : null;
    }

    public String getTitleLower() {
        return titleLower;
    }

    public void setTitleLower(String titleLower) {
        this.titleLower = titleLower;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        if (category != null && !category.trim().isEmpty()) {
            return category;
        }
        return majorId;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getMajorId() {
        return majorId;
    }

    public void setMajorId(String majorId) {
        this.majorId = majorId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public void setUserAvatarUrl(String userAvatarUrl) {
        this.userAvatarUrl = userAvatarUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getStatus() {
        return status != null ? status : STATUS_ACTIVE;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(long likeCount) {
        this.likeCount = likeCount;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public long getAnswerCount() {
        return answerCount;
    }

    public void setAnswerCount(long answerCount) {
        this.answerCount = answerCount;
    }
}