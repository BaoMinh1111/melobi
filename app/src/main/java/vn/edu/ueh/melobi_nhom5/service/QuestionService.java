package vn.edu.ueh.melobi_nhom5.service;

import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.util.Log;

import vn.edu.ueh.melobi_nhom5.entity.Like;
import vn.edu.ueh.melobi_nhom5.model.Question;
import vn.edu.ueh.melobi_nhom5.entity.Notification;
import vn.edu.ueh.melobi_nhom5.database.QuestionLocalStore;
import vn.edu.ueh.melobi_nhom5.model.QuestionPage;
import vn.edu.ueh.melobi_nhom5.util.NetworkUtils;
public class QuestionService {
    private static final String COL_QUESTIONS = "questions";
    private static final String COL_LIKES = "likes";
    private static final String TAG = "QuestionService";
   // null nếu tạo service không có Context -> không có offline

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final QuestionLocalStore localStore;
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
        question.setStatus(Question.STATUS_VISIBLE);
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
     * hoặc không ở trạng thái VISIBLE (đã ẩn / đã xóa mềm) - màn hình gọi tự xử lý trường hợp này.
     * Task thất bại (onFailure) chỉ khi lỗi mạng/quyền truy cập.
     */
    public Task<Question> getQuestionDetail(String questionId) {
        return db.collection(COL_QUESTIONS).document(questionId).get().continueWith(task -> {
            DocumentSnapshot snapshot = task.getResult();   // ném lại exception nếu tải lỗi
            if (snapshot == null || !snapshot.exists()) return null;
            Question q = toQuestion(snapshot);
            if (q == null || !Question.STATUS_VISIBLE.equals(q.getStatus())) return null;
            return q;
        });
    }

    // ---------- Danh sách / lọc ----------

    // ---------- Feed (có đọc SQLite khi mất mạng) ----------

    /** Câu truy vấn Firestore cho feed. category = null: tất cả ngành.
     *  Con trỏ: ưu tiên DocumentSnapshot; nếu chỉ có lastCreatedAt (trang trước đến từ SQLite) thì dùng giá trị createdAt.
     *  Cần composite index (status + createdAt) và (status + category + createdAt). */
    private Query feedQuery(String category, DocumentSnapshot lastVisible, long lastCreatedAt, int pageSize) {
        Query query = db.collection(COL_QUESTIONS).whereEqualTo("status", Question.STATUS_VISIBLE);
        if (category != null) query = query.whereEqualTo("category", category);
        query = query.orderBy("createdAt", Query.Direction.DESCENDING).limit(pageSize);
        if (lastVisible != null) query = query.startAfter(lastVisible);
        else if (lastCreatedAt > 0) query = query.startAfter(lastCreatedAt);
        return query;
    }

    /** Feed tất cả ngành. Lần đầu truyền lastVisible = null và lastCreatedAt = 0. */
    public Task<QuestionPage> getQuestions(DocumentSnapshot lastVisible, long lastCreatedAt, int pageSize) {
        return loadFeed(null, lastVisible, lastCreatedAt, pageSize);
    }

    /** Feed lọc theo tên ngành (field "category"). */
    public Task<QuestionPage> filterByCategory(String category, DocumentSnapshot lastVisible,
                                               long lastCreatedAt, int pageSize) {
        return loadFeed(category, lastVisible, lastCreatedAt, pageSize);
    }

    private Task<QuestionPage> loadFeed(String category, DocumentSnapshot lastVisible,
                                        long lastCreatedAt, int pageSize) {
        final boolean isFirstPage = lastVisible == null && lastCreatedAt <= 0;

        // Mất mạng: đọc thẳng SQLite
        if (localStore != null && !NetworkUtils.isOnline()) {
            return readLocal(category, lastCreatedAt, pageSize);
        }

        return feedQuery(category, lastVisible, lastCreatedAt, pageSize).get()
                .continueWithTask(task -> {
                    if (task.isSuccessful()) {
                        QuestionPage page = toPage(task.getResult(), pageSize);
                        // Dữ liệu thật từ server thì lưu lại cho lần offline sau
                        if (localStore != null && !page.fromCache) {
                            localStore.saveFeedPage(page.questions, category == null, isFirstPage, page.isLastPage)
                                    .addOnFailureListener(e -> Log.w(TAG, "Không lưu được cache SQLite", e));
                        }
                        return Tasks.forResult(page);
                    }
                    Exception e = task.getException();
                    // Có mạng giả (Wi-Fi không ra Internet): quay về SQLite. Lỗi khác (thiếu index, quyền) thì báo lên.
                    if (localStore != null && isConnectivityError(e)) {
                        return readLocal(category, lastCreatedAt, pageSize);
                    }
                    return Tasks.<QuestionPage>forException(e != null ? e : new Exception("Lỗi không xác định"));
                });
    }

