package vn.edu.ueh.melobi_nhom5.database;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import vn.edu.ueh.melobi_nhom5.entity.Question;

// Đánh dấu đây là bài test chạy trên thiết bị Android
@RunWith(AndroidJUnit4.class)
public class ContentLocalDaoTest {

    private ContentLocalDao localDao;
    private DatabaseHelper dbHelper;
    private Context context;

    /**
     * Chạy TRƯỚC MỖI bài test để chuẩn bị môi trường sạch
     */
    @Before
    public void setUp() {
        // Lấy Context thực tế của máy ảo/điện thoại
        context = ApplicationProvider.getApplicationContext();

        // Cực kỳ quan trọng: Xóa trắng DB cũ trước khi test để các bài test không bị nhiễu dữ liệu của nhau
        context.deleteDatabase("melobi_offline.db");

        localDao = new ContentLocalDao(context);
        dbHelper = DatabaseHelper.getInstance(context);
    }

    /**
     * tearDown(): Chạy SAU MỖI bài test để giải phóng tài nguyên (Frees resources)
     */
    @After
    public void tearDown() {
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    /**
     * BÀI TEST 1: Kiểm tra tính năng Lưu và Đọc chi tiết câu hỏi (Upsert & Point Query)
     */
    @Test
    public void testSaveAndGetQuestionById() {
        // 1. ARRANGE (Chuẩn bị)
        String testQuestionId = "q_123";
        Question mockQuestion = new Question();
        mockQuestion.setQuestionId(testQuestionId);
        mockQuestion.setUserId("user_01");
        mockQuestion.setTitle("Học Android bắt đầu từ đâu?");
        mockQuestion.setContent("Giáo sư chỉ em với");
        mockQuestion.setLikeCount(10); // Thuộc tính likeCount theo thiết kế[cite: 4]
        mockQuestion.setStatus("ACTIVE");

        // 2. ACT (Hành động)
        // Gọi hàm DAO để lưu vào SQLite với cờ trạng thái đồng bộ
        localDao.saveQuestion(mockQuestion, DatabaseHelper.SYNC_STATUS_PENDING_ADD);

        // 3. ASSERT (Xác nhận)
        // Đọc ngược lại từ SQLite
        Question result = localDao.getQuestionById(testQuestionId);

        // Xác nhận dữ liệu không bị rỗng
        assertNotNull("Câu hỏi lấy lên không được null", result);

        // So sánh từng trường dữ liệu xem có khớp không
        assertEquals("Tiêu đề phải khớp nhau", "Học Android bắt đầu từ đâu?", result.getTitle());
        assertEquals("Số like phải bằng 10", 10, result.getLikeCount());
        assertEquals("Trạng thái đồng bộ phải lưu đúng", DatabaseHelper.SYNC_STATUS_PENDING_ADD, result.getSyncStatus());
    }

    /**
     * BÀI TEST 2: Kiểm tra tính toàn vẹn của chức năng dọn dẹp Cache (clearOldCache)
     */
    @Test
    public void testClearOldCache() {
        // 1. ARRANGE
        // Tạo câu hỏi 1: Đã đồng bộ (SYNCED) nhưng mới tạo hôm nay -> KHÔNG ĐƯỢC XÓA
        Question q1 = new Question();
        q1.setQuestionId("q_new");
        q1.setCreatedAt(System.currentTimeMillis());
        localDao.saveQuestion(q1, DatabaseHelper.SYNC_STATUS_SYNCED);

        // Tạo câu hỏi 2: Tạo từ 10 ngày trước (cũ) VÀ đã đồng bộ (SYNCED) -> PHẢI BỊ XÓA
        Question q2 = new Question();
        q2.setQuestionId("q_old_synced");
        long tenDaysAgo = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000);
        q2.setCreatedAt(tenDaysAgo);
        localDao.saveQuestion(q2, DatabaseHelper.SYNC_STATUS_SYNCED);

        // Tạo câu hỏi 3: Tạo từ 10 ngày trước (cũ) NHƯNG chưa đồng bộ (PENDING_ADD) -> KHÔNG ĐƯỢC XÓA
        Question q3 = new Question();
        q3.setQuestionId("q_old_pending");
        q3.setCreatedAt(tenDaysAgo);
        localDao.saveQuestion(q3, DatabaseHelper.SYNC_STATUS_PENDING_ADD);

        // 2. ACT
        localDao.clearOldCache();

        // 3. ASSERT
        assertNotNull("Câu hỏi mới tạo không được xóa", localDao.getQuestionById("q_new"));
        assertNull("Câu hỏi cũ ĐÃ ĐỒNG BỘ phải bị xóa để tiết kiệm bộ nhớ", localDao.getQuestionById("q_old_synced"));
        assertNotNull("Câu hỏi cũ CHƯA ĐỒNG BỘ tuyệt đối không được xóa để tránh mất dữ liệu", localDao.getQuestionById("q_old_pending"));
    }
}
/*1 số lỗi sau:
* setStatus("ACTIVE") sai giá trị. ACTIVE là trạng thái của User. Question dùng VISIBLE / HIDDEN. 
Nếu getQuestions() lọc status = 'VISIBLE' thì câu hỏi trong test sẽ không hiện ra. Dùng hằng số Question.STATUS_VISIBLE.
* Thứ tự trong setUp() có thể làm test chập chờn. Lớp đang deleteDatabase(...) trước, nhưng DatabaseHelper là singleton có thể còn giữ kết nối mở từ test khác 
(ví dụ test của SyncManager). Khi đó ghi dữ liệu vào file đã bị xóa và test thấy dữ liệu cũ.
* Test đang xóa dữ liệu thật của app. Instrumented test chạy chung thư mục dữ liệu với app đã cài, 
nên deleteDatabase sẽ xóa luôn cache offline thật trên máy/emulator đó.*/
