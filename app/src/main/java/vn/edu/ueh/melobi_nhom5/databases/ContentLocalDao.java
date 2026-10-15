package vn.edu.ueh.melobi_nhom5.databases;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import vn.edu.ueh.melobi_nhom5.entities.Question;

import java.util.ArrayList;
import java.util.List;

public class ContentLocalDao {

    private final DatabaseHelper dbHelper;

    public ContentLocalDao(Context context) {
        this.dbHelper = DatabaseHelper.getInstance(context);
    }

    /**
     * Lưu/Cập nhật danh sách câu hỏi từ Firebase xuống SQLite (Offline Caching)
     * Sử dụng Transaction để tối ưu hiệu năng ghi dữ liệu hàng loạt.
     */
    public void saveQuestions(List<Question> questionList) {
        if (questionList == null || questionList.isEmpty()) return;

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        // 1. Bắt đầu Transaction (Giao dịch CSDL)
        db.beginTransaction();
        try {
            for (Question q : questionList) {
                ContentValues values = new ContentValues();
                values.put(DatabaseHelper.COL_Q_ID, q.getQuestionId());
                values.put(DatabaseHelper.COL_Q_USER_ID, q.getUserId());
                values.put(DatabaseHelper.COL_Q_MAJOR_ID, q.getMajorId());
                values.put(DatabaseHelper.COL_Q_TITLE, q.getTitle());
                values.put(DatabaseHelper.COL_Q_CONTENT, q.getContent());
                values.put(DatabaseHelper.COL_Q_IMAGE_URL, q.getImageUrl());
                values.put(DatabaseHelper.COL_Q_STATUS, q.getStatus());
                values.put(DatabaseHelper.COL_Q_LIKE_COUNT, q.getLikeCount());
                values.put(DatabaseHelper.COL_Q_ANSWER_COUNT, q.getAnswerCount());
                values.put(DatabaseHelper.COL_Q_CREATED_AT, q.getCreatedAt());
                values.put(DatabaseHelper.COL_Q_UPDATED_AT, q.getUpdatedAt());

                // Sử dụng CONFLICT_REPLACE: Nếu trùng questionId thì ghi đè dữ liệu mới vào
                db.insertWithOnConflict(
                        DatabaseHelper.TABLE_QUESTION,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                );
            }
            // 2. Đánh dấu Transaction thành công
            db.setTransactionSuccessful();
        } finally {
            // 3. Kết thúc Transaction (Bắt buộc nằm trong block finally để tránh treo CSDL)
            db.endTransaction();
        }
    }

    /**
     * Đọc danh sách câu hỏi lưu trong SQLite ra để hiển thị khi mất mạng.
     * Đợt xem được sắp xếp theo thời gian tạo mới nhất (createdAt DESC).
     */
    public List<Question> getQuestions() {
        List<Question> questionList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Lệnh truy vấn SQL lấy tất cả câu hỏi, xếp từ mới nhất tới cũ nhất
        String selectQuery = "SELECT * FROM " + DatabaseHelper.TABLE_QUESTION
                + " ORDER BY " + DatabaseHelper.COL_Q_CREATED_AT + " DESC";

        Cursor cursor = db.rawQuery(selectQuery, null);

        try {
            // Duyệt từng dòng (row) kết quả trả về từ Cursor
            if (cursor.moveToFirst()) {
                do {
                    Question q = new Question();

                    // Đọc giá trị từ các cột theo tên
                    q.setQuestionId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_ID)));
                    q.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_USER_ID)));
                    q.setMajorId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_MAJOR_ID)));
                    q.setTitle(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_TITLE)));
                    q.setContent(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_CONTENT)));
                    q.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_IMAGE_URL)));
                    q.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_STATUS)));
                    q.setLikeCount(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_LIKE_COUNT)));
                    q.setAnswerCount(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_ANSWER_COUNT)));
                    q.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_CREATED_AT)));
                    q.setUpdatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_UPDATED_AT)));

                    questionList.add(q);
                } while (cursor.moveToNext());
            }
        } finally {
            // Luôn đóng Cursor sau khi đọc xong để giải phóng bộ nhớ RAM
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        return questionList;
    }
}
/*1 số lỗi sau:
* Chưa hỗ trợ đồng bộ hai chiều: SyncManager.pushPendingChanges() cần biết bản ghi nào được tạo hoặc sửa lúc offline. DAO chưa có cột hay hàm nào cho việc này
-> Cách sửa: thêm:
- Cột syncStatus (SYNCED / PENDING_CREATE / PENDING_UPDATE / PENDING_DELETE) vào bảng.
- Các hàm getPendingQuestions(), markAsSynced(id), saveQuestion(Question q, String syncStatus).
* Chưa có phân trang và lọc: QuestionService.getQuestions(page), filterByMajor() và searchQuestions() đều cần có phiên bản offline. Hiện getQuestions() 
trả toàn bộ bảng không giới hạn
-> Cách sửa: thêm:
- getQuestions(int limit, int offset).
- getQuestionsByMajor(majorId).
- searchQuestions(keyword) dùng WHERE title LIKE ? OR content LIKE ?
* Chưa lọc nội dung bị ẩn
-> Cách sửa: Câu truy vấn nên có WHERE status != 'HIDDEN'
* Dữ liệu hiển thị offline còn thiếu: Question chỉ lưu userId. Khi offline, danh sách câu hỏi sẽ không có tên và avatar người đăng
* Rủi ro với CONFLICT_REPLACE: REPLACE thực chất là xóa rồi chèn lại. Nếu bảng Answer có FOREIGN KEY ... ON DELETE CASCADE tới Question, 
việc ghi đè một câu hỏi sẽ xóa luôn các câu trả lời đã cache*/
