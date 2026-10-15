package vn.edu.ueh.melobi_nhom5.util;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import vn.edu.ueh.melobi_nhom5.model.User;

public class AuthHelper {
    private static final String TAG = "AuthHelper";

    public static void ensureAuth(Runnable onReady) {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) {
            if (onReady != null) onReady.run();
        } else {
            FirebaseAuth.getInstance().signInAnonymously()
                    .addOnSuccessListener(authResult -> {
                        if (authResult.getUser() != null) {
                            Log.d(TAG, "Đăng nhập ẩn danh thành công: " + authResult.getUser().getUid());
                        }
                        if (onReady != null) onReady.run();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Đăng nhập ẩn danh thất bại: ", e);
                        if (onReady != null) onReady.run();
                    });
        }
    }

    public static User getCurrentUser() {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser != null) {
            String name = fbUser.getDisplayName() != null && !fbUser.getDisplayName().isEmpty()
                    ? fbUser.getDisplayName() : "Người dùng Melobi";
            String email = fbUser.getEmail() != null ? fbUser.getEmail() : "";
            String avatar = fbUser.getPhotoUrl() != null ? fbUser.getPhotoUrl().toString() : "";
            return new User(fbUser.getUid(), name, email, avatar, "Công nghệ thông tin");
        }

        // Mock user for testing if Firebase Auth is not signed in
        return new User(
                "user_giang_test",
                "Hương Giang (Thành viên)",
                "giang.test@melobi.edu.vn",
                "",
                "Công nghệ thông tin"
        );
    }

    public static String getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
