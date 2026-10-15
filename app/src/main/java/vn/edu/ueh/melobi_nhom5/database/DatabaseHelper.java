package vn.edu.ueh.melobi_nhom5.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    /* Khai báo Constant -> Tránh sai chính tả */
    private static final String DATABASE_NAME = "melobi_offline.db";
    private static final int DATABASE_VERSION = 1; //

    private static final String TABLE_QUESTION = "Question";
    private static final String TABLE_ANSWER = "Answer";

    private static final String COLUMN_ID = "id";
    private static final String COLUMN_CONTENT = "content";
    private static final String COLUMN_LIKE_COUNT = "likeCount";
    private static final String COLUMN_CREATED_AT = "createdAt";

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
        // Lệnh tạo bảng Question
        String CREATE_QUESTION_TABLE = "CREATE TABLE " + TABLE_QUESTION + " (" + COLUMN_ID + " TEXT PRIMARY KEY, " + "userId TEXT, " + "major TEXT, " + COLUMN_CONTENT + " TEXT, " + COLUMN_LIKE_COUNT + " INTEGER DEFAULT 0, " + COLUMN_CREATED_AT + " INTEGER)";

        // Lệnh tạo bảng Answer
        String CREATE_ANSWER_TABLE = "CREATE TABLE " + TABLE_ANSWER + " (" + COLUMN_ID + " TEXT PRIMARY KEY, " + "questionId TEXT, " + "userId TEXT, " + COLUMN_CONTENT + " TEXT, " + COLUMN_LIKE_COUNT + " INTEGER DEFAULT 0, " + COLUMN_CREATED_AT + " INTEGER)";

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