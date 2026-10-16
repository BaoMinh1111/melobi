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

import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.adapter.QuestionAdapter;
import vn.edu.ueh.melobi_nhom5.model.Question;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.service.QuestionService;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;
import vn.edu.ueh.melobi_nhom5.util.SampleDataInitializer;

/**
 * Feed câu hỏi: tải theo trang (QuestionService.getQuestions / filterByCategory),
 * tự tải thêm khi cuộn gần cuối danh sách, lọc theo chip chuyên ngành ngay trên Firestore.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PAGE_SIZE = 10;
    private static final String CATEGORY_ALL = "Tất cả";

    private User currentUser;
    private QuestionService questionService;
    private QuestionAdapter questionAdapter;

    private RecyclerView rvQuestions;
    private ProgressBar pbMainLoading;     // tải trang đầu
    private ProgressBar pbLoadMore;        // tải các trang sau
    private TextView tvMainEmpty;

    // ---- trạng thái phân trang ----
    private String currentCategory = CATEGORY_ALL;
    private DocumentSnapshot lastVisible;  // document cuối của trang đã tải
    private int loadedCount;
    private boolean isLoading;
    private boolean isLastPage;
    private int loadToken;                 // tăng mỗi khi đổi bộ lọc/làm mới -> bỏ qua kết quả trễ
    private boolean sampleInitTried;
    private boolean needsRefresh;          // true khi người dùng vừa đi sang màn hình khác

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
        questionService = new QuestionService();

        initViews();
        setupRecyclerView();
        setupCategoryChips();

        findViewById(R.id.fabAskQuestion).setOnClickListener(v -> {
            needsRefresh = true;
            startActivity(new Intent(this, CreateQuestionActivity.class));
        });

        AuthHelper.ensureAuth(this::loadFirstPage);
    }

    private void initViews() {
        rvQuestions = findViewById(R.id.rvQuestions);
        pbMainLoading = findViewById(R.id.pbMainLoading);
        pbLoadMore = findViewById(R.id.pbLoadMore);
        tvMainEmpty = findViewById(R.id.tvMainEmpty);
    }

    private void setupRecyclerView() {
        questionAdapter = new QuestionAdapter(question -> {
            needsRefresh = true;   // quay lại sẽ làm mới (số câu trả lời có thể đã đổi)
            Intent intent = new Intent(MainActivity.this, QuestionDetailActivity.class);
            intent.putExtra(QuestionDetailActivity.EXTRA_QUESTION_ID, question.getId());
            intent.putExtra(QuestionDetailActivity.EXTRA_QUESTION, question);   // hiển thị tạm trong lúc tải
            startActivity(intent);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvQuestions.setLayoutManager(layoutManager);
        rvQuestions.setAdapter(questionAdapter);

        // Cuộn gần cuối (còn <= 3 item) thì tải trang tiếp theo
        rvQuestions.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) return;
                int total = layoutManager.getItemCount();
                int lastVisiblePos = layoutManager.findLastVisibleItemPosition();
                if (total > 0 && lastVisiblePos >= total - 3) {
                    loadNextPage();
                }
            }
        });
    }

    private void setupCategoryChips() {
        ChipGroup chipGroup = findViewById(R.id.chipGroupCategory);
        if (chipGroup == null) return;
        chipGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) {
                // bấm lại chip đang chọn -> giữ nguyên lựa chọn, không để trống
                group.check(chipIdOf(currentCategory));
                return;
            }
            String category = categoryOf(checkedId);
            if (category.equals(currentCategory)) return;
            currentCategory = category;
            loadFirstPage();
        });
    }

    private static String categoryOf(int chipId) {
        if (chipId == R.id.chipIT) return "Công nghệ thông tin";
        if (chipId == R.id.chipEcon) return "Kinh tế";
        if (chipId == R.id.chipMarketing) return "Marketing";
        if (chipId == R.id.chipOther) return "Khác";
        return CATEGORY_ALL;
    }

    private static int chipIdOf(String category) {
        switch (category) {
            case "Công nghệ thông tin": return R.id.chipIT;
            case "Kinh tế": return R.id.chipEcon;
            case "Marketing": return R.id.chipMarketing;
            case "Khác": return R.id.chipOther;
            default: return R.id.chipAll;
        }
    }

    // ---------- Tải dữ liệu ----------

    /** Xóa danh sách hiện tại và tải lại từ trang đầu (đổi bộ lọc / làm mới). */
    private void loadFirstPage() {
        loadToken++;
        lastVisible = null;
        loadedCount = 0;
        isLastPage = false;
        isLoading = false;
        questionAdapter.setQuestions(new ArrayList<>());
        tvMainEmpty.setVisibility(View.GONE);
        pbLoadMore.setVisibility(View.GONE);
        loadPage();
    }

    private void loadNextPage() {
        if (loadedCount == 0) return;   // trang đầu chưa xong/đang rỗng
        loadPage();
    }

    private void loadPage() {
        if (isLoading || isLastPage) return;
        isLoading = true;
        final boolean isFirstPage = (lastVisible == null);
        final int token = loadToken;
        (isFirstPage ? pbMainLoading : pbLoadMore).setVisibility(View.VISIBLE);

        Task<QuerySnapshot> task = CATEGORY_ALL.equals(currentCategory)
                ? questionService.getQuestions(lastVisible, PAGE_SIZE)
                : questionService.filterByCategory(currentCategory, lastVisible, PAGE_SIZE);

        task.addOnSuccessListener(snapshot -> {
            if (token != loadToken || isFinishing() || isDestroyed()) return;   // kết quả của bộ lọc cũ
            isLoading = false;
            pbMainLoading.setVisibility(View.GONE);
            pbLoadMore.setVisibility(View.GONE);

            List<DocumentSnapshot> docs = snapshot.getDocuments();
            List<Question> page = new ArrayList<>();
            for (DocumentSnapshot doc : docs) {
                Question q = QuestionService.toQuestion(doc);
                if (q != null) page.add(q);
            }
            if (!docs.isEmpty()) lastVisible = docs.get(docs.size() - 1);
            isLastPage = docs.size() < PAGE_SIZE;

            if (isFirstPage) {
                questionAdapter.setQuestions(page);
            } else {
                questionAdapter.addQuestions(page);
            }
            loadedCount += page.size();

            // Chưa có dữ liệu nào ở feed "Tất cả" -> nạp dữ liệu mẫu (chỉ thử một lần)
            if (loadedCount == 0 && CATEGORY_ALL.equals(currentCategory) && !sampleInitTried) {
                sampleInitTried = true;
                initSampleData();
                return;
            }
            tvMainEmpty.setVisibility(loadedCount == 0 ? View.VISIBLE : View.GONE);
        }).addOnFailureListener(e -> {
            if (token != loadToken || isFinishing() || isDestroyed()) return;
            isLoading = false;
            pbMainLoading.setVisibility(View.GONE);
            pbLoadMore.setVisibility(View.GONE);
            Log.e(TAG, "Lỗi tải câu hỏi: ", e);
            if (e instanceof FirebaseFirestoreException
                    && ((FirebaseFirestoreException) e).getCode()
                    == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
                // Thiếu composite index: Logcat có link để tạo index chỉ với một cú bấm
                Toast.makeText(this, "Thiếu index Firestore - xem Logcat (tag " + TAG + ") để lấy link tạo", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Lỗi tải danh sách câu hỏi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            if (loadedCount == 0) tvMainEmpty.setVisibility(View.VISIBLE);
        });
    }

    private void initSampleData() {
        pbMainLoading.setVisibility(View.VISIBLE);
        SampleDataInitializer.initializeSampleQuestions(new SampleDataInitializer.InitCallback() {
            @Override
            public void onSuccess(String message) {
                pbMainLoading.setVisibility(View.GONE);
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                loadFirstPage();
            }

            @Override
            public void onError(Exception e) {
                pbMainLoading.setVisibility(View.GONE);
                tvMainEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(MainActivity.this, "Lỗi khởi tạo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Chỉ làm mới khi vừa quay lại từ màn hình khác (đăng câu hỏi / xem chi tiết),
        // để không bị nhảy về đầu danh sách ở những lần resume khác.
        if (needsRefresh) {
            needsRefresh = false;
            loadFirstPage();
        }
    }
}