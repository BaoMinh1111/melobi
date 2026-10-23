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
import vn.edu.ueh.melobi_nhom5.model.Answer;

public class ProfileAnswerAdapter extends RecyclerView.Adapter<ProfileAnswerAdapter.ViewHolder> {

    public interface OnAnswerClickListener {
        void onAnswerClick(Answer answer);
    }

    private final List<Answer> answers = new ArrayList<>();
    private final OnAnswerClickListener listener;

    public ProfileAnswerAdapter(OnAnswerClickListener listener) {
        this.listener = listener;
    }

    public void setAnswers(List<Answer> items) {
        answers.clear();
        if (items != null) {
            answers.addAll(items);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_profile_answer, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Answer answer = answers.get(position);
        holder.tvContent.setText(answer.getContent() != null ? answer.getContent() : "");
        long createdAt = answer.getCreatedAt() > 0 ? answer.getCreatedAt() : System.currentTimeMillis();
        holder.tvTime.setText(DateUtils.getRelativeTimeSpanString(
                createdAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
        ));
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onAnswerClick(answer);
            }
        });
    }

    @Override
    public int getItemCount() {
        return answers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvContent;
        final TextView tvTime;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvProfileAnswerContent);
            tvTime = itemView.findViewById(R.id.tvProfileAnswerTime);
        }
    }
}
