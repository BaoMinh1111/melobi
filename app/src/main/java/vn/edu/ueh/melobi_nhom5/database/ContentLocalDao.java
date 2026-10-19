package vn.edu.ueh.melobi_nhom5.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import vn.edu.ueh.melobi_nhom5.entity.Answer;
import vn.edu.ueh.melobi_nhom5.entity.Question;

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
        if (DatabaseHelper.SYNC_STATUS_SYNCED.equals(syncStatus)) {
            String query = "SELECT " + DatabaseHelper.COL_Q_SYNC_STATUS + " FROM " + DatabaseHelper.TABLE_QUESTION
                    + " WHERE " + DatabaseHelper.COL_Q_ID + " = ?";
            Cursor cursor = db.rawQuery(query, new String[]{q.getQuestionId()});
            if (cursor.moveToFirst()) {
                String currentLocalStatus = cursor.getString(0);
                if (DatabaseHelper.SYNC_STATUS_PENDING_UPDATE.equals(currentLocalStatus) ||
                        DatabaseHelper.SYNC_STATUS_PENDING_DELETE.equals(currentLocalStatus)) {
                    cursor.close();
                    return; // Dừng hàm lưu lại, bảo toàn dữ liệu offline
                }
            }
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

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

   // Lấy chi tiết một câu hỏi dựa vào ID (Dùng khi người dùng click vào xem chi tiết lúc Offline)
   public Question getQuestionById(String questionId) {
       SQLiteDatabase db = dbHelper.getReadableDatabase();
       String query = "SELECT * FROM " + DatabaseHelper.TABLE_QUESTION
               + " WHERE " + DatabaseHelper.COL_Q_ID + " = ?";

       Cursor cursor = db.rawQuery(query, new String[]{questionId});
       List<Question> result = parseQuestionListFromCursor(cursor);

       // Nếu danh sách trả về có dữ liệu, lấy phần tử đầu tiên. Nếu không thì trả về null.
       if (result != null && !result.isEmpty()) {
           return result.get(0);
       }
       return null;
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

   /* Quản lý câu trả lời */
    // Thuật toán Upsert -> Lưu 1 câu trả lời
   public void saveAnswer(Answer a, String syncStatus) {
       SQLiteDatabase db = dbHelper.getWritableDatabase();
       if (DatabaseHelper.SYNC_STATUS_SYNCED.equals(syncStatus)) {
           String query = "SELECT " + DatabaseHelper.COL_A_SYNC_STATUS + " FROM " + DatabaseHelper.TABLE_ANSWER
                   + " WHERE " + DatabaseHelper.COL_A_ID + " = ?";
           Cursor cursor = db.rawQuery(query, new String[]{a.getAnswerId()});
           if (cursor.moveToFirst()) {
               String currentLocalStatus = cursor.getString(0);
               if (DatabaseHelper.SYNC_STATUS_PENDING_UPDATE.equals(currentLocalStatus) ||
                       DatabaseHelper.SYNC_STATUS_PENDING_DELETE.equals(currentLocalStatus)) {
                   cursor.close();
                   return; // Giữ nguyên phiên bản Local
               }
           }
           if (cursor != null && !cursor.isClosed()) {
               cursor.close();
           }
       }

       ContentValues values = new ContentValues();
       values.put(DatabaseHelper.COL_A_ID, a.getAnswerId());
       values.put(DatabaseHelper.COL_A_QUESTION_ID, a.getQuestionId());
       values.put(DatabaseHelper.COL_A_USER_ID, a.getUserId());
       values.put(DatabaseHelper.COL_A_CONTENT, a.getContent());
       values.put(DatabaseHelper.COL_A_STATUS, a.getStatus());
       values.put(DatabaseHelper.COL_A_LIKE_COUNT, a.getLikeCount());
       values.put(DatabaseHelper.COL_A_COMMENT_COUNT, a.getCommentCount());
       values.put(DatabaseHelper.COL_A_CREATED_AT, a.getCreatedAt());

       // Cột hiển thị và đồng bộ
       values.put(DatabaseHelper.COL_A_SYNC_STATUS, syncStatus);
       values.put(DatabaseHelper.COL_A_AUTHOR_NAME, a.getUserName());
       values.put(DatabaseHelper.COL_A_AUTHOR_AVATAR, a.getUserAvatarUrl());

       // Upsert: Thử Cập nhật trước
       int rows = db.update(DatabaseHelper.TABLE_ANSWER, values,
               DatabaseHelper.COL_A_ID + " = ?", new String[]{a.getAnswerId()});

       // Nếu cập nhật = 0 dòng -> Thêm mới
       if (rows == 0) {
           db.insert(DatabaseHelper.TABLE_ANSWER, null, values);
       }
   }

   // Lưu danh sách câu trả lời từ FireBase (Dùng Transaction tối ưu tốc độ)
   public void saveAnswers(List<Answer> answerList) {
       if (answerList == null || answerList.isEmpty()) return;

       SQLiteDatabase db = dbHelper.getWritableDatabase();
       db.beginTransaction();
       try {
           for (Answer a : answerList) {
               saveAnswer(a, DatabaseHelper.SYNC_STATUS_SYNCED);
           }
           db.setTransactionSuccessful();
       } finally {
           db.endTransaction();
       }
   }

   // Lấy danh sách câu trả lời của 1 câu hỏi cụ thể
    //Logic: Bỏ qua bài bị ẩn, sắp xếp theo Like giảm dần, Thời gian tăng dần
   public List<Answer> getAnswersByQuestion(String questionId) {
       SQLiteDatabase db = dbHelper.getReadableDatabase();
       String query = "SELECT * FROM " + DatabaseHelper.TABLE_ANSWER
               + " WHERE " + DatabaseHelper.COL_A_QUESTION_ID + " = ? AND " + DatabaseHelper.COL_A_STATUS + " != 'HIDDEN'"
               + " ORDER BY " + DatabaseHelper.COL_A_LIKE_COUNT + " DESC, "
               + DatabaseHelper.COL_A_CREATED_AT + " ASC";

       Cursor cursor = db.rawQuery(query, new String[]{questionId});
       return parseAnswerListFromCursor(cursor);
   }

   /* Dọn dẹp bộ nhớ cache */
    // Xóa dữ liệu cũ (>= 7 ngày) và đã đồng bộ (SYNCED)
   public void clearOldCache() {
       SQLiteDatabase db = dbHelper.getWritableDatabase();

       // Tính toán mốc thời gian của 7 ngày trước (tính bằng mili-giây)
       long sevenDaysInMillis = 7L * 24 * 60 * 60 * 1000;
       long cutoffTime = System.currentTimeMillis() - sevenDaysInMillis;

       // Chỉ xóa những dòng có syncStatus = 'SYNCED' (đã an toàn trên mây) VÀ thời gian tạo cũ hơn 7 ngày
       String whereClause = DatabaseHelper.COL_Q_SYNC_STATUS + " = ? AND " + DatabaseHelper.COL_Q_CREATED_AT + " < ?";
       String[] whereArgs = new String[]{DatabaseHelper.SYNC_STATUS_SYNCED, String.valueOf(cutoffTime)};

       db.beginTransaction();
       try {
           // Xóa ở bảng Question
           db.delete(DatabaseHelper.TABLE_QUESTION, whereClause, whereArgs);

           // Xóa ở bảng Answer (Tái sử dụng chung whereArgs vì logic thời gian và đồng bộ giống nhau)
           db.delete(DatabaseHelper.TABLE_ANSWER,
                   DatabaseHelper.COL_A_SYNC_STATUS + " = ? AND " + DatabaseHelper.COL_A_CREATED_AT + " < ?",
                   whereArgs);

           db.setTransactionSuccessful();
       } finally {
           db.endTransaction();
       }
   }

   /* Hàm tiện ích cho Question (Helper Method) -> Tránh lặp lại code */
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

    /* Hàm tiện ích cho Answer (Helper Method) -> Tránh lặp lại code */
    private List<Answer> parseAnswerListFromCursor(Cursor cursor) {
        List<Answer> list = new ArrayList<>();
        try {
            if (cursor.moveToFirst()) {
                do {
                    Answer a = new Answer();
                    a.setAnswerId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_ID)));
                    a.setQuestionId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_QUESTION_ID)));
                    a.setUserId(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_USER_ID)));
                    a.setContent(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_CONTENT)));

                    // [ĐÃ KHẮC PHỤC]: Đọc status (Trạng thái kiểm duyệt)
                    a.setStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_STATUS)));

                    a.setLikeCount(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_LIKE_COUNT)));
                    a.setCommentCount(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_COMMENT_COUNT)));
                    a.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_CREATED_AT)));

                    // [ĐÃ KHẮC PHỤC LỖI NHẦM HÀM]: Đọc syncStatus (Cờ đồng bộ)
                    a.setSyncStatus(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_SYNC_STATUS)));

                    a.setUserName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_AUTHOR_NAME)));
                    a.setUserAvatarUrl(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_A_AUTHOR_AVATAR)));

                    list.add(a);
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