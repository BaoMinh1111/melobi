package vn.edu.ueh.melobi_nhom5.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import vn.edu.ueh.melobi_nhom5.model.Question;

/**
 * Bộ nhớ đệm SQLite cho feed câu hỏi (bảng Question của DatabaseHelper).
 * Mọi thao tác chạy trên luồng nền và trả về Task.
 *
 * Quy ước với cột syncStatus:
 *  - Ghi từ Firestore chỉ đụng tới dòng SYNCED, không bao giờ ghi đè dòng PENDING_* (dữ liệu chưa đồng bộ).
 *  - Đọc feed bỏ qua dòng PENDING_DELETE.
 */
public class QuestionLocalStore {
    private static final int MAX_CACHED = 200;   // giữ tối đa 200 câu hỏi SYNCED mới nhất
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private final DatabaseHelper helper;

    public QuestionLocalStore(Context context) {
        this.helper = DatabaseHelper.getInstance(context);
    }

    // ---------- Đọc ----------

    /**
     * Lấy một trang feed, mới nhất trước.
     * @param category        null = tất cả ngành
     * @param beforeCreatedAt chỉ lấy câu hỏi cũ hơn mốc này; 0 = từ đầu
     */
    public Task<List<Question>> getFeedPage(String category, long beforeCreatedAt, int pageSize) {
        return Tasks.call(IO, () -> queryFeedPage(category, beforeCreatedAt, pageSize));
    }

    private List<Question> queryFeedPage(String category, long beforeCreatedAt, int pageSize) {
        StringBuilder where = new StringBuilder();
        List<String> args = new ArrayList<>();

        where.append("IFNULL(").append(DatabaseHelper.COL_Q_STATUS).append(",'")
                .append(Question.STATUS_VISIBLE).append("')=?");
        args.add(Question.STATUS_VISIBLE);

        where.append(" AND IFNULL(").append(DatabaseHelper.COL_Q_SYNC_STATUS).append(",'")
                .append(DatabaseHelper.SYNC_STATUS_SYNCED).append("')<>?");
        args.add(DatabaseHelper.SYNC_STATUS_PENDING_DELETE);

        if (category != null) {
            where.append(" AND ").append(DatabaseHelper.COL_Q_CATEGORY).append("=?");
            args.add(category);
        }
        if (beforeCreatedAt > 0) {
            where.append(" AND ").append(DatabaseHelper.COL_Q_CREATED_AT).append("<?");
            args.add(String.valueOf(beforeCreatedAt));
        }

        List<Question> result = new ArrayList<>();
        SQLiteDatabase db = helper.getReadableDatabase();
        try (Cursor c = db.query(DatabaseHelper.TABLE_QUESTION, null,
                where.toString(), args.toArray(new String[0]),
                null, null,
                DatabaseHelper.COL_Q_CREATED_AT + " DESC",
                String.valueOf(pageSize))) {
            while (c.moveToNext()) result.add(fromCursor(c));
        }
        return result;
    }

    private static Question fromCursor(Cursor c) {
        Question q = new Question();
        q.setId(str(c, DatabaseHelper.COL_Q_ID));
        q.setUserId(str(c, DatabaseHelper.COL_Q_USER_ID));
        q.setMajorId(str(c, DatabaseHelper.COL_Q_MAJOR_ID));
        q.setTitle(str(c, DatabaseHelper.COL_Q_TITLE));
        q.setContent(str(c, DatabaseHelper.COL_Q_CONTENT));
        q.setImageUrl(str(c, DatabaseHelper.COL_Q_IMAGE_URL));
        String status = str(c, DatabaseHelper.COL_Q_STATUS);
        q.setStatus(status != null ? status : Question.STATUS_VISIBLE);
        q.setLikeCount(lng(c, DatabaseHelper.COL_Q_LIKE_COUNT));
        q.setAnswerCount(lng(c, DatabaseHelper.COL_Q_ANSWER_COUNT));
        q.setCreatedAt(lng(c, DatabaseHelper.COL_Q_CREATED_AT));
        q.setUpdatedAt(lng(c, DatabaseHelper.COL_Q_UPDATED_AT));
        q.setUserName(str(c, DatabaseHelper.COL_Q_AUTHOR_NAME));
        q.setUserAvatarUrl(str(c, DatabaseHelper.COL_Q_AUTHOR_AVATAR));
        q.setCategory(str(c, DatabaseHelper.COL_Q_CATEGORY));
        return q;
    }

    private static String str(Cursor c, String col) {
        int i = c.getColumnIndexOrThrow(col);
        return c.isNull(i) ? null : c.getString(i);
    }

    private static long lng(Cursor c, String col) {
        int i = c.getColumnIndexOrThrow(col);
        return c.isNull(i) ? 0 : c.getLong(i);
    }

    // ---------- Ghi ----------

