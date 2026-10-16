
package vn.edu.ueh.melobi_nhom5;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.edu.ueh.melobi_nhom5.adapter.FollowUserAdapter;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.service.FollowService;

public class FollowListActivity extends AppCompatActivity {

    // User dang xem (du lieu demo)
    private static final String CURRENT_USER_ID = "U01";

    private static final String KEY_FROM = "follow_from";
    private static final String KEY_TO = "follow_to";
    private static final String KEY_TAB = "selected_tab";

    private FollowService followService;
    private FollowUserAdapter adapter;

    private final Map<String, User> users = new HashMap<>();

    private TextView tvFollowersCount;
    private TextView tvFollowingCount;
    private TextView tvEmptyFollow;

    private View tabFollowers;
    private View tabFollowing;

    private boolean showingFollowers = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_follow_list);

        // Khoi tao service
        followService = new FollowService();

        // Tao du lieu mau hoac khoi phuc trang thai
        initDemoUsers();
        restoreOrCreateFollows(savedInstanceState);

        if (savedInstanceState != null) {
            showingFollowers =
                    savedInstanceState.getBoolean(KEY_TAB, true);
        }

        initViews();
        setupRecyclerView();
        setupTabs();

        refreshUI();
    }

    private void initViews() {
        tvFollowersCount = findViewById(
                R.id.tvFollowersCount
        );

        tvFollowingCount = findViewById(
                R.id.tvFollowingCount
        );

        tvEmptyFollow = findViewById(
                R.id.tvEmptyFollow
        );

        tabFollowers = findViewById(
                R.id.tabFollowers
        );

        tabFollowing = findViewById(
                R.id.tabFollowing
        );

        MaterialToolbar toolbar = findViewById(
                R.id.toolbarFollow
        );

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    // Tao 4 user demo
    private void initDemoUsers() {
        addUser("U01", "Tài khoản của bạn");
        addUser("U02", "Nguyễn Văn An");
        addUser("U03", "Trần Thị Bình");
        addUser("U04", "Lê Minh Châu");
    }

    private void addUser(String id, String name) {
        User user = new User(id, name, "");
        users.put(id, user);
    }

    // Tao du lieu Follow ban dau
    private void restoreOrCreateFollows(Bundle state) {
        if (state == null) {
            followService.followUser("U02", "U01");
            followService.followUser("U03", "U01");

            followService.followUser("U01", "U03");
            followService.followUser("U01", "U04");
            return;
        }

        ArrayList<String> from = state.getStringArrayList(KEY_FROM);
        ArrayList<String> to = state.getStringArrayList(KEY_TO);

        if (from == null || to == null
                || from.size() != to.size()) {
            return;
        }

        for (int i = 0; i < from.size(); i++) {
            followService.followUser(from.get(i), to.get(i));
        }
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(
                R.id.rvFollowUsers
        );

        recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new FollowUserAdapter(
                CURRENT_USER_ID,
                followService,
                this::onFollowClick
        );

        recyclerView.setAdapter(adapter);
    }

    private void setupTabs() {
        tabFollowers.setOnClickListener(v -> {
            showingFollowers = true;
            refreshUI();
        });

        tabFollowing.setOnClickListener(v -> {
            showingFollowers = false;
            refreshUI();
        });
    }

    // Xu ly nhan nut Follow / Unfollow
    private void onFollowClick(User user) {
        String targetId = user.getUserId();

        if (CURRENT_USER_ID.equals(targetId)) {
            return;
        }

        if (followService.isFollowing(
                CURRENT_USER_ID, targetId)) {

            followService.unfollowUser(
                    CURRENT_USER_ID, targetId
            );

        } else {

            followService.followUser(
                    CURRENT_USER_ID, targetId
            );
        }

        refreshUI();
    }

    // Cap nhat bo dem va danh sach
    private void refreshUI() {
        updateCounts();
        updateList();

        tabFollowers.setAlpha(showingFollowers ? 1f : 0.55f);
        tabFollowing.setAlpha(showingFollowers ? 0.55f : 1f);
    }

    private void updateCounts() {
        // Dong bo count cho cac User demo
        for (User user : users.values()) {
            String id = user.getUserId();

            user.setFollowerCount(
                    followService.getFollowerCount(id)
            );

            user.setFollowingCount(
                    followService.getFollowingCount(id)
            );
        }

        User currentUser = users.get(CURRENT_USER_ID);

        if (currentUser == null) {
            return;
        }

        tvFollowersCount.setText(
                String.valueOf(currentUser.getFollowerCount())
        );

        tvFollowingCount.setText(
                String.valueOf(currentUser.getFollowingCount())
        );
    }

    private void updateList() {
        List<String> userIds;

        if (showingFollowers) {
            userIds = followService.getFollowers(
                    CURRENT_USER_ID
            );
        } else {
            userIds = followService.getFollowing(
                    CURRENT_USER_ID
            );
        }

        List<User> result = new ArrayList<>();

        for (String userId : userIds) {
            User user = users.get(userId);

            if (user != null) {
                result.add(user);
            }
        }

        adapter.setUsers(result);

        if (result.isEmpty()) {
            tvEmptyFollow.setVisibility(View.VISIBLE);

            tvEmptyFollow.setText(
                    showingFollowers
                            ? "Chưa có người theo dõi"
                            : "Bạn chưa theo dõi ai"
            );
        } else {
            tvEmptyFollow.setVisibility(View.GONE);
        }
    }

    // Giu du lieu demo khi xoay man hinh
    @Override
    protected void onSaveInstanceState(
            @NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        ArrayList<String> from = new ArrayList<>();
        ArrayList<String> to = new ArrayList<>();

        for (String userId : users.keySet()) {
            for (String targetId :
                    followService.getFollowing(userId)) {
                from.add(userId);
                to.add(targetId);
            }
        }

        outState.putStringArrayList(KEY_FROM, from);
        outState.putStringArrayList(KEY_TO, to);
        outState.putBoolean(KEY_TAB, showingFollowers);
    }
}
