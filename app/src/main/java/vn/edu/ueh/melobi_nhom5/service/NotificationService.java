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
        if (receiverId == null || receiverId.equals(actorId)) return Tasks.forResult(null);
        Notification n = new Notification(receiverId, actorId, type, targetId, targetType);
        return db.collection(COL_NOTIFICATIONS).add(n);
        // TODO pushFCM(n): gửi FCM cần server (Cloud Functions) - hỏi leader trước khi làm
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
        return db.collection(COL_NOTIFICATIONS).document(notificationId)
                .update("isRead", true);
    }

    public Task<Void> markAllAsRead(String userId) {
        return db.collection(COL_NOTIFICATIONS)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) throw task.getException();
                    WriteBatch batch = db.batch(); // tối đa 500 thao tác / batch
                    for (DocumentSnapshot d : task.getResult()) {
                        batch.update(d.getReference(), "isRead", true);
                    }
                    return batch.commit();
                });
    }

    public Task<Integer> getUnreadCount(String userId) {
        return db.collection(COL_NOTIFICATIONS)
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)
                .count()
                .get(AggregateSource.SERVER)
                .continueWith(task -> (int) task.getResult().getCount());
    }
}