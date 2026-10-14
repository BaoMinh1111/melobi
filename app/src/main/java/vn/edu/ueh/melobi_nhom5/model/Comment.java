package vn.edu.ueh.melobi_nhom5.model;

import java.io.Serializable;

public class Comment implements Serializable {
    private String id;
    private String answerId;
    private String userId;
    private String userName;
    private String userAvatarUrl;
    private String content;
    private long createdAt;
    private long updatedAt;

    public Comment() {
    }

    public Comment(String id, String answerId, String userId, String userName, String userAvatarUrl, String content, long createdAt, long updatedAt) {
        this.id = id;
        this.answerId = answerId;
        this.userId = userId;
        this.userName = userName;
        this.userAvatarUrl = userAvatarUrl;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAnswerId() {
        return answerId;
    }

    public void setAnswerId(String answerId) {
        this.answerId = answerId;
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
}
/*có 1 số lỗi sau:
* updatedAt thừa so với chức năng: CommentService chỉ có thêm, xóa, xem, không có sửa
* userName, userAvatarUrl chưa khai báo*/
