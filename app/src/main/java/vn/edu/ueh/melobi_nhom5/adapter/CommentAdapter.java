package vn.edu.ueh.melobi_nhom5.adapter;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import vn.edu.ueh.melobi_nhom5.R;
import vn.edu.ueh.melobi_nhom5.model.Comment;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    public interface OnCommentActionListener {
        void onEditClick(Comment comment);
        void onDeleteClick(Comment comment);
        default void onAuthorClick(Comment comment) {
        }
        default void onReplyClick(Comment comment) {
        }
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
            this.commentList.addAll(flatten(newComments));
        }
        notifyDataSetChanged();
    }

    public void addComment(Comment comment) {
        if (comment != null) {
            List<Comment> current = new ArrayList<>(commentList);
            current.add(comment);
            setComments(current);
        }
    }

    private List<Comment> flatten(List<Comment> source) {
        List<Comment> roots = new ArrayList<>();
        Map<String, List<Comment>> children = new LinkedHashMap<>();
        for (Comment comment : source) {
            if (comment.isReply()) {
                String parentId = comment.getParentId();
                List<Comment> replies = children.get(parentId);
                if (replies == null) {
                    replies = new ArrayList<>();
                    children.put(parentId, replies);
                }
                replies.add(comment);
            } else {
                roots.add(comment);
            }
        }
        Collections.sort(roots, (a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
        List<Comment> ordered = new ArrayList<>();
        for (Comment root : roots) {
            ordered.add(root);
            List<Comment> replies = children.remove(root.getId());
            if (replies != null) {
                Collections.sort(replies, (a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
                ordered.addAll(replies);
            }
        }
        for (List<Comment> leftover : children.values()) {
            Collections.sort(leftover, (a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
            ordered.addAll(leftover);
        }
        return ordered;
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
        private final TextView btnReplyComment;
        private final ImageView btnEditComment;
        private final ImageView btnDeleteComment;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCommentAvatar = itemView.findViewById(R.id.tvCommentAvatar);
            tvCommentAuthor = itemView.findViewById(R.id.tvCommentAuthor);
            tvCommentTime = itemView.findViewById(R.id.tvCommentTime);
            tvCommentContent = itemView.findViewById(R.id.tvCommentContent);
            btnReplyComment = itemView.findViewById(R.id.btnReplyComment);
            btnEditComment = itemView.findViewById(R.id.btnEditComment);
            btnDeleteComment = itemView.findViewById(R.id.btnDeleteComment);
        }

        public void bind(Comment comment, String currentUserId, OnCommentActionListener listener) {
            String authorName = vn.edu.ueh.melobi_nhom5.util.UserNameLookup.resolve(
                    comment.getUserId(), comment.getUserName());
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

            float density = itemView.getResources().getDisplayMetrics().density;
            int indent = comment.isReply() ? Math.round(28 * density) : 0;
            itemView.setPadding(indent, itemView.getPaddingTop(), itemView.getPaddingRight(), itemView.getPaddingBottom());

            String content = comment.getContent() != null ? comment.getContent() : "";
            String replyName = comment.getReplyToUserName();
            if (comment.getReplyToUserId() != null && !comment.getReplyToUserId().trim().isEmpty()) {
                replyName = vn.edu.ueh.melobi_nhom5.util.UserNameLookup.resolve(
                        comment.getReplyToUserId(), comment.getReplyToUserName());
            }
            if (comment.isReply() && replyName != null && !replyName.trim().isEmpty()) {
                String tag = "@" + replyName + " ";
                SpannableString spannable = new SpannableString(tag + content);
                spannable.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, tag.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                spannable.setSpan(new ForegroundColorSpan(itemView.getResources().getColor(R.color.melobi_primary, itemView.getContext().getTheme())),
                        0, tag.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tvCommentContent.setText(spannable);
            } else {
                tvCommentContent.setText(content);
            }

            boolean isAuthor = currentUserId != null && currentUserId.equals(comment.getUserId());
            btnEditComment.setVisibility(isAuthor ? View.VISIBLE : View.GONE);
            btnDeleteComment.setVisibility(isAuthor ? View.VISIBLE : View.GONE);

            btnEditComment.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(comment);
            });
            btnDeleteComment.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(comment);
            });
            btnReplyComment.setOnClickListener(v -> {
                if (listener != null) listener.onReplyClick(comment);
            });

            View.OnClickListener openAuthor = v -> {
                if (listener != null) listener.onAuthorClick(comment);
            };
            tvCommentAvatar.setOnClickListener(openAuthor);
            tvCommentAuthor.setOnClickListener(openAuthor);
        }
    }
}
