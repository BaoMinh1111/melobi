package vn.edu.ueh.melobi_nhom5.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import vn.edu.ueh.melobi_nhom5.R;
import vn.edu.ueh.melobi_nhom5.model.User;

public class FollowUserAdapter extends RecyclerView.Adapter<FollowUserAdapter.ViewHolder> {

    public interface OnFollowClickListener {
        void onFollowClick(User user);
    }

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    private final List<User> users = new ArrayList<>();
    private final Set<String> followingIds = new HashSet<>();
    private final String currentUserId;
    private final OnFollowClickListener followClickListener;
    private final OnUserClickListener userClickListener;
    private boolean hideFollowButton;
    private boolean compact;
    private String currentMajorId;
    private final Map<String, String> majorNames = new HashMap<>();
    private static final int COMPACT_LIMIT = 12;

    public FollowUserAdapter(String currentUserId,
                             OnFollowClickListener followClickListener,
                             OnUserClickListener userClickListener) {
        this.currentUserId = currentUserId;
        this.followClickListener = followClickListener;
        this.userClickListener = userClickListener;
    }

    public void setHideFollowButton(boolean hideFollowButton) {
        this.hideFollowButton = hideFollowButton;
        notifyDataSetChanged();
    }

    public void setCompact(boolean compact) {
        this.compact = compact;
        notifyDataSetChanged();
    }

    public void setCurrentMajorId(String majorId) {
        this.currentMajorId = majorId;
        notifyDataSetChanged();
    }

    public void setMajorNames(Map<String, String> names) {
        majorNames.clear();
        if (names != null) {
            majorNames.putAll(names);
        }
        notifyDataSetChanged();
    }

    public void setUsers(List<User> newUsers) {
        users.clear();
        if (newUsers != null) {
            if (compact && newUsers.size() > COMPACT_LIMIT) {
                users.addAll(newUsers.subList(0, COMPACT_LIMIT));
            } else {
                users.addAll(newUsers);
            }
        }
        notifyDataSetChanged();
    }

    public void setFollowingIds(Set<String> ids) {
        followingIds.clear();
        if (ids != null) {
            followingIds.addAll(ids);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = compact ? R.layout.item_suggest_user : R.layout.item_follow_user;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        String userId = user.getUserId();
        String name = vn.edu.ueh.melobi_nhom5.util.UserNameLookup.resolve(userId, user.getFullName());

        holder.tvName.setText(name);
        holder.tvUserId.setText(resolveMajorLabel(user, holder.itemView.getContext().getString(R.string.label_other_major)));
        holder.tvAvatar.setText(name.substring(0, 1).toUpperCase(Locale.getDefault()));

        boolean isSelf = currentUserId != null && currentUserId.equals(userId);
        if (hideFollowButton || isSelf) {
            holder.btnFollow.setVisibility(View.GONE);
        } else {
            holder.btnFollow.setVisibility(View.VISIBLE);
            boolean following = followingIds.contains(userId);
            holder.btnFollow.setText(following
                    ? holder.itemView.getContext().getString(R.string.action_following)
                    : holder.itemView.getContext().getString(R.string.action_follow));
            holder.btnFollow.setOnClickListener(v -> {
                if (followClickListener != null) {
                    followClickListener.onFollowClick(user);
                }
            });
        }

        holder.itemView.setOnClickListener(v -> {
            if (userClickListener != null) {
                userClickListener.onUserClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    private String resolveMajorLabel(User user, String fallback) {
        String majorId = user != null ? user.getMajorId() : null;
        if (majorId != null && !majorId.trim().isEmpty()) {
            String mapped = majorNames.get(majorId);
            if (mapped != null && !mapped.trim().isEmpty()) {
                return mapped;
            }
            for (String name : majorNames.values()) {
                if (majorId.equalsIgnoreCase(name)) {
                    return name;
                }
            }
            return majorId;
        }
        return fallback;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView tvAvatar;
        final TextView tvName;
        final TextView tvUserId;
        final MaterialButton btnFollow;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar = itemView.findViewById(R.id.tvFollowAvatar);
            tvName = itemView.findViewById(R.id.tvFollowName);
            tvUserId = itemView.findViewById(R.id.tvFollowUserId);
            btnFollow = itemView.findViewById(R.id.btnFollowAction);
        }
    }
}
