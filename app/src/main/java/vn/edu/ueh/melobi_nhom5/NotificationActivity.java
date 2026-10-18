package vn.edu.ueh.melobi_nhom5;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vn.edu.ueh.melobi_nhom5.adapter.NotificationAdapter;
import vn.edu.ueh.melobi_nhom5.entity.Like;
import vn.edu.ueh.melobi_nhom5.entity.Notification;   // entity của nhóm
import vn.edu.ueh.melobi_nhom5.service.NotificationService;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;

/** Màn hình danh sách thông báo: bấm để đánh dấu đã đọc + mở nội dung, nút "đọc tất cả". */
public class NotificationActivity extends AppCompatActivity {

    private static final String TAG = "NotificationActivity";

    private NotificationService notificationService;
    private NotificationAdapter adapter;
    private String userId;
    private int loadToken;   // bỏ qua kết quả trả về trễ

    private ProgressBar pbNotifications;
    private TextView tvNotifEmpty, tvUnreadSummary, tvMarkAllRead;
    private RecyclerView rvNotifications;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Chưa đăng nhập được, vui lòng kiểm tra mạng", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        userId = AuthHelper.getCurrentUser().getId();
        notificationService = new NotificationService();

        MaterialToolbar toolbar = findViewById(R.id.toolbarNotification);
        toolbar.setNavigationOnClickListener(v -> finish());
        pbNotifications = findViewById(R.id.pbNotifications);
        tvNotifEmpty = findViewById(R.id.tvNotifEmpty);
        tvUnreadSummary = findViewById(R.id.tvUnreadSummary);
        tvMarkAllRead = findViewById(R.id.tvMarkAllRead);
        rvNotifications = findViewById(R.id.rvNotifications);

        adapter = new NotificationAdapter(this::onNotificationClick);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        rvNotifications.setAdapter(adapter);

        tvMarkAllRead.setOnClickListener(v -> markAllRead());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (notificationService != null) loadNotifications();   // làm mới mỗi khi quay lại màn hình
    }

    private boolean isUiGone() {
        return isFinishing() || isDestroyed();
    }

    // ---------- Tải dữ liệu ----------

    private void loadNotifications() {
        final int token = ++loadToken;
        pbNotifications.setVisibility(View.VISIBLE);
        tvNotifEmpty.setVisibility(View.GONE);

        notificationService.getNotifications(userId)
                .addOnSuccessListener(snapshot -> {
                    if (isUiGone() || token != loadToken) return;
                    pbNotifications.setVisibility(View.GONE);

                    List<Notification> list = new ArrayList<>();
                    for (DocumentSnapshot d : snapshot.getDocuments()) {
                        Notification n = d.toObject(Notification.class);
                        if (n != null) list.add(n);
                    }
                    adapter.setItems(list);
                    tvNotifEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);

                    loadActorNames(list);
                    refreshUnreadCount();
                })
                .addOnFailureListener(e -> {
                    if (isUiGone() || token != loadToken) return;
                    pbNotifications.setVisibility(View.GONE);
                    Log.e(TAG, "Lỗi tải thông báo: ", e);
                    boolean missingIndex = e instanceof FirebaseFirestoreException
                            && ((FirebaseFirestoreException) e).getCode()
                            == FirebaseFirestoreException.Code.FAILED_PRECONDITION;
                    Toast.makeText(this, missingIndex
                                    ? "Thiếu index Firestore - xem Logcat (tag " + TAG + ") để lấy link tạo"
                                    : "Lỗi tải thông báo: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    /**
     * Tra tên người gây ra thông báo. GIẢ ĐỊNH: collection "users", document ID = uid,
     * field tên hiển thị là "displayName". Sai tên field thì chỉ hiện "Ai đó", không crash.
     */
    private void loadActorNames(List<Notification> list) {
        Set<String> ids = new HashSet<>();
        for (Notification n : list) {
            String id = n.getActorId();
            if (id != null && !id.isEmpty() && !adapter.hasActorName(id)) ids.add(id);
        }
        for (String id : ids) {
            FirebaseFirestore.getInstance().collection("users").document(id).get()
                    .addOnSuccessListener(doc -> {
                        if (isUiGone() || doc == null || !doc.exists()) return;
                        adapter.putActorName(id, doc.getString("displayName"));
                    });
        }
    }

    private void refreshUnreadCount() {
        notificationService.getUnreadCount(userId)
                .addOnSuccessListener(count -> {
                    if (isUiGone()) return;
                    tvUnreadSummary.setText(count > 0 ? count + " chưa đọc" : "Đã đọc hết");
                    tvMarkAllRead.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                })
                .addOnFailureListener(e -> {
                    if (isUiGone()) return;
                    Log.w(TAG, "Không đếm được thông báo chưa đọc", e);
                    tvUnreadSummary.setText("");
                });
    }

    // ---------- Thao tác ----------

    private void onNotificationClick(Notification n) {
        if (!n.isRead()) {
            n.setRead(true);          // cập nhật giao diện ngay, không chờ mạng
            adapter.refresh();
            notificationService.markAsRead(n.getNotificationId())
                    .addOnSuccessListener(v -> {
                        if (!isUiGone()) refreshUnreadCount();
                    })
                    .addOnFailureListener(e -> {
                        Log.w(TAG, "Không đánh dấu được đã đọc", e);
                        n.setRead(false);   // hoàn lại
                        if (!isUiGone()) adapter.refresh();
                    });
        }

        if (Like.TYPE_QUESTION.equals(n.getTargetType()) && n.getTargetId() != null) {
            Intent intent = new Intent(this, QuestionDetailActivity.class);
            intent.putExtra(QuestionDetailActivity.EXTRA_QUESTION_ID, n.getTargetId());
            startActivity(intent);
        } else {
            // FOLLOW / ANSWER / COMMENT: chưa có màn hình đích, bổ sung khi các bạn làm xong
            Toast.makeText(this, "Chưa hỗ trợ mở nội dung này", Toast.LENGTH_SHORT).show();
        }
    }

    private void markAllRead() {
        tvMarkAllRead.setEnabled(false);
        notificationService.markAllAsRead(userId)
                .addOnSuccessListener(v -> {
                    if (isUiGone()) return;
                    tvMarkAllRead.setEnabled(true);
                    adapter.markAllReadLocally();
                    refreshUnreadCount();   // còn >500 thông báo cũ thì số này vẫn > 0
                })
                .addOnFailureListener(e -> {
                    if (isUiGone()) return;
                    tvMarkAllRead.setEnabled(true);
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}