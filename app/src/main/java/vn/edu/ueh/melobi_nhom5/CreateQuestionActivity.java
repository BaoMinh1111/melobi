package vn.edu.ueh.melobi_nhom5;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;

import vn.edu.ueh.melobi_nhom5.model.Major;
import vn.edu.ueh.melobi_nhom5.model.Question;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.service.MajorService;
import vn.edu.ueh.melobi_nhom5.service.QuestionService;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;

/** Màn hình đăng câu hỏi mới. Đăng xong sẽ mở thẳng QuestionDetailActivity của câu hỏi đó. */
public class CreateQuestionActivity extends AppCompatActivity {

    public static final String EXTRA_EDIT_QUESTION_ID = "extra_edit_question_id";
    public static final String EXTRA_EDIT_TITLE = "extra_edit_title";
    public static final String EXTRA_EDIT_CONTENT = "extra_edit_content";
    public static final String EXTRA_EDIT_MAJOR_ID = "extra_edit_major_id";
    public static final String EXTRA_EDIT_IMAGE_URL = "extra_edit_image_url";

    private static final int TITLE_MIN = 10;
    private static final int TITLE_MAX = 150;
    private static final int CONTENT_MIN = 20;

    private QuestionService questionService;
    private MajorService majorService;
    private ArrayAdapter<Major> majorAdapter;
    private User currentUser;
    private Major selectedMajor;             // null cho tới khi người dùng chọn
    private String editQuestionId;
    private String editImageUrl;
    private String pendingMajorId;

    private MaterialToolbar toolbar;
    private TextInputLayout tilTitle, tilContent, tilMajor;
    private TextInputEditText etTitle, etContent;
    private MaterialAutoCompleteTextView actvMajor;
    private MaterialButton btnPost;
    private ProgressBar pbPosting;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_question);

        currentUser = AuthHelper.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để đặt câu hỏi", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        questionService = new QuestionService();
        majorService = new MajorService();
        editQuestionId = getIntent().getStringExtra(EXTRA_EDIT_QUESTION_ID);
        editImageUrl = getIntent().getStringExtra(EXTRA_EDIT_IMAGE_URL);
        pendingMajorId = getIntent().getStringExtra(EXTRA_EDIT_MAJOR_ID);

        initViews();
        setupMajorDropdown();
        bindEditMode();

        toolbar.setNavigationOnClickListener(v -> finish());
        btnPost.setOnClickListener(v -> submitQuestion());
    }

    private void bindEditMode() {
        if (editQuestionId == null || editQuestionId.trim().isEmpty()) {
            return;
        }
        toolbar.setTitle(R.string.title_edit_question);
        btnPost.setText(R.string.action_edit);
        String title = getIntent().getStringExtra(EXTRA_EDIT_TITLE);
        String content = getIntent().getStringExtra(EXTRA_EDIT_CONTENT);
        if (title != null) {
            etTitle.setText(title);
        }
        if (content != null) {
            etContent.setText(content);
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbarCreate);
        tilTitle = findViewById(R.id.tilQuestionTitle);
        tilContent = findViewById(R.id.tilQuestionContent);
        tilMajor = findViewById(R.id.tilQuestionMajor);
        etTitle = findViewById(R.id.etQuestionTitle);
        etContent = findViewById(R.id.etQuestionContent);
        actvMajor = findViewById(R.id.actvQuestionMajor);
        btnPost = findViewById(R.id.btnPostQuestion);
        pbPosting = findViewById(R.id.pbPosting);
    }

    private void setupMajorDropdown() {
        majorAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<>());
        actvMajor.setAdapter(majorAdapter);
        actvMajor.setOnItemClickListener((parent, view, position, id) -> {
            selectedMajor = majorAdapter.getItem(position);   // Major.toString() = tên ngành
            tilMajor.setError(null);
        });

        actvMajor.setEnabled(false);
        majorService.getAllMajors()
                .addOnSuccessListener(majors -> {
                    if (isFinishing() || isDestroyed()) return;
                    majorAdapter.clear();
                    majorAdapter.addAll(majors);
                    actvMajor.setEnabled(true);
                    if (majors.isEmpty()) {
                        Toast.makeText(this, "Chưa có dữ liệu ngành", Toast.LENGTH_SHORT).show();
                    }
                    preselectMajor(majors);
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Lỗi tải danh sách ngành: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void preselectMajor(java.util.List<Major> majors) {
        if (pendingMajorId == null || pendingMajorId.trim().isEmpty() || majors == null) {
            return;
        }
        for (int i = 0; i < majors.size(); i++) {
            Major major = majors.get(i);
            if (major != null && (pendingMajorId.equals(major.getMajorId())
                    || pendingMajorId.equals(major.getMajorName()))) {
                selectedMajor = major;
                actvMajor.setText(major.getMajorName(), false);
                return;
            }
        }
    }

    private boolean validate(String title, String content) {
        boolean ok = true;
        tilTitle.setError(null);
        tilContent.setError(null);
        tilMajor.setError(null);

        if (title.length() < TITLE_MIN) {
            tilTitle.setError("Tiêu đề cần ít nhất " + TITLE_MIN + " ký tự");
            ok = false;
        } else if (title.length() > TITLE_MAX) {
            tilTitle.setError("Tiêu đề tối đa " + TITLE_MAX + " ký tự");
            ok = false;
        }
        if (content.length() < CONTENT_MIN) {
            tilContent.setError("Nội dung cần ít nhất " + CONTENT_MIN + " ký tự");
            ok = false;
        }
        if (selectedMajor == null) {
            tilMajor.setError("Vui lòng chọn ngành");
            ok = false;
        }
        return ok;
    }

    private void submitQuestion() {
        String title = etTitle.getText() != null ? etTitle.getText().toString().trim() : "";
        String content = etContent.getText() != null ? etContent.getText().toString().trim() : "";
        if (!validate(title, content)) return;

        setPosting(true);

        if (editQuestionId != null && !editQuestionId.trim().isEmpty()) {
            questionService.updateQuestion(editQuestionId, title, content, editImageUrl)
                    .addOnSuccessListener(unused -> {
                        if (isFinishing() || isDestroyed()) return;
                        Toast.makeText(this, R.string.message_question_updated, Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        if (isFinishing() || isDestroyed()) return;
                        setPosting(false);
                        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
                    });
            return;
        }

        Question question = new Question();
        question.setUserId(currentUser.getId());
        question.setUserName(currentUser.getDisplayName());
        question.setUserAvatarUrl(currentUser.getAvatarUrl());
        question.setTitle(title);
        question.setContent(content);
        question.setMajorId(selectedMajor.getMajorId());
        question.setCategory(selectedMajor.getMajorName());   // MainActivity lọc/hiển thị theo category
        // id, status, createdAt, titleLower... do QuestionService.createQuestion tự điền

        questionService.createQuestion(question)
                .addOnSuccessListener(ref -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Đã đăng câu hỏi", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);

                    Intent intent = new Intent(this, QuestionDetailActivity.class);
                    intent.putExtra(QuestionDetailActivity.EXTRA_QUESTION_ID, ref.getId());
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    setPosting(false);
                    Toast.makeText(this, "Đăng câu hỏi thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setPosting(boolean posting) {
        pbPosting.setVisibility(posting ? View.VISIBLE : View.GONE);
        btnPost.setEnabled(!posting);
        etTitle.setEnabled(!posting);
        etContent.setEnabled(!posting);
        actvMajor.setEnabled(!posting);
    }
}