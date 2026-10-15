package vn.edu.ueh.melobi_nhom5.repository;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.model.Comment;

public class CommentRepository {
    private static final String TAG = "CommentRepository";
    private static final String COLLECTION_COMMENTS = "comments";
    private static final String COLLECTION_ANSWERS = "answers";

    private final FirebaseFirestore db;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public CommentRepository() {
        this.db = FirebaseFirestore.getInstance();
    }

    // Lấy danh sách bình luận theo answerId, sắp xếp theo thời gian tăng dần
    public void getCommentsByAnswerId(String answerId, Callback<List<Comment>> callback) {
        db.collection(COLLECTION_COMMENTS)
                .whereEqualTo("answerId", answerId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Comment> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Comment comment = doc.toObject(Comment.class);
                        if (comment != null) {
                            if (comment.getId() == null || comment.getId().isEmpty()) {
                                comment.setId(doc.getId());
                            }
                            list.add(comment);
                        }
                    }

                    // Sắp xếp theo thời gian tăng dần
                    Collections.sort(list, (c1, c2) -> Long.compare(c1.getCreatedAt(), c2.getCreatedAt()));

                    callback.onSuccess(list);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi lấy danh sách bình luận: ", e);
                    callback.onError(e);
                });
    }

    // Thêm bình luận mới vào câu trả lời
    public void addComment(Comment comment, Callback<Comment> callback) {
        DocumentReference commentRef = db.collection(COLLECTION_COMMENTS).document();
        comment.setId(commentRef.getId());

        long currentTime = System.currentTimeMillis();
        comment.setCreatedAt(currentTime);
        comment.setUpdatedAt(currentTime);

        DocumentReference answerRef = db.collection(COLLECTION_ANSWERS).document(comment.getAnswerId());

        WriteBatch batch = db.batch();
        batch.set(commentRef, comment);
        batch.update(answerRef, "commentCount", FieldValue.increment(1));

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess(comment))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi thêm bình luận: ", e);
                    callback.onError(e);
                });
    }

    // Chỉnh sửa bình luận
    public void updateComment(String commentId, String newContent, Callback<Void> callback) {
        long currentTime = System.currentTimeMillis();
        db.collection(COLLECTION_COMMENTS).document(commentId)
                .update("content", newContent, "updatedAt", currentTime)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi sửa bình luận: ", e);
                    callback.onError(e);
                });
    }

    // Xóa bình luận
    public void deleteComment(Comment comment, Callback<Void> callback) {
        DocumentReference commentRef = db.collection(COLLECTION_COMMENTS).document(comment.getId());
        DocumentReference answerRef = db.collection(COLLECTION_ANSWERS).document(comment.getAnswerId());

        WriteBatch batch = db.batch();
        batch.delete(commentRef);
        batch.update(answerRef, "commentCount", FieldValue.increment(-1));

        batch.commit()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi xóa bình luận: ", e);
                    callback.onError(e);
                });
    }
}