    private Task<QuestionPage> readLocal(String category, long lastCreatedAt, int pageSize) {
        return localStore.getFeedPage(category, lastCreatedAt, pageSize).continueWith(task -> {
            List<Question> list = task.getResult();
            long last = list.isEmpty() ? 0 : list.get(list.size() - 1).getCreatedAt();
            return new QuestionPage(list, null, last, list.size() < pageSize, true);
        });
    }

    private static QuestionPage toPage(QuerySnapshot snap, int pageSize) {
        List<DocumentSnapshot> docs = snap.getDocuments();
        List<Question> list = new ArrayList<>();
        for (DocumentSnapshot d : docs) {
            Question q = toQuestion(d);
            if (q != null) list.add(q);
        }
        DocumentSnapshot lastDoc = docs.isEmpty() ? null : docs.get(docs.size() - 1);
        long lastCreated = list.isEmpty() ? 0 : list.get(list.size() - 1).getCreatedAt();
        return new QuestionPage(list, lastDoc, lastCreated, docs.size() < pageSize,
                snap.getMetadata().isFromCache());
    }

    private static boolean isConnectivityError(Exception e) {
        if (!(e instanceof FirebaseFirestoreException)) return false;
        FirebaseFirestoreException.Code c = ((FirebaseFirestoreException) e).getCode();
        return c == FirebaseFirestoreException.Code.UNAVAILABLE
                || c == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED;
    }

    /** Cần composite index (status + majorId + createdAt). */
    public Task<QuerySnapshot> filterByMajor(String majorId, DocumentSnapshot lastVisible, int pageSize) {
        if (majorId == null || majorId.isEmpty()) return getQuestions(lastVisible, pageSize);
        Query query = db.collection(COL_QUESTIONS)
                .whereEqualTo("status", Question.STATUS_VISIBLE)
                .whereEqualTo("majorId", majorId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageSize);
        if (lastVisible != null) query = query.startAfter(lastVisible);
        return query.get();
    }
    /**
     * Tìm câu hỏi theo tiền tố tiêu đề (không phân biệt hoa/thường), chỉ lấy VISIBLE.
     * Firestore không có full-text search nên đây là tìm "bắt đầu bằng".
     * Kết quả xếp theo titleLower (không phải theo thời gian).
     * Cần composite index (status + titleLower).
     * Từ khóa rỗng thì trả về feed bình thường.
     */
    public Task<QuestionPage> searchQuestions(String keyword, DocumentSnapshot lastVisible, int pageSize) {
        String q = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        if (q.isEmpty()) return getQuestions(lastVisible, 0, pageSize);
        if (localStore != null && !NetworkUtils.isOnline()) {
            return Tasks.forException(new IllegalStateException("Tìm kiếm cần kết nối mạng"));
        }

        Query query = db.collection(COL_QUESTIONS)
                .whereEqualTo("status", Question.STATUS_VISIBLE)
                .whereGreaterThanOrEqualTo("titleLower", q)
                .whereLessThanOrEqualTo("titleLower", q + "\uf8ff")
                .orderBy("titleLower")
                .limit(pageSize);
        if (lastVisible != null) query = query.startAfter(lastVisible);
        return query.get().continueWith(task -> toPage(task.getResult(), pageSize));
    }
    // ---------- Like ----------

