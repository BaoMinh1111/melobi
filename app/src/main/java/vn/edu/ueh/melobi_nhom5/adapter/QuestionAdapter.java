package vn.edu.ueh.melobi_nhom5.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.R;
import vn.edu.ueh.melobi_nhom5.model.Question;

public class QuestionAdapter extends RecyclerView.Adapter<QuestionAdapter.QuestionViewHolder> {

    public interface OnQuestionClickListener {
        void onQuestionClick(Question question);
    }

    public interface OnAuthorClickListener {
        void onAuthorClick(Question question);
    }

    private final List<Question> questionList = new ArrayList<>();
    private final OnQuestionClickListener listener;
    private final OnAuthorClickListener authorClickListener;

    public QuestionAdapter(OnQuestionClickListener listener) {
        this(listener, null);
    }

    public QuestionAdapter(OnQuestionClickListener listener, OnAuthorClickListener authorClickListener) {
        this.listener = listener;
        this.authorClickListener = authorClickListener;
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
        holder.bind(question, listener, authorClickListener);
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

        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMainAvatar = itemView.findViewById(R.id.tvMainAvatar);
            tvMainCategory = itemView.findViewById(R.id.tvMainCategory);
            tvMainAnswerCount = itemView.findViewById(R.id.tvMainAnswerCount);
            tvMainTitle = itemView.findViewById(R.id.tvMainTitle);
            tvMainContent = itemView.findViewById(R.id.tvMainContent);
            tvMainAuthorName = itemView.findViewById(R.id.tvMainAuthorName);
            tvMainTime = itemView.findViewById(R.id.tvMainTime);
        }

        public void bind(Question question, OnQuestionClickListener listener, OnAuthorClickListener authorClickListener) {
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
    }
}
