
package vn.edu.ueh.melobi_nhom5;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.edu.ueh.melobi_nhom5.adapter.ConversationAdapter;
import vn.edu.ueh.melobi_nhom5.entity.Conversation;
import vn.edu.ueh.melobi_nhom5.service.ChatService;

public class ConversationListActivity
        extends AppCompatActivity {

    private ChatService chatService;
    private FirebaseFirestore db;
    private ConversationAdapter adapter;

    private String currentUserId;

    private RecyclerView rvConversations;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private MaterialButton btnNewConversation;

    private final Map<String, String> userNames =
            new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_conversation_list
        );

        FirebaseUser currentUser =
                FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(
                    this,
                    "Bạn cần đăng nhập để sử dụng Chat",
                    Toast.LENGTH_LONG
            ).show();

            finish();
            return;
        }

        currentUserId = currentUser.getUid();

        chatService = new ChatService();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupActions();
    }

    private void initViews() {
        rvConversations = findViewById(
                R.id.rvConversations
        );

        tvEmpty = findViewById(
                R.id.tvConversationsEmpty
        );

        progressBar = findViewById(
                R.id.progressConversations
        );

        btnNewConversation = findViewById(
                R.id.btnNewConversation
        );

        MaterialToolbar toolbar = findViewById(
                R.id.toolbarConversations
        );

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        rvConversations.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new ConversationAdapter(
                currentUserId,
                this::openConversation
        );

        rvConversations.setAdapter(adapter);
    }

    private void setupActions() {
        btnNewConversation.setOnClickListener(
                v -> showNewConversationDialog()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (currentUserId != null && chatService != null) {
            loadConversations();
        }
    }

    // Lay danh sach conversation tu Firestore
    private void loadConversations() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);

        chatService.getConversations(currentUserId)
                .addOnSuccessListener(conversations -> {

                    progressBar.setVisibility(View.GONE);

                    adapter.setConversations(conversations);

                    tvEmpty.setVisibility(
                            conversations.isEmpty()
                                    ? View.VISIBLE
                                    : View.GONE
                    );

                    loadUserNames(conversations);

                })
                .addOnFailureListener(e -> {

                    progressBar.setVisibility(View.GONE);

                    Toast.makeText(
                            this,
                            "Không tải được cuộc trò chuyện: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // Lay ten hien thi cua nguoi con lai
    private void loadUserNames(
            List<Conversation> conversations) {

        for (Conversation conversation : conversations) {

            String otherUserId;

            if (currentUserId.equals(
                    conversation.getUserId1())) {

                otherUserId = conversation.getUserId2();

            } else {

                otherUserId = conversation.getUserId1();
            }

            if (otherUserId == null
                    || userNames.containsKey(otherUserId)) {
                continue;
            }

            db.collection("users")
                    .document(otherUserId)
                    .get()
                    .addOnSuccessListener(doc -> {

                        String name = doc.getString("fullName");

                        if (name != null && !name.isEmpty()) {
                            userNames.put(otherUserId, name);
                            adapter.setUserNames(userNames);
                        }
                    });
        }
    }

    // Mo khung chat
    private void openConversation(
            Conversation conversation,
            String otherUserId) {

        Intent intent = new Intent(
                this,
                ChatActivity.class
        );

        intent.putExtra(
                ChatActivity.EXTRA_CONVERSATION_ID,
                conversation.getConversationId()
        );

        intent.putExtra(
                ChatActivity.EXTRA_OTHER_USER_ID,
                otherUserId
        );

        startActivity(intent);
    }

    // Tao conversation moi bang Firebase UID
    private void showNewConversationDialog() {
        EditText input = new EditText(this);

        input.setHint("Nhập Firebase UID người nhận");
        input.setSingleLine(true);

        new AlertDialog.Builder(this)
                .setTitle("Tạo cuộc trò chuyện")
                .setView(input)
                .setNegativeButton("Hủy", null)
                .setPositiveButton("Tạo", (dialog, which) -> {

                    String otherUserId =
                            input.getText().toString().trim();

                    if (otherUserId.isEmpty()) {
                        showToast("Vui lòng nhập UID");
                        return;
                    }

                    if (otherUserId.equals(currentUserId)) {
                        showToast("Không thể chat với chính mình");
                        return;
                    }

                    // Kiem tra nguoi nhan co ton tai
                    db.collection("users")
                            .document(otherUserId)
                            .get()
                            .addOnSuccessListener(doc -> {

                                if (!doc.exists()) {
                                    showToast("Không tìm thấy người dùng");
                                    return;
                                }

                                createConversation(otherUserId);

                            })
                            .addOnFailureListener(e ->
                                    showToast(
                                            "Không kiểm tra được người dùng: "
                                                    + e.getMessage()
                                    )
                            );
                })
                .show();
    }

    private void createConversation(String otherUserId) {
        chatService.createConversation(
                        currentUserId,
                        otherUserId
                )
                .addOnSuccessListener(conversation ->
                        openConversation(
                                conversation,
                                otherUserId
                        )
                )
                .addOnFailureListener(e ->
                        showToast(
                                "Không tạo được Chat: "
                                        + e.getMessage()
                        )
                );
    }

    private void showToast(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
