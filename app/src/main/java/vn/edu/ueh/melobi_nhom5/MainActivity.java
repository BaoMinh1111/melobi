package vn.edu.ueh.melobi_nhom5;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.adapter.QuestionAdapter;
import vn.edu.ueh.melobi_nhom5.model.Question;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;
import vn.edu.ueh.melobi_nhom5.util.SampleDataInitializer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private User currentUser;
    private QuestionAdapter questionAdapter;

    private RecyclerView rvQuestions;
    private ProgressBar pbMainLoading;
    private TextView tvMainEmpty;

    private final List<Question> allQuestions = new ArrayList<>();
    private String currentCategory = "Tất cả";

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

        currentUser = AuthHelper.getCurrentUser();

        initViews();
        setupRecyclerView();
        setupCategoryChips();

        AuthHelper.ensureAuth(this::loadQuestions);
    }

    private void initViews() {
        rvQuestions = findViewById(R.id.rvQuestions);
        pbMainLoading = findViewById(R.id.pbMainLoading);
        tvMainEmpty = findViewById(R.id.tvMainEmpty);
        findViewById(R.id.fabAskQuestion).setOnClickListener(v ->
                startActivity(new Intent(this, CreateQuestionActivity.class)));
    }

    private void setupRecyclerView() {
        questionAdapter = new QuestionAdapter(question -> {
            Intent intent = new Intent(MainActivity.this, QuestionDetailActivity.class);
            intent.putExtra(QuestionDetailActivity.EXTRA_QUESTION, question);
            startActivity(intent);
        });

        rvQuestions.setLayoutManager(new LinearLayoutManager(this));
        rvQuestions.setAdapter(questionAdapter);
    }

    private void setupCategoryChips() {
        ChipGroup chipGroup = findViewById(R.id.chipGroupCategory);
        if (chipGroup != null) {
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
                filterQuestions(currentCategory);
            });
        }
    }

    private void filterQuestions(String category) {
        List<Question> filtered = new ArrayList<>();
        if ("Tất cả".equals(category)) {
            filtered.addAll(allQuestions);
        } else {
            for (Question q : allQuestions) {
                if (q.getCategory() != null && q.getCategory().trim().equalsIgnoreCase(category.trim())) {
                    filtered.add(q);
                }
            }
        }
        if (filtered.isEmpty()) {
            tvMainEmpty.setVisibility(View.VISIBLE);
        } else {
            tvMainEmpty.setVisibility(View.GONE);
        }
        questionAdapter.setQuestions(filtered);
    }

    private void loadQuestions() {
        pbMainLoading.setVisibility(View.VISIBLE);
        tvMainEmpty.setVisibility(View.GONE);

        FirebaseFirestore.getInstance().collection("questions")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    pbMainLoading.setVisibility(View.GONE);
                    List<Question> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Question q = doc.toObject(Question.class);
                        if (q != null) {
                            if (q.getId() == null || q.getId().isEmpty()) {
                                q.setId(doc.getId());
                            }
                            list.add(q);
                        }
                    }

                    if (list.isEmpty()) {
                        initSampleData();
                        return;
                    }

                    allQuestions.clear();
                    allQuestions.addAll(list);
                    // Sort newest questions first
                    Collections.sort(allQuestions, (q1, q2) -> Long.compare(q2.getCreatedAt(), q1.getCreatedAt()));

                    filterQuestions(currentCategory);
                })
                .addOnFailureListener(e -> {
                    pbMainLoading.setVisibility(View.GONE);
                    Log.e(TAG, "Lỗi tải câu hỏi: ", e);
                    Toast.makeText(MainActivity.this, "Lỗi tải danh sách câu hỏi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
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
        // Refresh question answerCount when returning from detail screen
        loadQuestions();
    }
}
