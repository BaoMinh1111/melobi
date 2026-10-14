
package vn.edu.ueh.melobi_nhom5.entity;

import java.util.Date;

public class Follow {

    // Thuoc tinh
    private String followId;
    private String followerId;
    private String targetId;
    private String targetType;
    private Date createdAt;

    // Constructor rong
    public Follow() {
    }

    // Constructor day du
    public Follow(String followId, String followerId,
                  String targetId, String targetType,
                  Date createdAt) {
        this.followId = followId;
        this.followerId = followerId;
        this.targetId = targetId;
        this.targetType = targetType;
        this.createdAt = createdAt;
    }

    // Getter va Setter
    public String getFollowId() {
        return followId;
    }

    public void setFollowId(String followId) {
        this.followId = followId;
    }

    public String getFollowerId() {
        return followerId;
    }

    public void setFollowerId(String followerId) {
        this.followerId = followerId;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
