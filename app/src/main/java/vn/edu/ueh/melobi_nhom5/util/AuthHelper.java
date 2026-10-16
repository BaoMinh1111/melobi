package vn.edu.ueh.melobi_nhom5.util;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import vn.edu.ueh.melobi_nhom5.model.User;

public class AuthHelper {
    private static final String TAG = "AuthHelper";

    private AuthHelper() {
    }

    public static boolean isLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }

    public static User getCurrentUser() {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) {
            Log.d(TAG, "Chưa có người dùng đăng nhập");
            return null;
        }

        String name = fbUser.getDisplayName() != null && !fbUser.getDisplayName().trim().isEmpty()
                ? fbUser.getDisplayName().trim()
                : "Người dùng Melobi";
        String email = fbUser.getEmail() != null ? fbUser.getEmail() : "";
        String avatar = fbUser.getPhotoUrl() != null ? fbUser.getPhotoUrl().toString() : "";

        User user = new User(fbUser.getUid(), name, email, avatar, "it");
        user.setStatus(User.STATUS_ACTIVE);
        return user;
    }

    public static String getCurrentUserId() {
        User currentUser = getCurrentUser();
        return currentUser != null ? currentUser.getId() : null;
    }

    public static void signOut() {
        FirebaseAuth.getInstance().signOut();
    }
}
