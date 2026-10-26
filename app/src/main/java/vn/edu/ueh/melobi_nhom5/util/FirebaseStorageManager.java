package vn.edu.ueh.melobi_nhom5.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

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

    public static void loadBitmap(String url, int maxEdge, Callback<Bitmap> callback) {
        if (TextUtils.isEmpty(url)) {
            notifyStaticError(callback, new IllegalArgumentException("URL ảnh trống"));
            return;
        }
        Handler main = new Handler(Looper.getMainLooper());
        new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);
                connection.setInstanceFollowRedirects(true);
                connection.connect();
                int code = connection.getResponseCode();
                if (code >= 400) {
                    throw new IllegalStateException("Không tải được ảnh (" + code + ")");
                }
                byte[] data = readAllBytes(connection.getInputStream());
                Bitmap bitmap = decodeSampledBitmap(data, maxEdge);
                if (bitmap == null) {
                    throw new IllegalStateException("Không đọc được ảnh");
                }
                main.post(() -> {
                    if (callback != null) {
                        callback.onSuccess(bitmap);
                    }
                });
            } catch (Exception e) {
                main.post(() -> notifyStaticError(callback, e));
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private static byte[] readAllBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }

    private static Bitmap decodeSampledBitmap(byte[] data, int maxEdge) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, bounds);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = calculateInSampleSize(bounds, maxEdge, maxEdge);
        return BitmapFactory.decodeByteArray(data, 0, data.length, options);
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    private void notifyError(Callback<?> callback, Exception e) {
        notifyStaticError(callback, e);
    }

    private static void notifyStaticError(Callback<?> callback, Exception e) {
        if (callback != null) {
            callback.onError(e);
        }
    }
}
