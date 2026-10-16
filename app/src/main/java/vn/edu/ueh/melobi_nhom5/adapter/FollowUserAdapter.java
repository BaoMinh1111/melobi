
package vn.edu.ueh.melobi_nhom5.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vn.edu.ueh.melobi_nhom5.R;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.service.FollowService;

public class FollowUserAdapter
        extends RecyclerView.Adapter<FollowUserAdapter.ViewHolder> {

    private final List<User> users = new ArrayList<>();
    private final FollowService followService;
    private final String currentUserId;
    private final OnFollowClickListener listener;

    // Xu ly su kien khi nhan nut Follow
    public interface OnFollowClickListener {
        void onFollowClick(User user);
    }

    // Constructor
    public FollowUserAdapter(
            String currentUserId,
            FollowService followService,
            OnFollowClickListener listener) {

        this.currentUserId = currentUserId;
        this.followService = followService;
        this.listener = listener;
    }

    // Cap nhat danh sach nguoi dung
    public void setUsers(List<User> newUsers) {
        users.clear();

        if (newUsers != null) {
            users.addAll(newUsers);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_follow_user, parent, false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder, int position) {

        User user = users.get(position);

        String userId = user.getUserId();
        String name = user.getFullName();

        if (name == null || name.isEmpty()) {
            name = userId != null ? userId : "Nguoi dung";
        }

        // Hien thi thong tin
        holder.tvName.setText(name);
        holder.tvUserId.setText("@" + userId);

        // Avatar tam thoi bang chu cai dau
        String avatarLetter = name.substring(0, 1)
                .toUpperCase(Locale.getDefault());

        holder.tvAvatar.setText(avatarLetter);

        // Khong hien nut Follow voi chinh minh
        if (currentUserId.equals(userId)) {
            holder.btnFollow.setVisibility(View.GONE);
            return;
        }

        holder.btnFollow.setVisibility(View.VISIBLE);

        // Kiem tra trang thai Follow
        boolean following = followService.isFollowing(
                currentUserId, userId
        );

        holder.btnFollow.setText(
                following ? "Đang theo dõi" : "Theo dõi"
        );

        // Chuyen su kien ve Activity
        holder.btnFollow.setOnClickListener(v -> {
            if (listener != null) {
                listener.onFollowClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    // ViewHolder
    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        TextView tvAvatar;
        TextView tvName;
        TextView tvUserId;
        MaterialButton btnFollow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            tvAvatar = itemView.findViewById(
                    R.id.tvFollowAvatar
            );

            tvName = itemView.findViewById(
                    R.id.tvFollowName
            );

            tvUserId = itemView.findViewById(
                    R.id.tvFollowUserId
            );

            btnFollow = itemView.findViewById(
                    R.id.btnFollowAction
            );
        }
    }
}
