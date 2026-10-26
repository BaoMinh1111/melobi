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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import vn.edu.ueh.melobi_nhom5.R;
import vn.edu.ueh.melobi_nhom5.model.Question;
import vn.edu.ueh.melobi_nhom5.util.FirebaseStorageManager;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder> {

    public interface OnQuestionClickListener {
        void onQuestionClick(Question question);
    }

    public interface OnAuthorClickListener {
        void onAuthorClick(Question question);
    }

    public interface OnLikeClickListener {
        void onLikeClick(Question question);
    }

    private final List<Question> questionList = new ArrayList<>();
    private final Set<String> likedQuestionIds = new HashSet<>();
    private final OnQuestionClickListener listener;
    private final OnAuthorClickListener authorClickListener;
    private OnLikeClickListener likeClickListener;

    public QuestionAdapter(OnQuestionClickListener listener) {
        this(listener, null);
    }

    public QuestionAdapter(OnQuestionClickListener listener, OnAuthorClickListener authorClickListener) {
        this.listener = listener;
        this.authorClickListener = authorClickListener;
    }

    public void setOnLikeClickListener(OnLikeClickListener likeClickListener) {
        this.likeClickListener = likeClickListener;
    }

    public void setLikedQuestionIds(Set<String> ids) {
        likedQuestionIds.clear();
        if (ids != null) {
            likedQuestionIds.addAll(ids);
        }
        notifyDataSetChanged();
    }

    public void setQuestions(List<Question> newQuestions) {
        this.questionList.clear();
        if (newQuestions != null) {
            this.questionList.addAll(newQuestions);
        }
        notifyDataSetChanged();
    }

    /** Nối thêm trang mới vào cuối danh sách (phân trang feed). */
    public void addQuestions(List<Question> more) {
        if (more == null || more.isEmpty()) return;
        int start = questionList.size();
        questionList.addAll(more);
        notifyItemRangeInserted(start, more.size());
    }

    @NonNull
    @Override
    public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_question, parent, false);
        return new QuestionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull QuestionViewHolder holder, int position) {
        Question question = questionList.get(position);
        holder.bind(question, listener, authorClickListener, likeClickListener, likedQuestionIds);
    }

    @Override
    public int getItemCount() {
        return questionList.size();
    }

    static class QuestionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMainAvatar;
        private final TextView tvMainCategory;
        private final TextView tvMainAnswerCount;
        private final TextView tvMainTitle;
        private final TextView tvMainContent;
        private final TextView tvMainAuthorName;
        private final TextView tvMainTime;
        private final View btnQuestionLike;
        private final ImageView ivQuestionLike;
        private final TextView tvQuestionLikeCount;
        private final ImageView ivQuestionImage;

        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMainAvatar = itemView.findViewById(R.id.tvMainAvatar);
            tvMainCategory = itemView.findViewById(R.id.tvMainCategory);
            tvMainAnswerCount = itemView.findViewById(R.id.tvMainAnswerCount);
            tvMainTitle = itemView.findViewById(R.id.tvMainTitle);
            tvMainContent = itemView.findViewById(R.id.tvMainContent);
            tvMainAuthorName = itemView.findViewById(R.id.tvMainAuthorName);
            tvMainTime = itemView.findViewById(R.id.tvMainTime);
            btnQuestionLike = itemView.findViewById(R.id.btnQuestionLike);
            ivQuestionLike = itemView.findViewById(R.id.ivQuestionLike);
            tvQuestionLikeCount = itemView.findViewById(R.id.tvQuestionLikeCount);
            ivQuestionImage = itemView.findViewById(R.id.ivQuestionImage);
        }

        public void bind(Question question,
                         OnQuestionClickListener listener,
                         OnAuthorClickListener authorClickListener,
                         OnLikeClickListener likeClickListener,
                         Set<String> likedQuestionIds) {
            tvMainCategory.setText(question.getCategory() != null ? question.getCategory() : "Khác");
            tvMainAnswerCount.setText(question.getAnswerCount() + " câu trả lời");
            tvMainTitle.setText(question.getTitle());
            tvMainContent.setText(question.getContent());

            CharSequence relativeTime = DateUtils.getRelativeTimeSpanString(
                    question.getCreatedAt(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
            );
            String authorName = question.getUserName() != null ? question.getUserName() : "Thành viên";
            tvMainAuthorName.setText(authorName);
            tvMainAvatar.setText(authorName.substring(0, 1).toUpperCase());
            tvMainTime.setText(relativeTime);
            bindQuestionImage(question.getImageUrl());

            boolean liked = likedQuestionIds != null && likedQuestionIds.contains(question.getId());
            if (tvQuestionLikeCount != null) {
                tvQuestionLikeCount.setText(String.valueOf(question.getLikeCount()));
                tvQuestionLikeCount.setTextColor(itemView.getResources().getColor(
                        liked ? R.color.like_active : R.color.text_secondary, itemView.getContext().getTheme()));
            }
            if (ivQuestionLike != null) {
                ivQuestionLike.setImageResource(liked ? R.drawable.ic_thumb_up_filled : R.drawable.ic_thumb_up);
                ivQuestionLike.setColorFilter(itemView.getResources().getColor(
                        liked ? R.color.like_active : R.color.text_secondary, itemView.getContext().getTheme()));
            }
            if (btnQuestionLike != null) {
                btnQuestionLike.setOnClickListener(v -> {
                    if (likeClickListener != null) {
                        likeClickListener.onLikeClick(question);
                    }
                });
            }

            View.OnClickListener openAuthor = v -> {
                if (authorClickListener != null) {
                    authorClickListener.onAuthorClick(question);
                }
            };
            tvMainAvatar.setOnClickListener(openAuthor);
            tvMainAuthorName.setOnClickListener(openAuthor);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onQuestionClick(question);
                }
            });
        }

        private void bindQuestionImage(String imageUrl) {
            if (ivQuestionImage == null) {
                return;
            }
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                ivQuestionImage.setTag(null);
                ivQuestionImage.setImageDrawable(null);
                ivQuestionImage.setVisibility(View.GONE);
                return;
            }
            ivQuestionImage.setVisibility(View.VISIBLE);
            ivQuestionImage.setTag(imageUrl);
            ivQuestionImage.setImageDrawable(null);
            FirebaseStorageManager.loadBitmap(imageUrl, 720, new FirebaseStorageManager.Callback<android.graphics.Bitmap>() {
                @Override
                public void onSuccess(android.graphics.Bitmap result) {
                    if (imageUrl.equals(ivQuestionImage.getTag())) {
                        ivQuestionImage.setImageBitmap(result);
                    }
                }

                @Override
                public void onError(Exception e) {
                    if (imageUrl.equals(ivQuestionImage.getTag())) {
                        ivQuestionImage.setVisibility(View.GONE);
                    }
                }
            });
        }
    }
}
