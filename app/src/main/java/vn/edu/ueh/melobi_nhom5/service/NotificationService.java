package vn.edu.ueh.melobi_nhom5.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import vn.edu.ueh.melobi_nhom5.entity.Notification;

public class NotificationService {
    private static final String COL_NOTIFICATIONS = "notifications";
    private static final int NOTIFICATION_LIMIT = 50;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /** Các service khác (Question/Answer/Comment/Follow) gọi hàm này khi có tương tác mới.
     *  Trả về Task có kết quả null nếu bỏ qua (thiếu receiverId hoặc tự thông báo cho chính mình). */
    public Task<DocumentReference> createNotification(String receiverId, String actorId,
                                                      String type, String targetId,
                                                      String targetType) {
        if (receiverId == null || receiverId.isEmpty() || receiverId.equals(actorId)) {
            return Tasks.forResult(null);
        }
        Notification n = new Notification(receiverId, actorId, type, targetId, targetType);

        if (isToggleType(type) && actorId != null && targetId != null && targetType != null) {
            // ID gồm cả người nhận: hai người nhận khác nhau không bao giờ trùng ID
            DocumentReference ref = db.collection(COL_NOTIFICATIONS).document(
                    receiverId + "_" + actorId + "_" + type + "_" + targetType + "_" + targetId);
            return db.runTransaction(tx -> {
                if (!tx.get(ref).exists()) {   // đã có thì giữ nguyên, không đặt lại "chưa đọc"
                    tx.set(ref, n);
                }
                return ref;
            });
        }
        return db.collection(COL_NOTIFICATIONS).add(n);   // NEW_ANSWER, NEW_COMMENT: mỗi lần là một sự kiện mới
    }

    /** Các hành động bật/tắt được, cần chống spam thông báo. */
    private static boolean isToggleType(String type) {
        return Notification.TYPE_LIKE.equals(type) || Notification.TYPE_FOLLOW.equals(type);
    }

    /** Cần composite index (receiverId + createdAt). Lần chạy đầu Logcat sẽ in link tạo index. */
    public Task<QuerySnapshot> getNotifications(String userId) {
        return db.collection(COL_NOTIFICATIONS)
                .whereEqualTo("receiverId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(NOTIFICATION_LIMIT)
                .get();
    }

    public Task<Void> markAsRead(String notificationId) {
        if (notificationId == null || notificationId.isEmpty()) {
            return Tasks.forException(new IllegalArgumentException("notificationId không hợp lệ"));
        }
        return db.collection(COL_NOTIFICATIONS).document(notificationId).update("isRead", true);
    }

    public Task<Void> markAllAsRead(String userId) {
        return db.collection(COL_NOTIFICATIONS)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .limit(500)                       // batch tối đa 500 thao tác
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot d : task.getResult()) {
                        batch.update(d.getReference(), "isRead", true);
                    }
                    return batch.commit();
                });
    }

    public Task<Integer> getUnreadCount(String userId) {
        if (userId == null || userId.isEmpty()) return Tasks.forResult(0);
        return db.collection(COL_NOTIFICATIONS)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .count()
                .get(AggregateSource.SERVER)
                .continueWith(task -> (int) task.getResult().getCount());
    }
}
/* 1 số vấn đề nhỏ sau:
* hai thông báo LIKE khác nhau có thể trùng ID
* Chống trùng mới chỉ áp dụng cho LIKE
* Ghi đè thông báo cũ cũng đặt lại trạng thái chưa đọc*/
