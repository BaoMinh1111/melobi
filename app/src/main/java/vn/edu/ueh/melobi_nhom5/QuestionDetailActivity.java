package vn.edu.ueh.melobi_nhom5;

import android.content.Intent;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;

import java.io.Serializable;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.adapter.AnswerAdapter;
import vn.edu.ueh.melobi_nhom5.model.Answer;
import vn.edu.ueh.melobi_nhom5.model.Question;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.repository.AnswerRepository;
import vn.edu.ueh.melobi_nhom5.service.AnswerService;
import vn.edu.ueh.melobi_nhom5.service.QuestionService;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;
import vn.edu.ueh.melobi_nhom5.util.NetworkUtils;
import vn.edu.ueh.melobi_nhom5.util.SyncManager;
import vn.edu.ueh.melobi_nhom5.service.NotificationService;
import vn.edu.ueh.melobi_nhom5.entity.Notification;
import vn.edu.ueh.melobi_nhom5.entity.Like;

public class QuestionDetailActivity extends AppCompatActivity {

    /** Cách khuyến nghị: chỉ truyền ID, màn hình tự tải dữ liệu mới nhất từ Firestore. */
    public static final String EXTRA_QUESTION_ID = "extra_question_id";
    /** Có thể truyền cả đối tượng Question (model.Question) để hiển thị ngay trong lúc tải. */
    public static final String EXTRA_QUESTION = "extra_question";
    private static final String STATE_QUESTION_ID = "state_question_id";

    private String questionId;
    private Question question;               // có thể null cho tới khi tải xong
    private AnswerRepository answerRepository;
    private AnswerService answerService;
    private QuestionService questionService;
    private NotificationService notificationService;
    private SyncManager syncManager;
    private AnswerAdapter answerAdapter;
    private User currentUser;

    private MaterialToolbar toolbarDetail;
    private TextView tvQuestionAvatar;
    private TextView tvQuestionAuthorName;
    private TextView tvQuestionTime;
    private TextView tvQuestionCategory;
    private TextView tvQuestionTitle;
    private TextView tvQuestionContent;
    private TextView tvAnswersHeader;

    private RecyclerView rvAnswers;
    private ProgressBar pbAnswersLoading;
    private TextView tvEmptyAnswers;

