
package vn.edu.ueh.melobi_nhom5.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.edu.ueh.melobi_nhom5.R;
import vn.edu.ueh.melobi_nhom5.entity.Conversation;

public class ConversationAdapter
        extends RecyclerView.Adapter<ConversationAdapter.ViewHolder> {

    private final List<Conversation> conversations =
            new ArrayList<>();

    // Luu ten hien thi theo userId
    private final Map<String, String> userNames =
            new HashMap<>();

    private final String currentUserId;
    private final OnConversationClickListener listener;

    public interface OnConversationClickListener {
        void onConversationClick(
                Conversation conversation,
                String otherUserId
        );
    }

    public ConversationAdapter(
            String currentUserId,
            OnConversationClickListener listener) {

        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    // Cap nhat danh sach cuoc tro chuyen
    public void setConversations(
            List<Conversation> newConversations) {

        conversations.clear();

        if (newConversations != null) {
            conversations.addAll(newConversations);
        }

        notifyDataSetChanged();
    }

    // Cap nhat ten nguoi dung khi da tai tu Firebase
    public void setUserNames(Map<String, String> names) {
        userNames.clear();

        if (names != null) {
            userNames.putAll(names);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_conversation,
                        parent,
                        false
                );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        Conversation conversation =
                conversations.get(position);

        // Xac dinh nguoi dang tro chuyen voi minh
        String otherUserId;

        if (currentUserId.equals(conversation.getUserId1())) {
            otherUserId = conversation.getUserId2();

        } else if (currentUserId.equals(
                conversation.getUserId2())) {
            otherUserId = conversation.getUserId1();

        } else {
            otherUserId = null;
        }

        // Hien thi ten, tam dung ID neu chua tai profile
        String name = otherUserId == null
                ? "Người dùng"
                : userNames.get(otherUserId);

        if (name == null || name.trim().isEmpty()) {
            name = otherUserId;
        }

        if (name == null || name.trim().isEmpty()) {
            name = "Người dùng";
        }

        holder.tvName.setText(name);

        // Avatar bang chu cai dau
        holder.tvAvatar.setText(
                name.substring(0, 1)
                        .toUpperCase(Locale.getDefault())
        );

        // Tin nhan cuoi
        String lastMessage = conversation.getLastMessage();

        if (lastMessage == null || lastMessage.isEmpty()) {
            lastMessage = "Bắt đầu trò chuyện";
        }

        holder.tvLastMessage.setText(lastMessage);

        // Thoi gian
        if (conversation.getUpdatedAt() != null) {

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "dd/MM HH:mm",
                            Locale.getDefault()
                    );

            holder.tvTime.setText(
                    format.format(conversation.getUpdatedAt())
            );

        } else {
            holder.tvTime.setText("");
        }

        // Nhan vao de mo ChatActivity
        if (otherUserId != null && listener != null) {
            holder.itemView.setOnClickListener(v ->
                    listener.onConversationClick(
                            conversation,
                            otherUserId
                    )
            );
        } else {
            holder.itemView.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return conversations.size();
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvAvatar;
        TextView tvName;
        TextView tvLastMessage;
        TextView tvTime;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvAvatar = itemView.findViewById(
                    R.id.tvConversationAvatar
            );

            tvName = itemView.findViewById(
                    R.id.tvConversationName
            );

            tvLastMessage = itemView.findViewById(
                    R.id.tvConversationLastMessage
            );

            tvTime = itemView.findViewById(
                    R.id.tvConversationTime
            );
        }
    }
}
