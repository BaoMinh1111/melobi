
package vn.edu.ueh.melobi_nhom5.service;

import vn.edu.ueh.melobi_nhom5.entity.Follow;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class FollowService {

    private final List<Follow> follows = new ArrayList<>();

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

    private void validateIds(String followerId, String followingId) {
        if (followerId == null || followerId.isBlank()
                || followingId == null || followingId.isBlank()) {
            throw new IllegalArgumentException(
                    "User ID khong duoc de trong"
            );
        }
    }
}
