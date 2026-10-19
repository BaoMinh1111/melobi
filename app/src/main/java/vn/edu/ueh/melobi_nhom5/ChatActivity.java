
package vn.edu.ueh.melobi_nhom5;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.adapter.MessageAdapter;
import vn.edu.ueh.melobi_nhom5.entity.Conversation;
import vn.edu.ueh.melobi_nhom5.entity.Message;
import vn.edu.ueh.melobi_nhom5.service.ChatService;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID =
            "conversationId";

    public static final String EXTRA_OTHER_USER_ID =
            "otherUserId";

    private String currentUserId;
    private String conversationId;
    private String otherUserId;

    private ChatService chatService;
    private FirebaseFirestore db;
    private MessageAdapter adapter;

    private RecyclerView rvMessages;
    private EditText etMessage;
    private MaterialButton btnSend;
    private MaterialToolbar toolbar;

    private ListenerRegistration messageListener;

    private boolean screenActive = false;
    private boolean markingAsRead = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_chat);

        FirebaseUser user =
                FirebaseAuth.getInstance().getCurrentUser();

        if (user == null) {
            showToast("Bạn cần đăng nhập để nhắn tin");
            finish();
            return;
        }

        currentUserId = user.getUid();

        conversationId = getIntent().getStringExtra(
                EXTRA_CONVERSATION_ID
        );

        otherUserId = getIntent().getStringExtra(
                EXTRA_OTHER_USER_ID
        );

        if (conversationId == null
                || conversationId.trim().isEmpty()) {
            showToast("Không tìm thấy cuộc trò chuyện");
            finish();
            return;
        }

        chatService = new ChatService();
        db = FirebaseFirestore.getInstance();

        initViews();
        setupRecyclerView();
        setupActions();
        loadOtherUserName();
    }

    private void initViews() {
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        toolbar = findViewById(R.id.toolbarChat);

        toolbar.setTitle("Trò chuyện");
        toolbar.setNavigationOnClickListener(v -> finish());

        // Chi cho gui khi da xac nhan conversation
        btnSend.setEnabled(false);
    }

    private void setupRecyclerView() {
        LinearLayoutManager manager =
                new LinearLayoutManager(this);

        manager.setStackFromEnd(true);

        rvMessages.setLayoutManager(manager);

        adapter = new MessageAdapter(currentUserId);
        rvMessages.setAdapter(adapter);
    }

    private void setupActions() {
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void loadOtherUserName() {
        if (otherUserId == null || otherUserId.isEmpty()) {
            return;
        }

        toolbar.setTitle(otherUserId);

        db.collection("users")
                .document(otherUserId)
                .get()
                .addOnSuccessListener(doc -> {

                    String name = doc.getString("fullName");

                    if (name != null && !name.isEmpty()) {
                        toolbar.setTitle(name);
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();

        screenActive = true;

        if (db != null && conversationId != null) {
            verifyConversationAndListen();
        }
    }

    // Kiem tra nguoi dang nhap co tham gia chat
    private void verifyConversationAndListen() {
        db.collection("conversations")
                .document(conversationId)
                .get()
                .addOnSuccessListener(doc -> {

                    if (!screenActive) {
                        return;
                    }

                    Conversation conversation =
                            doc.toObject(Conversation.class);

                    if (conversation == null) {
                        showToast("Conversation không tồn tại");
                        finish();
                        return;
                    }

                    boolean isMember =
                            currentUserId.equals(
                                    conversation.getUserId1()
                            )
                                    || currentUserId.equals(
                                    conversation.getUserId2()
                            );

                    if (!isMember) {
                        showToast(
                                "Bạn không thuộc cuộc trò chuyện này"
                        );
                        finish();
                        return;
                    }

                    btnSend.setEnabled(true);
                    listenForMessages();

                })
                .addOnFailureListener(e ->
                        showToast(
                                "Không mở được Chat: "
                                        + e.getMessage()
                        )
                );
    }

    // Lang nghe tin nhan moi theo thoi gian thuc
    private void listenForMessages() {
        if (messageListener != null) {
            return;
        }

        messageListener = db
                .collection("conversations")
                .document(conversationId)
                .collection("messages")
                .orderBy(
                        "sentAt",
                        Query.Direction.ASCENDING
                )
                .limitToLast(100)
                .addSnapshotListener((snapshot, error) -> {

                    if (!screenActive) {
                        return;
                    }

                    if (error != null) {
                        showToast(
                                "Không tải được tin nhắn: "
                                        + error.getMessage()
                        );
                        return;
                    }

                    if (snapshot == null) {
                        return;
                    }

                    List<Message> messages =
                            new ArrayList<>();

                    boolean hasUnreadReceived = false;

                    for (DocumentSnapshot doc :
                            snapshot.getDocuments()) {

                        Message message =
                                doc.toObject(Message.class);

                        if (message == null) {
                            continue;
                        }

                        message.setMessageId(doc.getId());
                        messages.add(message);

                        // Chi danh dau tin do nguoi khac gui
                        if (!currentUserId.equals(
                                message.getSenderId())
                                && !message.getIsRead()) {

                            hasUnreadReceived = true;
                        }
                    }

                    adapter.setMessages(messages);

                    if (!messages.isEmpty()) {
                        rvMessages.scrollToPosition(
                                messages.size() - 1
                        );
                    }

                    if (hasUnreadReceived && !markingAsRead) {
                        markReceivedMessagesAsRead();
                    }
                });
    }

    // Cap nhat isRead tren Firestore
    private void markReceivedMessagesAsRead() {
        markingAsRead = true;

        chatService.markAsRead(
                        conversationId,
                        currentUserId
                )
                .addOnCompleteListener(task -> {

                    markingAsRead = false;

                    if (!task.isSuccessful()
                            && screenActive) {

                        showToast(
                                "Không cập nhật được đã xem: "
                                        + task.getException()
                        );
                    }
                });
    }

    // Gui tin nhan
    private void sendMessage() {
        String content =
                etMessage.getText().toString().trim();

        if (content.isEmpty()) {
            showToast("Vui lòng nhập tin nhắn");
            return;
        }

        btnSend.setEnabled(false);

        try {
            chatService.sendMessage(
                            conversationId,
                            currentUserId,
                            content
                    )
                    .addOnSuccessListener(message -> {

                        // Khong xoa noi dung moi neu da go tiep
                        if (content.equals(
                                etMessage.getText()
                                        .toString().trim())) {
                            etMessage.setText("");
                        }

                        // SnapshotListener tu cap nhat giao dien
                    })
                    .addOnFailureListener(e ->
                            showToast(
                                    "Gửi thất bại: "
                                            + e.getMessage()
                            )
                    )
                    .addOnCompleteListener(task ->
                            btnSend.setEnabled(true)
                    );

        } catch (IllegalArgumentException e) {
            btnSend.setEnabled(true);
            showToast(e.getMessage());
        }
    }

    @Override
    protected void onStop() {
        screenActive = false;

        if (messageListener != null) {
            messageListener.remove();
            messageListener = null;
        }

        super.onStop();
    }

    private void showToast(String message) {
        Toast.makeText(
                this,
                message,
                Toast.LENGTH_LONG
        ).show();
    }
}
