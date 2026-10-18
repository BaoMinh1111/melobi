package vn.edu.ueh.melobi_nhom5.model;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.List;

/**
 * Một trang câu hỏi của feed, dùng chung cho nguồn Firestore và nguồn SQLite (ngoại tuyến).
 * Đặt trong package model.
 */
public class QuestionPage {
    public final List<Question> questions;
    /** Con trỏ trang tiếp theo khi lấy từ Firestore; null nếu trang đến từ SQLite hoặc trang rỗng. */
    public final DocumentSnapshot lastDoc;
    /** createdAt của câu hỏi cuối trang (0 nếu rỗng). Dùng làm con trỏ cho SQLite. */
    public final long lastCreatedAt;
    public final boolean isLastPage;
    /** true: dữ liệu đến từ SQLite hoặc bộ nhớ đệm của Firestore (có thể đã cũ). */
    public final boolean fromCache;

    public QuestionPage(List<Question> questions, DocumentSnapshot lastDoc, long lastCreatedAt,
                        boolean isLastPage, boolean fromCache) {
        this.questions = questions;
        this.lastDoc = lastDoc;
        this.lastCreatedAt = lastCreatedAt;
        this.isLastPage = isLastPage;
        this.fromCache = fromCache;
    }
}