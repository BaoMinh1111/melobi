package vn.edu.ueh.melobi_nhom5.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import android.test.RenamingDelegatingContext; // Lớp bảo vệ dữ liệu thật

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import vn.edu.ueh.melobi_nhom5.entity.Question;

@RunWith(AndroidJUnit4.class)
public class ContentLocalDaoTest {

    private ContentLocalDao localDao;
    private DatabaseHelper dbHelper;
    private Context testContext;

    @Before
    public void setUp() {

        Context targetContext = ApplicationProvider.getApplicationContext();

        testContext = new RenamingDelegatingContext(targetContext, "test_");


        localDao = new ContentLocalDao(testContext);
        dbHelper = DatabaseHelper.getInstance(testContext);

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_QUESTION, null, null);
        db.delete(DatabaseHelper.TABLE_ANSWER, null, null);
    }

    @After
    public void tearDown() {
        // Giải phóng tài nguyên sau khi test xong
        if (dbHelper != null) {
            dbHelper.close();
        }
    }

    @Test
    public void testSaveAndGetQuestionById() {
        String testQuestionId = "q_123";
        Question mockQuestion = new Question();
        mockQuestion.setQuestionId(testQuestionId);
        mockQuestion.setUserId("user_01");
        mockQuestion.setTitle("Học Android bắt đầu từ đâu?");
        mockQuestion.setContent("Giáo sư hãy giảng dạy cho em");
        mockQuestion.setLikeCount(10);

        mockQuestion.setStatus(Question.STATUS_VISIBLE);

        localDao.saveQuestion(mockQuestion, DatabaseHelper.SYNC_STATUS_PENDING_ADD);

        // Đọc và kiểm chứng
        Question result = localDao.getQuestionById(testQuestionId);

        assertNotNull("Câu hỏi lấy lên không được null", result);
        assertEquals("Tiêu đề phải khớp nhau", "Học Android bắt đầu từ đâu?", result.getTitle());
        assertEquals("Số like phải bằng 10", 10, result.getLikeCount());
        assertEquals("Trạng thái đồng bộ phải lưu đúng", DatabaseHelper.SYNC_STATUS_PENDING_ADD, result.getSyncStatus());
    }

    @Test
    public void testClearOldCache() {
        // Chuẩn bị môi trường: Tạo dữ liệu rác
        Question q1 = new Question();
        q1.setQuestionId("q_new");
        q1.setCreatedAt(System.currentTimeMillis());
        localDao.saveQuestion(q1, DatabaseHelper.SYNC_STATUS_SYNCED);

        Question q2 = new Question();
        q2.setQuestionId("q_old_synced");
        long tenDaysAgo = System.currentTimeMillis() - (10L * 24 * 60 * 60 * 1000);
        q2.setCreatedAt(tenDaysAgo);
        localDao.saveQuestion(q2, DatabaseHelper.SYNC_STATUS_SYNCED);

        Question q3 = new Question();
        q3.setQuestionId("q_old_pending");
        q3.setCreatedAt(tenDaysAgo);
        localDao.saveQuestion(q3, DatabaseHelper.SYNC_STATUS_PENDING_ADD);

        // Chạy hàm dọn dẹp
        localDao.clearOldCache();

        // Xác nhận kết quả
        assertNotNull("Câu hỏi mới tạo không được xóa", localDao.getQuestionById("q_new"));
        assertNull("Câu hỏi cũ ĐÃ ĐỒNG BỘ phải bị xóa để tiết kiệm bộ nhớ", localDao.getQuestionById("q_old_synced"));
        assertNotNull("Câu hỏi cũ CHƯA ĐỒNG BỘ tuyệt đối không được xóa", localDao.getQuestionById("q_old_pending"));
    }
}