    private EditText etAnswerInput;
    private ImageButton btnSendAnswer;
    private View btnQuestionLike;
    private ImageView ivQuestionLike;
    private ImageView ivQuestionImage;
    private TextView tvQuestionLikeCount;
    private boolean skipNextResumeLoad;
    private boolean questionLiked;
    private ListenerRegistration questionListener;
    private ListenerRegistration answersListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question_detail);

        questionId = getIntent().getStringExtra(EXTRA_QUESTION_ID);
        Serializable extra = getIntent().getSerializableExtra(EXTRA_QUESTION);
        if (extra instanceof Question) {          // instanceof: nếu caller truyền nhầm lớp khác thì không crash
            question = (Question) extra;
            if (questionId == null || questionId.isEmpty()) questionId = question.getQuestionId();
        }
        if (savedInstanceState != null && (questionId == null || questionId.isEmpty())) {
            questionId = savedInstanceState.getString(STATE_QUESTION_ID);
        }

        if (questionId == null || questionId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin câu hỏi", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        answerRepository = new AnswerRepository();
        answerService = new AnswerService();
        questionService = new QuestionService();
        notificationService = new NotificationService();
        syncManager = SyncManager.getInstance(this);
        currentUser = AuthHelper.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        setupRecyclerView();
        setupOwnerMenu();

        toolbarDetail.setNavigationOnClickListener(v -> finish());
        tvQuestionAuthorName.setOnClickListener(v -> openAuthorProfile());
        tvQuestionAvatar.setOnClickListener(v -> openAuthorProfile());
        btnSendAnswer.setOnClickListener(v -> submitAnswer());
        btnSendAnswer.setEnabled(false);          // chỉ cho gửi sau khi xác nhận câu hỏi còn hiển thị

        if (question != null) displayQuestionInfo();   // hiển thị tạm bản truyền sang
        skipNextResumeLoad = true;
        loadQuestion();
        loadAnswers();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (skipNextResumeLoad) {
            skipNextResumeLoad = false;
            return;
        }
        refreshQuestionLikeState();
    }

    @Override
    protected void onDestroy() {
        if (questionListener != null) {
            questionListener.remove();
            questionListener = null;
        }
        if (answersListener != null) {
            answersListener.remove();
            answersListener = null;
        }
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_QUESTION_ID, questionId);
    }

    private void initViews() {
        toolbarDetail = findViewById(R.id.toolbarDetail);
        tvQuestionAvatar = findViewById(R.id.tvQuestionAvatar);
        tvQuestionAuthorName = findViewById(R.id.tvQuestionAuthorName);
        tvQuestionTime = findViewById(R.id.tvQuestionTime);
        tvQuestionCategory = findViewById(R.id.tvQuestionCategory);
        tvQuestionTitle = findViewById(R.id.tvQuestionTitle);
        tvQuestionContent = findViewById(R.id.tvQuestionContent);
        tvAnswersHeader = findViewById(R.id.tvAnswersHeader);

        rvAnswers = findViewById(R.id.rvAnswers);
        pbAnswersLoading = findViewById(R.id.pbAnswersLoading);
        tvEmptyAnswers = findViewById(R.id.tvEmptyAnswers);

        etAnswerInput = findViewById(R.id.etAnswerInput);
        btnSendAnswer = findViewById(R.id.btnSendAnswer);
        btnQuestionLike = findViewById(R.id.btnQuestionLike);
        ivQuestionLike = findViewById(R.id.ivQuestionLike);
        ivQuestionImage = findViewById(R.id.ivQuestionImage);
        tvQuestionLikeCount = findViewById(R.id.tvQuestionLikeCount);
        if (btnQuestionLike != null) {
            btnQuestionLike.setOnClickListener(v -> toggleQuestionLike());
        }
    }

    private void setupOwnerMenu() {
        toolbarDetail.setOnMenuItemClickListener(this::onOwnerMenuClick);
        bindOwnerMenu();
    }

    private boolean onOwnerMenuClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_edit_question) {
            openEditQuestion();
            return true;
        }
        if (itemId == R.id.action_delete_question) {
            confirmDeleteQuestion();
            return true;
        }
        return false;
    }

    private void bindOwnerMenu() {
        boolean owner = isQuestionOwner();
        MenuItem editItem = toolbarDetail.getMenu().findItem(R.id.action_edit_question);
        MenuItem deleteItem = toolbarDetail.getMenu().findItem(R.id.action_delete_question);
        if (editItem != null) {
            editItem.setVisible(owner);
        }
        if (deleteItem != null) {
            deleteItem.setVisible(owner);
        }
    }

    private boolean isQuestionOwner() {
        return currentUser != null && question != null
                && currentUser.getId() != null
                && currentUser.getId().equals(question.getUserId());
    }

    private boolean isOwnContent(String authorId) {
        return currentUser != null && currentUser.getId() != null && currentUser.getId().equals(authorId);
    }

    /** Tải câu hỏi mới nhất từ Firestore; đóng màn hình nếu đã bị xóa hoặc bị ẩn. */
    private void loadQuestion() {
        if (!NetworkUtils.isOnline() && question != null) {
            displayQuestionInfo();
            btnSendAnswer.setEnabled(false);
            Toast.makeText(this, R.string.message_offline_cached, Toast.LENGTH_SHORT).show();
            return;
        }
        if (questionListener != null) {
            return;
        }

        questionListener = questionService.listenQuestion(questionId, (snapshot, error) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (error != null) {
                if (showCachedQuestionIfNeeded()) {
                    return;
                }
                Toast.makeText(this, "Lỗi tải câu hỏi: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            if (snapshot == null || !snapshot.exists()) {
                Toast.makeText(this, "Câu hỏi không còn tồn tại", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            Question loaded = QuestionService.toQuestion(snapshot);
            if (loaded == null || !Question.STATUS_ACTIVE.equals(loaded.getStatus())) {
                Toast.makeText(this, "Câu hỏi không còn tồn tại", Toast.LENGTH_SHORT).show();
                finish();
                return;
            }
            question = loaded;
            displayQuestionInfo();
            btnSendAnswer.setEnabled(true);
            refreshQuestionLikeState();
        });
    }

    private boolean showCachedQuestionIfNeeded() {
        List<Question> cached = syncManager.getViewedQuestionsLastDay();
        if (cached == null) {
            return false;
        }
        for (Question item : cached) {
            if (questionId.equals(item.getQuestionId())) {
                question = item;
                displayQuestionInfo();
                btnSendAnswer.setEnabled(false);
                Toast.makeText(this, R.string.message_offline_cached, Toast.LENGTH_SHORT).show();
                return true;
            }
        }
        return false;
    }

    private void displayQuestionInfo() {
        // category = tên ngành (CreateQuestionActivity/dữ liệu mẫu đều có); majorId chỉ là dự phòng
        String major = question.getCategory() != null && !question.getCategory().isEmpty()
                ? question.getCategory() : question.getMajorId();
        tvQuestionCategory.setText(major != null && !major.isEmpty() ? major : "Khác");
        tvQuestionTitle.setText(question.getTitle());
        tvQuestionContent.setText(question.getContent());

        String name = question.getUserName();
        String author = name != null && !name.isEmpty() ? name : "Thành viên";
        tvQuestionAuthorName.setText(author);
        tvQuestionAvatar.setText(author.substring(0, 1).toUpperCase());

        // createdAt là long (mili giây); 0 nghĩa là chưa có dữ liệu
        long createdMillis = question.getCreatedAt() > 0
                ? question.getCreatedAt()
                : System.currentTimeMillis();
        CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                createdMillis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
        );
        tvQuestionTime.setText(relativeTime);
        tvQuestionAuthorName.setTextColor(getResources().getColor(R.color.melobi_primary, getTheme()));
        bindQuestionImage();
        bindQuestionLike();
        bindOwnerMenu();
    }

    private void bindQuestionImage() {
        if (ivQuestionImage == null) {
            return;
        }
        String imageUrl = question != null ? question.getImageUrl() : null;
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            ivQuestionImage.setVisibility(View.GONE);
            ivQuestionImage.setImageDrawable(null);
            return;
        }
        ivQuestionImage.setVisibility(View.VISIBLE);
        vn.edu.ueh.melobi_nhom5.util.FirebaseStorageManager.loadBitmap(
                imageUrl,
                1080,
                new vn.edu.ueh.melobi_nhom5.util.FirebaseStorageManager.Callback<android.graphics.Bitmap>() {
                    @Override
                    public void onSuccess(android.graphics.Bitmap result) {
                        if (!isFinishing() && !isDestroyed() && ivQuestionImage != null) {
                            ivQuestionImage.setImageBitmap(result);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (!isFinishing() && !isDestroyed() && ivQuestionImage != null) {
                            ivQuestionImage.setVisibility(View.GONE);
                        }
                    }
                }
        );
    }

    private void bindQuestionLike() {
        if (tvQuestionLikeCount == null || question == null) {
            return;
        }
        tvQuestionLikeCount.setText(question.getLikeCount() + " Thích");
        int color = getResources().getColor(questionLiked ? R.color.like_active : R.color.text_secondary, getTheme());
        tvQuestionLikeCount.setTextColor(color);
        if (ivQuestionLike != null) {
            ivQuestionLike.setImageResource(questionLiked ? R.drawable.ic_thumb_up_filled : R.drawable.ic_thumb_up);
            ivQuestionLike.setColorFilter(color);
        }
    }

    private void refreshQuestionLikeState() {
        if (question == null || currentUser == null) {
            return;
        }
        questionService.isQuestionLikedByUser(question.getId(), currentUser.getId())
                .addOnSuccessListener(liked -> {
                    questionLiked = Boolean.TRUE.equals(liked);
                    bindQuestionLike();
                });
    }

    private void toggleQuestionLike() {
        if (question == null || currentUser == null) {
            return;
        }
        String questionIdValue = question.getId();
        if (questionLiked) {
            questionService.unlikeQuestion(questionIdValue, currentUser.getId())
                    .addOnSuccessListener(changed -> {
                        questionLiked = false;
                        bindQuestionLike();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
        } else {
            questionService.likeQuestion(questionIdValue, question.getUserId(), currentUser.getId())
                    .addOnSuccessListener(changed -> {
                        questionLiked = true;
                        bindQuestionLike();
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
        }
    }

    private void openAuthorProfile() {
        if (question == null) {
            return;
        }
        ProfileActivity.openProfile(this, question.getUserId(), question.getUserName());
    }

    private void openEditQuestion() {
        if (!isQuestionOwner()) {
            Toast.makeText(this, R.string.error_own_content_only, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(this, CreateQuestionActivity.class);
        intent.putExtra(CreateQuestionActivity.EXTRA_EDIT_QUESTION_ID, question.getQuestionId());
        intent.putExtra(CreateQuestionActivity.EXTRA_EDIT_TITLE, question.getTitle());
        intent.putExtra(CreateQuestionActivity.EXTRA_EDIT_CONTENT, question.getContent());
        intent.putExtra(CreateQuestionActivity.EXTRA_EDIT_MAJOR_ID, question.getMajorId());
        intent.putExtra(CreateQuestionActivity.EXTRA_EDIT_IMAGE_URL, question.getImageUrl());
        startActivity(intent);
    }

    private void confirmDeleteQuestion() {
        if (!isQuestionOwner()) {
            Toast.makeText(this, R.string.error_own_content_only, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage(R.string.confirm_delete_question)
                .setPositiveButton("Xóa", (dialog, which) -> {
                    questionService.deleteQuestion(questionId)
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, R.string.message_question_deleted, Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void setupRecyclerView() {
        answerAdapter = new AnswerAdapter(currentUser.getId(), new AnswerAdapter.OnAnswerActionListener() {
            @Override
            public void onLikeClick(Answer answer) {
                toggleLike(answer);
            }

            @Override
            public void onCommentClick(Answer answer) {
                openCommentsBottomSheet(answer);
            }

            @Override
            public void onEditClick(Answer answer) {
                showEditAnswerDialog(answer);
            }

            @Override
            public void onDeleteClick(Answer answer) {
                confirmDeleteAnswer(answer);
            }

            @Override
            public void onAuthorClick(Answer answer) {
                ProfileActivity.openProfile(QuestionDetailActivity.this, answer.getUserId(), answer.getUserName());
            }
        });

        rvAnswers.setLayoutManager(new LinearLayoutManager(this));
        rvAnswers.setAdapter(answerAdapter);
    }

    private void loadAnswers() {
        pbAnswersLoading.setVisibility(View.VISIBLE);
        tvEmptyAnswers.setVisibility(View.GONE);

        if (!NetworkUtils.isOnline()) {
            List<Answer> cached = syncManager.getCachedAnswers(questionId);
            bindAnswers(cached);
            return;
        }
        if (answersListener != null) {
            pbAnswersLoading.setVisibility(View.GONE);
            return;
        }

        answersListener = answerRepository.listenAnswers(questionId, (snapshot, error) -> {
            if (isFinishing() || isDestroyed()) {
                return;
            }
            if (error != null) {
                List<Answer> cached = syncManager.getCachedAnswers(questionId);
                if (cached != null && !cached.isEmpty()) {
                    bindAnswers(cached);
                    return;
                }
                pbAnswersLoading.setVisibility(View.GONE);
                Toast.makeText(this, "Lỗi tải câu trả lời: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            List<Answer> list = answerRepository.mapAnswers(snapshot);
            bindAnswers(list);
            if (question != null) {
                syncManager.cacheQuestionDetail(question, list);
            }
        });
    }

    private void bindAnswers(List<Answer> list) {
        pbAnswersLoading.setVisibility(View.GONE);
        List<Answer> answers = list != null ? list : java.util.Collections.emptyList();
        if (answers.isEmpty()) {
            tvEmptyAnswers.setVisibility(View.VISIBLE);
        } else {
            tvEmptyAnswers.setVisibility(View.GONE);
        }
        tvAnswersHeader.setText("Câu trả lời (" + answers.size() + ")");
        answerAdapter.setAnswers(answers);
    }

    private void submitAnswer() {
        if (question == null) return;             // chưa tải xong câu hỏi
        if (!NetworkUtils.isOnline()) {
            Toast.makeText(this, R.string.error_no_internet, Toast.LENGTH_SHORT).show();
            return;
        }
        String content = etAnswerInput.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập nội dung câu trả lời", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSendAnswer.setEnabled(false);

        answerService.createAnswer(
                questionId,
                currentUser.getId(),
                currentUser.getDisplayName(),
                currentUser.getAvatarUrl(),
                content,
                new AnswerService.Callback<Answer>() {
                    @Override
                    public void onSuccess(Answer result) {
                        btnSendAnswer.setEnabled(true);
                        etAnswerInput.setText("");
                        Toast.makeText(QuestionDetailActivity.this, "Đã gửi câu trả lời", Toast.LENGTH_SHORT).show();
                        // Báo cho chủ câu hỏi (bỏ qua nếu tự trả lời câu hỏi của mình)
                        notificationService.createNotification(
                                question.getUserId(), currentUser.getId(),
                                Notification.TYPE_NEW_ANSWER, questionId, Like.TYPE_QUESTION);
                        loadAnswers();
                    }

                    @Override
                    public void onError(Exception e) {
                        btnSendAnswer.setEnabled(true);
                        Toast.makeText(QuestionDetailActivity.this, "Gửi câu trả lời thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void toggleLike(Answer answer) {
        answerRepository.toggleLike(answer.getId(), currentUser.getId(), new AnswerRepository.Callback<Answer>() {
            @Override
            public void onSuccess(Answer result) {
                if (result != null && result.isLikedBy(currentUser.getId())) {
                    notificationService.createNotification(
                            answer.getUserId(),
                            currentUser.getId(),
                            Notification.TYPE_LIKE,
                            questionId,
                            Like.TYPE_ANSWER);
                }
                loadAnswers();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(QuestionDetailActivity.this, "Lỗi tương tác thích: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditAnswerDialog(Answer answer) {
        if (!isOwnContent(answer.getUserId())) {
            Toast.makeText(this, R.string.error_own_content_only, Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_text, null);
        TextInputEditText etInput = dialogView.findViewById(R.id.etDialogInput);
        etInput.setText(answer.getContent());

        new AlertDialog.Builder(this)
                .setTitle("Sửa câu trả lời")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String updatedText = etInput.getText() != null ? etInput.getText().toString().trim() : "";
                    if (updatedText.isEmpty()) {
                        Toast.makeText(this, "Nội dung không được để trống", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    answerRepository.updateAnswer(answer.getId(), updatedText, new AnswerRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(QuestionDetailActivity.this, "Đã cập nhật câu trả lời", Toast.LENGTH_SHORT).show();
                            loadAnswers();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(QuestionDetailActivity.this, "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmDeleteAnswer(Answer answer) {
        if (!isOwnContent(answer.getUserId())) {
            Toast.makeText(this, R.string.error_own_content_only, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc muốn xóa câu trả lời này cùng tất cả bình luận liên quan?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    answerRepository.deleteAnswer(answer, new AnswerRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            Toast.makeText(QuestionDetailActivity.this, "Đã xóa câu trả lời", Toast.LENGTH_SHORT).show();
                            loadAnswers();
                        }

                        @Override
                        public void onError(Exception e) {
                            Toast.makeText(QuestionDetailActivity.this, "Lỗi xóa câu trả lời: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void openCommentsBottomSheet(Answer answer) {
        CommentBottomSheetDialogFragment bottomSheet = CommentBottomSheetDialogFragment.newInstance(answer.getId());
        bottomSheet.setOnCommentUpdatedListener(() -> loadAnswers());
        bottomSheet.show(getSupportFragmentManager(), "CommentBottomSheet");
    }
}
