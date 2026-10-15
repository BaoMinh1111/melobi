package vn.edu.ueh.melobi_nhom5.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.R;
import vn.edu.ueh.melobi_nhom5.model.Comment;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    public interface OnCommentActionListener {
        void onEditClick(Comment comment);
        void onDeleteClick(Comment comment);
    }

    private final List<Comment> commentList = new ArrayList<>();
    private final String currentUserId;
    private final OnCommentActionListener listener;

    public CommentAdapter(String currentUserId, OnCommentActionListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setComments(List<Comment> newComments) {
        this.commentList.clear();
        if (newComments != null) {
            this.commentList.addAll(newComments);
        }
        notifyDataSetChanged();
    }

    public void addComment(Comment comment) {
        if (comment != null) {
            this.commentList.add(comment);
            notifyItemInserted(commentList.size() - 1);
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = commentList.get(position);
        holder.bind(comment, currentUserId, listener);
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvCommentAvatar;
        private final TextView tvCommentAuthor;
        private final TextView tvCommentTime;
        private final TextView tvCommentContent;
        private final ImageView btnEditComment;
        private final ImageView btnDeleteComment;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCommentAvatar = itemView.findViewById(R.id.tvCommentAvatar);
            tvCommentAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvCommentTime = itemView.findViewById(R.id.tvCommentTime);
            tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
            btnEditComment = itemView.findViewById(R.id.btnEditComment);
            btnDeleteComment = itemView.findViewById(R.id.btnDeleteComment);
        }

        public void bind(Comment comment, String currentUserId, OnCommentActionListener listener) {
            String authorName = comment.getUserName() != null && !comment.getUserName().isEmpty()
                    ? comment.getUserName() : "Người dùng";
            tvCommentAuthor.setText(authorName);

            String initial = authorName.substring(0, 1).toUpperCase();
            tvCommentAvatar.setText(initial);

            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    comment.getCreatedAt(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            );
            tvCommentTime.setText(relativeTime);

            tvCommentContent.setText(comment.getContent());

            boolean isAuthor = currentUserId != null && currentUserId.equals(comment.getUserId());
            btnEditComment.setVisibility(isAuthor ? View.VISIBLE : View.GONE);
            btnDeleteComment.setVisibility(isAuthor ? View.VISIBLE : View.GONE);

            btnEditComment.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(comment);
            });

            btnDeleteComment.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(comment);
            });
        }
    }
}
