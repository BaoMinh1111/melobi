package vn.edu.ueh.melobi_nhom5.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import vn.edu.ueh.melobi_nhom5.entity.Follow;

public class FollowService {
    private static final String TARGET_USER = "USER";
    private static final String TARGET_QUESTION = "QUESTION";

    private final List<Follow> follows = new ArrayList<>();

    public boolean followUser(String followerId, String followingId) {
        return followTarget(followerId, followingId, TARGET_USER);
    }

    public boolean unfollowUser(String followerId, String followingId) {
        return unfollowTarget(followerId, followingId, TARGET_USER);
    }

    public boolean followQuestion(String followerId, String questionId) {
        return followTarget(followerId, questionId, TARGET_QUESTION);
    }

    public boolean unfollowQuestion(String followerId, String questionId) {
        return unfollowTarget(followerId, questionId, TARGET_QUESTION);
    }

    public boolean isFollowing(String followerId, String followingId) {
        return isFollowing(followerId, followingId, TARGET_USER);
    }

    public boolean isFollowing(String userId, String targetId, String targetType) {
        validateIds(userId, targetId);
        for (Follow follow : follows) {
            if (targetType.equals(follow.getTargetType())
                    && userId.equals(follow.getFollowerId())
                    && targetId.equals(follow.getTargetId())) {
                return true;
            }
        }
        return false;
    }

    private boolean followTarget(String followerId, String targetId, String targetType) {
        validateIds(followerId, targetId);
        if (TARGET_USER.equals(targetType) && followerId.equals(targetId)) {
            throw new IllegalArgumentException("Khong the follow chinh minh");
        }
        if (isFollowing(followerId, targetId, targetType)) {
            return false;
        }

        follows.add(new Follow(
                UUID.randomUUID().toString(),
                followerId,
                targetId,
                targetType,
                new Date()
        ));
        return true;
    }

    private boolean unfollowTarget(String followerId, String targetId, String targetType) {
        validateIds(followerId, targetId);
        Iterator<Follow> iterator = follows.iterator();
        while (iterator.hasNext()) {
            Follow follow = iterator.next();
            if (targetType.equals(follow.getTargetType())
                    && followerId.equals(follow.getFollowerId())
                    && targetId.equals(follow.getTargetId())) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    private void validateIds(String followerId, String targetId) {
        if (followerId == null || followerId.isBlank() || targetId == null || targetId.isBlank()) {
            throw new IllegalArgumentException("User ID khong duoc de trong");
        }
    }
}
