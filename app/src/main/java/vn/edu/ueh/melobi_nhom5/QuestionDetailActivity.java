package vn.edu.ueh.melobi_nhom5;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

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
import vn.edu.ueh.melobi_nhom5.service.NotificationService;
import vn.edu.ueh.melobi_nhom5.entity.Notification;
import vn.edu.ueh.melobi_nhom5.entity.Like;

public class QuestionDetailActivity extends AppCompatActivity {

    /** Cách khuyến nghị: chỉ truyền ID, màn hình tự tải dữ liệu mới nhất từ Firestore. */
    public static final String EXTRA_QUESTION_ID = "extra_question_id";
    /** Có thể truyền cả đối tượng Question (model.Question) để hiển thị ngay trong lúc tải. */
    public static final String EXTRA_QUESTION = "extra_question";

    private String questionId;
    private Question question;               // có thể null cho tới khi tải xong
    private AnswerRepository answerRepository;
    private AnswerService answerService;
    private QuestionService questionService;
    private NotificationService notificationService;
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

        if (questionId == null || questionId.isEmpty()) {
            Toast.makeText(this, "Không tìm thấy thông tin câu hỏi", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        answerRepository = new AnswerRepository();
        answerService = new AnswerService();
        questionService = new QuestionService();
        notificationService = new NotificationService();
        currentUser = AuthHelper.getCurrentUser();

        initViews();
        setupRecyclerView();

        toolbarDetail.setNavigationOnClickListener(v -> finish());
        btnSendAnswer.setOnClickListener(v -> submitAnswer());
        btnSendAnswer.setEnabled(false);          // chỉ cho gửi sau khi xác nhận câu hỏi còn hiển thị

        if (question != null) displayQuestionInfo();   // hiển thị tạm bản truyền sang
        loadQuestion();
        loadAnswers();
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
    }

    /** Tải câu hỏi mới nhất từ Firestore; đóng màn hình nếu đã bị xóa hoặc bị ẩn. */
    private void loadQuestion() {
        questionService.getQuestionDetail(questionId)
                .addOnSuccessListener(loaded -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (loaded == null) {      // không tồn tại hoặc không còn VISIBLE
                        Toast.makeText(this, "Câu hỏi không còn tồn tại", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    question = loaded;
                    displayQuestionInfo();
                    btnSendAnswer.setEnabled(true);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Lỗi tải câu hỏi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
        });

        rvAnswers.setLayoutManager(new LinearLayoutManager(this));
        rvAnswers.setAdapter(answerAdapter);
    }

    private void loadAnswers() {
        pbAnswersLoading.setVisibility(View.VISIBLE);
        tvEmptyAnswers.setVisibility(View.GONE);

        answerRepository.getAnswersByQuestionId(questionId, new AnswerRepository.Callback<List<Answer>>() {
            @Override
            public void onSuccess(List<Answer> list) {
                pbAnswersLoading.setVisibility(View.GONE);
                if (list.isEmpty()) {
                    tvEmptyAnswers.setVisibility(View.VISIBLE);
                } else {
                    tvEmptyAnswers.setVisibility(View.GONE);
                }
                tvAnswersHeader.setText("Câu trả lời (" + list.size() + ")");
                answerAdapter.setAnswers(list);
            }

            @Override
            public void onError(Exception e) {
                pbAnswersLoading.setVisibility(View.GONE);
                Toast.makeText(QuestionDetailActivity.this, "Lỗi tải câu trả lời: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitAnswer() {
        if (question == null) return;             // chưa tải xong câu hỏi
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
                loadAnswers();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(QuestionDetailActivity.this, "Lỗi tương tác thích: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showEditAnswerDialog(Answer answer) {
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