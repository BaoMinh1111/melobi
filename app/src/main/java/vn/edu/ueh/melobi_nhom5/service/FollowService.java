package vn.edu.ueh.melobi_nhom5.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import vn.edu.ueh.melobi_nhom5.entity.Follow;
import vn.edu.ueh.melobi_nhom5.entity.Notification;
import vn.edu.ueh.melobi_nhom5.model.User;

public class FollowService {
    public static final String TARGET_USER = "USER";
    public static final String TARGET_QUESTION = "QUESTION";
    private static final String COL_FOLLOWS = "follows";
    private static final String COL_USERS = "users";
    private static final int SUGGESTION_LIMIT = 20;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    private final List<Follow> follows = new ArrayList<>();
    private final FirebaseFirestore db;
    private final NotificationService notificationService;

    public FollowService() {
        this.db = tryFirestore();
        this.notificationService = db != null ? new NotificationService() : null;
    }

    public boolean followUser(String followerId, String followingId) {
        boolean created = followTarget(followerId, followingId, TARGET_USER);
        persistFollow(followerId, followingId, TARGET_USER, created);
        if (created) {
            notifyFollow(followerId, followingId);
            updateUserCounts(followerId, followingId, 1);
        }
        return created;
    }

    public void followUser(String followerId, String followingId, Callback<Boolean> callback) {
        try {
            boolean created = followUser(followerId, followingId);
            if (callback != null) {
                callback.onSuccess(created);
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e);
            } else {
                throw e;
            }
        }
    }

    public boolean unfollowUser(String followerId, String followingId) {
        boolean removed = unfollowTarget(followerId, followingId, TARGET_USER);
        if (db != null) {
            deleteFollow(followerId, followingId, TARGET_USER);
            updateUserCounts(followerId, followingId, -1);
            return true;
        }
        return removed;
    }

    public void unfollowUser(String followerId, String followingId, Callback<Boolean> callback) {
        try {
            boolean removed = unfollowUser(followerId, followingId);
            if (callback != null) {
                callback.onSuccess(removed);
            }
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e);
            } else {
                throw e;
            }
        }
    }

    public boolean followQuestion(String followerId, String questionId) {
        boolean created = followTarget(followerId, questionId, TARGET_QUESTION);
        persistFollow(followerId, questionId, TARGET_QUESTION, created);
        return created;
    }

    public boolean unfollowQuestion(String followerId, String questionId) {
        boolean removed = unfollowTarget(followerId, questionId, TARGET_QUESTION);
        deleteFollow(followerId, questionId, TARGET_QUESTION);
        return removed;
    }

    public boolean isFollowing(String followerId, String followingId) {
        return isFollowing(followerId, followingId, TARGET_USER);
    }

    public boolean isFollowing(String userId, String targetId, String targetType) {
        validateIds(userId, targetId);
        String type = targetType != null ? targetType : TARGET_USER;
        for (Follow follow : follows) {
            if (type.equals(follow.getTargetType())
                    && userId.equals(follow.getFollowerId())
                    && targetId.equals(follow.getTargetId())) {
                return true;
            }
        }
        return false;
    }

    public void isFollowing(String userId, String targetId, String targetType, Callback<Boolean> callback) {
        try {
            validateIds(userId, targetId);
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e);
            }
            return;
        }

        String type = targetType != null ? targetType : TARGET_USER;
        if (db == null) {
            if (callback != null) {
                callback.onSuccess(isFollowing(userId, targetId, type));
            }
            return;
        }

        db.collection(COL_FOLLOWS)
                .document(buildFollowId(userId, type, targetId))
                .get()
                .addOnSuccessListener(snapshot -> {
                    boolean following = snapshot != null && snapshot.exists();
                    if (following && !isFollowing(userId, targetId, type)) {
                        follows.add(new Follow(snapshot.getId(), userId, targetId, type, new Date()));
                    }
                    if (callback != null) {
                        callback.onSuccess(following);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    public List<String> getFollowers(String userId) {
        validateUserId(userId);
        List<String> result = new ArrayList<>();
        for (Follow follow : follows) {
            if (TARGET_USER.equals(follow.getTargetType()) && userId.equals(follow.getTargetId())) {
                result.add(follow.getFollowerId());
            }
        }
        return result;
    }

    public void getFollowers(String userId, Callback<List<String>> callback) {
        loadIds(userId, true, callback);
    }

    public List<String> getFollowing(String userId) {
        validateUserId(userId);
        List<String> result = new ArrayList<>();
        for (Follow follow : follows) {
            if (TARGET_USER.equals(follow.getTargetType()) && userId.equals(follow.getFollowerId())) {
                result.add(follow.getTargetId());
            }
        }
        return result;
    }

    public void getFollowing(String userId, Callback<List<String>> callback) {
        loadIds(userId, false, callback);
    }

    public List<String> getFollowedQuestions(String userId) {
        validateUserId(userId);
        List<String> result = new ArrayList<>();
        for (Follow follow : follows) {
            if (TARGET_QUESTION.equals(follow.getTargetType()) && userId.equals(follow.getFollowerId())) {
                result.add(follow.getTargetId());
            }
        }
        return result;
    }

    public void suggestUsers(String majorId, Callback<List<User>> callback) {
        if (db == null) {
            if (callback != null) {
                callback.onSuccess(new ArrayList<>());
            }
            return;
        }

        String currentUserId = currentUserId();
        db.collection(COL_USERS)
                .get()
                .continueWithTask(task -> {
                    QuerySnapshot snapshot = task.getResult();
                    List<User> candidates = new ArrayList<>();
                    String myMajor = majorId;
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            User user = doc.toObject(User.class);
                            if (user == null) {
                                continue;
                            }
                            if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                                user.setUserId(doc.getId());
                            }
                            if (currentUserId != null && currentUserId.equals(user.getUserId())) {
                                if (user.getMajorId() != null && !user.getMajorId().trim().isEmpty()) {
                                    myMajor = user.getMajorId();
                                }
                                continue;
                            }
                            if (isSuggestable(user, currentUserId)) {
                                candidates.add(user);
                            }
                        }
                    }
                    List<User> sameMajor = new ArrayList<>();
                    List<User> others = new ArrayList<>();
                    for (User user : candidates) {
                        if (isSameMajor(myMajor, user.getMajorId())) {
                            sameMajor.add(user);
                        } else {
                            others.add(user);
                        }
                    }
                    Collections.sort(sameMajor, (a, b) -> Integer.compare(b.getFollowerCount(), a.getFollowerCount()));
                    Collections.sort(others, (a, b) -> Integer.compare(b.getFollowerCount(), a.getFollowerCount()));

                    List<User> combined = new ArrayList<>(sameMajor);
                    combined.addAll(others);
                    return filterAlreadyFollowed(currentUserId, combined);
                })
                .addOnSuccessListener(users -> {
                    if (callback != null) {
                        callback.onSuccess(users);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    private Task<List<User>> filterAlreadyFollowed(String currentUserId, List<User> candidates) {
        if (currentUserId == null || currentUserId.trim().isEmpty() || db == null) {
            return Tasks.forResult(limit(candidates));
        }
        return db.collection(COL_FOLLOWS)
                .whereEqualTo("followerId", currentUserId)
                .whereEqualTo("targetType", TARGET_USER)
                .get()
                .continueWith(task -> {
                    Set<String> followingIds = new HashSet<>();
                    QuerySnapshot snapshot = task.getResult();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            String targetId = doc.getString("targetId");
                            if (targetId != null) {
                                followingIds.add(targetId);
                            }
                        }
                    }
                    List<User> filtered = new ArrayList<>();
                    for (User user : candidates) {
                        if (!followingIds.contains(user.getUserId())) {
                            filtered.add(user);
                        }
                    }
                    return limit(filtered);
                });
    }

    private List<User> limit(List<User> users) {
        if (users.size() <= SUGGESTION_LIMIT) {
            return users;
        }
        return new ArrayList<>(users.subList(0, SUGGESTION_LIMIT));
    }

    public static boolean isSameMajor(String left, String right) {
        String groupLeft = majorGroup(left);
        String groupRight = majorGroup(right);
        if (groupLeft.isEmpty() || groupRight.isEmpty()) {
            return false;
        }
        return groupLeft.equals(groupRight);
    }

    private static String majorGroup(String value) {
        String normalized = normalizeMajor(value);
        if (normalized.isEmpty()) {
            return "";
        }
        if (normalized.equals("it") || normalized.contains("cntt")
                || normalized.contains("cong nghe") || normalized.contains("thong tin")) {
            return "it";
        }
        if (normalized.equals("econ") || normalized.contains("kinh te") || normalized.contains("kinhte")) {
            return "econ";
        }
        if (normalized.contains("market")) {
            return "marketing";
        }
        if (normalized.equals("other") || normalized.equals("khac")) {
            return "other";
        }
        return normalized;
    }

    private static String normalizeMajor(String value) {
        if (value == null) {
            return "";
        }
        String normalized = Normalizer.normalize(value.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}+", "");
    }

    private boolean isSuggestable(User user, String currentUserId) {
        if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
            return false;
        }
        if (currentUserId != null && currentUserId.equals(user.getUserId())) {
            return false;
        }
        if (User.STATUS_LOCKED.equalsIgnoreCase(user.getStatus())) {
            return false;
        }
        return !user.isAdmin();
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
                buildFollowId(followerId, targetType, targetId),
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

    private void persistFollow(String followerId, String targetId, String targetType, boolean created) {
        if (db == null || !created) {
            return;
        }
        String followId = buildFollowId(followerId, targetType, targetId);
        Follow follow = new Follow(followId, followerId, targetId, targetType, new Date());
        db.collection(COL_FOLLOWS).document(followId).set(follow, SetOptions.merge());
    }

    private void deleteFollow(String followerId, String targetId, String targetType) {
        if (db == null) {
            return;
        }
        db.collection(COL_FOLLOWS)
                .document(buildFollowId(followerId, targetType, targetId))
                .delete();
    }

    private void updateUserCounts(String followerId, String followingId, int delta) {
        if (db == null) {
            return;
        }
        db.collection(COL_USERS).document(followerId)
                .update("followingCount", FieldValue.increment(delta));
        db.collection(COL_USERS).document(followingId)
                .update("followerCount", FieldValue.increment(delta));
    }

    private void notifyFollow(String followerId, String followingId) {
        if (notificationService == null) {
            return;
        }
        notificationService.createNotification(
                followingId,
                followerId,
                Notification.TYPE_FOLLOW,
                followerId,
                TARGET_USER
        );
    }

    private void loadIds(String userId, boolean followers, Callback<List<String>> callback) {
        try {
            validateUserId(userId);
        } catch (Exception e) {
            if (callback != null) {
                callback.onError(e);
            }
            return;
        }
        if (db == null) {
            if (callback != null) {
                callback.onSuccess(followers ? getFollowers(userId) : getFollowing(userId));
            }
            return;
        }

        String field = followers ? "targetId" : "followerId";
        db.collection(COL_FOLLOWS)
                .whereEqualTo(field, userId)
                .whereEqualTo("targetType", TARGET_USER)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<String> ids = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        String value = followers ? doc.getString("followerId") : doc.getString("targetId");
                        if (value != null && !value.trim().isEmpty()) {
                            ids.add(value);
                        }
                    }
                    if (callback != null) {
                        callback.onSuccess(ids);
                    }
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    private String buildFollowId(String followerId, String targetType, String targetId) {
        return followerId + "_" + targetType + "_" + targetId;
    }

    private String currentUserId() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        return firebaseUser != null ? firebaseUser.getUid() : null;
    }

    private FirebaseFirestore tryFirestore() {
        try {
            return FirebaseFirestore.getInstance();
        } catch (Throwable ignored) {
            return null;
        }
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID khong duoc de trong");
        }
    }

    private void validateIds(String followerId, String targetId) {
        if (followerId == null || followerId.trim().isEmpty()
                || targetId == null || targetId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID khong duoc de trong");
        }
    }
}
