

package vn.edu.ueh.melobi_nhom5.service;

import android.text.TextUtils;

import java.util.List;

import vn.edu.ueh.melobi_nhom5.model.Comment;
import vn.edu.ueh.melobi_nhom5.repository.CommentRepository;

public class CommentService {

    private final CommentRepository commentRepository;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    // Khoi tao Service voi Repository mac dinh.
    public CommentService() {
        this(new CommentRepository());
    }

    // Tiem Repository tu ben ngoai de de kiem thu va thay the phu thuoc.
    public CommentService(CommentRepository commentRepository) {
        if (commentRepository == null) {
            throw new IllegalArgumentException(
                    "CommentRepository khong duoc null"
            );
        }
        this.commentRepository = commentRepository;
    }

    // Tao binh luan tu thong tin nguoi dung va chuyen sang ham xu ly chung.
    public void addComment(
            String answerId,
            String userId,
            String userName,
            String userAvatarUrl,
            String content,
            Callback<Comment> callback
    ) {
        Comment comment = new Comment();
        comment.setAnswerId(answerId);
        comment.setUserId(userId);
        comment.setUserName(
                TextUtils.isEmpty(userName) ? "Thành viên" : userName
        );
        comment.setUserAvatarUrl(
                userAvatarUrl != null ? userAvatarUrl : ""
        );
        comment.setContent(content != null ? content.trim() : "");

        addComment(comment, callback);
    }

    // Kiem tra du lieu truoc khi luu de tranh binh luan thieu thong tin.
    public void addComment(
            Comment comment,
            Callback<Comment> callback
    ) {
        if (comment == null) {
            fail(callback, "Bình luận không hợp lệ");
            return;
        }

        if (TextUtils.isEmpty(comment.getAnswerId())) {
            fail(callback, "ID câu trả lời không hợp lệ");
            return;
        }

        if (TextUtils.isEmpty(comment.getUserId())) {
            fail(callback, "Người dùng chưa đăng nhập");
            return;
        }

        if (TextUtils.isEmpty(comment.getContent())
                || comment.getContent().trim().isEmpty()) {
            fail(callback, "Nội dung bình luận không được để trống");
            return;
        }

        // Chuan hoa noi dung va bo sung gia tri mac dinh cho thong tin hien thi.
        comment.setContent(comment.getContent().trim());

        if (TextUtils.isEmpty(comment.getUserName())) {
            comment.setUserName("Thành viên");
        }

        if (comment.getUserAvatarUrl() == null) {
            comment.setUserAvatarUrl("");
        }

        // Chuyen thao tac luu du lieu sang Repository va tra ket qua qua callback.
        commentRepository.addComment(
                comment,
                new CommentRepository.Callback<Comment>() {
                    @Override
                    public void onSuccess(Comment result) {
                        if (callback != null) {
                            callback.onSuccess(result);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                }
        );
    }

    // Chi cho phep chu binh luan yeu cau xoa binh luan cua minh.
    public void deleteComment(
            Comment comment,
            String currentUserId,
            Callback<Void> callback
    ) {
        if (comment == null || TextUtils.isEmpty(comment.getId())) {
            fail(callback, "Bình luận không hợp lệ");
            return;
        }

        if (TextUtils.isEmpty(currentUserId)) {
            fail(callback, "Người dùng chưa đăng nhập");
            return;
        }

        // Tu choi xoa neu ID nguoi dang nhap khong trung voi chu binh luan.
        if (TextUtils.isEmpty(comment.getUserId())
                || !currentUserId.equals(comment.getUserId())) {
            if (callback != null) {
                callback.onError(
                        new SecurityException(
                                "Bạn không có quyền xóa bình luận này"
                        )
                );
            }
            return;
        }

        // Chi thuc hien xoa sau khi cac dieu kien hop le.
        commentRepository.deleteComment(
                comment,
                new CommentRepository.Callback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (callback != null) {
                            callback.onSuccess(result);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                }
        );
    }

    // Lay danh sach binh luan cua mot cau tra loi.
    public void getCommentsByAnswer(
            String answerId,
            Callback<List<Comment>> callback
    ) {
        if (TextUtils.isEmpty(answerId)) {
            fail(callback, "ID câu trả lời không hợp lệ");
            return;
        }

        // Chuyen truy van sang Repository va tra danh sach hoac loi cho ben goi.
        commentRepository.getCommentsByAnswerId(
                answerId,
                new CommentRepository.Callback<List<Comment>>() {
                    @Override
                    public void onSuccess(List<Comment> result) {
                        if (callback != null) {
                            callback.onSuccess(result);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        if (callback != null) {
                            callback.onError(e);
                        }
                    }
                }
        );
    }

    // Tra loi loi dau vao thong qua callback neu ben goi co dang ky xu ly.
    private void fail(Callback<?> callback, String message) {
        if (callback != null) {
            callback.onError(new IllegalArgumentException(message));
        }
    }
}
