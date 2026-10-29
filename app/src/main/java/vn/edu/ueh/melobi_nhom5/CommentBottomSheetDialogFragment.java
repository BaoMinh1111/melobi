package vn.edu.ueh.melobi_nhom5;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;

import vn.edu.ueh.melobi_nhom5.adapter.CommentAdapter;
import vn.edu.ueh.melobi_nhom5.model.Comment;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.repository.CommentRepository;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;
import vn.edu.ueh.melobi_nhom5.util.NetworkUtils;

public class CommentBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private static final String ARG_ANSWER_ID = "arg_answer_id";

    public interface OnCommentUpdatedListener {
        void onCommentUpdated();
    }

    private String answerId;
    private CommentRepository commentRepository;
    private CommentAdapter commentAdapter;
    private User currentUser;
    private OnCommentUpdatedListener listener;
    private Comment replyTarget;
    private ListenerRegistration commentsListener;

    private RecyclerView rvComments;
    private ProgressBar pbCommentsLoading;
    private TextView tvEmptyComments;
    private EditText etCommentInput;
    private ImageButton btnSendComment;
    private LinearLayout layoutReplyBar;
    private TextView tvReplyBar;
    private TextView btnCancelReply;

    public static CommentBottomSheetDialogFragment newInstance(String answerId) {
        CommentBottomSheetDialogFragment fragment = new CommentBottomSheetDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_ANSWER_ID, answerId);
        fragment.setArguments(args);
        return fragment;
    }

    public void setOnCommentUpdatedListener(OnCommentUpdatedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            answerId = getArguments().getString(ARG_ANSWER_ID);
        }
        commentRepository = new CommentRepository();
        currentUser = AuthHelper.getCurrentUser();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_comments, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (currentUser == null) {
            Toast.makeText(getContext(), "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            dismissAllowingStateLoss();
            return;
        }

        rvComments = view.findViewById(R.id.rvComments);
        pbCommentsLoading = view.findViewById(R.id.pbCommentsLoading);
        tvEmptyComments = view.findViewById(R.id.tvEmptyComments);
        etCommentInput = view.findViewById(R.id.etCommentInput);
        btnSendComment = view.findViewById(R.id.btnSendComment);
        layoutReplyBar = view.findViewById(R.id.layoutReplyBar);
        tvReplyBar = view.findViewById(R.id.tvReplyBar);
        btnCancelReply = view.findViewById(R.id.btnCancelReply);

        setupRecyclerView();
        btnSendComment.setOnClickListener(v -> submitComment());
        if (btnCancelReply != null) {
            btnCancelReply.setOnClickListener(v -> clearReplyTarget());
        }
        listenComments();
    }

    @Override
    public void onDestroyView() {
        if (commentsListener != null) {
            commentsListener.remove();
            commentsListener = null;
        }
        super.onDestroyView();
    }

    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter(currentUser.getId(), new CommentAdapter.OnCommentActionListener() {
            @Override
            public void onEditClick(Comment comment) {
                if (!isOwnComment(comment)) {
                    Toast.makeText(getContext(), R.string.error_own_content_only, Toast.LENGTH_SHORT).show();
                    return;
                }
                showEditCommentDialog(comment);
            }

            @Override
            public void onDeleteClick(Comment comment) {
                if (!isOwnComment(comment)) {
                    Toast.makeText(getContext(), R.string.error_own_content_only, Toast.LENGTH_SHORT).show();
                    return;
                }
                confirmDeleteComment(comment);
            }

            @Override
            public void onAuthorClick(Comment comment) {
                ProfileActivity.openProfile(requireContext(), comment.getUserId(), comment.getUserName());
            }

            @Override
            public void onReplyClick(Comment comment) {
                setReplyTarget(comment);
            }
        });

        rvComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvComments.setAdapter(commentAdapter);
    }

    private void listenComments() {
        pbCommentsLoading.setVisibility(View.VISIBLE);
        tvEmptyComments.setVisibility(View.GONE);
        if (commentsListener != null) {
            commentsListener.remove();
        }
        commentsListener = commentRepository.listenComments(answerId, (snapshot, error) -> {
            if (!isAdded()) {
                return;
            }
            pbCommentsLoading.setVisibility(View.GONE);
            if (error != null) {
                Toast.makeText(getContext(), "Lỗi tải bình luận: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
            List<Comment> result = commentRepository.mapComments(snapshot);
            tvEmptyComments.setVisibility(result.isEmpty() ? View.VISIBLE : View.GONE);
            commentAdapter.setComments(result);
        });
    }

    private void setReplyTarget(Comment comment) {
        replyTarget = comment;
        String name = comment.getUserName() != null ? comment.getUserName() : "người dùng";
        if (layoutReplyBar != null) {
            layoutReplyBar.setVisibility(View.VISIBLE);
        }
        if (tvReplyBar != null) {
            tvReplyBar.setText(getString(R.string.label_replying_to, name));
        }
        etCommentInput.setHint(getString(R.string.hint_reply_comment, name));
        etCommentInput.requestFocus();
    }

    private void clearReplyTarget() {
        replyTarget = null;
        if (layoutReplyBar != null) {
            layoutReplyBar.setVisibility(View.GONE);
        }
        etCommentInput.setHint(R.string.hint_write_comment);
    }

    private void submitComment() {
        if (!NetworkUtils.isOnline()) {
            Toast.makeText(getContext(), R.string.error_no_internet, Toast.LENGTH_SHORT).show();
            return;
        }
        String content = etCommentInput.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(getContext(), "Vui lòng nhập nội dung bình luận", Toast.LENGTH_SHORT).show();
            return;
        }

        Comment newComment = new Comment();
        newComment.setAnswerId(answerId);
        newComment.setUserId(currentUser.getId());
        newComment.setUserName(currentUser.getDisplayName());
        newComment.setUserAvatarUrl(currentUser.getAvatarUrl());
        newComment.setContent(content);
        if (replyTarget != null) {
            String parentId = replyTarget.isReply() ? replyTarget.getParentId() : replyTarget.getId();
            newComment.setParentId(parentId);
            newComment.setReplyToUserId(replyTarget.getUserId());
            newComment.setReplyToUserName(replyTarget.getUserName());
        }

        btnSendComment.setEnabled(false);

        commentRepository.addComment(newComment, new CommentRepository.Callback<Comment>() {
            @Override
            public void onSuccess(Comment result) {
                if (!isAdded()) return;
                btnSendComment.setEnabled(true);
                etCommentInput.setText("");
                clearReplyTarget();
                Toast.makeText(getContext(), "Đã thêm bình luận", Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onCommentUpdated();
                }
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                btnSendComment.setEnabled(true);
                Toast.makeText(getContext(), "Thêm bình luận thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isOwnComment(Comment comment) {
        return currentUser != null && comment != null
                && currentUser.getId() != null
                && currentUser.getId().equals(comment.getUserId());
    }

    private void showEditCommentDialog(Comment comment) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_text, null);
        TextInputEditText etInput = dialogView.findViewById(R.id.etDialogInput);
        etInput.setText(comment.getContent());

        new AlertDialog.Builder(requireContext())
                .setTitle("Sửa bình luận")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String updatedText = etInput.getText() != null ? etInput.getText().toString().trim() : "";
                    if (updatedText.isEmpty()) {
                        Toast.makeText(getContext(), "Nội dung bình luận không được để trống", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    commentRepository.updateComment(comment.getId(), updatedText, new CommentRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "Đã cập nhật bình luận", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(Exception e) {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "Lỗi cập nhật: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmDeleteComment(Comment comment) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa bình luận này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    commentRepository.deleteComment(comment, new CommentRepository.Callback<Void>() {
                        @Override
                        public void onSuccess(Void result) {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "Đã xóa bình luận", Toast.LENGTH_SHORT).show();
                            if (listener != null) {
                                listener.onCommentUpdated();
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            if (!isAdded()) return;
                            Toast.makeText(getContext(), "Xóa bình luận thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
}
