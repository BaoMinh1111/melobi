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

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.adapter.QuestionAdapter;
import vn.edu.ueh.melobi_nhom5.model.Question;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.service.UserService;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;
import vn.edu.ueh.melobi_nhom5.util.SampleDataInitializer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private User currentUser;
    private UserService userService;
    private QuestionAdapter questionAdapter;

    private MaterialToolbar toolbarMain;
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

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupCategoryChips();
        SampleDataInitializer.initializeSampleMajors(null);
        loadCurrentProfile();

        loadQuestions();
    }

    private void initViews() {
        toolbarMain = findViewById(R.id.toolbarMain);
        rvQuestions = findViewById(R.id.rvQuestions);
        pbMainLoading = findViewById(R.id.pbMainLoading);
        tvMainEmpty = findViewById(R.id.tvMainEmpty);
        View fabAskQuestion = findViewById(R.id.fabAskQuestion);
        if (fabAskQuestion != null) {
            fabAskQuestion.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, CreateQuestionActivity.class)));
        }
    }

    private void setupToolbar() {
        toolbarMain.setSubtitle(currentUser.getDisplayName());
        applyAdminMenuVisibility();
        toolbarMain.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
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
            if (itemId == R.id.action_profile) {
                startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                return true;
            }
            if (itemId == R.id.action_logout) {
                userService.logout();
                navigateToLogin();
                return true;
            }
            return false;
        });
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
                    toolbarMain.setSubtitle(currentUser.getDisplayName());
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
        if (rvQuestions == null || questionAdapter == null) {
            return;
        }
        // Refresh question answerCount when returning from detail screen
        loadQuestions();
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
