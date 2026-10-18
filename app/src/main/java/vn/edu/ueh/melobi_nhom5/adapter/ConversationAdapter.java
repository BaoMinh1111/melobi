package vn.edu.ueh.melobi_nhom5.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.edu.ueh.melobi_nhom5.R;
import vn.edu.ueh.melobi_nhom5.entity.Conversation;

public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.ConversationViewHolder> {

    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation, String otherUserId);
    }

    private final String currentUserId;
    private final OnConversationClickListener listener;
    private final List<Conversation> conversations = new ArrayList<>();
    private final Map<String, String> partnerNames = new HashMap<>();

    public ConversationAdapter(String currentUserId, OnConversationClickListener listener) {
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    public void setConversations(List<Conversation> items) {
        conversations.clear();
        if (items != null) {
            conversations.addAll(items);
        }
        notifyDataSetChanged();
    }

    public void setPartnerName(String userId, String name) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        partnerNames.put(userId, name);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {
        holder.bind(conversations.get(position), currentUserId, partnerNames, listener);
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    static class ConversationViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvConversationAvatar;
        private final TextView tvConversationName;
        private final TextView tvConversationLastMessage;
        private final TextView tvConversationTime;
        private final View viewUnreadDot;

        ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvConversationAvatar = itemView.findViewById(R.id.tvConversationAvatar);
            tvConversationName = itemView.findViewById(R.id.tvConversationName);
            tvConversationLastMessage = itemView.findViewById(R.id.tvConversationLastMessage);
            tvConversationTime = itemView.findViewById(R.id.tvConversationTime);
            viewUnreadDot = itemView.findViewById(R.id.viewUnreadDot);
        }

        void bind(Conversation conversation,
                  String currentUserId,
                  Map<String, String> partnerNames,
                  OnConversationClickListener listener) {
            String otherUserId = resolveOtherUserId(conversation, currentUserId);
            String partnerName = partnerNames.get(otherUserId);
            if (partnerName == null || partnerName.trim().isEmpty()) {
                partnerName = "Thành viên";
            }

            tvConversationName.setText(partnerName);
            tvConversationAvatar.setText(partnerName.substring(0, 1).toUpperCase());

            String lastMessage = conversation.getLastMessage();
            tvConversationLastMessage.setText(
                    lastMessage != null && !lastMessage.trim().isEmpty()
                            ? lastMessage
                            : itemView.getContext().getString(R.string.empty_last_message)
            );

            Date updatedAt = conversation.getUpdatedAt();
            if (updatedAt != null) {
                tvConversationTime.setText(DateUtils.getRelativeTimeSpanString(
                        updatedAt.getTime(),
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                ));
            } else {
                tvConversationTime.setText("");
            }
            viewUnreadDot.setVisibility(View.GONE);

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onConversationClick(conversation, otherUserId);
                }
            });
        }

        private String resolveOtherUserId(Conversation conversation, String currentUserId) {
            if (currentUserId != null && currentUserId.equals(conversation.getUser1Id())) {
                return conversation.getUser2Id();
            }
            return conversation.getUser1Id();
        }
    }
}
