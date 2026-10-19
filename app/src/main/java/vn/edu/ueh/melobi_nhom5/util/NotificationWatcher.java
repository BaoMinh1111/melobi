package vn.edu.ueh.melobi_nhom5.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.HashSet;
import java.util.Set;

import vn.edu.ueh.melobi_nhom5.NotificationActivity;
import vn.edu.ueh.melobi_nhom5.entity.Notification;   // entity của nhóm, không phải android.app.Notification

/**
 * "Push" không cần server: lắng nghe realtime collection notifications của người dùng
 * và hiện thông báo hệ thống khi có thông báo mới. Chỉ hoạt động khi tiến trình của app còn sống
 * (app đang mở hoặc vừa thu nhỏ). App đã bị tắt hẳn thì phải dùng FCM thật (Cloud Functions).
 */
public class NotificationWatcher {
    private static final String TAG = "NotificationWatcher";
    private static final String CHANNEL_ID = "melobi_default";
    private static final long MAX_AGE_MS = 2 * 60 * 1000;   // bỏ qua thông báo cũ hơn 2 phút

    private final Context appContext;
    private final Set<String> shownIds = new HashSet<>();
    private ListenerRegistration registration;
    private boolean firstSnapshot;

    public NotificationWatcher(Context context) {
        this.appContext = context.getApplicationContext();
        createChannel();
    }

    public void start(String userId) {
        stop();
        if (userId == null || userId.isEmpty()) return;
        firstSnapshot = true;

        registration = FirebaseFirestore.getInstance().collection("notifications")
                .whereEqualTo("receiverId", userId)
                .whereEqualTo("isRead", false)        // chỉ điều kiện bằng nhau nên không cần composite index
                .addSnapshotListener((snap, e) -> {
                    if (e != null || snap == null) {
                        Log.w(TAG, "Lỗi lắng nghe thông báo", e);
                        return;
                    }
                    if (firstSnapshot) {              // lần đầu trả về toàn bộ thông báo chưa đọc: không báo
                        firstSnapshot = false;
                        return;
                    }
                    for (DocumentChange change : snap.getDocumentChanges()) {
                        if (change.getType() != DocumentChange.Type.ADDED) continue;
                        Notification n = change.getDocument().toObject(Notification.class);
                        if (n == null || n.getNotificationId() == null) continue;
                        if (n.getCreatedAt() != null
                                && System.currentTimeMillis() - n.getCreatedAt().getTime() > MAX_AGE_MS) continue;
                        if (!shownIds.add(n.getNotificationId())) continue;   // đã báo rồi
                        resolveNameAndShow(n);
                    }
                });
    }

    public void stop() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    // ---------- nội bộ ----------

    /** GIẢ ĐỊNH giống NotificationActivity: users/{uid}.displayName. Không có thì hiện "Ai đó". */
    private void resolveNameAndShow(Notification n) {
        if (n.getActorId() == null) {
            show(n, "Ai đó");
            return;
        }
        FirebaseFirestore.getInstance().collection("users").document(n.getActorId()).get()
                .addOnCompleteListener(task -> {
                    String name = null;
                    if (task.isSuccessful() && task.getResult() != null) {
                        name = task.getResult().getString("displayName");
                    }
                    show(n, name != null && !name.isEmpty() ? name : "Ai đó");
                });
    }

    @SuppressLint("MissingPermission")   // đã kiểm tra quyền ngay bên dưới
    private void show(Notification n, String actorName) {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;   // người dùng chưa cấp quyền hiện thông báo
        }

        Intent intent = new Intent(appContext, NotificationActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int id = n.getNotificationId().hashCode();
        PendingIntent pendingIntent = PendingIntent.getActivity(appContext, id, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(appContext, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_popup_reminder)   // thay bằng icon riêng của nhóm nếu có
                .setContentTitle("Melobi")
                .setContentText(actorName + " " + actionText(n.getType()))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(appContext).notify(id, builder.build());
    }

    private static String actionText(String type) {
        if (Notification.TYPE_LIKE.equals(type)) return "đã thích nội dung của bạn";
        if (Notification.TYPE_NEW_ANSWER.equals(type)) return "đã trả lời câu hỏi của bạn";
        if (Notification.TYPE_NEW_COMMENT.equals(type)) return "đã bình luận câu trả lời của bạn";
        if (Notification.TYPE_FOLLOW.equals(type)) return "đã theo dõi bạn";
        return "có tương tác mới với bạn";
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Thông báo Melobi", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = appContext.getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }
}