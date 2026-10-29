package vn.edu.ueh.melobi_nhom5.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.entity.Notification;

public class NotificationService {
    private static final String COL_NOTIFICATIONS = "notifications";
    private static final int NOTIFICATION_LIMIT = 50;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public Task<DocumentReference> createNotification(String receiverId, String actorId,
                                                      String type, String targetId,
                                                      String targetType) {
        if (receiverId == null || receiverId.equals(actorId)) {
            return Tasks.forResult(null);
        }
        Notification n = new Notification(receiverId, actorId, type, targetId, targetType);
        return db.collection(COL_NOTIFICATIONS).add(n);
    }

    /** Chỉ lọc theo receiverId rồi sắp xếp trên máy, tránh lỗi thiếu composite index. */
    public Task<List<Notification>> getNotifications(String userId) {
        return db.collection(COL_NOTIFICATIONS)
                .whereEqualTo("receiverId", userId)
                .get()
                .continueWith(task -> mapNotifications(task.getResult()));
    }

    public ListenerRegistration listenNotifications(String userId, EventListener<QuerySnapshot> listener) {
        return db.collection(COL_NOTIFICATIONS)
                .whereEqualTo("receiverId", userId)
                .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE, listener);
    }

    public Task<Void> markAsRead(String notificationId) {
        return db.collection(COL_NOTIFICATIONS).document(notificationId)
                .update("isRead", true);
    }

    public Task<Void> markAllAsRead(String userId) {
        return db.collection(COL_NOTIFICATIONS)
                .whereEqualTo("receiverId", userId)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    WriteBatch batch = db.batch();
                    int count = 0;
                    for (DocumentSnapshot snapshot : task.getResult()) {
                        Notification notification = snapshot.toObject(Notification.class);
                        if (notification != null && !notification.getIsRead()) {
                            batch.update(snapshot.getReference(), "isRead", true);
                            count++;
                        }
                    }
                    if (count == 0) {
                        return Tasks.forResult(null);
                    }
                    return batch.commit();
                });
    }

    public Task<Integer> getUnreadCount(String userId) {
        return getNotifications(userId).continueWith(task -> {
            int count = 0;
            List<Notification> notifications = task.getResult();
            if (notifications != null) {
                for (Notification notification : notifications) {
                    if (!notification.getIsRead()) {
                        count++;
                    }
                }
            }
            return count;
        });
    }

    public static List<Notification> mapNotifications(QuerySnapshot snapshot) {
        List<Notification> notifications = new ArrayList<>();
        if (snapshot == null) {
            return notifications;
        }
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Notification notification = doc.toObject(Notification.class);
            if (notification == null) {
                continue;
            }
            notification.setNotificationId(doc.getId());
            notifications.add(notification);
        }
        Collections.sort(notifications, (left, right) -> Long.compare(right.getCreatedAt(), left.getCreatedAt()));
        if (notifications.size() > NOTIFICATION_LIMIT) {
            return new ArrayList<>(notifications.subList(0, NOTIFICATION_LIMIT));
        }
        return notifications;
    }
}
