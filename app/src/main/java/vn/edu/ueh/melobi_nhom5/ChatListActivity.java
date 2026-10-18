package vn.edu.ueh.melobi_nhom5;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

import vn.edu.ueh.melobi_nhom5.adapter.ConversationAdapter;
import vn.edu.ueh.melobi_nhom5.entity.Conversation;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.service.ChatService;
import vn.edu.ueh.melobi_nhom5.service.UserService;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;

public class ChatListActivity extends AppCompatActivity {

    private ChatService chatService;
    private UserService userService;
    private ConversationAdapter conversationAdapter;
    private String currentUserId;

    private MaterialToolbar toolbarChatList;
    private RecyclerView rvConversations;
    private ProgressBar pbChatListLoading;
    private TextView tvChatListEmpty;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthHelper.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        setContentView(R.layout.activity_chat_list);

        currentUserId = AuthHelper.getCurrentUserId();
        if (currentUserId == null || currentUserId.trim().isEmpty()) {
            navigateToLogin();
            return;
        }

        chatService = new ChatService();
        userService = new UserService();

        toolbarChatList = findViewById(R.id.toolbarChatList);
        rvConversations = findViewById(R.id.rvConversations);
        pbChatListLoading = findViewById(R.id.pbChatListLoading);
        tvChatListEmpty = findViewById(R.id.tvChatListEmpty);

        toolbarChatList.setNavigationOnClickListener(v -> finish());
        conversationAdapter = new ConversationAdapter(currentUserId, this::openConversation);
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        rvConversations.setAdapter(conversationAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (chatService != null) {
            loadConversations();
        }
    }

    private void loadConversations() {
        pbChatListLoading.setVisibility(View.VISIBLE);
        tvChatListEmpty.setVisibility(View.GONE);

        chatService.getConversations(currentUserId)
                .addOnSuccessListener(conversations -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    pbChatListLoading.setVisibility(View.GONE);
                    List<Conversation> items = conversations;
                    if (items == null || items.isEmpty()) {
                        conversationAdapter.setConversations(null);
                        tvChatListEmpty.setVisibility(View.VISIBLE);
                        return;
                    }
                    tvChatListEmpty.setVisibility(View.GONE);
                    conversationAdapter.setConversations(items);
                    loadPartnerNames(items);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    pbChatListLoading.setVisibility(View.GONE);
                    tvChatListEmpty.setVisibility(View.VISIBLE);
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void loadPartnerNames(List<Conversation> conversations) {
        for (Conversation conversation : conversations) {
            String otherUserId = currentUserId.equals(conversation.getUser1Id())
                    ? conversation.getUser2Id()
                    : conversation.getUser1Id();
            if (otherUserId == null || otherUserId.trim().isEmpty()) {
                continue;
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
                        conversationAdapter.setPartnerName(otherUserId, name);
                    }
                }

                @Override
                public void onError(Exception e) {
                    // Giữ tên mặc định nếu không tải được hồ sơ đối phương.
                }
            });
        }
    }

    private void openConversation(Conversation conversation, String otherUserId) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.getConversationId());
        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, otherUserId);
        startActivity(intent);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
