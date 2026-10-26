package vn.edu.ueh.melobi_nhom5.adapter;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import vn.edu.ueh.melobi_nhom5.R;
import vn.edu.ueh.melobi_nhom5.entity.Like;
import vn.edu.ueh.melobi_nhom5.entity.Notification;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    private final List<Notification> items = new ArrayList<>();
    private final Map<String, String> actorNames = new HashMap<>();
    private final OnNotificationClickListener listener;

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setNotifications(List<Notification> notifications) {
        items.clear();
        if (notifications != null) {
            items.addAll(notifications);
        }
        notifyDataSetChanged();
    }

    public void setActorName(String actorId, String name) {
        if (actorId == null || actorId.trim().isEmpty()) {
            return;
        }
        actorNames.put(actorId, name);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notification notification = items.get(position);
        String actorName = actorNames.get(notification.getActorId());
        if (actorName == null || actorName.trim().isEmpty()) {
            actorName = holder.itemView.getContext().getString(R.string.notification_someone);
        }
        holder.tvText.setText(buildText(holder, notification, actorName));
        holder.tvAvatar.setText(actorName.substring(0, 1).toUpperCase(Locale.getDefault()));
        long createdAt = notification.getCreatedAt();
        if (createdAt > 0) {
            holder.tvTime.setText(DateUtils.getRelativeTimeSpanString(
                    createdAt,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS));
        } else {
            holder.tvTime.setText("");
        }
        holder.vUnread.setVisibility(notification.isRead() || notification.getIsRead() ? View.GONE : View.VISIBLE);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onNotificationClick(notification);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private String buildText(ViewHolder holder, Notification notification, String actorName) {
        String type = notification.getType() != null ? notification.getType() : "";
        String targetType = notification.getTargetType() != null ? notification.getTargetType() : "";
        if (Notification.TYPE_LIKE.equalsIgnoreCase(type) && Like.TYPE_ANSWER.equalsIgnoreCase(targetType)) {
            return holder.itemView.getContext().getString(R.string.notification_like_answer, actorName);
        }
        if (Notification.TYPE_LIKE.equalsIgnoreCase(type)) {
            return holder.itemView.getContext().getString(R.string.notification_like_question, actorName);
        }
        if (Notification.TYPE_NEW_ANSWER.equalsIgnoreCase(type)) {
            return holder.itemView.getContext().getString(R.string.notification_new_answer, actorName);
        }
        if (Notification.TYPE_NEW_COMMENT.equalsIgnoreCase(type)) {
            return holder.itemView.getContext().getString(R.string.notification_new_comment, actorName);
        }
        if (Notification.TYPE_FOLLOW.equalsIgnoreCase(type)) {
            return holder.itemView.getContext().getString(R.string.notification_follow, actorName);
        }
        if (Notification.TYPE_MESSAGE.equalsIgnoreCase(type)) {
            return holder.itemView.getContext().getString(R.string.notification_message, actorName);
        }
        return holder.itemView.getContext().getString(R.string.notification_generic, actorName);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAvatar;
        final TextView tvText;
        final TextView tvTime;
        final View vUnread;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvNotificationAvatar);
            tvText = itemView.findViewById(R.id.tvNotificationText);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            vUnread = itemView.findViewById(R.id.vUnreadDot);
        }
    }
}
