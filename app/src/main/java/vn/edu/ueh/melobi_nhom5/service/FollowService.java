
package vn.edu.ueh.melobi_nhom5.service;

import vn.edu.ueh.melobi_nhom5.entity.Follow;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import vn.edu.ueh.melobi_nhom5.entity.Notification;

public class FollowService {

    private final List<Follow> follows = new ArrayList<>();
    private NotificationService notificationService;

    // Cho phep gan service thong bao khi can
    public void setNotificationService(NotificationService service) {
        this.notificationService = service;
    }
    // Theo doi nguoi dung
    public boolean followUser(String followerId, String followingId) {
        validateIds(followerId, followingId);

        // Khong duoc follow chinh minh
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException(
                    "Khong the follow chinh minh"
            );
        }

        // Khong follow trung
        if (isFollowing(followerId, followingId)) {
            return false;
        }

        Follow follow = new Follow(
                UUID.randomUUID().toString(),
                followerId,
                followingId,
                "USER",
                new Date()
        );

        follows.add(follow);

// Chi thong bao khi follow moi thanh cong
        if (notificationService != null) {
            notificationService.createNotification(
                    followingId,              // Nguoi nhan
                    followerId,               // Nguoi thuc hien
                    Notification.TYPE_FOLLOW,  // Loai FOLLOW
                    followerId,               // Mo profile nguoi follow
                    "USER"
            ).addOnFailureListener(e ->
                    android.util.Log.e(
                            "FollowService",
                            "Khong tao duoc thong bao FOLLOW",
                            e
                    )
            );
        }

        return true;
    }

    // Huy theo doi
    public boolean unfollowUser(String followerId, String followingId) {
        validateIds(followerId, followingId);

        Iterator<Follow> iterator = follows.iterator();

        while (iterator.hasNext()) {
            Follow follow = iterator.next();

            if ("USER".equals(follow.getTargetType())
                    && followerId.equals(follow.getFollowerId())
                    && followingId.equals(follow.getTargetId())) {

                iterator.remove();
                return true;
            }
        }

        return false;
    }

    // Kiem tra da follow hay chua
    public boolean isFollowing(String followerId, String followingId) {
        validateIds(followerId, followingId);

        for (Follow follow : follows) {
            if ("USER".equals(follow.getTargetType())
                    && followerId.equals(follow.getFollowerId())
                    && followingId.equals(follow.getTargetId())) {
                return true;
            }
        }

        return false;
    }


    // Lay danh sach ID nguoi dang theo doi userId
    public List<String> getFollowers(String userId) {
        validateUserId(userId);

        List<String> followers = new ArrayList<>();

        for (Follow follow : follows) {
            if ("USER".equals(follow.getTargetType())
                    && userId.equals(follow.getTargetId())) {
                followers.add(follow.getFollowerId());
            }
        }

        return followers;
    }

    // Lay danh sach ID nguoi ma userId dang theo doi
    public List<String> getFollowing(String userId) {
        validateUserId(userId);

        List<String> following = new ArrayList<>();

        for (Follow follow : follows) {
            if ("USER".equals(follow.getTargetType())
                    && userId.equals(follow.getFollowerId())) {
                following.add(follow.getTargetId());
            }
        }

        return following;
    }

    // Dem so nguoi theo doi
    public int getFollowerCount(String userId) {
        return getFollowers(userId).size();
    }

    // Dem so nguoi dang theo doi
    public int getFollowingCount(String userId) {
        return getFollowing(userId).size();
    }

    // Theo doi cau hoi
    public boolean followQuestion(String userId, String questionId) {
        validateIds(userId, questionId);

        if (isFollowingQuestion(userId, questionId)) {
            return false;
        }

        Follow follow = new Follow(
                UUID.randomUUID().toString(),
                userId,
                questionId,
                "QUESTION",
                new Date()
        );

        follows.add(follow);
        return true;
    }

    // Huy theo doi cau hoi
    public boolean unfollowQuestion(String userId, String questionId) {
        validateIds(userId, questionId);

        Iterator<Follow> iterator = follows.iterator();

        while (iterator.hasNext()) {
            Follow follow = iterator.next();

            if ("QUESTION".equals(follow.getTargetType())
                    && userId.equals(follow.getFollowerId())
                    && questionId.equals(follow.getTargetId())) {

                iterator.remove();
                return true;
            }
        }

        return false;
    }

    // Kiem tra da theo doi cau hoi hay chua
    public boolean isFollowingQuestion(String userId, String questionId) {
        validateIds(userId, questionId);

        for (Follow follow : follows) {
            if ("QUESTION".equals(follow.getTargetType())
                    && userId.equals(follow.getFollowerId())
                    && questionId.equals(follow.getTargetId())) {
                return true;
            }
        }

        return false;
    }

    // Lay danh sach ID cau hoi da theo doi
    public List<String> getFollowedQuestions(String userId) {
        validateUserId(userId);

        List<String> result = new ArrayList<>();

        for (Follow follow : follows) {
            if ("QUESTION".equals(follow.getTargetType())
                    && userId.equals(follow.getFollowerId())) {
                result.add(follow.getTargetId());
            }
        }

        return result;
    }


    // Kiem tra ID hop le
    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "User ID khong duoc de trong"
            );
        }
    }

    private void validateIds(String followerId, String followingId) {
        if (followerId == null || followerId.isBlank()
                || followingId == null || followingId.isBlank()) {
            throw new IllegalArgumentException(
                    "User ID khong duoc de trong"
            );
        }
    }
}
/*1 số lỗi:
* Mất dữ liệu khi thoát app: Mở lại app thì mọi lượt follow biến mất
* Mỗi new FollowService() có danh sách riêng: Màn hình chị follow xong, màn hình Nghi tạo FollowService mới sẽ thấy “chưa follow”
* Không chia sẻ giữa các người dùng: Người được follow không thể biết có ai follow mình, nên getFollowers() luôn rỗng trên máy của họ
* Không có suggestUsers: Hàm này cần đọc users từ Firestore, nằm ngoài danh sách bộ nhớ*/