    /** Trả về true nếu like mới được tạo, false nếu đã like rồi. */
    public Task<Boolean> likeQuestion(String questionId, String ownerId, String userId) {
        if (isBlank(questionId) || isBlank(userId)) {
            return Tasks.forException(new IllegalArgumentException("questionId/userId không hợp lệ"));
        }
        DocumentReference likeRef = db.collection(COL_LIKES)
                .document(Like.buildId(userId, Like.TYPE_QUESTION, questionId));
        DocumentReference questionRef = db.collection(COL_QUESTIONS).document(questionId);

        return db.runTransaction(tx -> {
            // Phải đọc hết trước khi ghi
            DocumentSnapshot q = tx.get(questionRef);
            DocumentSnapshot like = tx.get(likeRef);

            if (!q.exists() || !Question.STATUS_VISIBLE.equals(q.getString("status"))) {
                throw new FirebaseFirestoreException("Câu hỏi không tồn tại hoặc đã bị ẩn",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            if (like.exists()) return false;

            tx.set(likeRef, new Like(userId, questionId, Like.TYPE_QUESTION));
            tx.update(questionRef, "likeCount", FieldValue.increment(1));
            return true;
        }).addOnSuccessListener(created -> {
            if (Boolean.TRUE.equals(created)) {
                notificationService.createNotification(
                        ownerId, userId, Notification.TYPE_LIKE, questionId, Like.TYPE_QUESTION);
            }
        });
    }

    /** Trả về true nếu bỏ like thành công, false nếu trước đó chưa like. */
    public Task<Boolean> unlikeQuestion(String questionId, String userId) {
        if (isBlank(questionId) || isBlank(userId)) {
            return Tasks.forException(new IllegalArgumentException("questionId/userId không hợp lệ"));
        }
        DocumentReference likeRef = db.collection(COL_LIKES)
                .document(Like.buildId(userId, Like.TYPE_QUESTION, questionId));
        DocumentReference questionRef = db.collection(COL_QUESTIONS).document(questionId);

        return db.runTransaction(tx -> {
            DocumentSnapshot like = tx.get(likeRef);
            DocumentSnapshot q = tx.get(questionRef);
            if (!like.exists()) return false;

            tx.delete(likeRef);
            if (q.exists()) {   // câu hỏi đã bị xóa cứng thì chỉ dọn like
                Long current = q.getLong("likeCount");
                long next = Math.max(0, (current != null ? current : 0) - 1);
                tx.update(questionRef, "likeCount", next);
            }
            return true;
        });
    }

    public Task<Boolean> isQuestionLikedByUser(String questionId, String userId) {
        if (isBlank(questionId) || isBlank(userId)) return Tasks.forResult(false);
        return db.collection(COL_LIKES)
                .document(Like.buildId(userId, Like.TYPE_QUESTION, questionId))
                .get()
                .continueWith(task -> task.getResult().exists());
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ---------- Các hàm service khác gọi sang ----------

    /** Tăng số câu trả lời. Thất bại (NOT_FOUND) nếu câu hỏi không tồn tại. */
    public Task<Void> increaseAnswerCount(String questionId) {
        return db.collection(COL_QUESTIONS).document(questionId)
                .update("answerCount", FieldValue.increment(1));
    }

    /** Giảm số câu trả lời, không bao giờ xuống dưới 0 (dùng transaction để kẹp giá trị). */
    public Task<Void> decreaseAnswerCount(String questionId) {
        DocumentReference ref = db.collection(COL_QUESTIONS).document(questionId);
        return db.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(ref);
            if (!snap.exists()) {
                throw new FirebaseFirestoreException("Câu hỏi không tồn tại",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            Long current = snap.getLong("answerCount");
            long next = Math.max(0, (current != null ? current : 0) - 1);
            tx.update(ref, "answerCount", next);
            return null;
        });
    }
    /** Ẩn câu hỏi (ModerationService gọi). Idempotent: ẩn rồi thì không làm gì thêm.
     *  Không cho ẩn câu hỏi đã bị xóa mềm. */
    public Task<Void> hideQuestion(String questionId) {
        DocumentReference ref = db.collection(COL_QUESTIONS).document(questionId);
        return db.runTransaction(tx -> {
            DocumentSnapshot snap = tx.get(ref);
            if (!snap.exists()) {
                throw new FirebaseFirestoreException("Câu hỏi không tồn tại",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            String status = snap.getString("status");
            if (Question.STATUS_DELETED.equals(status)) {
                throw new FirebaseFirestoreException("Câu hỏi đã bị xóa",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            if (Question.STATUS_HIDDEN.equals(status)) return null;   // đã ẩn rồi
            tx.update(ref, "status", Question.STATUS_HIDDEN,
                    "updatedAt", System.currentTimeMillis());
            return null;
        });
    }
    /** Tiện cho moderation: bỏ ẩn câu hỏi. */
    public Task<Void> unhideQuestion(String questionId) {
        return db.collection(COL_QUESTIONS).document(questionId)
                .update("status", Question.STATUS_VISIBLE,
                        "updatedAt", System.currentTimeMillis());
    }
    public QuestionService() {
        this.localStore = null;
    }

    public QuestionService(Context context) {
        this.localStore = context != null ? new QuestionLocalStore(context.getApplicationContext()) : null;
    }
}