package vn.edu.ueh.melobi_nhom5.service;

import android.net.Uri;
import android.text.TextUtils;
import android.util.Patterns;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.MetadataChanges;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Source;

import vn.edu.ueh.melobi_nhom5.util.NetworkUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vn.edu.ueh.melobi_nhom5.model.Major;
import vn.edu.ueh.melobi_nhom5.model.User;
import vn.edu.ueh.melobi_nhom5.util.AuthHelper;
import vn.edu.ueh.melobi_nhom5.util.SampleDataInitializer;
import vn.edu.ueh.melobi_nhom5.util.UserNameLookup;

public class UserService {
    private static final String COLLECTION_MAJORS = "majors";
    private static final String COLLECTION_USERS = "users";
    private static final String DEFAULT_MAJOR_ID = "it";

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public UserService() {
        this.auth = FirebaseAuth.getInstance();
        this.db = FirebaseFirestore.getInstance();
    }

    public void register(String email,
                         String password,
                         String fullName,
                         Callback<User> callback) {
        String normalizedName = fullName != null ? fullName.trim() : "";
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";

        if (TextUtils.isEmpty(normalizedName)) {
            notifyError(callback, new IllegalArgumentException("Họ tên không được để trống"));
            return;
        }
        if (TextUtils.isEmpty(normalizedEmail) || !Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            notifyError(callback, new IllegalArgumentException("Email không hợp lệ"));
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            notifyError(callback, new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự"));
            return;
        }

        auth.createUserWithEmailAndPassword(normalizedEmail, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        notifyError(callback, new IllegalStateException("Không tạo được tài khoản Firebase"));
                        return;
                    }

                    firebaseUser.updateProfile(
                            new UserProfileChangeRequest.Builder()
                                    .setDisplayName(normalizedName)
                                    .build()
                    ).addOnCompleteListener(task -> {
                        User newUser = new User(
                                firebaseUser.getUid(),
                                normalizedName,
                                normalizedEmail,
                                "",
                                DEFAULT_MAJOR_ID
                        );
                        newUser.setBio("");
                        newUser.setRole(User.ROLE_USER);
                        newUser.setStatus(User.STATUS_ACTIVE);
                        newUser.setFollowerCount(0);
                        newUser.setFollowingCount(0);
                        newUser.setCreatedAt(System.currentTimeMillis());

                        saveUserDocument(newUser, callback);
                    });
                })
                .addOnFailureListener(e -> notifyError(callback, mapAuthException(e)));
    }

    public void register(String email,
                         String password,
                         String fullName,
                         String majorId,
                         Callback<User> callback) {
        register(email, password, fullName, new Callback<User>() {
            @Override
            public void onSuccess(User result) {
                if (TextUtils.isEmpty(majorId)) {
                    if (callback != null) {
                        callback.onSuccess(result);
                    }
                    return;
                }

                updateMajor(result.getUserId(), majorId, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void ignored) {
                        result.setMajorId(majorId);
                        if (callback != null) {
                            callback.onSuccess(result);
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        notifyError(callback, e);
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                notifyError(callback, e);
            }
        });
    }

    public void login(String email, String password, Callback<User> callback) {
        String normalizedEmail = email != null ? email.trim().toLowerCase() : "";

        if (TextUtils.isEmpty(normalizedEmail) || !Patterns.EMAIL_ADDRESS.matcher(normalizedEmail).matches()) {
            notifyError(callback, new IllegalArgumentException("Email không hợp lệ"));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            notifyError(callback, new IllegalArgumentException("Mật khẩu không được để trống"));
            return;
        }

        auth.signInWithEmailAndPassword(normalizedEmail, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        notifyError(callback, new IllegalStateException("Không lấy được thông tin tài khoản"));
                        return;
                    }
                    handleSignedInFirebaseUser(firebaseUser, callback);
                })
                .addOnFailureListener(e -> notifyError(callback, mapAuthException(e)));
    }

    public void loginWithGoogle(String idToken, Callback<User> callback) {
        if (TextUtils.isEmpty(idToken)) {
            notifyError(callback, new IllegalArgumentException("Google ID Token không hợp lệ"));
            return;
        }

        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnSuccessListener(authResult -> {
                    FirebaseUser firebaseUser = authResult.getUser();
                    if (firebaseUser == null) {
                        notifyError(callback, new IllegalStateException("Không lấy được thông tin tài khoản Google"));
                        return;
                    }
                    handleSignedInFirebaseUser(firebaseUser, callback);
                })
                .addOnFailureListener(e -> notifyError(callback, mapAuthException(e)));
    }

    public void logout() {
        try {
            GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            GoogleSignIn.getClient(FirebaseApp.getInstance().getApplicationContext(), googleSignInOptions).signOut();
        } catch (Exception ignored) {
            // Không chặn đăng xuất Firebase nếu GoogleSignIn client không khả dụng.
        }
        AuthHelper.signOut();
    }

    public ListenerRegistration listenProfile(String userId, EventListener<DocumentSnapshot> listener) {
        return db.collection(COLLECTION_USERS).document(userId)
                .addSnapshotListener(MetadataChanges.INCLUDE, listener);
    }

    public ListenerRegistration listenUsers(EventListener<QuerySnapshot> listener) {
        return db.collection(COLLECTION_USERS)
                .addSnapshotListener(MetadataChanges.INCLUDE, listener);
    }

    public static User fromSnapshot(DocumentSnapshot snapshot) {
        if (snapshot == null || !snapshot.exists()) {
            return null;
        }
        User user = snapshot.toObject(User.class);
        if (user == null) {
            user = new User();
        }
        user.setUserId(snapshot.getId());
        String name = firstNonEmpty(
                snapshot.getString("fullName"),
                snapshot.getString("displayName"),
                snapshot.getString("name")
        );
        if (!TextUtils.isEmpty(name)) {
            user.setFullName(name);
            UserNameLookup.put(user.getUserId(), name);
        }
        return user;
    }

    private static String firstNonEmpty(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (!TextUtils.isEmpty(value) && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    public void getProfile(String userId, Callback<User> callback) {
        if (TextUtils.isEmpty(userId)) {
            notifyError(callback, new IllegalArgumentException("userId không hợp lệ"));
            return;
        }

        Source source = NetworkUtils.isOnline() ? Source.SERVER : Source.DEFAULT;
        db.collection(COLLECTION_USERS)
                .document(userId)
                .get(source)
                .addOnSuccessListener(documentSnapshot -> {
                    if (!documentSnapshot.exists()) {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                        return;
                    }

                    User user = fromSnapshot(documentSnapshot);
                    cacheIfCurrentUser(user);
                    if (callback != null) {
                        callback.onSuccess(user);
                    }
                })
                .addOnFailureListener(e -> {
                    if (source != Source.SERVER) {
                        notifyError(callback, e);
                        return;
                    }
                    db.collection(COLLECTION_USERS)
                            .document(userId)
                            .get(Source.DEFAULT)
                            .addOnSuccessListener(fallbackSnapshot -> {
                                if (!fallbackSnapshot.exists()) {
                                    if (callback != null) {
                                        callback.onSuccess(null);
                                    }
                                    return;
                                }
                                User user = fromSnapshot(fallbackSnapshot);
                                cacheIfCurrentUser(user);
                                if (callback != null) {
                                    callback.onSuccess(user);
                                }
                            })
                            .addOnFailureListener(fallbackError -> notifyError(callback, fallbackError));
                });
    }

    public void updateProfile(User user, Callback<User> callback) {
        if (user == null || TextUtils.isEmpty(user.getUserId())) {
            notifyError(callback, new IllegalArgumentException("Thông tin người dùng không hợp lệ"));
            return;
        }
        if (!NetworkUtils.isOnline()) {
            notifyError(callback, NetworkUtils.offlineException());
            return;
        }

        saveUserDocument(user, new Callback<User>() {
            @Override
            public void onSuccess(User result) {
                FirebaseUser currentFirebaseUser = auth.getCurrentUser();
                if (currentFirebaseUser != null && user.getUserId().equals(currentFirebaseUser.getUid())) {
                    UserProfileChangeRequest.Builder builder = new UserProfileChangeRequest.Builder()
                            .setDisplayName(user.getFullName());
                    if (!TextUtils.isEmpty(user.getAvatarUrl())) {
                        builder.setPhotoUri(Uri.parse(user.getAvatarUrl()));
                    }
                    currentFirebaseUser.updateProfile(builder.build());
                }
                if (callback != null) {
                    callback.onSuccess(result);
                }
            }

            @Override
            public void onError(Exception e) {
                notifyError(callback, e);
            }
        });
    }

    public void updateMajor(String userId, String majorId, Callback<Void> callback) {
        if (TextUtils.isEmpty(userId)) {
            notifyError(callback, new IllegalArgumentException("userId không hợp lệ"));
            return;
        }
        if (TextUtils.isEmpty(majorId)) {
            notifyError(callback, new IllegalArgumentException("majorId không hợp lệ"));
            return;
        }

        db.collection(COLLECTION_USERS)
                .document(userId)
                .set(new UserMajorUpdate(majorId), SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> notifyError(callback, e));
    }

    public void getAllMajors(Callback<List<Major>> callback) {
        db.collection(COLLECTION_MAJORS)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<Major> majors = new ArrayList<>();
                    for (DocumentSnapshot documentSnapshot : querySnapshot.getDocuments()) {
                        Major major = documentSnapshot.toObject(Major.class);
                        if (major != null) {
                            if (TextUtils.isEmpty(major.getMajorId())) {
                                major.setMajorId(documentSnapshot.getId());
                            }
                            majors.add(major);
                        }
                    }

                    if (!majors.isEmpty()) {
                        if (callback != null) {
                            callback.onSuccess(majors);
                        }
                        return;
                    }

                    List<Major> fallbackMajors = SampleDataInitializer.getSampleMajors();
                    SampleDataInitializer.initializeSampleMajors(new SampleDataInitializer.InitCallback() {
                        @Override
                        public void onSuccess(String message) {
                            if (callback != null) {
                                callback.onSuccess(fallbackMajors);
                            }
                        }

                        @Override
                        public void onError(Exception e) {
                            if (callback != null) {
                                callback.onSuccess(fallbackMajors);
                            }
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    if (callback != null) {
                        callback.onSuccess(SampleDataInitializer.getSampleMajors());
                    }
                });
    }

    public void lockUser(String userId, Callback<Void> callback) {
        updateUserStatus(userId, User.STATUS_LOCKED, callback);
    }

    public void unlockUser(String userId, Callback<Void> callback) {
        updateUserStatus(userId, User.STATUS_ACTIVE, callback);
    }

    public boolean isLoggedIn() {
        return auth.getCurrentUser() != null;
    }

    private void handleSignedInFirebaseUser(FirebaseUser firebaseUser, Callback<User> callback) {
        getProfile(firebaseUser.getUid(), new Callback<User>() {
            @Override
            public void onSuccess(User result) {
                if (result == null) {
                    User fallbackUser = buildFallbackUser(firebaseUser);
                    saveUserDocument(fallbackUser, callback);
                    return;
                }

                if (User.STATUS_LOCKED.equalsIgnoreCase(result.getStatus())) {
                    logout();
                    notifyError(callback, new IllegalStateException("Tài khoản này đã bị khóa. Vui lòng liên hệ quản trị viên."));
                    return;
                }

                if (TextUtils.isEmpty(result.getFullName())) {
                    result.setFullName(resolveDisplayName(firebaseUser));
                }
                if (TextUtils.isEmpty(result.getEmail())) {
                    result.setEmail(firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "");
                }
                if (TextUtils.isEmpty(result.getAvatarUrl()) && firebaseUser.getPhotoUrl() != null) {
                    result.setAvatarUrl(firebaseUser.getPhotoUrl().toString());
                }
                if (TextUtils.isEmpty(result.getMajorId())) {
                    result.setMajorId(DEFAULT_MAJOR_ID);
                }
                if (TextUtils.isEmpty(result.getRole())) {
                    result.setRole(User.ROLE_USER);
                }
                if (TextUtils.isEmpty(result.getStatus())) {
                    result.setStatus(User.STATUS_ACTIVE);
                }

                cacheIfCurrentUser(result);
                if (callback != null) {
                    callback.onSuccess(result);
                }
            }

            @Override
            public void onError(Exception e) {
                notifyError(callback, e);
            }
        });
    }

    private User buildFallbackUser(FirebaseUser firebaseUser) {
        User user = new User(
                firebaseUser.getUid(),
                resolveDisplayName(firebaseUser),
                firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "",
                firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "",
                DEFAULT_MAJOR_ID
        );
        user.setBio("");
        user.setRole(User.ROLE_USER);
        user.setStatus(User.STATUS_ACTIVE);
        user.setFollowerCount(0);
        user.setFollowingCount(0);
        user.setCreatedAt(System.currentTimeMillis());
        return user;
    }

    private String resolveDisplayName(FirebaseUser firebaseUser) {
        if (firebaseUser.getDisplayName() != null && !firebaseUser.getDisplayName().trim().isEmpty()) {
            return firebaseUser.getDisplayName().trim();
        }
        if (firebaseUser.getEmail() != null && firebaseUser.getEmail().contains("@")) {
            return firebaseUser.getEmail().substring(0, firebaseUser.getEmail().indexOf('@'));
        }
        return "Người dùng Melobi";
    }

    private void saveUserDocument(User user, Callback<User> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getUserId());
        data.put("fullName", user.getFullName() != null ? user.getFullName() : "");
        data.put("email", user.getEmail() != null ? user.getEmail() : "");
        data.put("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : "");
        data.put("bio", user.getBio() != null ? user.getBio() : "");
        data.put("majorId", user.getMajorId() != null ? user.getMajorId() : "");
        data.put("role", user.getRole() != null ? user.getRole() : User.ROLE_USER);
        data.put("status", user.getStatus() != null ? user.getStatus() : User.STATUS_ACTIVE);
        data.put("followerCount", user.getFollowerCount());
        data.put("followingCount", user.getFollowingCount());
        data.put("displayName", FieldValue.delete());
        if (user.getCreatedAt() > 0) {
            data.put("createdAt", user.getCreatedAt());
        }
        db.collection(COLLECTION_USERS)
                .document(user.getUserId())
                .set(data, SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    cacheIfCurrentUser(user);
                    if (callback != null) {
                        callback.onSuccess(user);
                    }
                })
                .addOnFailureListener(e -> notifyError(callback, e));
    }

    private void cacheIfCurrentUser(User user) {
        if (user != null) {
            UserNameLookup.put(user.getUserId(), user.getFullName());
        }
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (user != null && firebaseUser != null && firebaseUser.getUid().equals(user.getUserId())) {
            AuthHelper.setCurrentUser(user);
        }
    }

    private void updateUserStatus(String userId, String status, Callback<Void> callback) {
        if (TextUtils.isEmpty(userId)) {
            notifyError(callback, new IllegalArgumentException("userId không hợp lệ"));
            return;
        }

        db.collection(COLLECTION_USERS)
                .document(userId)
                .set(new UserStatusUpdate(status), SetOptions.merge())
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> notifyError(callback, e));
    }

    private void notifyError(Callback<?> callback, Exception e) {
        if (callback != null) {
            callback.onError(e);
        }
    }

    private Exception mapAuthException(Exception e) {
        if (e instanceof FirebaseAuthUserCollisionException) {
            return new IllegalStateException("Email này đã được đăng ký tài khoản.", e);
        }
        if (e instanceof FirebaseAuthWeakPasswordException) {
            return new IllegalStateException("Mật khẩu quá yếu, vui lòng dùng ít nhất 6 ký tự.", e);
        }
        if (e instanceof FirebaseAuthInvalidCredentialsException) {
            return new IllegalStateException("Email hoặc mật khẩu không chính xác.", e);
        }
        if (e instanceof FirebaseAuthInvalidUserException) {
            return new IllegalStateException("Tài khoản không tồn tại hoặc đã bị vô hiệu hóa.", e);
        }
        if (e instanceof FirebaseNetworkException) {
            return new IllegalStateException("Không có kết nối mạng. Vui lòng kiểm tra Internet.", e);
        }
        return e;
    }

    private static class UserMajorUpdate {
        private final String majorId;

        UserMajorUpdate(String majorId) {
            this.majorId = majorId;
        }

        @SuppressWarnings("unused")
        public String getMajorId() {
            return majorId;
        }
    }

    private static class UserStatusUpdate {
        private final String status;

        UserStatusUpdate(String status) {
            this.status = status;
        }

        @SuppressWarnings("unused")
        public String getStatus() {
            return status;
        }
    }
}
