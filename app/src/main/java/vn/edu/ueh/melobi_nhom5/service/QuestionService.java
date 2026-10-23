package vn.edu.ueh.melobi_nhom5.service;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.edu.ueh.melobi_nhom5.entity.Like;
import vn.edu.ueh.melobi_nhom5.model.Question;

public class QuestionService {
    private static final String COL_QUESTIONS = "questions";
    private static final String COL_LIKES = "likes";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final NotificationService notificationService = new NotificationService();

    // ---------- CRUD ----------

    /**
     * Tạo câu hỏi mới. Service tự chuẩn hóa: trim tiêu đề/nội dung, điền titleLower, status,
     * likeCount, answerCount, createdAt/updatedAt, và lưu luôn id của document vào field "id"
     * (MainActivity/QuestionAdapter đọc field này).
     * imageUrl (nếu có) phải được upload trước rồi gán vào question.
     * Task trả về DocumentReference của câu hỏi mới (ref.getId() = id câu hỏi).
     */
    public Task<DocumentReference> createQuestion(Question question) {
        String title = question.getTitle() != null ? question.getTitle().trim() : "";
        String content = question.getContent() != null ? question.getContent().trim() : "";
        long now = System.currentTimeMillis();

        question.setTitle(title);
        question.setContent(content);
        question.setTitleLower(title.toLowerCase(Locale.ROOT));
        question.setStatus(Question.STATUS_ACTIVE);
        question.setLikeCount(0);
        question.setAnswerCount(0);
        question.setCreatedAt(now);
        question.setUpdatedAt(now);

        DocumentReference ref = db.collection(COL_QUESTIONS).document();   // sinh id trước
        question.setId(ref.getId());
        return ref.set(question).continueWith(task -> {
            task.getResult();               // ném lại exception nếu ghi thất bại
            return ref;
        });
    }

    public Task<Void> updateQuestion(String questionId, String title, String content, String imageUrl) {
        String cleanTitle = title != null ? title.trim() : "";
        Map<String, Object> data = new HashMap<>();
        data.put("title", cleanTitle);
        data.put("titleLower", cleanTitle.toLowerCase(Locale.ROOT));
        data.put("content", content);
        data.put("imageUrl", imageUrl);
        data.put("updatedAt", System.currentTimeMillis());
        return db.collection(COL_QUESTIONS).document(questionId).update(data);
    }

    /** Gọi sau khi kiểm tra question.getUserId() == người đang đăng nhập.
     *  TODO: xóa luôn Answer/Comment liên quan (composition) - phối hợp bạn làm Answer. */
    public Task<Void> deleteQuestion(String questionId) {
        return db.collection(COL_QUESTIONS).document(questionId).delete();
    }

    /**
     * Chuyển document thành Question và điền id từ document ID (phòng khi document thiếu field "id").
     * Trả về null nếu document không chuyển được.
     */
    public static Question toQuestion(DocumentSnapshot doc) {
        Question q = doc.toObject(Question.class);
        if (q != null && (q.getId() == null || q.getId().isEmpty())) q.setId(doc.getId());
        return q;
    }

    /**
     * Lấy chi tiết một câu hỏi. Kết quả của Task là null nếu câu hỏi không tồn tại
     * hoặc không ở trạng thái ACTIVE (đã ẩn / đã xóa mềm) - màn hình gọi tự xử lý trường hợp này.
     * Task thất bại (onFailure) chỉ khi lỗi mạng/quyền truy cập.
     */
    public Task<Question> getQuestionDetail(String questionId) {
        return db.collection(COL_QUESTIONS).document(questionId).get().continueWith(task -> {
            DocumentSnapshot snapshot = task.getResult();   // ném lại exception nếu tải lỗi
            if (snapshot == null || !snapshot.exists()) return null;
            Question q = toQuestion(snapshot);
            if (q == null || !Question.STATUS_ACTIVE.equals(q.getStatus())) return null;
            return q;
        });
    }

    // ---------- Danh sách / lọc ----------

