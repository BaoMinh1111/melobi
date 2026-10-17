package vn.edu.ueh.melobi_nhom5.service;

import android.text.TextUtils;

import java.util.List;

import vn.edu.ueh.melobi_nhom5.model.Comment;
import vn.edu.ueh.melobi_nhom5.repository.CommentRepository;

public class CommentService {
    private final CommentRepository commentRepository;

    public CommentService() {
        this.commentRepository = new CommentRepository();
    }

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    /**
     * Thêm bình luận mới vào câu trả lời với các thông tin chi tiết của người dùng.
     */
    public void addComment(String answerId, String userId, String userName, String userAvatarUrl, String content, Callback<Comment> callback) {
        Comment comment = new Comment();
        comment.setAnswerId(answerId);
        comment.setUserId(userId);
        comment.setUserName(TextUtils.isEmpty(userName) ? "Thành viên" : userName);
        comment.setUserAvatarUrl(userAvatarUrl != null ? userAvatarUrl : "");
        comment.setContent(content != null ? content.trim() : "");
        addComment(comment, callback);
    }

    /**
     * Thêm đối tượng Comment vào Firestore sau khi kiểm tra tính hợp lệ dữ liệu.
     */
    public void addComment(Comment comment, Callback<Comment> callback) {
        if (comment == null) {
            if (callback != null) callback.onError(new IllegalArgumentException("Bình luận không hợp lệ"));
            return;
        }
        if (TextUtils.isEmpty(comment.getAnswerId())) {
            if (callback != null) callback.onError(new IllegalArgumentException("ID câu trả lời không hợp lệ"));
            return;
        }
        if (TextUtils.isEmpty(comment.getUserId())) {
            if (callback != null) callback.onError(new IllegalArgumentException("ID người dùng không hợp lệ"));
            return;
        }
        if (TextUtils.isEmpty(comment.getContent()) || comment.getContent().trim().isEmpty()) {
            if (callback != null) callback.onError(new IllegalArgumentException("Nội dung bình luận không được để trống"));
            return;
        }

        comment.setContent(comment.getContent().trim());
        if (TextUtils.isEmpty(comment.getUserName())) {
            comment.setUserName("Thành viên");
        }
        if (comment.getUserAvatarUrl() == null) {
            comment.setUserAvatarUrl("");
        }

        commentRepository.addComment(comment, new CommentRepository.Callback<Comment>() {
            @Override
            public void onSuccess(Comment result) {
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    /**
     * Xóa bình luận, kiểm tra quyền sở hữu (chỉ cho phép người tạo xóa bình luận của mình).
     */
    public void deleteComment(Comment comment, String currentUserId, Callback<Void> callback) {
        if (comment == null || TextUtils.isEmpty(comment.getId())) {
            if (callback != null) callback.onError(new IllegalArgumentException("Bình luận không hợp lệ"));
            return;
        }
        if (TextUtils.isEmpty(currentUserId)) {
            if (callback != null) callback.onError(new IllegalArgumentException("Người dùng chưa đăng nhập"));
            return;
        }
        if (!currentUserId.equals(comment.getUserId())) {
            if (callback != null) callback.onError(new SecurityException("Bạn không có quyền xóa bình luận này"));
            return;
        }

        commentRepository.deleteComment(comment, new CommentRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    /**
     * Xóa bình luận (hàm quá tải không kiểm tra trực tiếp currentUserId, dùng cho repository/admin).
     */
    public void deleteComment(Comment comment, Callback<Void> callback) {
        if (comment == null || TextUtils.isEmpty(comment.getId())) {
            if (callback != null) callback.onError(new IllegalArgumentException("Bình luận không hợp lệ"));
            return;
        }

        commentRepository.deleteComment(comment, new CommentRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }

    /**
     * Lấy danh sách bình luận thuộc một câu trả lời cụ thể, sắp xếp theo thời gian tăng dần.
     */
    public void getCommentsByAnswer(String answerId, Callback<List<Comment>> callback) {
        if (TextUtils.isEmpty(answerId)) {
            if (callback != null) callback.onError(new IllegalArgumentException("ID câu trả lời không hợp lệ"));
            return;
        }

        commentRepository.getCommentsByAnswerId(answerId, new CommentRepository.Callback<List<Comment>>() {
            @Override
            public void onSuccess(List<Comment> result) {
                if (callback != null) callback.onSuccess(result);
            }

            @Override
            public void onError(Exception e) {
                if (callback != null) callback.onError(e);
            }
        });
    }
}
