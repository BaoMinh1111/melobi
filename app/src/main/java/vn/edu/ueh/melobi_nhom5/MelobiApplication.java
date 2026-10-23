package vn.edu.ueh.melobi_nhom5;

import android.app.Application;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import vn.edu.ueh.melobi_nhom5.util.NetworkUtils;
import vn.edu.ueh.melobi_nhom5.util.SyncManager;

public class MelobiApplication extends Application {
    private static final String TAG = "MelobiApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        enableFirestorePersistence();
        NetworkUtils.initialize(this);
        NetworkUtils.registerNetworkListener();
        SyncManager.getInstance(this);
    }

    private void enableFirestorePersistence() {
        try {
            FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                    .setPersistenceEnabled(true)
                    .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build();
            FirebaseFirestore.getInstance().setFirestoreSettings(settings);
        } catch (Exception e) {
            Log.w(TAG, "Firestore persistence da duoc cau hinh", e);
        }
    }
}