    /** Feed: câu hỏi ACTIVE, mới nhất trước. Phân trang: lần đầu truyền lastVisible = null,
     *  các lần sau truyền document cuối của trang trước (querySnapshot.getDocuments() phần tử cuối).
     *  Trang cuối khi số document trả về < pageSize. Dùng toQuestion(doc) để chuyển thành Question.
     *  Cần composite index (status + createdAt). Lưu ý: document chưa có field status
     *  sẽ không xuất hiện ở truy vấn này. */
    public Task<QuerySnapshot> getQuestions(DocumentSnapshot lastVisible, int pageSize) {
        Query query = db.collection(COL_QUESTIONS)
                .whereEqualTo("status", Question.STATUS_ACTIVE)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageSize);
        if (lastVisible != null) query = query.startAfter(lastVisible);
        return query.get();
        // TODO: dùng NetworkUtils.isOnline() để chọn Firebase hay SQLite khi offline
    }

    /** Lọc theo tên ngành (field "category" - feed đang dùng, có cả ở dữ liệu mẫu).
     *  Cần composite index (status + category + createdAt). */
    public Task<QuerySnapshot> filterByCategory(String category, DocumentSnapshot lastVisible, int pageSize) {
        Query query = db.collection(COL_QUESTIONS)
                .whereEqualTo("status", Question.STATUS_ACTIVE)
                .whereEqualTo("category", category)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageSize);
        if (lastVisible != null) query = query.startAfter(lastVisible);
        return query.get();
    }

    /** Tìm câu hỏi liên quan từ khóa trong tiêu đề, nội dung hoặc chuyên ngành. */
    public Task<List<Question>> searchQuestions(String keyword) {
        String kw = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        return db.collection(COL_QUESTIONS)
                .get()
                .continueWith(task -> {
                    QuerySnapshot snapshot = task.getResult();
                    List<Question> matched = new ArrayList<>();
                    if (snapshot == null) {
                        return matched;
                    }
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        Question question = toQuestion(doc);
                        if (question == null || !Question.STATUS_ACTIVE.equals(question.getStatus())) {
                            continue;
                        }
                        if (kw.isEmpty() || matchesKeyword(question, kw)) {
                            matched.add(question);
                        }
                    }
                    Collections.sort(matched, (left, right) -> Long.compare(right.getCreatedAt(), left.getCreatedAt()));
                    return matched;
                });
    }

    private boolean matchesKeyword(Question question, String keyword) {
        return containsIgnoreCase(question.getTitle(), keyword)
                || containsIgnoreCase(question.getTitleLower(), keyword)
                || containsIgnoreCase(question.getContent(), keyword)
                || containsIgnoreCase(question.getCategory(), keyword)
                || containsIgnoreCase(question.getMajorId(), keyword);
    }

    private boolean containsIgnoreCase(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    /** Cần composite index (status + majorId + createdAt). */
    public Task<QuerySnapshot> filterByMajor(String majorId, DocumentSnapshot lastVisible, int pageSize) {
        Query query = db.collection(COL_QUESTIONS)
                .whereEqualTo("status", Question.STATUS_ACTIVE)
                .whereEqualTo("majorId", majorId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageSize);
        if (lastVisible != null) query = query.startAfter(lastVisible);
        return query.get();
    }

    // ---------- Like ----------

    /** Trả về true nếu like mới được tạo, false nếu trước đó đã like rồi. */
    public Task<Boolean> likeQuestion(String questionId, String ownerId, String userId) {
        DocumentReference likeRef = db.collection(COL_LIKES)
                .document(Like.buildId(userId, Like.TYPE_QUESTION, questionId));
        DocumentReference questionRef = db.collection(COL_QUESTIONS).document(questionId);

        return db.runTransaction(tx -> {
            if (tx.get(likeRef).exists()) return false;          // đã like rồi
            tx.set(likeRef, new Like(userId, questionId, Like.TYPE_QUESTION));
            tx.update(questionRef, "likeCount", FieldValue.increment(1));
            return true;
        }).addOnSuccessListener(created -> {
            if (created) {
                notificationService.createNotification(
                        ownerId, userId, "LIKE", questionId, Like.TYPE_QUESTION);
            }
        });
    }

    /** Trả về true nếu bỏ like thành công, false nếu trước đó chưa like. */
    public Task<Boolean> unlikeQuestion(String questionId, String userId) {
        DocumentReference likeRef = db.collection(COL_LIKES)
                .document(Like.buildId(userId, Like.TYPE_QUESTION, questionId));
        DocumentReference questionRef = db.collection(COL_QUESTIONS).document(questionId);

        return db.runTransaction(tx -> {
            if (!tx.get(likeRef).exists()) return false;
            tx.delete(likeRef);
            tx.update(questionRef, "likeCount", FieldValue.increment(-1));
            return true;
        });
    }

    public Task<Boolean> isQuestionLikedByUser(String questionId, String userId) {
        return db.collection(COL_LIKES)
                .document(Like.buildId(userId, Like.TYPE_QUESTION, questionId))
                .get()
                .continueWith(task -> task.getResult().exists());
    }

    // ---------- Các hàm service khác gọi sang ----------

    public Task<Void> increaseAnswerCount(String questionId) {   // AnswerService gọi
        return db.collection(COL_QUESTIONS).document(questionId)
                .update("answerCount", FieldValue.increment(1));
    }

    public Task<Void> decreaseAnswerCount(String questionId) {   // AnswerService gọi
        return db.collection(COL_QUESTIONS).document(questionId)
                .update("answerCount", FieldValue.increment(-1));
    }

    public Task<Void> hideQuestion(String questionId) {          // ModerationService gọi
        return db.collection(COL_QUESTIONS).document(questionId)
                .update("status", Question.STATUS_HIDDEN);
    }
}