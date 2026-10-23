package vn.edu.ueh.melobi_nhom5;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import vn.edu.ueh.melobi_nhom5.adapter.FollowUserAdapter;
import vn.edu.ueh.melobi_nhom5.adapter.NotificationAdapter;
import vn.edu.ueh.melobi_nhom5.adapter.QuestionAdapter;
import vn.edu.ueh.melobi_nhom5.entity.Notification;
import vn.edu.ueh.melobi_nhom5.model.Question;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.service.FollowService;
import vn.edu.ueh.melobi_nhom5.service.NotificationService;
import vn.edu.ueh.melobi_nhom5.service.QuestionService;
import vn.edu.ueh.melobi_nhom5.service.UserService;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;
import vn.edu.ueh.melobi_nhom5.util.NetworkUtils;
import vn.edu.ueh.melobi_nhom5.util.SampleDataInitializer;
import vn.edu.ueh.melobi_nhom5.util.SyncManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    public static final String EXTRA_OPEN_TAB = "extra_open_tab";
    public static final String TAB_HOME = "home";
    public static final String TAB_SEARCH = "search";
    public static final String TAB_NOTIFICATIONS = "notifications";
    private static final String STATE_KEYWORD = "state_keyword";
    private static final String STATE_CATEGORY = "state_category";
    private static final String STATE_SEARCH_MODE = "state_search_mode";
    private static final String STATE_TAB = "state_tab";

    private User currentUser;
    private UserService userService;
    private QuestionService questionService;
    private FollowService followService;
    private NotificationService notificationService;
    private SyncManager syncManager;
    private QuestionAdapter questionAdapter;
    private FollowUserAdapter suggestionAdapter;
    private FollowUserAdapter searchUserAdapter;
    private NotificationAdapter notificationAdapter;
    private OnBackPressedCallback backPressedCallback;

    private MaterialToolbar toolbarMain;
    private RecyclerView rvQuestions;
    private ProgressBar pbMainLoading;
    private TextView tvMainEmpty;

    private final List<Question> allQuestions = new ArrayList<>();
    private String currentCategory = "Tất cả";
    private String currentKeyword = "";
    private boolean searchMode;
    private EditText etSearchQuestion;
    private ImageButton btnSearch;
    private RecyclerView rvSuggestedUsers;
    private RecyclerView rvSearchUsers;
    private TextView tvSuggestionTitle;
    private TextView tvSearchUserTitle;
    private View headerHome;
    private View headerSearch;
    private View panelFeed;
    private View panelNotifications;
    private View fabAskQuestion;
    private RecyclerView rvNotifications;
    private TextView tvNotificationEmpty;
    private ProgressBar pbNotifications;
    private ImageButton navHome;
    private ImageButton navSearch;
    private ImageButton navChat;
    private ImageButton navNotifications;
    private ImageButton navProfile;
    private TextView tvNotificationBadge;
    private TextView tvNavAvatar;
    private String currentTab = TAB_HOME;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
        } catch (Exception e) {
            Log.e(TAG, "EdgeToEdge error: ", e);
        }
        setContentView(R.layout.activity_main);

        try {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        } catch (Exception e) {
            Log.e(TAG, "WindowInsets error: ", e);
        }

        if (!AuthHelper.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        currentUser = AuthHelper.getCurrentUser();
        if (currentUser == null) {
            navigateToLogin();
            return;
        }
        userService = new UserService();
        questionService = new QuestionService();
        followService = new FollowService();
        notificationService = new NotificationService();
        syncManager = SyncManager.getInstance(this);

        restoreSearchState(savedInstanceState);
        initViews();
        setupToolbar();
        setupBackNavigation();
        setupRecyclerView();
        setupCategoryChips();
        setupSearch();
        setupBottomNav();
        SampleDataInitializer.initializeSampleMajors(null);
        loadCurrentProfile();

        loadQuestions();
        loadSuggestions();
        applyOpenTab(getIntent());
        if (searchMode && TAB_SEARCH.equals(currentTab)) {
            applySearchModeUi();
            applyFilters();
            searchUsers(currentKeyword);
        }
        refreshNotificationBadge();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyOpenTab(intent);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_KEYWORD, currentKeyword);
        outState.putString(STATE_CATEGORY, currentCategory);
        outState.putBoolean(STATE_SEARCH_MODE, searchMode);
        outState.putString(STATE_TAB, currentTab);
    }

    private void restoreSearchState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        String keyword = savedInstanceState.getString(STATE_KEYWORD);
        if (keyword != null) {
            currentKeyword = keyword;
        }
        String category = savedInstanceState.getString(STATE_CATEGORY);
        if (category != null && !category.trim().isEmpty()) {
            currentCategory = category;
        }
        searchMode = savedInstanceState.getBoolean(STATE_SEARCH_MODE, false);
        String tab = savedInstanceState.getString(STATE_TAB);
        if (tab != null && !tab.trim().isEmpty()) {
            currentTab = tab;
        }
    }

    private void initViews() {
        toolbarMain = findViewById(R.id.toolbarMain);
        rvQuestions = findViewById(R.id.rvQuestions);
        pbMainLoading = findViewById(R.id.pbMainLoading);
        tvMainEmpty = findViewById(R.id.tvMainEmpty);
        etSearchQuestion = findViewById(R.id.etSearchQuestion);
        btnSearch = findViewById(R.id.btnSearch);
        tvSearchUserTitle = findViewById(R.id.tvSearchUserTitle);
        rvSearchUsers = findViewById(R.id.rvSearchUsers);
        tvSuggestionTitle = findViewById(R.id.tvSuggestionTitle);
        rvSuggestedUsers = findViewById(R.id.rvSuggestedUsers);
        headerHome = findViewById(R.id.headerHome);
        headerSearch = findViewById(R.id.headerSearch);
        panelFeed = findViewById(R.id.panelFeed);
        panelNotifications = findViewById(R.id.panelNotifications);
        rvNotifications = findViewById(R.id.rvNotifications);
        tvNotificationEmpty = findViewById(R.id.tvNotificationEmpty);
        pbNotifications = findViewById(R.id.pbNotifications);
        navHome = findViewById(R.id.navHome);
        navSearch = findViewById(R.id.navSearch);
        navChat = findViewById(R.id.navChat);
        navNotifications = findViewById(R.id.navNotifications);
        navProfile = findViewById(R.id.navProfile);
        tvNotificationBadge = findViewById(R.id.tvNotificationBadge);
        tvNavAvatar = findViewById(R.id.tvNavAvatar);
        fabAskQuestion = findViewById(R.id.fabAskQuestion);
        if (fabAskQuestion != null) {
            fabAskQuestion.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, CreateQuestionActivity.class)));
        }
        bindNavAvatar();
        if (etSearchQuestion != null && currentKeyword != null && !currentKeyword.isEmpty()) {
            etSearchQuestion.setText(currentKeyword);
        }
    }

    private void setupToolbar() {
        toolbarMain.setTitle(R.string.app_name);
        toolbarMain.setNavigationIcon(R.drawable.ic_more_vert);
        toolbarMain.setNavigationIconTint(getResources().getColor(R.color.white, getTheme()));
        toolbarMain.setNavigationOnClickListener(this::showOverflowMenu);
        applySearchModeUi();
    }

    private void setupBackNavigation() {
        backPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (!TAB_HOME.equals(currentTab)) {
                    selectTab(TAB_HOME);
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
        updateBackCallback();
    }

    private void updateBackCallback() {
        if (backPressedCallback != null) {
            backPressedCallback.setEnabled(!TAB_HOME.equals(currentTab));
        }
    }

    private void applySearchModeUi() {
        if (headerHome != null) {
            headerHome.setVisibility(TAB_SEARCH.equals(currentTab) ? View.GONE : View.VISIBLE);
        }
        if (headerSearch != null) {
            headerSearch.setVisibility(TAB_SEARCH.equals(currentTab) ? View.VISIBLE : View.GONE);
        }
        updateBackCallback();
    }

    private void leaveSearchMode() {
        searchMode = false;
        currentKeyword = "";
        if (etSearchQuestion != null) {
            etSearchQuestion.setText("");
        }
        applySearchModeUi();
        hideUserSearchResults();
        filterQuestions(currentCategory, "");
    }

    private void loadCurrentProfile() {
        String userId = currentUser.getUserId();
        userService.getProfile(userId, new UserService.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                if (result != null && User.STATUS_LOCKED.equalsIgnoreCase(result.getStatus())) {
                    userService.logout();
                    Toast.makeText(MainActivity.this, "Tài khoản này đã bị khóa. Vui lòng liên hệ quản trị viên.", Toast.LENGTH_SHORT).show();
                    navigateToLogin();
                    return;
                }
                if (result != null) {
                    currentUser = result;
                    bindNavAvatar();
                    loadSuggestions();
                }
                applyAdminMenuVisibility();
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Lỗi tải hồ sơ: ", e);
            }
        });
    }

    private void applyAdminMenuVisibility() {
        if (toolbarMain.getMenu().findItem(R.id.action_admin) != null) {
            toolbarMain.getMenu().findItem(R.id.action_admin).setVisible(isCurrentUserAdmin());
        }
        if (toolbarMain.getMenu().findItem(R.id.action_init_sample_data) != null) {
            toolbarMain.getMenu().findItem(R.id.action_init_sample_data).setVisible(isCurrentUserAdmin());
        }
    }

    private boolean isCurrentUserAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    private void setupRecyclerView() {
        questionAdapter = new QuestionAdapter(question -> {
            Intent intent = new Intent(MainActivity.this, QuestionDetailActivity.class);
            intent.putExtra(QuestionDetailActivity.EXTRA_QUESTION, question);
            startActivity(intent);
        }, question -> ProfileActivity.openProfile(
                MainActivity.this,
                question.getUserId(),
                question.getUserName()
        ));

        suggestionAdapter = new FollowUserAdapter(
                currentUser.getId(),
                this::toggleSuggestionFollow,
                user -> ProfileActivity.openProfile(this, user.getUserId(), user.getDisplayName())
        );
        suggestionAdapter.setCompact(true);
        suggestionAdapter.setCurrentMajorId(currentUser.getMajorId());
        if (rvSuggestedUsers != null) {
            rvSuggestedUsers.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
            rvSuggestedUsers.setNestedScrollingEnabled(false);
            rvSuggestedUsers.setAdapter(suggestionAdapter);
        }

        searchUserAdapter = new FollowUserAdapter(
                currentUser.getId(),
                this::toggleSuggestionFollow,
                user -> ProfileActivity.openProfile(this, user.getUserId(), user.getDisplayName())
        );
        if (rvSearchUsers != null) {
            rvSearchUsers.setLayoutManager(new LinearLayoutManager(this));
            rvSearchUsers.setNestedScrollingEnabled(false);
            rvSearchUsers.setAdapter(searchUserAdapter);
        }

        if (rvQuestions != null) {
            rvQuestions.setLayoutManager(new LinearLayoutManager(this) {
                @Override
                public boolean canScrollVertically() {
                    return false;
                }
            });
            rvQuestions.setNestedScrollingEnabled(false);
            rvQuestions.setHasFixedSize(false);
            rvQuestions.setAdapter(questionAdapter);
        }

        notificationAdapter = new NotificationAdapter(this::openNotification);
        if (rvNotifications != null) {
            rvNotifications.setLayoutManager(new LinearLayoutManager(this));
            rvNotifications.setAdapter(notificationAdapter);
        }
    }

    private void setupSearch() {
        if (etSearchQuestion == null) {
            return;
        }
        etSearchQuestion.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                runSearch();
                return true;
            }
            return false;
        });
        if (btnSearch != null) {
            btnSearch.setOnClickListener(v -> runSearch());
        }
    }

    private void runSearch() {
        currentKeyword = etSearchQuestion.getText() != null
                ? etSearchQuestion.getText().toString().trim()
                : "";
        searchMode = true;
        applySearchModeUi();
        applyFilters();
        searchUsers(currentKeyword);
    }

    private void setupCategoryChips() {
        ChipGroup chipGroup = findViewById(R.id.chipGroupCategory);
        if (chipGroup != null) {
            restoreCheckedChip(chipGroup);
            chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.chipAll) {
                    currentCategory = "Tất cả";
                } else if (checkedId == R.id.chipIT) {
                    currentCategory = "Công nghệ thông tin";
                } else if (checkedId == R.id.chipEcon) {
                    currentCategory = "Kinh tế";
                } else if (checkedId == R.id.chipMarketing) {
                    currentCategory = "Marketing";
                } else if (checkedId == R.id.chipOther) {
                    currentCategory = "Khác";
                }
                applyFilters();
            });
        }
    }

    private void restoreCheckedChip(ChipGroup chipGroup) {
        int chipId = R.id.chipAll;
        if ("Công nghệ thông tin".equals(currentCategory)) {
            chipId = R.id.chipIT;
        } else if ("Kinh tế".equals(currentCategory)) {
            chipId = R.id.chipEcon;
        } else if ("Marketing".equals(currentCategory)) {
            chipId = R.id.chipMarketing;
        } else if ("Khác".equals(currentCategory)) {
            chipId = R.id.chipOther;
        }
        chipGroup.check(chipId);
    }

    private void applyFilters() {
        if (currentKeyword == null || currentKeyword.trim().isEmpty()) {
            hideUserSearchResults();
            filterQuestions(currentCategory, "");
            return;
        }
        questionService.searchQuestions(currentKeyword)
                .addOnSuccessListener(results -> {
                    List<Question> filtered = new ArrayList<>();
                    for (Question q : results) {
                        if (matchesCategory(q, currentCategory)) {
                            filtered.add(q);
                        }
                    }
                    showQuestionResults(filtered);
                })
                .addOnFailureListener(e -> filterQuestions(currentCategory, currentKeyword));
    }

    private void searchUsers(String keyword) {
        if (searchUserAdapter == null) {
            return;
        }
        if (keyword == null || keyword.trim().isEmpty()) {
            hideUserSearchResults();
            return;
        }
        String kw = keyword.trim().toLowerCase(Locale.ROOT);
        String selfId = currentUser != null ? currentUser.getUserId() : null;
        FirebaseFirestore.getInstance().collection("users")
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<User> matched = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user == null) {
                            continue;
                        }
                        if (user.getUserId() == null || user.getUserId().trim().isEmpty()) {
                            user.setUserId(doc.getId());
                        }
                        if (selfId != null && selfId.equals(user.getUserId())) {
                            continue;
                        }
                        if (User.STATUS_LOCKED.equalsIgnoreCase(user.getStatus())) {
                            continue;
                        }
                        if (containsIgnoreCase(user.getFullName(), kw)
                                || containsIgnoreCase(user.getDisplayName(), kw)
                                || containsIgnoreCase(user.getEmail(), kw)
                                || containsIgnoreCase(user.getUserId(), kw)
                                || containsIgnoreCase(user.getMajorId(), kw)) {
                            matched.add(user);
                        }
                    }
                    if (matched.size() > 5) {
                        matched = new ArrayList<>(matched.subList(0, 5));
                    }
                    showUserSearchResults(matched);
                })
                .addOnFailureListener(e -> hideUserSearchResults());
    }

    private void showUserSearchResults(List<User> users) {
        if (searchUserAdapter == null || rvSearchUsers == null || tvSearchUserTitle == null) {
            return;
        }
        searchUserAdapter.setUsers(users);
        boolean empty = users == null || users.isEmpty();
        tvSearchUserTitle.setVisibility(View.VISIBLE);
        rvSearchUsers.setVisibility(empty ? View.GONE : View.VISIBLE);
        if (tvSuggestionTitle != null) {
            tvSuggestionTitle.setVisibility(View.GONE);
        }
        if (rvSuggestedUsers != null) {
            rvSuggestedUsers.setVisibility(View.GONE);
        }
    }

    private void hideUserSearchResults() {
        if (tvSearchUserTitle != null) {
            tvSearchUserTitle.setVisibility(View.GONE);
        }
        if (rvSearchUsers != null) {
            rvSearchUsers.setVisibility(View.GONE);
        }
        if (searchUserAdapter != null) {
            searchUserAdapter.setUsers(null);
        }
        boolean hasSuggestions = suggestionAdapter != null && suggestionAdapter.getItemCount() > 0;
        if (tvSuggestionTitle != null) {
            tvSuggestionTitle.setVisibility(hasSuggestions ? View.VISIBLE : View.GONE);
        }
        if (rvSuggestedUsers != null) {
            rvSuggestedUsers.setVisibility(hasSuggestions ? View.VISIBLE : View.GONE);
        }
    }

    private void filterQuestions(String category, String keyword) {
        String kw = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        List<Question> filtered = new ArrayList<>();
        for (Question q : allQuestions) {
            boolean matchKeyword = kw.isEmpty()
                    || containsIgnoreCase(q.getTitle(), kw)
                    || containsIgnoreCase(q.getContent(), kw)
                    || containsIgnoreCase(q.getCategory(), kw)
                    || containsIgnoreCase(q.getMajorId(), kw);
            if (matchesCategory(q, category) && matchKeyword) {
                filtered.add(q);
            }
        }
        showQuestionResults(filtered);
    }

    private void showQuestionResults(List<Question> filtered) {
        boolean empty = filtered == null || filtered.isEmpty();
        boolean hasHeader = (suggestionAdapter != null && suggestionAdapter.getItemCount() > 0)
                || (searchUserAdapter != null && searchUserAdapter.getItemCount() > 0);
        tvMainEmpty.setVisibility(empty && !hasHeader ? View.VISIBLE : View.GONE);
        questionAdapter.setQuestions(filtered);
    }

    private boolean matchesCategory(Question question, String category) {
        if (category == null || "Tất cả".equals(category)) {
            return true;
        }
        if (containsIgnoreCase(question.getCategory(), category.toLowerCase(Locale.ROOT))
                || category.equalsIgnoreCase(question.getMajorId())) {
            return true;
        }
        String majorId = question.getMajorId() != null ? question.getMajorId().toLowerCase(Locale.ROOT) : "";
        if ("Công nghệ thông tin".equals(category) && (majorId.equals("it") || majorId.contains("cntt"))) {
            return true;
        }
        if ("Kinh tế".equals(category) && (majorId.equals("econ") || majorId.contains("kinh"))) {
            return true;
        }
        if ("Marketing".equals(category) && majorId.contains("market")) {
            return true;
        }
        return "Khác".equals(category) && (majorId.equals("other") || majorId.equals("khac"));
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private void loadSuggestions() {
        if (suggestionAdapter == null || currentUser == null) {
            return;
        }
        String majorId = currentUser.getMajorId();
        suggestionAdapter.setCurrentMajorId(majorId);
        followService.suggestUsers(majorId, new FollowService.Callback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                List<User> users = result != null ? result : new ArrayList<>();
                suggestionAdapter.setUsers(users);
                boolean searching = searchMode && currentKeyword != null && !currentKeyword.trim().isEmpty();
                int visibility = users.isEmpty() || searching ? View.GONE : View.VISIBLE;
                if (tvSuggestionTitle != null) {
                    tvSuggestionTitle.setVisibility(visibility);
                }
                if (rvSuggestedUsers != null) {
                    rvSuggestedUsers.setVisibility(visibility);
                }
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "Lỗi tải gợi ý người dùng: ", e);
            }
        });
    }

    private void toggleSuggestionFollow(User user) {
        String currentUserId = AuthHelper.getCurrentUserId();
        if (currentUserId == null || user == null) {
            return;
        }
        followService.isFollowing(currentUserId, user.getUserId(), FollowService.TARGET_USER, new FollowService.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean following) {
                FollowService.Callback<Boolean> after = new FollowService.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        loadSuggestions();
                        if (searchMode && currentKeyword != null && !currentKeyword.trim().isEmpty()) {
                            searchUsers(currentKeyword);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                };
                if (Boolean.TRUE.equals(following)) {
                    followService.unfollowUser(currentUserId, user.getUserId(), after);
                } else {
                    followService.followUser(currentUserId, user.getUserId(), after);
                }
            }

            @Override
            public void onError(Exception e) {
                followService.followUser(currentUserId, user.getUserId(), new FollowService.Callback<Boolean>() {
                    @Override
                    public void onSuccess(Boolean result) {
                        loadSuggestions();
                    }

                    @Override
                    public void onError(Exception ex) {
                        Toast.makeText(MainActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void loadQuestions() {
        if (!NetworkUtils.isOnline()) {
            showCachedQuestions();
            return;
        }

        pbMainLoading.setVisibility(View.VISIBLE);
        tvMainEmpty.setVisibility(View.GONE);

        FirebaseFirestore.getInstance().collection("questions")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    pbMainLoading.setVisibility(View.GONE);
                    List<Question> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Question q = QuestionService.toQuestion(doc);
                        if (q != null) {
                            list.add(q);
                        }
                    }

                    if (list.isEmpty()) {
                        initSampleData();
                        return;
                    }

                    bindQuestionList(list);
                    syncManager.cacheQuestions(list);
                })
                .addOnFailureListener(e -> {
                    pbMainLoading.setVisibility(View.GONE);
                    Log.e(TAG, "Lỗi tải câu hỏi: ", e);
                    showCachedQuestions();
                });
    }

    private void showCachedQuestions() {
        List<Question> cached = syncManager.getViewedQuestionsLastDay();
        pbMainLoading.setVisibility(View.GONE);
        if (cached == null || cached.isEmpty()) {
            tvMainEmpty.setVisibility(View.VISIBLE);
            Toast.makeText(this, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
            return;
        }
        Toast.makeText(this, R.string.message_offline_cached, Toast.LENGTH_SHORT).show();
        bindQuestionList(cached);
    }

    private void bindQuestionList(List<Question> list) {
        allQuestions.clear();
        allQuestions.addAll(list);
        Collections.sort(allQuestions, (q1, q2) -> Long.compare(q2.getCreatedAt(), q1.getCreatedAt()));
        applyFilters();
    }

    private void initSampleData() {
        pbMainLoading.setVisibility(View.VISIBLE);
        SampleDataInitializer.initializeSampleQuestions(new SampleDataInitializer.InitCallback() {
            @Override
            public void onSuccess(String message) {
                pbMainLoading.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                loadQuestions();
            }

            @Override
            public void onError(Exception e) {
                pbMainLoading.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, "Lỗi khởi tạo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotificationBadge();
        if (rvQuestions == null || questionAdapter == null) {
            return;
        }
        if (TAB_HOME.equals(currentTab) || TAB_SEARCH.equals(currentTab)) {
            loadQuestions();
        }
        if (TAB_NOTIFICATIONS.equals(currentTab)) {
            loadNotifications();
        }
    }

    private void setupBottomNav() {
        if (navHome != null) {
            navHome.setOnClickListener(v -> selectTab(TAB_HOME));
        }
        if (navSearch != null) {
            navSearch.setOnClickListener(v -> selectTab(TAB_SEARCH));
        }
        if (navChat != null) {
            navChat.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, ChatListActivity.class)));
        }
        if (navNotifications != null) {
            navNotifications.setOnClickListener(v -> selectTab(TAB_NOTIFICATIONS));
        }
        if (navProfile != null) {
            navProfile.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
        }
        highlightBottomNav();
    }

    private void applyOpenTab(Intent intent) {
        String tab = intent != null ? intent.getStringExtra(EXTRA_OPEN_TAB) : null;
        if (tab == null || tab.trim().isEmpty()) {
            tab = currentTab;
        }
        selectTab(tab);
    }

    private void selectTab(String tab) {
        if (tab == null || tab.trim().isEmpty()) {
            tab = TAB_HOME;
        }
        currentTab = tab;
        boolean notifications = TAB_NOTIFICATIONS.equals(tab);
        boolean search = TAB_SEARCH.equals(tab);
        if (panelFeed != null) {
            panelFeed.setVisibility(notifications ? View.GONE : View.VISIBLE);
        }
        if (panelNotifications != null) {
            panelNotifications.setVisibility(notifications ? View.VISIBLE : View.GONE);
        }
        if (fabAskQuestion != null) {
            fabAskQuestion.setVisibility(TAB_HOME.equals(tab) ? View.VISIBLE : View.GONE);
        }
        if (search) {
            searchMode = true;
            applySearchModeUi();
            if (etSearchQuestion != null) {
                etSearchQuestion.requestFocus();
            }
            if (currentKeyword != null && !currentKeyword.trim().isEmpty()) {
                applyFilters();
                searchUsers(currentKeyword);
            }
        } else if (TAB_HOME.equals(tab)) {
            leaveSearchMode();
        } else {
            searchMode = false;
            applySearchModeUi();
        }
        if (notifications) {
            loadNotifications();
        }
        highlightBottomNav();
        updateBackCallback();
    }

    private void highlightBottomNav() {
        int selected = getResources().getColor(R.color.melobi_primary, getTheme());
        int idle = getResources().getColor(R.color.text_tertiary, getTheme());
        if (navHome != null) {
            navHome.setColorFilter(TAB_HOME.equals(currentTab) ? selected : idle);
        }
        if (navSearch != null) {
            navSearch.setColorFilter(TAB_SEARCH.equals(currentTab) ? selected : idle);
        }
        if (navChat != null) {
            navChat.setColorFilter(idle);
        }
        if (navNotifications != null) {
            navNotifications.setColorFilter(TAB_NOTIFICATIONS.equals(currentTab) ? selected : idle);
        }
        if (navProfile != null) {
            navProfile.setColorFilter(idle);
        }
    }

    private void bindNavAvatar() {
        if (tvNavAvatar == null || navProfile == null) {
            return;
        }
        String name = currentUser != null ? currentUser.getDisplayName() : null;
        if (name != null && !name.trim().isEmpty()) {
            tvNavAvatar.setText(name.substring(0, 1).toUpperCase(Locale.getDefault()));
            tvNavAvatar.setVisibility(View.VISIBLE);
            navProfile.setImageDrawable(null);
            tvNavAvatar.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class)));
        } else {
            tvNavAvatar.setVisibility(View.GONE);
            navProfile.setImageResource(R.drawable.ic_person);
        }
    }

    private void showOverflowMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(this, anchor);
        popupMenu.getMenuInflater().inflate(R.menu.menu_overflow, popupMenu.getMenu());
        MenuItem adminItem = popupMenu.getMenu().findItem(R.id.action_admin);
        MenuItem sampleItem = popupMenu.getMenu().findItem(R.id.action_init_sample_data);
        if (adminItem != null) {
            adminItem.setVisible(isCurrentUserAdmin());
        }
        if (sampleItem != null) {
            sampleItem.setVisible(isCurrentUserAdmin());
        }
        popupMenu.setOnMenuItemClickListener(this::onOverflowItemSelected);
        popupMenu.show();
    }

    private boolean onOverflowItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_profile) {
            startActivity(new Intent(this, ProfileActivity.class));
            return true;
        }
        if (itemId == R.id.action_help) {
            showHelpDialog();
            return true;
        }
        if (itemId == R.id.action_settings_privacy) {
            showSettingsDialog();
            return true;
        }
        if (itemId == R.id.action_policies) {
            showPoliciesDialog();
            return true;
        }
        if (itemId == R.id.action_init_sample_data || itemId == R.id.action_admin) {
            if (!isCurrentUserAdmin()) {
                Toast.makeText(this, R.string.error_admin_forbidden, Toast.LENGTH_SHORT).show();
                return true;
            }
            if (itemId == R.id.action_init_sample_data) {
                initSampleData();
            }
            return true;
        }
        if (itemId == R.id.action_logout) {
            userService.logout();
            navigateToLogin();
            return true;
        }
        return false;
    }

    private void showHelpDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.title_help)
                .setMessage(R.string.message_help)
                .setPositiveButton(R.string.action_call_hotline, (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + getString(R.string.support_phone)));
                    startActivity(intent);
                })
                .setNeutralButton(R.string.action_send_email, (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getString(R.string.support_email)));
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Hỗ trợ Melobi");
                    startActivity(intent);
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showSettingsDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.title_settings_privacy)
                .setMessage(R.string.message_settings_privacy)
                .setPositiveButton(R.string.action_open_account_settings, (dialog, which) ->
                        startActivity(new Intent(this, ProfileActivity.class)))
                .setNeutralButton(R.string.action_logout, (dialog, which) -> {
                    userService.logout();
                    navigateToLogin();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showPoliciesDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.title_policies)
                .setMessage(R.string.message_policies)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void loadNotifications() {
        if (notificationService == null || currentUser == null || notificationAdapter == null) {
            return;
        }
        if (pbNotifications != null) {
            pbNotifications.setVisibility(View.VISIBLE);
        }
        notificationService.getNotifications(currentUser.getUserId())
                .addOnSuccessListener(snapshot -> {
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    if (pbNotifications != null) {
                        pbNotifications.setVisibility(View.GONE);
                    }
                    List<Notification> notifications = new ArrayList<>();
                    if (snapshot != null) {
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Notification notification = doc.toObject(Notification.class);
                            if (notification == null) {
                                continue;
                            }
                            notification.setNotificationId(doc.getId());
                            notifications.add(notification);
                        }
                    }
                    notificationAdapter.setNotifications(notifications);
                    if (tvNotificationEmpty != null) {
                        tvNotificationEmpty.setVisibility(notifications.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                    for (Notification notification : notifications) {
                        resolveActorName(notification.getActorId());
                    }
                    refreshNotificationBadge();
                })
                .addOnFailureListener(e -> {
                    if (pbNotifications != null) {
                        pbNotifications.setVisibility(View.GONE);
                    }
                    if (tvNotificationEmpty != null) {
                        tvNotificationEmpty.setVisibility(View.VISIBLE);
                    }
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void resolveActorName(String actorId) {
        if (actorId == null || actorId.trim().isEmpty()) {
            return;
        }
        userService.getProfile(actorId, new UserService.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                if (result != null && notificationAdapter != null) {
                    String name = result.getDisplayName();
                    if (name == null || name.trim().isEmpty()) {
                        name = result.getFullName();
                    }
                    if (name != null && !name.trim().isEmpty()) {
                        notificationAdapter.setActorName(actorId, name);
                    }
                }
            }

            @Override
            public void onError(Exception e) {
                // Giữ nhãn mặc định nếu không tải được tên.
            }
        });
    }

    private void openNotification(Notification notification) {
        if (notification == null) {
            return;
        }
        if (notification.getNotificationId() != null) {
            notificationService.markAsRead(notification.getNotificationId());
        }
        String type = notification.getType();
        if (Notification.TYPE_FOLLOW.equalsIgnoreCase(type)) {
            ProfileActivity.openProfile(this, notification.getActorId(), null);
            return;
        }
        String questionId = notification.getTargetId();
        if (questionId == null || questionId.trim().isEmpty()) {
            return;
        }
        questionService.getQuestionDetail(questionId)
                .addOnSuccessListener(question -> {
                    Intent intent = new Intent(MainActivity.this, QuestionDetailActivity.class);
                    if (question != null) {
                        intent.putExtra(QuestionDetailActivity.EXTRA_QUESTION, question);
                    } else {
                        intent.putExtra(QuestionDetailActivity.EXTRA_QUESTION_ID, questionId);
                    }
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    Intent intent = new Intent(MainActivity.this, QuestionDetailActivity.class);
                    intent.putExtra(QuestionDetailActivity.EXTRA_QUESTION_ID, questionId);
                    startActivity(intent);
                });
    }

    private void refreshNotificationBadge() {
        if (notificationService == null || currentUser == null || tvNotificationBadge == null) {
            return;
        }
        notificationService.getUnreadCount(currentUser.getUserId())
                .addOnSuccessListener(count -> {
                    if (isFinishing() || isDestroyed() || tvNotificationBadge == null) {
                        return;
                    }
                    if (count == null || count <= 0) {
                        tvNotificationBadge.setVisibility(View.GONE);
                        return;
                    }
                    tvNotificationBadge.setVisibility(View.VISIBLE);
                    tvNotificationBadge.setText(count > 9 ? "9+" : String.valueOf(count));
                });
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
