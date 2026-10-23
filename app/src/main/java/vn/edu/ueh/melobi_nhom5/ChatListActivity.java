package vn.edu.ueh.melobi_nhom5;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.adapter.ConversationAdapter;
import vn.edu.ueh.melobi_nhom5.adapter.FollowUserAdapter;
import vn.edu.ueh.melobi_nhom5.entity.Conversation;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.service.ChatService;
import vn.edu.ueh.melobi_nhom5.service.FollowService;
import vn.edu.ueh.melobi_nhom5.service.UserService;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;

public class ChatListActivity extends AppCompatActivity {

    private ChatService chatService;
    private UserService userService;
    private FollowService followService;
    private ConversationAdapter conversationAdapter;
    private FollowUserAdapter suggestionAdapter;
    private final List<ListenerRegistration> conversationListeners = new ArrayList<>();
    private String currentUserId;

    private MaterialToolbar toolbarChatList;
    private RecyclerView rvConversations;
    private RecyclerView rvSuggestedUsers;
    private TextView tvSuggestionTitle;
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
        followService = new FollowService();

        toolbarChatList = findViewById(R.id.toolbarChatList);
        rvConversations = findViewById(R.id.rvConversations);
        rvSuggestedUsers = findViewById(R.id.rvSuggestedUsers);
        tvSuggestionTitle = findViewById(R.id.tvSuggestionTitle);
        pbChatListLoading = findViewById(R.id.pbChatListLoading);
        tvChatListEmpty = findViewById(R.id.tvChatListEmpty);

        toolbarChatList.setNavigationOnClickListener(v -> finish());
        conversationAdapter = new ConversationAdapter(currentUserId, this::openConversation);
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        rvConversations.setAdapter(conversationAdapter);

        suggestionAdapter = new FollowUserAdapter(
                currentUserId,
                null,
                user -> ProfileActivity.openProfile(this, user.getUserId(), user.getDisplayName())
        );
        suggestionAdapter.setHideFollowButton(true);
        suggestionAdapter.setCompact(true);
        User currentUser = AuthHelper.getCurrentUser();
        suggestionAdapter.setCurrentMajorId(currentUser != null ? currentUser.getMajorId() : null);
        rvSuggestedUsers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvSuggestedUsers.setAdapter(suggestionAdapter);

        bindBottomNav();
        startConversationListeners();
        loadSuggestions();
    }

    @Override
    protected void onDestroy() {
        removeConversationListeners();
        super.onDestroy();
    }

    private void startConversationListeners() {
        removeConversationListeners();
        pbChatListLoading.setVisibility(View.VISIBLE);
        List<ListenerRegistration> registrations = chatService.listenConversations(
                currentUserId,
                this::bindConversations,
                e -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    pbChatListLoading.setVisibility(View.GONE);
                    if (conversationAdapter.getItemCount() == 0) {
                        tvChatListEmpty.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
        conversationListeners.addAll(registrations);
    }

    private void removeConversationListeners() {
        for (ListenerRegistration registration : conversationListeners) {
            registration.remove();
        }
        conversationListeners.clear();
    }

    private void bindConversations(List<Conversation> conversations) {
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

    private void loadSuggestions() {
        User currentUser = AuthHelper.getCurrentUser();
        String majorId = currentUser != null ? currentUser.getMajorId() : null;
        suggestionAdapter.setCurrentMajorId(majorId);
        followService.suggestUsers(majorId, new FollowService.Callback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                List<User> users = result != null ? result : new ArrayList<>();
                suggestionAdapter.setUsers(users);
                int visibility = users.isEmpty() ? View.GONE : View.VISIBLE;
                tvSuggestionTitle.setVisibility(visibility);
                rvSuggestedUsers.setVisibility(visibility);
            }

            @Override
            public void onError(Exception e) {
                tvSuggestionTitle.setVisibility(View.GONE);
                rvSuggestedUsers.setVisibility(View.GONE);
            }
        });
    }

    private void openConversation(Conversation conversation, String otherUserId) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.getConversationId());
        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, otherUserId);
        startActivity(intent);
    }

    private void bindBottomNav() {
        ImageButton navHome = findViewById(R.id.navHome);
        ImageButton navSearch = findViewById(R.id.navSearch);
        ImageButton navChat = findViewById(R.id.navChat);
        ImageButton navNotifications = findViewById(R.id.navNotifications);
        ImageButton navProfile = findViewById(R.id.navProfile);
        int selected = getResources().getColor(R.color.melobi_primary, getTheme());
        int idle = getResources().getColor(R.color.text_tertiary, getTheme());
        if (navHome != null) {
            navHome.setColorFilter(idle);
            navHome.setOnClickListener(v -> openMainTab(MainActivity.TAB_HOME));
        }
        if (navSearch != null) {
            navSearch.setColorFilter(idle);
            navSearch.setOnClickListener(v -> openMainTab(MainActivity.TAB_SEARCH));
        }
        if (navChat != null) {
            navChat.setColorFilter(selected);
        }
        if (navNotifications != null) {
            navNotifications.setColorFilter(idle);
            navNotifications.setOnClickListener(v -> openMainTab(MainActivity.TAB_NOTIFICATIONS));
        }
        if (navProfile != null) {
            navProfile.setColorFilter(idle);
            navProfile.setOnClickListener(v ->
                    startActivity(new Intent(this, ProfileActivity.class)));
        }
    }

    private void openMainTab(String tab) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_TAB, tab);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
