package vn.edu.ueh.melobi_nhom5.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NetworkUtils {
    public interface ConnectivityListener {
        void onConnectivityChanged(boolean online);
    }

    private static final String TAG = "NetworkUtils";
    private static final String PROBE_URL = "https://connectivitycheck.gstatic.com/generate_204";
    private static final long PROBE_INTERVAL_MS = 8000L;

    private static Context appContext;
    private static NetworkCallback registeredCallback;
    private static volatile Boolean onlineState;
    private static volatile long lastProbeAt;
    private static final List<ConnectivityListener> listeners = new CopyOnWriteArrayList<>();
    private static final ExecutorService probeExecutor = Executors.newSingleThreadExecutor();
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    private NetworkUtils() {
    }

    public static void initialize(Context context) {
        if (context == null) {
            return;
        }
        appContext = context.getApplicationContext();
        onlineState = hasValidatedLink(appContext);
        startTracking(appContext);
        refreshNow(null);
    }

    /**
     * Đăng ký lắng nghe mạng theo tài liệu: khi có mạng trở lại thì kích hoạt đồng bộ.
     */
    public static NetworkCallback registerNetworkListener() {
        addListener(online -> {
            if (online && appContext != null) {
                SyncManager.getInstance(appContext).syncAll();
            }
        });
        return registeredCallback;
    }

    public static boolean isOnline() {
        if (onlineState != null) {
            return onlineState;
        }
        return appContext != null && hasValidatedLink(appContext);
    }

    public static boolean isOnline(Context context) {
        if (onlineState != null) {
            return onlineState;
        }
        return hasValidatedLink(context);
    }

    public static void addListener(ConnectivityListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public static void removeListener(ConnectivityListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    public static IllegalStateException offlineException() {
        return new IllegalStateException("Đang ngoại tuyến. Vui lòng thử lại khi có mạng.");
    }

    public static void refreshNow(Runnable after) {
        probeExecutor.execute(() -> {
            probeInternal();
            if (after != null) {
                mainHandler.post(after);
            }
        });
    }

    public static void markOffline() {
        setOnlineState(false);
    }

    public static NetworkCallback registerNetworkListener(Runnable onNetworkAvailable) {
        if (onNetworkAvailable != null) {
            addListener(online -> {
                if (online) {
                    onNetworkAvailable.run();
                }
            });
        }
        return registeredCallback;
    }

    public static NetworkCallback registerNetworkListener(Context context, Runnable onNetworkAvailable) {
        if (context != null && appContext == null) {
            initialize(context);
        }
        return registerNetworkListener(onNetworkAvailable);
    }

    private static void startTracking(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }
        if (registeredCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(registeredCallback);
            } catch (Exception ignored) {
                // Callback cũ có thể đã bị hủy.
            }
            registeredCallback = null;
        }

        registeredCallback = new NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                refreshNow(null);
            }

            @Override
            public void onLost(Network network) {
                if (!hasValidatedLink(appContext)) {
                    setOnlineState(false);
                } else {
                    refreshNow(null);
                }
            }

            @Override
            public void onUnavailable() {
                setOnlineState(false);
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                if (!isValidated(networkCapabilities)) {
                    setOnlineState(false);
                    return;
                }
                long now = System.currentTimeMillis();
                if (now - lastProbeAt >= PROBE_INTERVAL_MS) {
                    refreshNow(null);
                }
            }
        };
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        try {
            connectivityManager.registerNetworkCallback(request, registeredCallback);
        } catch (Exception e) {
            Log.w(TAG, "Khong dang ky duoc NetworkCallback", e);
            registeredCallback = null;
        }
    }

    private static void probeInternal() {
        lastProbeAt = System.currentTimeMillis();
        if (!hasValidatedLink(appContext)) {
            setOnlineState(false);
            return;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(PROBE_URL).openConnection();
            connection.setConnectTimeout(2500);
            connection.setReadTimeout(2500);
            connection.setInstanceFollowRedirects(false);
            connection.setUseCaches(false);
            connection.setRequestMethod("GET");
            int code = connection.getResponseCode();
            setOnlineState(code >= 200 && code < 400);
        } catch (Exception e) {
            Log.d(TAG, "Probe internet that bai: " + e.getMessage());
            setOnlineState(false);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static void setOnlineState(boolean online) {
        Boolean previous = onlineState;
        onlineState = online;
        applyFirestoreNetwork(online);
        if (previous != null && previous == online) {
            return;
        }
        mainHandler.post(() -> {
            for (ConnectivityListener listener : listeners) {
                listener.onConnectivityChanged(online);
            }
        });
    }

    private static void applyFirestoreNetwork(boolean online) {
        try {
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            if (online) {
                firestore.enableNetwork().addOnCompleteListener(task -> {
                    if (appContext != null) {
                        SyncManager.getInstance(appContext).syncAll();
                    }
                });
            } else {
                firestore.disableNetwork();
            }
        } catch (Exception ignored) {
            // Firestore chua san sang.
        }
    }

    private static boolean hasValidatedLink(Context context) {
        if (context == null) {
            return false;
        }
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return false;
        }
        Network activeNetwork = connectivityManager.getActiveNetwork();
        if (activeNetwork == null) {
            return false;
        }
        return isValidated(connectivityManager.getNetworkCapabilities(activeNetwork));
    }

    private static boolean isValidated(NetworkCapabilities capabilities) {
        if (capabilities == null) {
            return false;
        }
        boolean hasTransport = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
        return hasTransport && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }
}
