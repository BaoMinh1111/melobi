package vn.edu.ueh.melobi_nhom5.service;

import android.text.TextUtils;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.model.Answer;
import vn.edu.ueh.melobi_nhom5.repository.AnswerRepository;

public class AnswerService {
    private static final String COLLECTION_ANSWERS = "answers";
    private final AnswerRepository answerRepository;
    private final FirebaseFirestore db;

    public AnswerService() {
        this.answerRepository = new AnswerRepository();
        this.db = FirebaseFirestore.getInstance();
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public void createAnswer(String questionId, String userId, String userName, String userAvatarUrl, String content, Callback<Answer> callback) {
        Answer answer = new Answer();
        answer.setQuestionId(questionId);
        answer.setUserId(userId);
        answer.setUserName(TextUtils.isEmpty(userName) ? "Thành viên" : userName);
        answer.setUserAvatarUrl(userAvatarUrl != null ? userAvatarUrl : "");
        answer.setContent(content != null ? content.trim() : "");
        createAnswer(answer, callback);
    }

    public void createAnswer(Answer answer, Callback<Answer> callback) {
        if (answer == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Câu trả lời không hợp lệ"));
            return;
        }
        if (TextUtils.isEmpty(answer.getQuestionId())) {
            if (callback != null) callback.onError(new IllegalArgumentException("ID câu hỏi không hợp lệ"));
            return;
        }
        if (TextUtils.isEmpty(answer.getUserId())) {
            if (callback != null) callback.onError(new IllegalArgumentException("ID người dùng không hợp lệ"));
            return;
        }
        if (TextUtils.isEmpty(answer.getContent()) || answer.getContent().trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Nội dung câu trả lời không được để trống"));
            return;
        }

        answer.setContent(answer.getContent().trim());
        if (TextUtils.isEmpty(answer.getUserName())) {
            answer.setUserName("Thành viên");
        }
        if (answer.getUserAvatarUrl() == null) {
            answer.setUserAvatarUrl("");
        }

        answerRepository.addAnswer(answer, new AnswerRepository.Callback<Answer>() {
            @Override
            public void onSuccess(Answer result) {
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    public void getAnswersByQuestion(String questionId, Callback<List<Answer>> callback) {
        if (TextUtils.isEmpty(questionId)) {
            if (callback != null) callback.onError(new IllegalArgumentException("ID câu hỏi không hợp lệ"));
            return;
        }

        answerRepository.getAnswersByQuestionId(questionId, new AnswerRepository.Callback<List<Answer>>() {
            @Override
            public void onSuccess(List<Answer> result) {
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    /**
     * Thích một câu trả lời sử dụng Firestore Transaction để đảm bảo tính nhất quán dữ liệu.
     * Ngăn chặn việc tăng lượt thích nhiều lần bởi cùng một người dùng.
     */
    @SuppressWarnings("unchecked")
    public void likeAnswer(String answerId, String userId, Callback<Void> callback) {
        if (TextUtils.isEmpty(answerId) || TextUtils.isEmpty(userId)) {
            if (callback != null) callback.onError(new IllegalArgumentException("ID câu trả lời hoặc người dùng không hợp lệ"));
            return;
        }

        DocumentReference answerRef = db.collection(COLLECTION_ANSWERS).document(answerId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(answerRef);
            if (!snapshot.exists()) {
                throw new IllegalStateException("Câu trả lời không tồn tại trên Firestore");
            }

            Answer answerObj = snapshot.toObject(Answer.class);
            if (answerObj == null) {
                answerObj = new Answer();
            }
            if (answerObj.getId() == null || answerObj.getId().isEmpty()) {
                answerObj.setId(snapshot.getId());
            }

            List<String> likedUsers = answerObj.getLikedUserIds();
            if (likedUsers == null) {
                likedUsers = new ArrayList<>();
            }
            List<String> rawLikedUsers = (List<String>) snapshot.get("likedUserIds");
            if (rawLikedUsers != null) {
                likedUsers = new ArrayList<>(rawLikedUsers);
            }

            long currentLikes = answerObj.getLikes();
            Long rawLikes = snapshot.getLong("likes");
            if (rawLikes != null) {
                currentLikes = rawLikes;
            }
            Long rawLikeCount = snapshot.getLong("likeCount");
            if (rawLikeCount != null) {
                currentLikes = rawLikeCount;
            }

            // Đảm bảo không thể tăng lượt thích nhiều lần do cùng một người dùng thích cùng một câu trả lời nhiều lần
            if (!likedUsers.contains(userId)) {
                likedUsers.add(userId);
                currentLikes = currentLikes + 1;
            }

            answerObj.setLikes(currentLikes);
            answerObj.setLikeCount(currentLikes);
            answerObj.setLikedUserIds(likedUsers);

            transaction.set(answerRef, answerObj, SetOptions.merge());
            return null;
        }).addOnSuccessListener(aVoid -> {
            if (callback != null) callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError(e);
        });
    }

    /**
     * Bỏ thích một câu trả lời sử dụng Firestore Transaction.
     * Đảm bảo khi bỏ thích, số lượt thích không bị âm (Math.max(0, currentLikes - 1)).
     */
    @SuppressWarnings("unchecked")
    public void unlikeAnswer(String answerId, String userId, Callback<Void> callback) {
        if (TextUtils.isEmpty(answerId) || TextUtils.isEmpty(userId)) {
            if (callback != null) callback.onError(new IllegalArgumentException("ID câu trả lời hoặc người dùng không hợp lệ"));
            return;
        }

        DocumentReference answerRef = db.collection(COLLECTION_ANSWERS).document(answerId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(answerRef);
            if (!snapshot.exists()) {
                throw new IllegalStateException("Câu trả lời không tồn tại trên Firestore");
            }

            Answer answerObj = snapshot.toObject(Answer.class);
            if (answerObj == null) {
                answerObj = new Answer();
            }
            if (answerObj.getId() == null || answerObj.getId().isEmpty()) {
                answerObj.setId(snapshot.getId());
            }

            List<String> likedUsers = answerObj.getLikedUserIds();
            if (likedUsers == null) {
                likedUsers = new ArrayList<>();
            }
            List<String> rawLikedUsers = (List<String>) snapshot.get("likedUserIds");
            if (rawLikedUsers != null) {
                likedUsers = new ArrayList<>(rawLikedUsers);
            }

            long currentLikes = answerObj.getLikes();
            Long rawLikes = snapshot.getLong("likes");
            if (rawLikes != null) {
                currentLikes = rawLikes;
            }
            Long rawLikeCount = snapshot.getLong("likeCount");
            if (rawLikeCount != null) {
                currentLikes = rawLikeCount;
            }

            if (likedUsers.contains(userId)) {
                likedUsers.remove(userId);
                currentLikes = Math.max(0, currentLikes - 1);
            }

            answerObj.setLikes(currentLikes);
            answerObj.setLikeCount(currentLikes);
            answerObj.setLikedUserIds(likedUsers);

            transaction.set(answerRef, answerObj, SetOptions.merge());
            return null;
        }).addOnSuccessListener(aVoid -> {
            if (callback != null) callback.onSuccess(null);
        }).addOnFailureListener(e -> {
            if (callback != null) callback.onError(e);
        });
    }

    /**
     * Kiểm tra người dùng hiện tại đã thích câu trả lời hay chưa.
     */
    @SuppressWarnings("unchecked")
    public void isLikedByUser(String answerId, String userId, Callback<Boolean> callback) {
        if (TextUtils.isEmpty(answerId) || TextUtils.isEmpty(userId)) {
            if (callback != null) callback.onError(new IllegalArgumentException("ID câu trả lời hoặc người dùng không hợp lệ"));
            return;
        }

        db.collection(COLLECTION_ANSWERS).document(answerId).get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.exists()) {
                        if (callback != null) callback.onSuccess(false);
                        return;
                    }
                    Answer answerObj = snapshot.toObject(Answer.class);
                    boolean isLiked = false;
                    if (answerObj != null) {
                        isLiked = answerObj.isLikedBy(userId);
                    }
                    if (!isLiked) {
                        List<String> rawLikedUsers = (List<String>) snapshot.get("likedUserIds");
                        if (rawLikedUsers != null && rawLikedUsers.contains(userId)) {
                            isLiked = true;
                        }
                    }
                    if (callback != null) callback.onSuccess(isLiked);
                })
                .addOnFailureListener(e -> {
                    if (callback != null) callback.onError(e);
                });
    }
}
/*1 số lỗi sau:
* Hai bản cài đặt like cùng tồn tại. AnswerRepository.toggleLike() (chỉ cập nhật likes) và AnswerService.likeAnswer()/unlikeAnswer() (cập nhật cả hai). 
Giao diện gọi bản nào thì bản kia không được cập nhật. Giữ một bản trong service, xóa hoặc đổi bản còn lại.
* Thứ tự xếp hạng khi bằng like không khớp. Thiết kế và ContentLocalDao ghi: bằng like thì câu cũ hơn lên trước (createdAt tăng dần). 
Nhưng AnswerRepository.getAnswersByQuestionId sắp xếp bằng Long.compare(a2.getCreatedAt(), a1.getCreatedAt()). Nên chú ý
* Thiết kế có entity Like (đa hình, ID userId_targetType_targetId) và Nghi đã làm likeQuestion đúng kiểu đó. Còn like của Answer lại lưu bằng mảng likedUserIds */
