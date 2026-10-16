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

    // Hàm Upsert (Update or Insert) lưu câu hỏi an toàn
    // Khắc phục lỗi CASCADE DELETE của CONFLICT_REPLACE
    public void saveQuestion(Question q, String syncStatus) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
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
        values.put(DatabaseHelper.COL_Q_SYNC_STATUS, syncStatus);
        values.put(DatabaseHelper.COL_Q_AUTHOR_NAME, q.getAuthorName());
        values.put(DatabaseHelper.COL_Q_AUTHOR_AVATAR, q.getAuthorAvatarUrl());

        // THUẬT TOÁN UPSERT AN TOÀN
        // Bước 1: Thử Cập nhật (UPDATE) dựa trên ID
        int rowsAffected = db.update(DatabaseHelper.TABLE_QUESTION, values,
                DatabaseHelper.COL_Q_ID + " = ?", new String[]{q.getQuestionId()});

        // Bước 2: Nếu không có dòng nào được cập nhật (Rows = 0), nghĩa là dữ liệu chưa tồn tại -> Thêm mới (INSERT)
        if (rowsAffected == 0) {
            db.insert(DatabaseHelper.TABLE_QUESTION, null, values);
        }
    }

    // Lưu danh sách từ FireBase (Sử dụng Transaction để tối ưu I/O)
    public void saveQuestionsList(List<Question> questionList) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction(); // Gom toàn bộ lệnh vào 1 block duy nhất
        try {
            for (Question q : questionList) {
                // Lưu trạng thái là SYNCED vì dữ liệu này vừa kéo từ mạng về
                saveQuestion(q, DatabaseHelper.SYNC_STATUS_SYNCED);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

   // Lấy danh sách các câu hỏi được tạo/sửa lúc Offline để SyncManager đẩy lên FireBase
   public List<Question> getPendingQuestions() {
       SQLiteDatabase db = dbHelper.getReadableDatabase();
       // Lệnh SQL: Lấy các dòng có trạng thái KHÁC "SYNCED"
       String query = "SELECT * FROM " + DatabaseHelper.TABLE_QUESTION
               + " WHERE " + DatabaseHelper.COL_Q_SYNC_STATUS + " != ?";

       Cursor cursor = db.rawQuery(query, new String[]{DatabaseHelper.SYNC_STATUS_SYNCED});
       return parseQuestionListFromCursor(cursor);
   }

   // Đánh dấu câu hỏi đã đồng bộ thành công lên FireBase
   public void markAsSynced(String questionId) {
       SQLiteDatabase db = dbHelper.getWritableDatabase();
       ContentValues values = new ContentValues();
       values.put(DatabaseHelper.COL_Q_SYNC_STATUS, DatabaseHelper.SYNC_STATUS_SYNCED);
       db.update(DatabaseHelper.TABLE_QUESTION, values, DatabaseHelper.COL_Q_ID + " = ?", new String[]{questionId});
   }

   // Lấy danh sách câu hỏi phân trang -> Tránh sập RAM & Lọc bài bị ẩn
   public List<Question> getQuestions(int limit, int offset) {
       SQLiteDatabase db = dbHelper.getReadableDatabase();
       // Cú pháp chuẩn: SELECT * FROM bảng WHERE status != 'HIDDEN' ORDER BY time DESC LIMIT ? OFFSET ?
       String query = "SELECT * FROM " + DatabaseHelper.TABLE_QUESTION
               + " WHERE " + DatabaseHelper.COL_Q_STATUS + " != 'HIDDEN'"
               + " ORDER BY " + DatabaseHelper.COL_Q_CREATED_AT + " DESC"
               + " LIMIT ? OFFSET ?";

       Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(limit), String.valueOf(offset)});
       return parseQuestionListFromCursor(cursor);
   }

   // Lấy danh sách câu hỏi theo chuyên ngành
   public List<Question> getQuestionsByMajor(String majorId, int limit, int offset) {
       SQLiteDatabase db = dbHelper.getReadableDatabase();
       String query = "SELECT * FROM " + DatabaseHelper.TABLE_QUESTION
               + " WHERE " + DatabaseHelper.COL_Q_STATUS + " != 'HIDDEN' AND " + DatabaseHelper.COL_Q_MAJOR_ID + " = ?"
               + " ORDER BY " + DatabaseHelper.COL_Q_CREATED_AT + " DESC LIMIT ? OFFSET ?";

       Cursor cursor = db.rawQuery(query, new String[]{majorId, String.valueOf(limit), String.valueOf(offset)});
       return parseQuestionListFromCursor(cursor);
   }

   // Tìm kiếm câu hỏi
   public List<Question> searchQuestions(String keyword, int limit, int offset) {
       SQLiteDatabase db = dbHelper.getReadableDatabase();
       String searchPattern = "%" + keyword + "%"; // Dấu % đại diện cho bất kỳ chuỗi nào phía trước/sau

       String query = "SELECT * FROM " + DatabaseHelper.TABLE_QUESTION
               + " WHERE " + DatabaseHelper.COL_Q_STATUS + " != 'HIDDEN' AND "
               + "(" + DatabaseHelper.COL_Q_TITLE + " LIKE ? OR " + DatabaseHelper.COL_Q_CONTENT + " LIKE ?)"
               + " ORDER BY " + DatabaseHelper.COL_Q_CREATED_AT + " DESC LIMIT ? OFFSET ?";

       Cursor cursor = db.rawQuery(query, new String[]{searchPattern, searchPattern, String.valueOf(limit), String.valueOf(offset)});
       return parseQuestionListFromCursor(cursor);
   }

   /* Hàm tiện ích (Helper Method) -> Tránh lặp lại code */
   private List<Question> parseQuestionListFromCursor(Cursor cursor) {
       List<Question> list = new ArrayList<>();
       try {
           if (cursor.moveToFirst()) {
               do {
                   Question q = new Question();
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

                   // Lấy thêm 3 cột mới
                   q.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_SYNC_STATUS)));
                   q.setAuthorName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_AUTHOR_NAME)));
                   q.setAuthorAvatarUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_Q_AUTHOR_AVATAR)));

                   list.add(q);
               } while (cursor.moveToNext());
           }
       } finally {
           if (cursor != null && !cursor.isClosed()) {
               cursor.close();
           }
       }
       return list;
   }
}

