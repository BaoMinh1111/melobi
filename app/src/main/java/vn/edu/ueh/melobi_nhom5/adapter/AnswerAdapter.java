package vn.edu.ueh.melobi_nhom5.adapter;

import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.R;
import vn.edu.ueh.melobi_nhom5.model.Answer;

public class AnswerAdapter extends RecyclerView.Adapter<AnswerAdapter.AnswerViewHolder> {

    public interface OnAnswerActionListener {
        void onLikeClick(Answer answer);
        void onCommentClick(Answer answer);
        void onEditClick(Answer answer);
        void onDeleteClick(Answer answer);
        default void onAuthorClick(Answer answer) {
        }
    }

    private final List<Answer> answerList = new ArrayList<>();
    private final String currentUserId;
    private final OnAnswerActionListener listener;

    public AnswerAdapter(String currentUserId, OnAnswerActionListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setAnswers(List<Answer> newAnswers) {
        this.answerList.clear();
        if (newAnswers != null) {
            this.answerList.addAll(newAnswers);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AnswerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_answer, parent, false);
        return new AnswerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnswerViewHolder holder, int position) {
        Answer answer = answerList.get(position);
        holder.bind(answer, currentUserId, listener);
    }

    @Override
    public int getItemCount() {
        return answerList.size();
    }

    static class AnswerViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvAvatarPlaceholder;
        private final TextView tvAuthorName;
        private final TextView tvCreatedAt;
        private final TextView tvContent;
        private final TextView tvLikeCount;
        private final TextView tvCommentCount;
        private final ImageView ivLikeIcon;
        private final View btnLike;
        private final View btnComment;
        private final ImageView btnEdit;
        private final ImageView btnDelete;

        public AnswerViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatarPlaceholder = itemView.findViewById(R.id.tvAvatarPlaceholder);
            tvAuthorName = itemView.findViewById(R.id.tvAuthorName);
            tvCreatedAt = itemView.findViewById(R.id.tvCreatedAt);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            ivLikeIcon = itemView.findViewById(R.id.ivLikeIcon);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }

        public void bind(Answer answer, String currentUserId, OnAnswerActionListener listener) {
            String authorName = vn.edu.ueh.melobi_nhom5.util.UserNameLookup.resolve(
                    answer.getUserId(), answer.getUserName());
            tvAuthorName.setText(authorName);

            String initial = authorName.substring(0, 1).toUpperCase();
            tvAvatarPlaceholder.setText(initial);

            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    answer.getCreatedAt(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            );
            tvCreatedAt.setText(relativeTime);

            tvContent.setText(answer.getContent());

            // Like state
            boolean isLiked = answer.isLikedBy(currentUserId);
            tvLikeCount.setText(answer.getLikes() + " Thích");
            if (isLiked) {
                ivLikeIcon.setImageResource(R.drawable.ic_thumb_up_filled);
                ivLikeIcon.setColorFilter(Color.parseColor("#1A73E8"));
                tvLikeCount.setTextColor(Color.parseColor("#1A73E8"));
            } else {
                ivLikeIcon.setImageResource(R.drawable.ic_thumb_up);
                ivLikeIcon.setColorFilter(Color.parseColor("#5F6368"));
                tvLikeCount.setTextColor(Color.parseColor("#5F6368"));
            }

            // Comment count
            tvCommentCount.setText(answer.getCommentCount() + " Bình luận");

            // Author permissions
            boolean isAuthor = currentUserId != null && currentUserId.equals(answer.getUserId());
            btnEdit.setVisibility(isAuthor ? View.VISIBLE : View.GONE);
            btnDelete.setVisibility(isAuthor ? View.VISIBLE : View.GONE);

            btnLike.setOnClickListener(v -> {
                if (listener != null) listener.onLikeClick(answer);
            });

            btnComment.setOnClickListener(v -> {
                if (listener != null) listener.onCommentClick(answer);
            });

            btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEditClick(answer);
            });

            btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDeleteClick(answer);
            });

            View.OnClickListener openAuthor = v -> {
                if (listener != null) listener.onAuthorClick(answer);
            };
            tvAvatarPlaceholder.setOnClickListener(openAuthor);
            tvAuthorName.setOnClickListener(openAuthor);
        }
    }
}
