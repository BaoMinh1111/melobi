package vn.edu.ueh.melobi_nhom5;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.model.Major;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.service.UserService;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;
import vn.edu.ueh.melobi_nhom5.util.FirebaseStorageManager;

public class ProfileActivity extends AppCompatActivity {

    private MaterialToolbar toolbarProfile;
    private ImageView ivProfileAvatar;
    private TextView tvProfileInitial;
    private TextView tvAvatarStatus;
    private TextView tvFollowerCount;
    private TextView tvFollowingCount;
    private TextInputEditText etProfileFullName;
    private TextInputEditText etProfileEmail;
    private TextInputEditText etProfileBio;
    private Spinner spinnerProfileMajor;
    private Button btnChooseAvatar;
    private Button btnSaveProfile;
    private Button btnLogoutProfile;
    private ProgressBar pbProfile;

    private UserService userService;
    private FirebaseStorageManager firebaseStorageManager;
    private final List<Major> majorList = new ArrayList<>();
    private User currentProfile;
    private Uri selectedAvatarUri;

    private final ActivityResultLauncher<String> pickAvatarLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri == null) {
                    return;
                }
                selectedAvatarUri = uri;
                ivProfileAvatar.setVisibility(View.VISIBLE);
                ivProfileAvatar.setImageURI(uri);
                tvProfileInitial.setVisibility(View.GONE);
                tvAvatarStatus.setText("Đã chọn ảnh đại diện mới");
            });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!AuthHelper.isLoggedIn()) {
            navigateToLogin();
            return;
        }

        setContentView(R.layout.activity_profile);

        userService = new UserService();
        firebaseStorageManager = new FirebaseStorageManager();

        initViews();
        setupActions();
        loadMajors();
        loadProfile();
    }

    private void initViews() {
        toolbarProfile = findViewById(R.id.toolbarProfile);
        ivProfileAvatar = findViewById(R.id.ivProfileAvatar);
        tvProfileInitial = findViewById(R.id.tvProfileInitial);
        tvAvatarStatus = findViewById(R.id.tvAvatarStatus);
        tvFollowerCount = findViewById(R.id.tvFollowerCount);
        tvFollowingCount = findViewById(R.id.tvFollowingCount);
        etProfileFullName = findViewById(R.id.etProfileFullName);
        etProfileEmail = findViewById(R.id.etProfileEmail);
        etProfileBio = findViewById(R.id.etProfileBio);
        spinnerProfileMajor = findViewById(R.id.spinnerProfileMajor);
        btnChooseAvatar = findViewById(R.id.btnChooseAvatar);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnLogoutProfile = findViewById(R.id.btnLogoutProfile);
        pbProfile = findViewById(R.id.pbProfile);
    }

    private void setupActions() {
        toolbarProfile.setNavigationOnClickListener(v -> finish());
        btnChooseAvatar.setOnClickListener(v -> pickAvatarLauncher.launch("image/*"));
        btnSaveProfile.setOnClickListener(v -> saveProfile());
        btnLogoutProfile.setOnClickListener(v -> {
            userService.logout();
            navigateToLogin();
        });
    }

    private void loadMajors() {
        userService.getAllMajors(new UserService.Callback<List<Major>>() {
            @Override
            public void onSuccess(List<Major> result) {
                majorList.clear();
                if (result != null) {
                    majorList.addAll(result);
                }
                ArrayAdapter<Major> adapter = new ArrayAdapter<>(
                        ProfileActivity.this,
                        android.R.layout.simple_spinner_item,
                        majorList
                );
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerProfileMajor.setAdapter(adapter);
                syncMajorSelection();
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadProfile() {
        String userId = AuthHelper.getCurrentUserId();
        if (userId == null || userId.isEmpty()) {
            navigateToLogin();
            return;
        }

        setLoading(true);
        userService.getProfile(userId, new UserService.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                setLoading(false);
                currentProfile = result != null ? result : AuthHelper.getCurrentUser();
                if (currentProfile == null) {
                    navigateToLogin();
                    return;
                }
                bindProfile();
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void bindProfile() {
        etProfileFullName.setText(currentProfile.getFullName());
        etProfileEmail.setText(currentProfile.getEmail());
        etProfileBio.setText(currentProfile.getBio() != null ? currentProfile.getBio() : "");
        tvFollowerCount.setText(getString(R.string.label_follower_count) + ": " + currentProfile.getFollowerCount());
        tvFollowingCount.setText(getString(R.string.label_following_count) + ": " + currentProfile.getFollowingCount());
        syncMajorSelection();
        renderAvatar();
    }

    private void syncMajorSelection() {
        if (currentProfile == null || majorList.isEmpty()) {
            return;
        }

        String currentMajorId = currentProfile.getMajorId();
        for (int i = 0; i < majorList.size(); i++) {
            if (majorList.get(i).getMajorId().equals(currentMajorId)) {
                spinnerProfileMajor.setSelection(i);
                return;
            }
        }
    }

    private void renderAvatar() {
        if (selectedAvatarUri != null) {
            ivProfileAvatar.setVisibility(View.VISIBLE);
            ivProfileAvatar.setImageURI(selectedAvatarUri);
            tvProfileInitial.setVisibility(View.GONE);
            tvAvatarStatus.setText("Đã chọn ảnh đại diện mới");
            return;
        }

        String fullName = currentProfile != null ? currentProfile.getFullName() : "";
        String avatarUrl = currentProfile != null ? currentProfile.getAvatarUrl() : "";
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            tvAvatarStatus.setText("Đã có ảnh đại diện");
            loadAvatarFromUrl(avatarUrl, fullName);
            return;
        }

        showAvatarPlaceholder(fullName);
        tvAvatarStatus.setText(R.string.hint_avatar_status);
    }

    private void loadAvatarFromUrl(String avatarUrl, String fallbackName) {
        new Thread(() -> {
            try (InputStream inputStream = new URL(avatarUrl).openStream()) {
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                runOnUiThread(() -> {
                    if (bitmap != null) {
                        ivProfileAvatar.setVisibility(View.VISIBLE);
                        ivProfileAvatar.setImageBitmap(bitmap);
                        tvProfileInitial.setVisibility(View.GONE);
                    } else {
                        showAvatarPlaceholder(fallbackName);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> showAvatarPlaceholder(fallbackName));
            }
        }).start();
    }

    private void showAvatarPlaceholder(String fullName) {
        String initial = (fullName != null && !fullName.trim().isEmpty())
                ? fullName.trim().substring(0, 1).toUpperCase()
                : "M";
        ivProfileAvatar.setImageDrawable(null);
        ivProfileAvatar.setVisibility(View.INVISIBLE);
        tvProfileInitial.setVisibility(View.VISIBLE);
        tvProfileInitial.setText(initial);
    }

    private void saveProfile() {
        if (currentProfile == null) {
            Toast.makeText(this, "Không tìm thấy hồ sơ người dùng", Toast.LENGTH_SHORT).show();
            return;
        }

        String fullName = etProfileFullName.getText() != null ? etProfileFullName.getText().toString().trim() : "";
        String bio = etProfileBio.getText() != null ? etProfileBio.getText().toString().trim() : "";
        Major selectedMajor = (Major) spinnerProfileMajor.getSelectedItem();

        if (fullName.isEmpty()) {
            Toast.makeText(this, "Họ tên không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedMajor == null) {
            Toast.makeText(this, "Vui lòng chọn chuyên ngành", Toast.LENGTH_SHORT).show();
            return;
        }

        currentProfile.setFullName(fullName);
        currentProfile.setBio(bio);

        setLoading(true);
        if (selectedAvatarUri != null) {
            String oldAvatarUrl = currentProfile.getAvatarUrl();
            firebaseStorageManager.uploadAvatar(currentProfile.getUserId(), selectedAvatarUri, new FirebaseStorageManager.Callback<String>() {
                @Override
                public void onSuccess(String uploadedUrl) {
                    currentProfile.setAvatarUrl(uploadedUrl);
                    persistProfile(selectedMajor.getMajorId(), oldAvatarUrl);
                }

                @Override
                public void onError(Exception e) {
                    setLoading(false);
                    Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        persistProfile(selectedMajor.getMajorId(), null);
    }

    private void persistProfile(String selectedMajorId, String oldAvatarUrl) {
        userService.updateProfile(currentProfile, new UserService.Callback<User>() {
            @Override
            public void onSuccess(User result) {
                if (!selectedMajorId.equals(result.getMajorId())) {
                    userService.updateMajor(result.getUserId(), selectedMajorId, new UserService.Callback<Void>() {
                        @Override
                        public void onSuccess(Void ignored) {
                            result.setMajorId(selectedMajorId);
                            onProfileSaved(result, oldAvatarUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            setLoading(false);
                            Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }

                onProfileSaved(result, oldAvatarUrl);
            }

            @Override
            public void onError(Exception e) {
                setLoading(false);
                Toast.makeText(ProfileActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onProfileSaved(User updatedUser, String oldAvatarUrl) {
        currentProfile = updatedUser;
        selectedAvatarUri = null;
        setLoading(false);
        bindProfile();
        Toast.makeText(this, R.string.message_profile_updated, Toast.LENGTH_SHORT).show();

        if (oldAvatarUrl != null
                && !oldAvatarUrl.trim().isEmpty()
                && !oldAvatarUrl.equals(updatedUser.getAvatarUrl())) {
            firebaseStorageManager.deleteImage(oldAvatarUrl, new FirebaseStorageManager.Callback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    // Không cần thao tác UI bổ sung.
                }

                @Override
                public void onError(Exception e) {
                    // Không chặn luồng cập nhật hồ sơ nếu xóa ảnh cũ thất bại.
                }
            });
        }
    }

    private void setLoading(boolean isLoading) {
        pbProfile.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        btnChooseAvatar.setEnabled(!isLoading);
        btnSaveProfile.setEnabled(!isLoading);
        btnLogoutProfile.setEnabled(!isLoading);
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
