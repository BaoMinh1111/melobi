package vn.edu.ueh.melobi_nhom5;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
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

import java.util.List;

import vn.edu.ueh.melobi_nhom5.adapter.CommentAdapter;
import vn.edu.ueh.melobi_nhom5.model.Comment;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.repository.CommentRepository;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;

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

    private RecyclerView rvComments;
    private ProgressBar pbCommentsLoading;
    private TextView tvEmptyComments;
    private EditText etCommentInput;
    private ImageButton btnSendComment;

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

        setupRecyclerView();

        btnSendComment.setOnClickListener(v -> submitComment());

        loadComments();
    }

    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter(currentUser.getId(), new CommentAdapter.OnCommentActionListener() {
            @Override
            public void onEditClick(Comment comment) {
                showEditCommentDialog(comment);
            }

            @Override
            public void onDeleteClick(Comment comment) {
                confirmDeleteComment(comment);
            }
        });

        rvComments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvComments.setAdapter(commentAdapter);
    }

    private void loadComments() {
        pbCommentsLoading.setVisibility(View.VISIBLE);
        tvEmptyComments.setVisibility(View.GONE);

        commentRepository.getCommentsByAnswerId(answerId, new CommentRepository.Callback<List<Comment>>() {
            @Override
            public void onSuccess(List<Comment> result) {
                if (!isAdded()) return;
                pbCommentsLoading.setVisibility(View.GONE);
                if (result.isEmpty()) {
                    tvEmptyComments.setVisibility(View.VISIBLE);
                } else {
                    tvEmptyComments.setVisibility(View.GONE);
                }
                commentAdapter.setComments(result);
            }

            @Override
            public void onError(Exception e) {
                if (!isAdded()) return;
                pbCommentsLoading.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Lỗi tải bình luận: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void submitComment() {
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

        btnSendComment.setEnabled(false);

        commentRepository.addComment(newComment, new CommentRepository.Callback<Comment>() {
            @Override
            public void onSuccess(Comment result) {
                if (!isAdded()) return;
                btnSendComment.setEnabled(true);
                etCommentInput.setText("");
                Toast.makeText(getContext(), "Đã thêm bình luận", Toast.LENGTH_SHORT).show();

                // Instant update on UI
                commentAdapter.addComment(result);
                rvComments.smoothScrollToPosition(commentAdapter.getItemCount() - 1);
                tvEmptyComments.setVisibility(View.GONE);

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
                            loadComments();
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
                            loadComments();
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
