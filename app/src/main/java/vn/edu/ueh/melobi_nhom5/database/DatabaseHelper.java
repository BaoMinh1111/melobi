package vn.edu.ueh.melobi_nhom5.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    /* Khai báo Constant -> Tránh sai chính tả */
    private static final String DATABASE_NAME = "melobi_offline.db";
    private static final int DATABASE_VERSION = 2;

    public static final String TABLE_QUESTION = "Question";
    public static final String TABLE_ANSWER = "Answer";
    public static final String TABLE_USER_CACHE = "UserCache";

    // Các trạng thái đồng bộ
    public static final String SYNC_STATUS_SYNCED = "SYNCED";
    public static final String SYNC_STATUS_PENDING_ADD = "PENDING_ADD";
    public static final String SYNC_STATUS_PENDING_UPDATE = "PENDING_UPDATE";
    public static final String SYNC_STATUS_PENDING_DELETE = "PENDING_DELETE";

    // Cột của bảng QUESTION
    public static final String COL_Q_ID = "questionId";
    public static final String COL_Q_USER_ID = "userId";
    public static final String COL_Q_MAJOR_ID = "majorId";
    public static final String COL_Q_TITLE = "title";
    public static final String COL_Q_CONTENT = "content";
    public static final String COL_Q_IMAGE_URL = "imageUrl";
    public static final String COL_Q_STATUS = "status";
    public static final String COL_Q_LIKE_COUNT = "likeCount";      // Theo yêu cầu đồ án
    public static final String COL_Q_ANSWER_COUNT = "answerCount";
    public static final String COL_Q_CREATED_AT = "createdAt";
    public static final String COL_Q_UPDATED_AT = "updatedAt";
    public static final String COL_Q_SYNC_STATUS = "syncStatus";
    public static final String COL_Q_AUTHOR_NAME = "authorName";
    public static final String COL_Q_AUTHOR_AVATAR = "authorAvatarUrl";

    // Cột của bảng ANSWER
    public static final String COL_A_ID = "answerId";
    public static final String COL_A_QUESTION_ID = "questionId";
    public static final String COL_A_USER_ID = "userId";
    public static final String COL_A_CONTENT = "content";
    public static final String COL_A_STATUS = "status";
    public static final String COL_A_LIKE_COUNT = "likeCount";      // Tiêu chí xếp hạng câu trả lời
    public static final String COL_A_COMMENT_COUNT = "commentCount";
    public static final String COL_A_CREATED_AT = "createdAt";
    public static final String COL_A_SYNC_STATUS = "syncStatus";
    public static final String COL_A_AUTHOR_NAME = "authorName";
    public static final String COL_A_AUTHOR_AVATAR = "authorAvatarUrl";

    // Cột bảng USER CACHE
    public static final String COL_U_ID = "userId";
    public static final String COL_U_NAME = "fullName";
    public static final String COL_U_AVATAR = "avatarUrl";

    /* Singleton */
    private static DatabaseHelper instance;

    private DatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context);
        }
        return instance;
    }

    /* Khởi tạo cấu trúc bảng (Chỉ chạy 1 lần duy nhất khi file .db được tạo) */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Câu lệnh SQL tạo bảng Question
        String CREATE_QUESTION_TABLE = "CREATE TABLE " + TABLE_QUESTION + " ("
                + COL_Q_ID + " TEXT PRIMARY KEY, "
                + COL_Q_USER_ID + " TEXT, "
                + COL_Q_MAJOR_ID + " TEXT, "
                + COL_Q_TITLE + " TEXT, "
                + COL_Q_CONTENT + " TEXT, "
                + COL_Q_IMAGE_URL + " TEXT, "
                + COL_Q_STATUS + " TEXT, "
                + COL_Q_LIKE_COUNT + " INTEGER DEFAULT 0, "
                + COL_Q_ANSWER_COUNT + " INTEGER DEFAULT 0, "
                + COL_Q_CREATED_AT + " INTEGER, "
                + COL_Q_UPDATED_AT + " INTEGER, "
                + COL_Q_SYNC_STATUS + " TEXT DEFAULT '" + SYNC_STATUS_SYNCED + "', "
                + COL_Q_AUTHOR_NAME + " TEXT, "
                + COL_Q_AUTHOR_AVATAR + " TEXT)";;

        // Câu lệnh SQL tạo bảng Answer
        String CREATE_ANSWER_TABLE = "CREATE TABLE " + TABLE_ANSWER + " ("
                + COL_A_ID + " TEXT PRIMARY KEY, "
                + COL_A_QUESTION_ID + " TEXT, "
                + COL_A_USER_ID + " TEXT, "
                + COL_A_CONTENT + " TEXT, "
                + COL_A_STATUS + " TEXT, "
                + COL_A_LIKE_COUNT + " INTEGER DEFAULT 0, "
                + COL_A_COMMENT_COUNT + " INTEGER DEFAULT 0, "
                + COL_A_CREATED_AT + " INTEGER, "
                + COL_A_SYNC_STATUS + " TEXT DEFAULT '" + SYNC_STATUS_SYNCED + "', "
                + COL_A_AUTHOR_NAME + " TEXT, "
                + COL_A_AUTHOR_AVATAR + " TEXT)";;

        // Câu lệnh tạo bảng USER CACHE RÚT GỌN
        String CREATE_USER_CACHE_TABLE = "CREATE TABLE " + TABLE_USER_CACHE + " ("
                + COL_U_ID + " TEXT PRIMARY KEY, "
                + COL_U_NAME + " TEXT, "
                + COL_U_AVATAR + " TEXT)";

        // Thực thi lệnh tạo bảng
        db.execSQL(CREATE_QUESTION_TABLE);
        db.execSQL(CREATE_ANSWER_TABLE);
        db.execSQL(CREATE_USER_CACHE_TABLE);

        // Tạo INDEX cho cột SYNC_STATUS
        db.execSQL("CREATE INDEX idx_question_sync ON " + TABLE_QUESTION + "(" + COL_Q_SYNC_STATUS + ")");
        db.execSQL("CREATE INDEX idx_answer_sync ON " + TABLE_ANSWER + "(" + COL_A_SYNC_STATUS + ")");
    }

    /* Hàm xử lý khi nâng cấp phiên bản Database */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Upgrade từ Version 1 (bảng cũ) lên Version 2 (bảng mới)
        if (oldVersion < 2) {
            try {
                // Sử dụng ALTER TABLE để thêm cột vào bảng cũ mà KHÔNG làm mất dữ liệu hiện tại
                db.execSQL("ALTER TABLE " + TABLE_QUESTION + " ADD COLUMN " + COL_Q_SYNC_STATUS + " TEXT DEFAULT '" + SYNC_STATUS_SYNCED + "'");
                db.execSQL("ALTER TABLE " + TABLE_QUESTION + " ADD COLUMN " + COL_Q_AUTHOR_NAME + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_QUESTION + " ADD COLUMN " + COL_Q_AUTHOR_AVATAR + " TEXT");

                db.execSQL("ALTER TABLE " + TABLE_ANSWER + " ADD COLUMN " + COL_A_SYNC_STATUS + " TEXT DEFAULT '" + SYNC_STATUS_SYNCED + "'");
                db.execSQL("ALTER TABLE " + TABLE_ANSWER + " ADD COLUMN " + COL_A_AUTHOR_NAME + " TEXT");
                db.execSQL("ALTER TABLE " + TABLE_ANSWER + " ADD COLUMN " + COL_A_AUTHOR_AVATAR + " TEXT");

                // Tạo bảng UserCache vì version 1 chưa có
                String CREATE_USER_CACHE_TABLE = "CREATE TABLE IF NOT EXISTS " + TABLE_USER_CACHE + " ("
                        + COL_U_ID + " TEXT PRIMARY KEY, "
                        + COL_U_NAME + " TEXT, "
                        + COL_U_AVATAR + " TEXT)";
                db.execSQL(CREATE_USER_CACHE_TABLE);

                // Thêm Index
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_question_sync ON " + TABLE_QUESTION + "(" + COL_Q_SYNC_STATUS + ")");
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_answer_sync ON " + TABLE_ANSWER + "(" + COL_A_SYNC_STATUS + ")");

            } catch (Exception e) {
                // Bắt lỗi nếu lỡ cột đã tồn tại
                e.printStackTrace();
            }
        }
    }

}