    /**
     * Lưu một trang vừa tải từ Firestore.
     * @param reconcile  true khi trang thuộc feed KHÔNG lọc: dọn các dòng SYNCED nằm trong khoảng thời gian
     *                   của trang nhưng không còn trong kết quả (câu hỏi đã bị ẩn/xóa trên server)
     * @param isFirstPage trang đầu của feed (không có cận trên)
     * @param isLastPage  trang cuối của feed (không có cận dưới)
     */
    public Task<Void> saveFeedPage(List<Question> page, boolean reconcile,
                                   boolean isFirstPage, boolean isLastPage) {
        return Tasks.call(IO, () -> {
            SQLiteDatabase db = helper.getWritableDatabase();
            db.beginTransaction();
            try {
                for (Question q : page) upsertSynced(db, q);
                if (reconcile) reconcile(db, page, isFirstPage, isLastPage);
                trim(db);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
            return null;
        });
    }

    private void upsertSynced(SQLiteDatabase db, Question q) {
        if (q == null || q.getId() == null || q.getId().isEmpty()) return;

        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COL_Q_ID, q.getId());
        v.put(DatabaseHelper.COL_Q_USER_ID, q.getUserId());
        v.put(DatabaseHelper.COL_Q_MAJOR_ID, q.getMajorId());
        v.put(DatabaseHelper.COL_Q_TITLE, q.getTitle());
        v.put(DatabaseHelper.COL_Q_CONTENT, q.getContent());
        v.put(DatabaseHelper.COL_Q_IMAGE_URL, q.getImageUrl());
        v.put(DatabaseHelper.COL_Q_STATUS, q.getStatus() != null ? q.getStatus() : Question.STATUS_VISIBLE);
        v.put(DatabaseHelper.COL_Q_LIKE_COUNT, q.getLikeCount());
        v.put(DatabaseHelper.COL_Q_ANSWER_COUNT, q.getAnswerCount());
        v.put(DatabaseHelper.COL_Q_CREATED_AT, q.getCreatedAt());
        v.put(DatabaseHelper.COL_Q_UPDATED_AT, q.getUpdatedAt());
        v.put(DatabaseHelper.COL_Q_SYNC_STATUS, DatabaseHelper.SYNC_STATUS_SYNCED);
        v.put(DatabaseHelper.COL_Q_AUTHOR_NAME, q.getUserName());
        v.put(DatabaseHelper.COL_Q_AUTHOR_AVATAR, q.getUserAvatarUrl());
        v.put(DatabaseHelper.COL_Q_CATEGORY, q.getCategory());

        // Cập nhật nếu dòng đã có và đang SYNCED; nếu dòng là PENDING_* thì update = 0 và insert bị IGNORE
        int rows = db.update(DatabaseHelper.TABLE_QUESTION, v,
                DatabaseHelper.COL_Q_ID + "=? AND IFNULL(" + DatabaseHelper.COL_Q_SYNC_STATUS + ",'"
                        + DatabaseHelper.SYNC_STATUS_SYNCED + "')=?",
                new String[]{q.getId(), DatabaseHelper.SYNC_STATUS_SYNCED});
        if (rows == 0) {
            db.insertWithOnConflict(DatabaseHelper.TABLE_QUESTION, null, v, SQLiteDatabase.CONFLICT_IGNORE);
        }
    }

    private void reconcile(SQLiteDatabase db, List<Question> page, boolean isFirstPage, boolean isLastPage) {
        String syncedCond = "IFNULL(" + DatabaseHelper.COL_Q_SYNC_STATUS + ",'"
                + DatabaseHelper.SYNC_STATUS_SYNCED + "')=?";

        if (page.isEmpty()) {
            if (isFirstPage) {   // server không còn câu hỏi nào
                db.delete(DatabaseHelper.TABLE_QUESTION, syncedCond,
                        new String[]{DatabaseHelper.SYNC_STATUS_SYNCED});
            }
            return;
        }

        // Cận chặt (> và <) để không xóa nhầm câu hỏi trùng createdAt nằm ở trang kề bên
        long newest = page.get(0).getCreatedAt();
        long oldest = page.get(page.size() - 1).getCreatedAt();
        long hi = isFirstPage ? Long.MAX_VALUE : newest;
        long lo = isLastPage ? -1 : oldest;

        StringBuilder placeholders = new StringBuilder();
        List<String> args = new ArrayList<>();
        args.add(DatabaseHelper.SYNC_STATUS_SYNCED);
        args.add(String.valueOf(lo));
        args.add(String.valueOf(hi));
        for (Question q : page) {
            if (q.getId() == null) continue;
            if (placeholders.length() > 0) placeholders.append(',');
            placeholders.append('?');
            args.add(q.getId());
        }
        if (placeholders.length() == 0) return;

        db.delete(DatabaseHelper.TABLE_QUESTION,
                syncedCond
                        + " AND " + DatabaseHelper.COL_Q_CREATED_AT + ">?"
                        + " AND " + DatabaseHelper.COL_Q_CREATED_AT + "<?"
                        + " AND " + DatabaseHelper.COL_Q_ID + " NOT IN (" + placeholders + ")",
                args.toArray(new String[0]));
    }

    /** Giữ cache gọn: chỉ giữ MAX_CACHED dòng mới nhất, không bao giờ xóa dòng PENDING_*. */
    private void trim(SQLiteDatabase db) {
        db.execSQL("DELETE FROM " + DatabaseHelper.TABLE_QUESTION
                + " WHERE IFNULL(" + DatabaseHelper.COL_Q_SYNC_STATUS + ",'" + DatabaseHelper.SYNC_STATUS_SYNCED
                + "')='" + DatabaseHelper.SYNC_STATUS_SYNCED + "'"
                + " AND " + DatabaseHelper.COL_Q_ID + " NOT IN (SELECT " + DatabaseHelper.COL_Q_ID
                + " FROM " + DatabaseHelper.TABLE_QUESTION
                + " ORDER BY " + DatabaseHelper.COL_Q_CREATED_AT + " DESC LIMIT " + MAX_CACHED + ")");
    }
}