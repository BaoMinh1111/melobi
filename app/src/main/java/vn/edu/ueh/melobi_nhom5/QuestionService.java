package vn.edu.ueh.melobi_nhom5;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class QuestionService {
    private static final String COL_QUESTIONS = "questions";
    private static final String COL_LIKES = "likes";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final NotificationService notificationService = new NotificationService();

    // ---------- CRUD ----------

    /** imageUrl (nếu có) phải được upload trước qua FirebaseStorageManager rồi gán vào question. */
    public Task<DocumentReference> createQuestion(Question question) {
        return db.collection(COL_QUESTIONS).add(question);
    }

    public Task<Void> updateQuestion(String questionId, String title, String content, String imageUrl) {
        Map<String, Object> data = new HashMap<>();
        data.put("title", title);
        data.put("content", content);
        data.put("imageUrl", imageUrl);
        data.put("updatedAt", FieldValue.serverTimestamp());
        return db.collection(COL_QUESTIONS).document(questionId).update(data);
    }

    /** Gọi sau khi kiểm tra question.getUserId() == người đang đăng nhập.
     *  TODO: xóa luôn Answer/Comment liên quan (composition) - phối hợp bạn làm Answer. */
    public Task<Void> deleteQuestion(String questionId) {
        return db.collection(COL_QUESTIONS).document(questionId).delete();
    }

    public Task<DocumentSnapshot> getQuestionDetail(String questionId) {
        // dùng: snapshot.toObject(Question.class)
        return db.collection(COL_QUESTIONS).document(questionId).get();
    }

    // ---------- Danh sách / lọc ----------

    /** Phân trang: lần đầu truyền lastVisible = null, các lần sau truyền document cuối của trang trước.
     *  Cần composite index (status + createdAt). */
    public Task<QuerySnapshot> getQuestions(DocumentSnapshot lastVisible, int pageSize) {
        Query query = db.collection(COL_QUESTIONS)
                .whereEqualTo("status", Question.STATUS_VISIBLE)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(pageSize);
        if (lastVisible != null) query = query.startAfter(lastVisible);
        return query.get();
        // TODO: dùng NetworkUtils.isOnline() để chọn Firebase hay SQLite khi offline
    }

    /** Cần composite index (status + majorId + createdAt). */
    public Task<QuerySnapshot> filterByMajor(String majorId, DocumentSnapshot lastVisible, int pageSize) {
        Query query = db.collection(COL_QUESTIONS)
                .whereEqualTo("status", Question.STATUS_VISIBLE)
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
/*1 số lỗi nhỏ như sau:
* updateQuestion xóa mất ảnh cũ. Nếu người dùng chỉ sửa chữ và truyền imageUrl = null, dòng data.put("imageUrl", imageUrl) sẽ ghi đè ảnh cũ thành null
-> Cách sửa: if (imageUrl != null) data.put("imageUrl", imageUrl);
* mâu thuẫn với uploadQuestionImage(questionId, file). Thiết kế cần questionId để đặt tên ảnh, nhưng add() chỉ có ID sau khi tạo xong.
-> Cách sửa: tạo ID trước, upload ảnh, rồi mới ghi câu hỏi.
* deleteQuestion để lại dữ liệu mồ côi. Firestore không tự xóa Answer, Comment, Like liên quan (khác với Composition trong sơ đồ lớp)
-> Cách sửa: 
- Xóa mềm: đổi status thành DELETED rồi lọc ra khi hiển thị. Đơn giản, không mất dữ liệu
- Xóa thật kèm xóa Answer, Comment bằng batch, phải phối hợp với C.
* thiếu searchQuestions(keyword)*/
