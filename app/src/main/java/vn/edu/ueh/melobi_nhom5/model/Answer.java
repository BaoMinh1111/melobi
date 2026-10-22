
package vn.edu.ueh.melobi_nhom5.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Answer implements Serializable {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_HIDDEN = "HIDDEN";
    public static final String STATUS_DELETED = "DELETED";

    private String id;
    private String questionId;
    private String userId;

    // Thong tin hien thi nguoi tra loi
    private String userName;
    private String userAvatarUrl;

    private String content;

    // So luot thich va danh sach user da thich
    private long likes;
    private List<String> likedUserIds = new ArrayList<>();

    private long commentCount;
    private long createdAt;
    private long updatedAt;

    // Trang thai kiem duyet cau tra loi
    private String status = STATUS_ACTIVE;

    public Answer() {
        this.likedUserIds = new ArrayList<>();
        this.status = STATUS_ACTIVE;
    }

    public Answer(
            String id,
            String questionId,
            String userId,
            String userName,
            String userAvatarUrl,
            String content,
            long likes,
            List<String> likedUserIds,
            long commentCount,
            long createdAt,
            long updatedAt
    ) {
        this.id = id;
        this.questionId = questionId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatarUrl = userAvatarUrl;
        this.content = content;
        this.likes = Math.max(0, likes);
        this.likedUserIds = likedUserIds != null
                ? new ArrayList<>(likedUserIds)
                : new ArrayList<>();
        this.commentCount = Math.max(0, commentCount);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.status = STATUS_ACTIVE;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public long getLikes() {
        return likes;
    }

    public void setLikes(long likes) {
        this.likes = Math.max(0, likes);
    }

    public List<String> getLikedUserIds() {
        if (likedUserIds == null) {
            likedUserIds = new ArrayList<>();
        }
        return likedUserIds;
    }

    public void setLikedUserIds(List<String> likedUserIds) {
        this.likedUserIds = likedUserIds != null
                ? new ArrayList<>(likedUserIds)
                : new ArrayList<>();
    }

    public long getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(long commentCount) {
        this.commentCount = Math.max(0, commentCount);
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

    public String getStatus() {
        return status == null ? STATUS_ACTIVE : status;
    }

    public void setStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            this.status = STATUS_ACTIVE;
        } else {
            this.status = status;
        }
    }

    public boolean isHidden() {
        return STATUS_HIDDEN.equalsIgnoreCase(getStatus())
                || STATUS_DELETED.equalsIgnoreCase(getStatus());
    }

    public void hideAnswer() {
        this.status = STATUS_HIDDEN;
    }

    public void restoreAnswer() {
        this.status = STATUS_ACTIVE;
    }

    public void deleteAnswer() {
        this.status = STATUS_DELETED;
    }

    public boolean isLikedBy(String currentUserId) {
        return currentUserId != null
                && getLikedUserIds().contains(currentUserId);
    }
}
