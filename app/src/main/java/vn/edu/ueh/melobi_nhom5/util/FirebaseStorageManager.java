package vn.edu.ueh.melobi_nhom5.util;

import android.net.Uri;
import android.text.TextUtils;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class FirebaseStorageManager {
    private final FirebaseStorage firebaseStorage;

    public interface Callback<T> {
        void onSuccess(T result);
        void onError(Exception e);
    }

    public FirebaseStorageManager() {
        this.firebaseStorage = FirebaseStorage.getInstance();
    }

    public void uploadAvatar(String userId, Uri file, Callback<String> callback) {
        if (TextUtils.isEmpty(userId)) {
            notifyError(callback, new IllegalArgumentException("userId không hợp lệ"));
            return;
        }
        if (file == null) {
            notifyError(callback, new IllegalArgumentException("File ảnh đại diện không hợp lệ"));
            return;
        }

        StorageReference avatarRef = firebaseStorage.getReference()
                .child("avatars")
                .child(userId)
                .child(System.currentTimeMillis() + ".jpg");
        uploadFile(avatarRef, file, callback);
    }

    public void uploadQuestionImage(String questionId, Uri file, Callback<String> callback) {
        if (TextUtils.isEmpty(questionId)) {
            notifyError(callback, new IllegalArgumentException("questionId không hợp lệ"));
            return;
        }
        if (file == null) {
            notifyError(callback, new IllegalArgumentException("File ảnh câu hỏi không hợp lệ"));
            return;
        }

        StorageReference questionImageRef = firebaseStorage.getReference()
                .child("question_images")
                .child(questionId)
                .child(System.currentTimeMillis() + ".jpg");
        uploadFile(questionImageRef, file, callback);
    }

    public void deleteImage(String url, Callback<Void> callback) {
        if (TextUtils.isEmpty(url)) {
            if (callback != null) {
                callback.onSuccess(null);
            }
            return;
        }

        firebaseStorage.getReferenceFromUrl(url)
                .delete()
                .addOnSuccessListener(unused -> {
                    if (callback != null) {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> notifyError(callback, e));
    }

    private void uploadFile(StorageReference storageReference, Uri file, Callback<String> callback) {
        storageReference.putFile(file)
                .continueWithTask(task -> {
                    if (!task.isSuccessful() && task.getException() != null) {
                        throw task.getException();
                    }
                    return storageReference.getDownloadUrl();
                })
                .addOnSuccessListener(uri -> {
                    if (callback != null) {
                        callback.onSuccess(uri.toString());
                    }
                })
                .addOnFailureListener(e -> notifyError(callback, e));
    }

    private void notifyError(Callback<?> callback, Exception e) {
        if (callback != null) {
            callback.onError(e);
        }
    }
}
