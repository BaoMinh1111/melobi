package vn.edu.ueh.melobi_nhom5.adapter;

import android.graphics.Color;
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
import java.util.Map;

import vn.edu.ueh.melobi_nhom5.R;
import vn.edu.ueh.melobi_nhom5.entity.Notification;   // entity của nhóm, không phải android.app.Notification

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }

    private static final String DEFAULT_ACTOR = "Ai đó";

    private final List<Notification> items = new ArrayList<>();
    private final Map<String, String> actorNames = new HashMap<>();   // actorId -> tên hiển thị
    private final OnNotificationClickListener listener;

    public NotificationAdapter(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<Notification> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public List<Notification> getItems() {
        return items;
    }

    public void putActorName(String actorId, String name) {
        if (actorId == null || name == null || name.isEmpty()) return;
        actorNames.put(actorId, name);
        notifyDataSetChanged();
    }

    public boolean hasActorName(String actorId) {
        return actorNames.containsKey(actorId);
    }

    public void markAllReadLocally() {
        for (Notification n : items) n.setRead(true);
        notifyDataSetChanged();
    }

    public void refresh() {
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Notification n = items.get(position);

        String actor = actorNames.get(n.getActorId());
        if (actor == null) actor = DEFAULT_ACTOR;

        h.tvAvatar.setText(actor.substring(0, 1).toUpperCase());
        h.tvMessage.setText(actor + " " + actionText(n.getType()));

        if (n.getCreatedAt() != null) {   // null khi server timestamp chưa kịp ghi
            h.tvTime.setText(DateUtils.getRelativeTimeSpanString(
                    n.getCreatedAt().getTime(), System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE));
        } else {
            h.tvTime.setText("Vừa xong");
        }

        h.tvUnreadDot.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);
        h.root.setBackgroundColor(n.isRead() ? Color.TRANSPARENT : Color.parseColor("#E8F0FE"));

        h.root.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(n);
        });
    }

    private static String actionText(String type) {
        if (Notification.TYPE_LIKE.equals(type)) return "đã thích nội dung của bạn";
        if (Notification.TYPE_NEW_ANSWER.equals(type)) return "đã trả lời câu hỏi của bạn";
        if (Notification.TYPE_NEW_COMMENT.equals(type)) return "đã bình luận câu trả lời của bạn";
        if (Notification.TYPE_FOLLOW.equals(type)) return "đã theo dõi bạn";
        return "có tương tác mới với bạn";
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final View root;
        final TextView tvAvatar, tvMessage, tvTime, tvUnreadDot;

        VH(@NonNull View itemView) {
            super(itemView);
            root = itemView.findViewById(R.id.notifRoot);
            tvAvatar = itemView.findViewById(R.id.tvNotifAvatar);
            tvMessage = itemView.findViewById(R.id.tvNotifMessage);
            tvTime = itemView.findViewById(R.id.tvNotifTime);
            tvUnreadDot = itemView.findViewById(R.id.tvUnreadDot);
        }
    }
}