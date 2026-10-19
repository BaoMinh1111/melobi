package vn.edu.ueh.melobi_nhom5;

import android.content.Intent;
import android.os.Bundle;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.net.ConnectivityManager;
import android.Manifest;
import android.content.pm.PackageManager;


import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.adapter.QuestionAdapter;
import vn.edu.ueh.melobi_nhom5.model.Question;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.service.QuestionService;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;
import vn.edu.ueh.melobi_nhom5.util.SampleDataInitializer;
import vn.edu.ueh.melobi_nhom5.service.NotificationService;
import vn.edu.ueh.melobi_nhom5.model.QuestionPage;
import vn.edu.ueh.melobi_nhom5.util.NetworkUtils;
import vn.edu.ueh.melobi_nhom5.util.NotificationWatcher;

/**
 * Feed câu hỏi: tải theo trang (QuestionService.getQuestions / filterByCategory),
 * tự tải thêm khi cuộn gần cuối danh sách, lọc theo chip chuyên ngành ngay trên Firestore.
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PAGE_SIZE = 10;
    private static final String CATEGORY_ALL = "Tất cả";
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private NotificationWatcher notificationWatcher;
    private final ActivityResultLauncher<String> notifPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> { });
    private User currentUser;
    private QuestionService questionService;
    private QuestionAdapter questionAdapter;
    private long lastCreatedAt;                 // con trỏ createdAt của trang cuối (dùng khi đọc SQLite)
    private boolean hasLoadedPage;              // đã tải xong ít nhất một trang của bộ lọc hiện tại
    private boolean showingCache;               // trang gần nhất đến từ cache/SQLite
    private ConnectivityManager.NetworkCallback networkCallback;

    private RecyclerView rvQuestions;
    private ProgressBar pbMainLoading;     // tải trang đầu
    private ProgressBar pbLoadMore;        // tải các trang sau
    private TextView tvMainEmpty;
    private NotificationService notificationService;
    private TextView tvNotifBadge;
    private EditText etSearch;
    private String currentKeyword = "";
    private Runnable searchRunnable;


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
        NetworkUtils.initialize(getApplicationContext());   // an toàn nếu Application chưa gọi
        questionService = new QuestionService(getApplicationContext());
        notificationService = new NotificationService();

        initViews();
        setupRecyclerView();
        setupCategoryChips();
        setupSearch();
        setupNotificationBell();

        findViewById(R.id.fabAskQuestion).setOnClickListener(v -> {
            needsRefresh = true;
            startActivity(new Intent(this, CreateQuestionActivity.class));
        });

        AuthHelper.ensureAuth(() -> {
            loadFirstPage();
            refreshNotificationBadge();          // nếu bạn đã làm nút chuông
            startNotificationWatcher();
        });
        networkCallback = NetworkUtils.registerNetworkListener(() -> runOnUiThread(() -> {
            if (showingCache && !isFinishing() && !isDestroyed()) loadFirstPage();
        }));
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
            if (!currentKeyword.isEmpty()) {
                currentKeyword = "";
                etSearch.setText("");   // watcher thấy rỗng == currentKeyword nên không tải lại hai lần
            }
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
        lastCreatedAt = 0;
        hasLoadedPage = false;
        showingCache = false;
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
        final boolean isFirstPage = !hasLoadedPage;
        final int token = loadToken;
        (isFirstPage ? pbMainLoading : pbLoadMore).setVisibility(View.VISIBLE);

        Task<QuestionPage> task;
        if (!currentKeyword.isEmpty()) {
            task = questionService.searchQuestions(currentKeyword, lastVisible, PAGE_SIZE);
        } else if (CATEGORY_ALL.equals(currentCategory)) {
            task = questionService.getQuestions(lastVisible, lastCreatedAt, PAGE_SIZE);
        } else {
            task = questionService.filterByCategory(currentCategory, lastVisible, lastCreatedAt, PAGE_SIZE);
        }

        task.addOnSuccessListener(result -> {
            if (token != loadToken || isFinishing() || isDestroyed()) return;   // kết quả của bộ lọc cũ
            isLoading = false;
            pbMainLoading.setVisibility(View.GONE);
            pbLoadMore.setVisibility(View.GONE);

            List<Question> page = result.questions;
            // Trang từ SQLite thì bỏ con trỏ Firestore, trang sau (dù online hay offline) dùng createdAt
            if (result.fromCache && currentKeyword.isEmpty()) lastVisible = null;
            else if (result.lastDoc != null) lastVisible = result.lastDoc;
            if (result.lastCreatedAt > 0) lastCreatedAt = result.lastCreatedAt;
            isLastPage = result.isLastPage;
            hasLoadedPage = true;
            showingCache = result.fromCache;
            if (result.fromCache && isFirstPage) {
                Toast.makeText(this, "Đang xem dữ liệu đã lưu (ngoại tuyến)", Toast.LENGTH_SHORT).show();
            }

            if (isFirstPage) {
                questionAdapter.setQuestions(page);
            } else {
                questionAdapter.addQuestions(page);
            }
            loadedCount += page.size();

            // Chưa có dữ liệu nào ở feed "Tất cả" (và đang có mạng thật) -> nạp dữ liệu mẫu, chỉ thử một lần
            if (loadedCount == 0 && CATEGORY_ALL.equals(currentCategory) && currentKeyword.isEmpty()
                    && !result.fromCache && !sampleInitTried) {
                sampleInitTried = true;
                initSampleData();
                return;
            }
            tvMainEmpty.setVisibility(loadedCount == 0 ? View.VISIBLE : View.GONE);
        }).addOnFailureListener(e -> {
            // GIỮ NGUYÊN khối xử lý lỗi cũ của bạn
            if (token != loadToken || isFinishing() || isDestroyed()) return;
            isLoading = false;
            pbMainLoading.setVisibility(View.GONE);
            pbLoadMore.setVisibility(View.GONE);
            Log.e(TAG, "Lỗi tải câu hỏi: ", e);
            if (e instanceof FirebaseFirestoreException
                    && ((FirebaseFirestoreException) e).getCode()
                    == FirebaseFirestoreException.Code.FAILED_PRECONDITION) {
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
        refreshNotificationBadge();
    }
    private void setupNotificationBell() {
        MaterialToolbar toolbar = findViewById(R.id.toolbarMain);
        MenuItem item = toolbar.getMenu().findItem(R.id.action_notifications);
        if (item == null || item.getActionView() == null) return;
        View bell = item.getActionView();
        tvNotifBadge = bell.findViewById(R.id.tvNotifBadge);
        bell.setOnClickListener(v -> startActivity(new Intent(this, NotificationActivity.class)));
    }

    private void refreshNotificationBadge() {
        if (tvNotifBadge == null || FirebaseAuth.getInstance().getCurrentUser() == null) return;
        notificationService.getUnreadCount(AuthHelper.getCurrentUserId())
                .addOnSuccessListener(count -> {
                    if (isFinishing() || isDestroyed()) return;
                    tvNotifBadge.setText(count > 99 ? "99+" : String.valueOf(count));
                    tvNotifBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                });
    }
    private void setupSearch() {
        etSearch = findViewById(R.id.etSearchQuestion);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                final String text = s.toString();
                searchRunnable = () -> applySearch(text);
                searchHandler.postDelayed(searchRunnable, 400);   // chờ người dùng ngừng gõ
            }
        });
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
                applySearch(v.getText().toString());
                return true;
            }
            return false;
        });
    }

    private void applySearch(String raw) {
        String keyword = raw.trim();
        if (keyword.equals(currentKeyword)) return;   // không đổi thì khỏi tải lại
        currentKeyword = keyword;
        loadFirstPage();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
            if (cm != null) cm.unregisterNetworkCallback(networkCallback);
        }
        if (notificationWatcher != null) notificationWatcher.stop();
    }
    private void startNotificationWatcher() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;   // không có đăng nhập thật thì thôi
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);   // Android 13+ phải xin quyền
        }
        if (notificationWatcher == null) notificationWatcher = new NotificationWatcher(getApplicationContext());
        notificationWatcher.start(AuthHelper.getCurrentUserId());
    }
}