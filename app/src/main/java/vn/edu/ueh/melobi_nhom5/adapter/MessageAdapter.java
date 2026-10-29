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
import java.util.Date;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.R;
import vn.edu.ueh.melobi_nhom5.entity.Message;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_RECEIVED = 0;
    private static final int VIEW_TYPE_SENT = 1;

    private final String currentUserId;
    private final List<Message> messages = new ArrayList<>();

    public MessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void setMessages(List<Message> items) {
        messages.clear();
        if (items != null) {
            messages.addAll(items);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);
        if (currentUserId != null && currentUserId.equals(message.getSenderId())) {
            return VIEW_TYPE_SENT;
        }
        return VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutId = viewType == VIEW_TYPE_SENT
                ? R.layout.item_message_sent
                : R.layout.item_message_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutId, parent, false);
        return new MessageViewHolder(view, viewType == VIEW_TYPE_SENT);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        holder.bind(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessageContent;
        private final TextView tvMessageStatus;
        private final ImageView ivMessageImage;
        private final boolean sent;

        MessageViewHolder(@NonNull View itemView, boolean sent) {
            super(itemView);
            this.sent = sent;
            tvMessageContent = itemView.findViewById(R.id.tvMessageContent);
            tvMessageStatus = itemView.findViewById(R.id.tvMessageStatus);
            ivMessageImage = itemView.findViewById(R.id.ivMessageImage);
        }

        void bind(Message message) {
            String content = message.getContent() != null ? message.getContent() : "";
            if (content.trim().isEmpty()) {
                tvMessageContent.setVisibility(View.GONE);
            } else {
                tvMessageContent.setVisibility(View.VISIBLE);
                tvMessageContent.setText(content);
            }

            if (ivMessageImage != null) {
                ivMessageImage.setTag(null);
                ivMessageImage.setImageDrawable(null);
                ivMessageImage.setVisibility(View.GONE);
            }

            String timeText = "";
            Date sentAt = message.getSentAt();
            if (sentAt != null) {
                timeText = DateUtils.getRelativeTimeSpanString(
                        sentAt.getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                ).toString();
            }

            if (sent) {
                String readState = message.getIsRead()
                        ? itemView.getContext().getString(R.string.label_message_read)
                        : itemView.getContext().getString(R.string.label_message_sent);
                tvMessageStatus.setText(timeText.isEmpty() ? readState : readState + " • " + timeText);
            } else {
                tvMessageStatus.setText(timeText);
            }
        }
    }
}
