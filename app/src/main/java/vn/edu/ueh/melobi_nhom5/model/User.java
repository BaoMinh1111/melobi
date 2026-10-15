package vn.edu.ueh.melobi_nhom5.model;

import com.google.firebase.firestore.Exclude;

public class User {
    // Hằng số cho role và status, cả nhóm dùng chung để không gõ sai chuỗi
    public static final String ROLE_USER = "USER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_LOCKED = "LOCKED";

    private String userId;
    private String fullName;
    private String email;
    private String password;
    private String avatarUrl;
    private String bio;
    private String majorId;
    private String role;
    private String status;
    private int followerCount;
    private int followingCount;
    private long createdAt; // thời điểm tạo, tính bằng mili giây

    // Bắt buộc: Firestore cần constructor rỗng
    public User() {
    }

    // Constructor dùng khi đăng ký tài khoản mới
    public User(String userId, String fullName, String email) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.role = ROLE_USER;
        this.status = STATUS_ACTIVE;
        this.followerCount = 0;
        this.followingCount = 0;
        this.createdAt = System.currentTimeMillis();
    }

    public User(String userId, String fullName, String email, String avatarUrl, String majorId) {
        this.userId = userId;
        this.fullName = fullName;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.majorId = majorId;
        this.role = ROLE_USER;
        this.status = STATUS_ACTIVE;
        this.followerCount = 0;
        this.followingCount = 0;
        this.createdAt = System.currentTimeMillis();
    }

    public String getId() { return userId; }
    public void setId(String userId) { this.userId = userId; }

    public String getDisplayName() { return fullName; }
    public void setDisplayName(String displayName) { this.fullName = displayName; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    // @Exclude: không ghi password lên Firestore (xem lưu ý bên dưới)
    @Exclude
    public String getPassword() { return password; }
    @Exclude
    public void setPassword(String password) { this.password = password; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getMajorId() { return majorId; }
    public void setMajorId(String majorId) { this.majorId = majorId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getFollowerCount() { return followerCount; }
    public void setFollowerCount(int followerCount) { this.followerCount = followerCount; }

    public int getFollowingCount() { return followingCount; }
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    // Các hàm kiểm tra: phải có @Exclude, nếu không Firestore sẽ coi
    // isActive/isAdmin là trường dữ liệu và ghi thêm lên database
    @Exclude
    public boolean isActive() {
        return STATUS_ACTIVE.equals(status);
    }

    @Exclude
    public boolean isAdmin() {
        return ROLE_ADMIN.equals(role);
    }
}
