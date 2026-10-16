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

    private static final int TITLE_MIN = 10;
    private static final int TITLE_MAX = 150;
    private static final int CONTENT_MIN = 20;

    private QuestionService questionService;
    private MajorService majorService;
    private ArrayAdapter<Major> majorAdapter;
    private User currentUser;
    private Major selectedMajor;             // null cho tới khi người dùng chọn

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

        initViews();
        setupMajorDropdown();

        toolbar.setNavigationOnClickListener(v -> finish());
        btnPost.setOnClickListener(v -> submitQuestion());
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
                })
                .addOnFailureListener(e -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, "Lỗi tải danh sách ngành: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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