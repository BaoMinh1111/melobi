package vn.edu.ueh.melobi_nhom5;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.adapter.FollowUserAdapter;
import vn.edu.ueh.melobi_nhom5.adapter.ProfileAnswerAdapter;
import vn.edu.ueh.melobi_nhom5.adapter.QuestionAdapter;
import vn.edu.ueh.melobi_nhom5.model.Answer;
import vn.edu.ueh.melobi_nhom5.model.Major;
import vn.edu.ueh.melobi_nhom5.model.Question;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.service.FollowService;
import vn.edu.ueh.melobi_nhom5.service.QuestionService;
import vn.edu.ueh.melobi_nhom5.service.UserService;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;
import vn.edu.ueh.melobi_nhom5.util.FirebaseStorageManager;
import vn.edu.ueh.melobi_nhom5.util.NetworkUtils;
import vn.edu.ueh.melobi_nhom5.util.SyncManager;

public class ProfileActivity extends AppCompatActivity {

    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_USER_NAME = "extra_user_name";

    public static void openProfile(Context context, String userId, String fallbackName) {
        if (context == null) {
            return;
        }
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(context, R.string.error_user_not_found, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(context, ProfileActivity.class);
        intent.putExtra(EXTRA_USER_ID, userId);
        if (fallbackName != null) {
            intent.putExtra(EXTRA_USER_NAME, fallbackName);
        }
        if (!(context instanceof android.app.Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    private MaterialToolbar toolbarProfile;
    private ImageView ivProfileAvatar;
    private TextView tvProfileInitial;
    private TextView tvAvatarStatus;
    private TextView tvFollowerCount;
    private TextView tvFollowingCount;
    private TextInputEditText etProfileFullName;
    private TextInputEditText etProfileEmail;
    private TextInputEditText etProfileBio;
    private Spinner spinnerProfileMajor;
    private Button btnChooseAvatar;
    private Button btnSaveProfile;
    private Button btnLogoutProfile;
    private Button btnFollowProfile;
    private Button btnMessageProfile;
    private View layoutPublicActions;
    private ProgressBar pbProfile;
    private RecyclerView rvProfileQuestions;
    private RecyclerView rvProfileAnswers;
    private RecyclerView rvLikedQuestions;
    private TextView tvEmptyQuestions;
    private TextView tvEmptyAnswers;
    private TextView tvEmptyLiked;
    private TextView tvLikedTitle;

    private UserService userService;
    private FollowService followService;
    private QuestionService questionService;
    private QuestionAdapter likedAdapter;
    private ListenerRegistration profileListener;
    private ListenerRegistration questionsListener;
    private QuestionAdapter questionAdapter;
    private ProfileAnswerAdapter answerAdapter;
    private final List<Major> majorList = new ArrayList<>();
    private User currentProfile;
    private String targetUserId;
    private String fallbackName;
    private boolean viewingSelf = true;
    private boolean followingTarget;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthHelper.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        setContentView(R.layout.activity_profile);

        userService = new UserService();
        followService = new FollowService();
        questionService = new QuestionService();

        String currentUserId = AuthHelper.getCurrentUserId();
        fallbackName = getIntent().getStringExtra(EXTRA_USER_NAME);
        targetUserId = getIntent().getStringExtra(EXTRA_USER_ID);
        if (targetUserId == null || targetUserId.trim().isEmpty()) {
            targetUserId = currentUserId;
        }
        viewingSelf = currentUserId != null && currentUserId.equals(targetUserId);

        initViews();
        setupActions();
        bindBottomNav();
        loadMajors();
        loadProfile();
        loadHistory();
    }

    private void initViews() {
        toolbarProfile = findViewById(R.id.toolbarProfile);
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        tvProfileInitial = findViewById(R.id.tvProfileInitial);
        tvAvatarStatus = findViewById(R.id.tvAvatarStatus);
        tvFollowerCount = findViewById(R.id.tvFollowerCount);
        tvFollowingCount = findViewById(R.id.tvFollowingCount);
        etProfileFullName = findViewById(R.id.etProfileFullName);
        etProfileEmail = findViewById(R.id.etProfileEmail);
        etProfileBio = findViewById(R.id.etProfileBio);
        spinnerProfileMajor = findViewById(R.id.spinnerProfileMajor);
        btnChooseAvatar = findViewById(R.id.btnChooseAvatar);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogoutProfile = findViewById(R.id.btnLogoutProfile);
        btnFollowProfile = findViewById(R.id.btnFollowProfile);
        btnMessageProfile = findViewById(R.id.btnMessageProfile);
        layoutPublicActions = findViewById(R.id.layoutPublicActions);
        pbProfile = findViewById(R.id.pbProfile);
        rvProfileQuestions = findViewById(R.id.rvProfileQuestions);
        rvProfileAnswers = findViewById(R.id.rvProfileAnswers);
        rvLikedQuestions = findViewById(R.id.rvLikedQuestions);
        tvEmptyQuestions = findViewById(R.id.tvEmptyQuestions);
        tvEmptyAnswers = findViewById(R.id.tvEmptyAnswers);
        tvEmptyLiked = findViewById(R.id.tvEmptyLiked);
        tvLikedTitle = findViewById(R.id.tvLikedTitle);

        questionAdapter = new QuestionAdapter(question -> {
            if (!NetworkUtils.isOnline()
                    && !vn.edu.ueh.melobi_nhom5.util.SyncManager.getInstance(this)
                    .isQuestionCachedLastDay(question.getQuestionId())) {
                Toast.makeText(this, R.string.error_offline_question_detail, Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(this, QuestionDetailActivity.class);
            intent.putExtra(QuestionDetailActivity.EXTRA_QUESTION, question);
            startActivity(intent);
        }, null);
        rvProfileQuestions.setLayoutManager(new LinearLayoutManager(this));
        rvProfileQuestions.setAdapter(questionAdapter);

        likedAdapter = new QuestionAdapter(question -> {
            if (!NetworkUtils.isOnline()
                    && !vn.edu.ueh.melobi_nhom5.util.SyncManager.getInstance(this)
                    .isQuestionCachedLastDay(question.getQuestionId())) {
                Toast.makeText(this, R.string.error_offline_question_detail, Toast.LENGTH_LONG).show();
                return;
            }
            Intent intent = new Intent(this, QuestionDetailActivity.class);
            intent.putExtra(QuestionDetailActivity.EXTRA_QUESTION, question);
            startActivity(intent);
        }, null);
        if (rvLikedQuestions != null) {
            rvLikedQuestions.setLayoutManager(new LinearLayoutManager(this));
            rvLikedQuestions.setAdapter(likedAdapter);
        }

        answerAdapter = new ProfileAnswerAdapter(answer -> {
            Intent intent = new Intent(this, QuestionDetailActivity.class);
            intent.putExtra(QuestionDetailActivity.EXTRA_QUESTION_ID, answer.getQuestionId());
            startActivity(intent);
        });
        rvProfileAnswers.setLayoutManager(new LinearLayoutManager(this));
        rvProfileAnswers.setAdapter(answerAdapter);
    }

    private void setupActions() {
        toolbarProfile.setNavigationOnClickListener(v -> finish());
        if (btnChooseAvatar != null) {
            btnChooseAvatar.setVisibility(View.GONE);
        }
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnLogoutProfile.setOnClickListener(v -> {
            userService.logout();
            navigateToLogin();
        });
        btnFollowProfile.setOnClickListener(v -> toggleFollow());
        btnMessageProfile.setOnClickListener(v -> openChat());
        tvFollowerCount.setOnClickListener(v -> showFollowList(true));
        tvFollowingCount.setOnClickListener(v -> showFollowList(false));
        applyViewMode();
    }

    private void applyViewMode() {
        int editVisibility = viewingSelf ? View.VISIBLE : View.GONE;
        if (btnChooseAvatar != null) {
            btnChooseAvatar.setVisibility(View.GONE);
        }
        btnSaveProfile.setVisibility(editVisibility);
        btnLogoutProfile.setVisibility(editVisibility);
        layoutPublicActions.setVisibility(viewingSelf ? View.GONE : View.VISIBLE);
        etProfileFullName.setEnabled(viewingSelf);
        etProfileBio.setEnabled(viewingSelf);
        spinnerProfileMajor.setEnabled(viewingSelf);
        toolbarProfile.setTitle(viewingSelf ? getString(R.string.title_profile) : getString(R.string.title_member_profile));
        int likedVisibility = viewingSelf ? View.VISIBLE : View.GONE;
        if (tvLikedTitle != null) {
            tvLikedTitle.setVisibility(likedVisibility);
        }
        if (rvLikedQuestions != null) {
            rvLikedQuestions.setVisibility(likedVisibility);
        }
        if (tvEmptyLiked != null && !viewingSelf) {
            tvEmptyLiked.setVisibility(View.GONE);
        }
    }

    private void loadMajors() {
        userService.getAllMajors(new UserService.Callback<List<Major>>() {
            @Override
            public void onSuccess(List<Major> result) {
                majorList.clear();
                if (result != null) {
                    majorList.addAll(result);
                }
                ArrayAdapter<Major> adapter = new ArrayAdapter<>(
                        ProfileActivity.this,
                        android.R.layout.simple_spinner_item,
                        majorList
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerProfileMajor.setAdapter(adapter);
                syncMajorSelection();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProfile() {
        if (targetUserId == null || targetUserId.isEmpty()) {
            navigateToLogin();
            return;
        }
        if (profileListener != null) {
            return;
        }

        setLoading(true);
        profileListener = userService.listenProfile(targetUserId, (snapshot, error) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            setLoading(false);
            if (error != null || snapshot == null || !snapshot.exists()) {
                if (currentProfile == null) {
                    currentProfile = new User(targetUserId, fallbackName != null ? fallbackName : "Thành viên", "");
                    bindProfile();
                }
                return;
            }
            if (snapshot.getMetadata().hasPendingWrites()) {
                return;
            }
            User result = UserService.fromSnapshot(snapshot);
            currentProfile = result != null
                    ? result
                    : new User(targetUserId, fallbackName != null ? fallbackName : "Thành viên", "");
            if (viewingSelf) {
                AuthHelper.setCurrentUser(currentProfile);
            }
            bindProfile();
            refreshFollowCounts();
            if (!viewingSelf) {
                refreshFollowState();
            }
        });
    }

    private void bindProfile() {
        etProfileFullName.setText(currentProfile.getFullName());
        etProfileEmail.setText(currentProfile.getEmail());
        etProfileBio.setText(currentProfile.getBio() != null ? currentProfile.getBio() : "");
        tvFollowerCount.setText(getString(R.string.label_follower_count) + ": " + currentProfile.getFollowerCount());
        tvFollowingCount.setText(getString(R.string.label_following_count) + ": " + currentProfile.getFollowingCount());
        syncMajorSelection();
        renderAvatar();
        if (!viewingSelf) {
            updateFollowButton();
        }
    }

    private void refreshFollowState() {
        String currentUserId = AuthHelper.getCurrentUserId();
        followService.isFollowing(currentUserId, targetUserId, FollowService.TARGET_USER, new FollowService.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                followingTarget = Boolean.TRUE.equals(result);
                updateFollowButton();
            }

            @Override
            public void onError(Exception e) {
                followingTarget = followService.isFollowing(currentUserId, targetUserId);
                updateFollowButton();
            }
        });
    }

    private void updateFollowButton() {
        btnFollowProfile.setText(followingTarget ? R.string.action_following : R.string.action_follow);
    }

    private void toggleFollow() {
        if (!NetworkUtils.isOnline()) {
            Toast.makeText(this, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = AuthHelper.getCurrentUserId();
        if (currentUserId == null) {
            navigateToLogin();
            return;
        }
        setLoading(true);
        FollowService.Callback<Boolean> callback = new FollowService.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                setLoading(false);
                followingTarget = !followingTarget;
                if (currentProfile != null) {
                    int next = currentProfile.getFollowerCount() + (followingTarget ? 1 : -1);
                    currentProfile.setFollowerCount(Math.max(0, next));
                }
                bindProfile();
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        if (followingTarget) {
            followService.unfollowUser(currentUserId, targetUserId, callback);
        } else {
            followService.followUser(currentUserId, targetUserId, callback);
        }
    }

    private void openChat() {
        if (!NetworkUtils.isOnline()) {
            Toast.makeText(this, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
            return;
        }
        String currentUserId = AuthHelper.getCurrentUserId();
        if (currentUserId != null && currentUserId.equals(targetUserId)) {
            Toast.makeText(this, R.string.error_chat_self, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(ChatActivity.EXTRA_OTHER_USER_ID, targetUserId);
        String name = etProfileFullName.getText() != null ? etProfileFullName.getText().toString() : fallbackName;
        if (name != null && !name.trim().isEmpty()) {
            intent.putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, name);
        }
        startActivity(intent);
    }

    private void loadHistory() {
        if (!NetworkUtils.isOnline()) {
            List<Question> viewed = SyncManager.getInstance(this).getViewedQuestionsLastDay();
            List<Question> mine = new ArrayList<>();
            for (Question question : viewed) {
                if (targetUserId != null && targetUserId.equals(question.getUserId())) {
                    mine.add(question);
                }
            }
            tvEmptyQuestions.setVisibility(mine.isEmpty() ? View.VISIBLE : View.GONE);
            questionAdapter.setQuestions(mine);
            tvEmptyAnswers.setVisibility(View.VISIBLE);
            if (tvEmptyLiked != null) {
                tvEmptyLiked.setVisibility(viewingSelf ? View.VISIBLE : View.GONE);
            }
            if (likedAdapter != null) {
                likedAdapter.setQuestions(new ArrayList<>());
            }
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (questionsListener == null) {
            questionsListener = db.collection("questions")
                    .whereEqualTo("userId", targetUserId)
                    .addSnapshotListener(com.google.firebase.firestore.MetadataChanges.INCLUDE, (snapshot, error) -> {
                        if (isFinishing() || isDestroyed()) {
                            return;
                        }
                        if (error != null || snapshot == null) {
                            tvEmptyQuestions.setVisibility(View.VISIBLE);
                            return;
                        }
                        List<Question> questions = new ArrayList<>();
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            Question question = mapQuestion(doc);
                            if (question != null) {
                                questions.add(question);
                            }
                        }
                        Collections.sort(questions, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                        tvEmptyQuestions.setVisibility(questions.isEmpty() ? View.VISIBLE : View.GONE);
                        questionAdapter.setQuestions(questions);
                    });
        }
        loadLikedQuestions();

        db.collection("answers")
                .whereEqualTo("userId", targetUserId)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<Answer> answers = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Answer answer = doc.toObject(Answer.class);
                        if (answer != null) {
                            if (answer.getId() == null || answer.getId().isEmpty()) {
                                answer.setId(doc.getId());
                            }
                            if (!answer.isHidden()) {
                                answers.add(answer);
                            }
                        }
                    }
                    Collections.sort(answers, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                    tvEmptyAnswers.setVisibility(answers.isEmpty() ? View.VISIBLE : View.GONE);
                    answerAdapter.setAnswers(answers);
                })
                .addOnFailureListener(e -> tvEmptyAnswers.setVisibility(View.VISIBLE));
    }

    private Question mapQuestion(DocumentSnapshot doc) {
        Question question = vn.edu.ueh.melobi_nhom5.service.QuestionService.toQuestion(doc);
        if (question == null || !Question.STATUS_ACTIVE.equals(question.getStatus())) {
            return null;
        }
        question.setUserName(vn.edu.ueh.melobi_nhom5.util.UserNameLookup.resolve(
                question.getUserId(), question.getUserName()));
        return question;
    }

    private void loadLikedQuestions() {
        if (!viewingSelf || rvLikedQuestions == null || likedAdapter == null) {
            return;
        }
        questionService.getLikedQuestions(targetUserId)
                .addOnSuccessListener(result -> {
                    List<Question> liked = result != null ? result : new ArrayList<>();
                    likedAdapter.setQuestions(liked);
                    if (tvEmptyLiked != null) {
                        tvEmptyLiked.setVisibility(liked.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    if (tvEmptyLiked != null) {
                        tvEmptyLiked.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void refreshFollowCounts() {
        followService.getFollowUsers(targetUserId, true, new FollowService.Callback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                int count = result != null ? result.size() : 0;
                tvFollowerCount.setText(getString(R.string.label_followers, count));
            }

            @Override
            public void onError(Exception e) {
            }
        });
        followService.getFollowUsers(targetUserId, false, new FollowService.Callback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                int count = result != null ? result.size() : 0;
                tvFollowingCount.setText(getString(R.string.label_following, count));
            }

            @Override
            public void onError(Exception e) {
            }
        });
    }

    private void showFollowList(boolean followers) {
        View content = getLayoutInflater().inflate(R.layout.dialog_user_list, null, false);
        RecyclerView rv = content.findViewById(R.id.rvDialogUsers);
        TextView empty = content.findViewById(R.id.tvDialogUsersEmpty);
        ProgressBar progress = content.findViewById(R.id.pbDialogUsers);
        rv.setLayoutManager(new LinearLayoutManager(this));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(followers ? R.string.title_followers : R.string.title_following)
                .setView(content)
                .setNegativeButton(android.R.string.ok, null)
                .create();

        FollowUserAdapter adapter = new FollowUserAdapter(
                AuthHelper.getCurrentUserId(),
                null,
                user -> {
                    dialog.dismiss();
                    Intent intent = new Intent(this, ProfileActivity.class);
                    intent.putExtra(EXTRA_USER_ID, user.getUserId());
                    intent.putExtra(EXTRA_USER_NAME, user.getFullName());
                    startActivity(intent);
                });
        adapter.setHideFollowButton(true);
        rv.setAdapter(adapter);

        followService.getFollowUsers(targetUserId, followers, new FollowService.Callback<List<User>>() {
            @Override
            public void onSuccess(List<User> result) {
                progress.setVisibility(View.GONE);
                List<User> users = result != null ? result : new ArrayList<>();
                adapter.setUsers(users);
                empty.setVisibility(users.isEmpty() ? View.VISIBLE : View.GONE);
                if (followers) {
                    tvFollowerCount.setText(getString(R.string.label_followers, users.size()));
                } else {
                    tvFollowingCount.setText(getString(R.string.label_following, users.size()));
                }
            }

            @Override
            public void onError(Exception e) {
                progress.setVisibility(View.GONE);
                empty.setVisibility(View.VISIBLE);
                empty.setText(e.getMessage());
            }
        });
        dialog.show();
    }

    private void syncMajorSelection() {
        if (currentProfile == null || majorList.isEmpty()) {
            return;
        }

        String currentMajorId = currentProfile.getMajorId();
        for (int i = 0; i < majorList.size(); i++) {
            if (majorList.get(i).getMajorId().equals(currentMajorId)) {
                spinnerProfileMajor.setSelection(i);
                return;
            }
        }
    }

    private void renderAvatar() {
        String fullName = currentProfile != null ? currentProfile.getFullName() : fallbackName;
        String avatarUrl = currentProfile != null ? currentProfile.getAvatarUrl() : "";
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            tvAvatarStatus.setText("Đã có ảnh đại diện");
            loadAvatarFromUrl(avatarUrl, fullName);
            return;
        }

        showAvatarPlaceholder(fullName);
        tvAvatarStatus.setText(R.string.hint_avatar_status);
    }

    private void loadAvatarFromUrl(String avatarUrl, String fallbackName) {
        FirebaseStorageManager.loadBitmap(avatarUrl, 512, new FirebaseStorageManager.Callback<Bitmap>() {
            @Override
            public void onSuccess(Bitmap bitmap) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (bitmap != null) {
                    ivProfileAvatar.setVisibility(View.VISIBLE);
                    ivProfileAvatar.setImageBitmap(bitmap);
                    tvProfileInitial.setVisibility(View.GONE);
                } else {
                    showAvatarPlaceholder(fallbackName);
                }
            }

            @Override
            public void onError(Exception e) {
                if (!isFinishing() && !isDestroyed()) {
                    showAvatarPlaceholder(fallbackName);
                }
            }
        });
    }

    private void showAvatarPlaceholder(String fullName) {
        String initial = (fullName != null && !fullName.trim().isEmpty())
                ? fullName.trim().substring(0, 1).toUpperCase()
                : "M";
        ivProfileAvatar.setImageDrawable(null);
        ivProfileAvatar.setVisibility(View.INVISIBLE);
        tvProfileInitial.setVisibility(View.VISIBLE);
        tvProfileInitial.setText(initial);
    }

    private void saveProfile() {
        if (!viewingSelf || currentProfile == null) {
            Toast.makeText(this, "Không tìm thấy hồ sơ người dùng", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!NetworkUtils.isOnline()) {
            Toast.makeText(this, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = etProfileFullName.getText() != null ? etProfileFullName.getText().toString().trim() : "";
        String bio = etProfileBio.getText() != null ? etProfileBio.getText().toString().trim() : "";
        Major selectedMajor = (Major) spinnerProfileMajor.getSelectedItem();

        if (fullName.isEmpty()) {
            Toast.makeText(this, "Họ tên không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedMajor == null) {
            Toast.makeText(this, "Vui lòng chọn chuyên ngành", Toast.LENGTH_SHORT).show();
            return;
        }

        currentProfile.setFullName(fullName);
        currentProfile.setBio(bio);

        setLoading(true);
        persistProfile(selectedMajor.getMajorId());
    }

    private void persistProfile(String selectedMajorId) {
        userService.updateProfile(currentProfile, new UserService.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                if (!selectedMajorId.equals(result.getMajorId())) {
                    userService.updateMajor(result.getUserId(), selectedMajorId, new UserService.Callback<Void>() {
                        @Override
                        public void onSuccess(Void ignored) {
                            result.setMajorId(selectedMajorId);
                            onProfileSaved(result);
                        }

                        @Override
                        public void onError(Exception e) {
                            setLoading(false);
                            Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                onProfileSaved(result);
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onProfileSaved(User updatedUser) {
        currentProfile = updatedUser;
        AuthHelper.setCurrentUser(updatedUser);
        vn.edu.ueh.melobi_nhom5.util.UserNameLookup.put(updatedUser.getUserId(), updatedUser.getFullName());
        questionService.updateAuthorName(updatedUser.getUserId(), updatedUser.getFullName());
        setLoading(false);
        bindProfile();
        if (questionAdapter != null) {
            questionAdapter.notifyDataSetChanged();
        }
        if (likedAdapter != null) {
            likedAdapter.notifyDataSetChanged();
        }
        Toast.makeText(this, R.string.message_profile_updated, Toast.LENGTH_SHORT).show();
    }

    private void setLoading(boolean isLoading) {
        pbProfile.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        if (btnChooseAvatar != null) {
            btnChooseAvatar.setEnabled(false);
        }
        btnSaveProfile.setEnabled(!isLoading);
        btnLogoutProfile.setEnabled(!isLoading);
        btnFollowProfile.setEnabled(!isLoading);
        btnMessageProfile.setEnabled(!isLoading);
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
            navChat.setColorFilter(idle);
            navChat.setOnClickListener(v ->
                    startActivity(new Intent(this, ChatListActivity.class)));
        }
        if (navNotifications != null) {
            navNotifications.setColorFilter(idle);
            navNotifications.setOnClickListener(v -> openMainTab(MainActivity.TAB_NOTIFICATIONS));
        }
        if (navProfile != null) {
            navProfile.setColorFilter(viewingSelf ? selected : idle);
            if (!viewingSelf) {
                navProfile.setOnClickListener(v ->
                        startActivity(new Intent(this, ProfileActivity.class)));
            }
        }
    }

    private void openMainTab(String tab) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_OPEN_TAB, tab);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (targetUserId != null && !targetUserId.isEmpty()) {
            refreshFollowCounts();
            loadLikedQuestions();
        }
    }

    @Override
    protected void onDestroy() {
        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
        if (questionsListener != null) {
            questionsListener.remove();
            questionsListener = null;
        }
        super.onDestroy();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
