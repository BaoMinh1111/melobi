package vn.edu.ueh.melobi_nhom5.repository;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.model.Answer;

public class AnswerRepository {
    private static final String TAG = "AnswerRepository";
    private static final String COLLECTION_ANSWERS = "answers";
    private static final String COLLECTION_QUESTIONS = "questions";
    private static final String COLLECTION_COMMENTS = "comments";

    private final FirebaseFirestore db;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public AnswerRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    public ListenerRegistration listenAnswers(String questionId, EventListener<QuerySnapshot> listener) {
        return db.collection(COLLECTION_ANSWERS)
                .whereEqualTo("questionId", questionId)
                .addSnapshotListener(MetadataChanges.INCLUDE, listener);
    }

    public List<Answer> mapAnswers(QuerySnapshot snapshot) {
        List<Answer> list = new ArrayList<>();
        if (snapshot == null) {
            return list;
        }
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Answer answer = doc.toObject(Answer.class);
            if (answer == null || answer.isHidden()) {
                continue;
            }
            if (answer.getId() == null || answer.getId().isEmpty()) {
                answer.setId(doc.getId());
            }
            list.add(answer);
        }
        Collections.sort(list, (a1, a2) -> {
            int likeCompare = Long.compare(a2.getLikes(), a1.getLikes());
            if (likeCompare != 0) {
                return likeCompare;
            }
            return Long.compare(a2.getCreatedAt(), a1.getCreatedAt());
        });
        return list;
    }

    // Xem danh sách câu trả lời theo questionId, sắp xếp theo lượt thích giảm dần, lọc bỏ câu trả lời đã ẩn
    public void getAnswersByQuestionId(String questionId, Callback<List<Answer>> callback) {
        db.collection(COLLECTION_ANSWERS)
                .whereEqualTo("questionId", questionId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Answer> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Answer answer = doc.toObject(Answer.class);
                        if (answer != null) {
                            if (answer.isHidden()) {
                                continue; // Không hiển thị câu trả lời đã bị ẩn
                            }
                            if (answer.getId() == null || answer.getId().isEmpty()) {
                                answer.setId(doc.getId());
                            }
                            list.add(answer);
                        }
                    }

                    // Sắp xếp theo lượt like giảm dần; nếu bằng lượt like thì xếp theo thời gian mới nhất
                    Collections.sort(list, (a1, a2) -> {
                        int likeCompare = Long.compare(a2.getLikes(), a1.getLikes());
                        if (likeCompare != 0) {
                            return likeCompare;
                        }
                        return Long.compare(a2.getCreatedAt(), a1.getCreatedAt());
                    });

                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi lấy danh sách câu trả lời: ", e);
                    callback.onError(e);
                });
    }

    // Thêm câu trả lời mới cho câu hỏi
    public void addAnswer(Answer answer, Callback<Answer> callback) {
        DocumentReference newDocRef = db.collection(COLLECTION_ANSWERS).document();
        answer.setId(newDocRef.getId());

        long currentTime = System.currentTimeMillis();
        answer.setCreatedAt(currentTime);
        answer.setUpdatedAt(currentTime);
        answer.setLikes(0);
        answer.setCommentCount(0);
        answer.setStatus(Answer.STATUS_ACTIVE);
        if (answer.getLikedUserIds() == null) {
            answer.setLikedUserIds(new ArrayList<>());
        }

        DocumentReference questionRef = db.collection(COLLECTION_QUESTIONS).document(answer.getQuestionId());

        WriteBatch batch = db.batch();
        batch.set(newDocRef, answer);
        batch.update(questionRef, "answerCount", FieldValue.increment(1));

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess(answer))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi khi thêm câu trả lời: ", e);
                    callback.onError(e);
                });
    }

    // Chỉnh sửa nội dung câu trả lời
    public void updateAnswer(String answerId, String newContent, Callback<Void> callback) {
        long currentTime = System.currentTimeMillis();
        db.collection(COLLECTION_ANSWERS).document(answerId)
                .update("content", newContent, "updatedAt", currentTime)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi cập nhật câu trả lời: ", e);
                    callback.onError(e);
                });
    }

    // Xóa câu trả lời và dọn dẹp các bình luận thuộc câu trả lời đó
    public void deleteAnswer(Answer answer, Callback<Void> callback) {
        DocumentReference answerRef = db.collection(COLLECTION_ANSWERS).document(answer.getId());
        DocumentReference questionRef = db.collection(COLLECTION_QUESTIONS).document(answer.getQuestionId());

        // Tìm tất cả comment của answerId này để xóa
        db.collection(COLLECTION_COMMENTS)
                .whereEqualTo("answerId", answer.getId())
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.delete(answerRef);
                    batch.update(questionRef, "answerCount", FieldValue.increment(-1));

                    batch.commit()
                            .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Lỗi xóa câu trả lời: ", e);
                                callback.onError(e);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tìm bình luận khi xóa câu trả lời: ", e);
                    callback.onError(e);
                });
    }

    // Thích / Bỏ thích câu trả lời (xử lý atomic Transaction)
    @SuppressWarnings("unchecked")
    public void toggleLike(String answerId, String userId, Callback<Answer> callback) {
        if (answerId == null || answerId.isEmpty() || userId == null || userId.isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("ID câu trả lời hoặc người dùng không hợp lệ"));
            return;
        }

        DocumentReference answerRef = db.collection(COLLECTION_ANSWERS).document(answerId);

        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(answerRef);
            if (!snapshot.exists()) {
                throw new IllegalStateException("Câu trả lời không tồn tại trên Firestore");
            }

            Answer answer = snapshot.toObject(Answer.class);
            if (answer == null) {
                answer = new Answer();
            }
            if (answer.getId() == null || answer.getId().isEmpty()) {
                answer.setId(snapshot.getId());
            }

            List<String> likedUsers = answer.getLikedUserIds();
            if (likedUsers == null) {
                likedUsers = new ArrayList<>();
            }
            List<String> rawLikedUsers = (List<String>) snapshot.get("likedUserIds");
            if (rawLikedUsers != null) {
                likedUsers = new ArrayList<>(rawLikedUsers);
            }

            long currentLikes = answer.getLikes();
            Long rawLikes = snapshot.getLong("likes");
            if (rawLikes != null) {
                currentLikes = rawLikes;
            }

            if (likedUsers.contains(userId)) {
                // Bỏ thích
                likedUsers.remove(userId);
                currentLikes = Math.max(0, currentLikes - 1);
            } else {
                // Thích
                likedUsers.add(userId);
                currentLikes = currentLikes + 1;
            }

            answer.setLikes(currentLikes);
            answer.setLikedUserIds(likedUsers);

            // Use set with merge to robustly update fields even if sparse
            transaction.set(answerRef, answer, SetOptions.merge());

            return answer;
        }).addOnSuccessListener(updatedAnswer -> {
            if (callback != null) callback.onSuccess(updatedAnswer);
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Lỗi toggle like: ", e);
            if (callback != null) callback.onError(e);
        });
    }
}
