package vn.edu.ueh.melobi_nhom5.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Answer implements Serializable {
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_HIDDEN = "HIDDEN";

    private String answerId;
    private String questionId;
    private String userId;
    private String userName;
    private String userAvatarUrl;
    private String content;
    private long createdAt;
    private long updatedAt;
    private long likeCount;
    private long commentCount;
    private List<String> likedUserIds;
    private String status;

    public Answer() {
        this.likedUserIds = new ArrayList<>();
        this.status = STATUS_ACTIVE;
    }

    public Answer(String id, String questionId, String userId, String userName, String userAvatarUrl, String content, long createdAt, long updatedAt, long likes, long commentCount, List<String> likedUserIds, String status) {
        this.answerId = id;
        this.questionId = questionId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatarUrl = userAvatarUrl;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.likeCount = likes;
        this.commentCount = commentCount;
        this.likedUserIds = likedUserIds != null ? likedUserIds : new ArrayList<>();
        this.status = status != null ? status : STATUS_ACTIVE;
    }

    public boolean isLikedBy(String userId) {
        if (likedUserIds == null || userId == null) return false;
        return likedUserIds.contains(userId);
    }

    public String getId() {
        return answerId;
    }

    public void setId(String id) {
        this.answerId = id;
    }

    public String getAnswerId() {
        return answerId;
    }

    public void setAnswerId(String answerId) {
        this.answerId = answerId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public long getLikes() {
        return likeCount;
    }

    public void setLikes(long likes) {
        this.likeCount = likes;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(long likeCount) {
        this.likeCount = likeCount;
    }

    public long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(long commentCount) {
        this.commentCount = commentCount;
    }

    public List<String> getLikedUserIds() {
        if (likedUserIds == null) {
            likedUserIds = new ArrayList<>();
        }
        return likedUserIds;
    }

    public void setLikedUserIds(List<String> likedUserIds) {
        this.likedUserIds = likedUserIds != null ? likedUserIds : new ArrayList<>();
    }

    public String getStatus() {
        return status != null ? status : STATUS_ACTIVE;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isHidden() {
        return STATUS_HIDDEN.equals(status);
    }

    public void hideAnswer() {
        this.status = STATUS_HIDDEN;
    }
}