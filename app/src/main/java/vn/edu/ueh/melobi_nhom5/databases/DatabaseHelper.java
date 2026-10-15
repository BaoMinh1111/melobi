package vn.edu.ueh.melobi_nhom5.databases;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    /* Khai báo Constant -> Tránh sai chính tả */
    private static final String DATABASE_NAME = "melobi_offline.db";
    private static final int DATABASE_VERSION = 1; //

    public static final String TABLE_QUESTION = "Question";
    public static final String TABLE_ANSWER = "Answer";

    // Cột của bảng Question
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

    // Cột của bảng Answer
    public static final String COL_A_ID = "answerId";
    public static final String COL_A_QUESTION_ID = "questionId";
    public static final String COL_A_USER_ID = "userId";
    public static final String COL_A_CONTENT = "content";
    public static final String COL_A_STATUS = "status";
    public static final String COL_A_LIKE_COUNT = "likeCount";      // Tiêu chí xếp hạng câu trả lời
    public static final String COL_A_COMMENT_COUNT = "commentCount";
    public static final String COL_A_CREATED_AT = "createdAt";

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
                + COL_Q_UPDATED_AT + " INTEGER)";

        // Câu lệnh SQL tạo bảng Answer
        String CREATE_ANSWER_TABLE = "CREATE TABLE " + TABLE_ANSWER + " ("
                + COL_A_ID + " TEXT PRIMARY KEY, "
                + COL_A_QUESTION_ID + " TEXT, "
                + COL_A_USER_ID + " TEXT, "
                + COL_A_CONTENT + " TEXT, "
                + COL_A_STATUS + " TEXT, "
                + COL_A_LIKE_COUNT + " INTEGER DEFAULT 0, "
                + COL_A_COMMENT_COUNT + " INTEGER DEFAULT 0, "
                + COL_A_CREATED_AT + " INTEGER)";

        // Thực thi lệnh tạo bảng
        db.execSQL(CREATE_QUESTION_TABLE);
        db.execSQL(CREATE_ANSWER_TABLE);
    }

    /* Hàm xử lý khi nâng cấp phiên bản Database */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Xóa bảng cũ nêú tồn tại (Thực tế: Dùng ALTER TABLE để giữ data)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_QUESTION);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ANSWER);

        // Tạo lại cấu trúc mới
        onCreate(db);
    }

}
