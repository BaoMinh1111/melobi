package vn.edu.ueh.melobi_nhom5;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

import vn.edu.ueh.melobi_nhom5.adapter.MessageAdapter;
import vn.edu.ueh.melobi_nhom5.entity.Conversation;
import vn.edu.ueh.melobi_nhom5.entity.Message;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.service.ChatService;
import vn.edu.ueh.melobi_nhom5.service.UserService;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_CONVERSATION_ID = "extra_conversation_id";
    public static final String EXTRA_OTHER_USER_ID = "extra_other_user_id";
    public static final String EXTRA_OTHER_USER_NAME = "extra_other_user_name";
    private static final String STATE_CONVERSATION_ID = "state_conversation_id";
    private static final String STATE_OTHER_USER_ID = "state_other_user_id";
    private static final String STATE_OTHER_USER_NAME = "state_other_user_name";
    private static final String STATE_DRAFT = "state_draft";

    private ChatService chatService;
    private UserService userService;
    private MessageAdapter messageAdapter;
    private ListenerRegistration messagesListener;
    private String currentUserId;
    private String conversationId;
    private String otherUserId;
    private String otherUserName;
    private boolean sending;

    private MaterialToolbar toolbarChat;
    private RecyclerView rvMessages;
    private ProgressBar pbChatLoading;
    private TextView tvChatEmpty;
    private EditText etMessageInput;
    private ImageButton btnSendMessage;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthHelper.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        setContentView(R.layout.activity_chat);

        currentUserId = AuthHelper.getCurrentUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            navigateToLogin();
            return;
        }

        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        otherUserId = getIntent().getStringExtra(EXTRA_OTHER_USER_ID);
        otherUserName = getIntent().getStringExtra(EXTRA_OTHER_USER_NAME);
        restoreChatState(savedInstanceState);

        if ((conversationId == null || conversationId.trim().isEmpty())
                && (otherUserId == null || otherUserId.trim().isEmpty())) {
            Toast.makeText(this, "Không tìm thấy cuộc trò chuyện", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        chatService = new ChatService();
        userService = new UserService();

        toolbarChat = findViewById(R.id.toolbarChat);
        rvMessages = findViewById(R.id.rvMessages);
        pbChatLoading = findViewById(R.id.pbChatLoading);
        tvChatEmpty = findViewById(R.id.tvChatEmpty);
        etMessageInput = findViewById(R.id.etMessageInput);
        btnSendMessage = findViewById(R.id.btnSendMessage);

        toolbarChat.setNavigationOnClickListener(v -> finish());
        if (otherUserName != null && !otherUserName.trim().isEmpty()) {
            toolbarChat.setTitle(otherUserName);
        }

        messageAdapter = new MessageAdapter(currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(messageAdapter);

        btnSendMessage.setOnClickListener(v -> sendMessage());
        if (savedInstanceState != null) {
            String draft = savedInstanceState.getString(STATE_DRAFT);
            if (draft != null) {
                etMessageInput.setText(draft);
            }
        }
        loadPartnerName();
        ensureConversationThenListen();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_CONVERSATION_ID, conversationId);
        outState.putString(STATE_OTHER_USER_ID, otherUserId);
        outState.putString(STATE_OTHER_USER_NAME, otherUserName);
        if (etMessageInput != null && etMessageInput.getText() != null) {
            outState.putString(STATE_DRAFT, etMessageInput.getText().toString());
        }
    }

    @Override
    protected void onDestroy() {
        removeMessagesListener();
        super.onDestroy();
    }

    private void restoreChatState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        if (conversationId == null || conversationId.trim().isEmpty()) {
            conversationId = savedInstanceState.getString(STATE_CONVERSATION_ID);
        }
        if (otherUserId == null || otherUserId.trim().isEmpty()) {
            otherUserId = savedInstanceState.getString(STATE_OTHER_USER_ID);
        }
        if (otherUserName == null || otherUserName.trim().isEmpty()) {
            otherUserName = savedInstanceState.getString(STATE_OTHER_USER_NAME);
        }
        String draft = savedInstanceState.getString(STATE_DRAFT);
        if (draft != null && etMessageInput != null) {
            etMessageInput.setText(draft);
        }
    }

    private void loadPartnerName() {
        if (otherUserId == null || otherUserId.trim().isEmpty()) {
            return;
        }
        userService.getProfile(otherUserId, new UserService.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                if (isFinishing() || isDestroyed() || result == null) {
                    return;
                }
                String name = result.getDisplayName();
                if (name == null || name.trim().isEmpty()) {
                    name = result.getFullName();
                }
                if (name != null && !name.trim().isEmpty()) {
                    otherUserName = name;
                    toolbarChat.setTitle(name);
                }
            }

            @Override
            public void onError(Exception e) {
                // Giữ tiêu đề mặc định nếu không tải được hồ sơ đối phương.
            }
        });
    }

    private void ensureConversationThenListen() {
        if (otherUserId != null && !otherUserId.trim().isEmpty()) {
            pbChatLoading.setVisibility(View.VISIBLE);
            chatService.createConversation(currentUserId, otherUserId)
                    .addOnSuccessListener(conversation -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        applyConversation(conversation);
                        startMessagesListener();
                    })
                    .addOnFailureListener(e -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        pbChatLoading.setVisibility(View.GONE);
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
            return;
        }

        if (conversationId != null && !conversationId.trim().isEmpty()) {
            startMessagesListener();
            return;
        }

        Toast.makeText(this, "Không tìm thấy cuộc trò chuyện", Toast.LENGTH_SHORT).show();
        finish();
    }

    private void applyConversation(Conversation conversation) {
        if (conversation == null) {
            return;
        }
        conversationId = conversation.getConversationId();
        if (otherUserId == null || otherUserId.trim().isEmpty()) {
            otherUserId = currentUserId.equals(conversation.getUser1Id())
                    ? conversation.getUser2Id()
                    : conversation.getUser1Id();
            loadPartnerName();
        }
    }

    private void startMessagesListener() {
        if (conversationId == null || conversationId.trim().isEmpty()) {
            return;
        }
        removeMessagesListener();
        pbChatLoading.setVisibility(View.VISIBLE);
        messagesListener = chatService.listenMessages(conversationId, (snapshot, error) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            pbChatLoading.setVisibility(View.GONE);
            if (error != null) {
                tvChatEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(this, error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            List<Message> messages = ChatService.mapMessages(snapshot);
            bindMessages(messages);
            chatService.markAsRead(conversationId);
        });
    }

    private void removeMessagesListener() {
        if (messagesListener != null) {
            messagesListener.remove();
            messagesListener = null;
        }
    }

    private void bindMessages(List<Message> messages) {
        boolean empty = messages == null || messages.isEmpty();
        tvChatEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        messageAdapter.setMessages(messages);
        if (!empty) {
            rvMessages.scrollToPosition(messageAdapter.getItemCount() - 1);
        }
    }

    private void sendMessage() {
        if (sending) {
            return;
        }
        if (conversationId == null || conversationId.trim().isEmpty()) {
            Toast.makeText(this, "Cuộc trò chuyện chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }

        String content = etMessageInput.getText() != null ? etMessageInput.getText().toString().trim() : "";
        if (content.isEmpty()) {
            Toast.makeText(this, R.string.error_empty_message, Toast.LENGTH_SHORT).show();
            return;
        }

        sending = true;
        btnSendMessage.setEnabled(false);
        etMessageInput.setText("");
        chatService.sendMessage(conversationId, currentUserId, content)
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    etMessageInput.setText(content);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnCompleteListener(task -> {
                    sending = false;
                    if (!isFinishing() && !isDestroyed()) {
                        btnSendMessage.setEnabled(true);
                    }
                });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
