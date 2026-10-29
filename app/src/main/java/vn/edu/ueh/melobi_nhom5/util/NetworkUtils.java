package vn.edu.ueh.melobi_nhom5.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.NetworkCapabilities;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class NetworkUtils {
    public interface ConnectivityListener {
        void onConnectivityChanged(boolean online);
    }

    private static Context appContext;
    private static NetworkCallback registeredCallback;
    private static volatile Boolean onlineState;
    private static final List<ConnectivityListener> listeners = new CopyOnWriteArrayList<>();

    private NetworkUtils() {
    }

    public static void initialize(Context context) {
        if (context == null) {
            return;
        }
        appContext = context.getApplicationContext();
        onlineState = hasValidatedInternet(appContext);
        startTracking(appContext);
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
        return appContext != null && hasValidatedInternet(appContext);
    }

    public static boolean isOnline(Context context) {
        if (onlineState != null) {
            return onlineState;
        }
        return hasValidatedInternet(context);
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
        return new IllegalStateException("Không có kết nối Internet. Vui lòng thử lại.");
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
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                setOnlineState(isValidated(capabilities));
            }

            @Override
            public void onLost(Network network) {
                setOnlineState(hasValidatedInternet(appContext));
            }

            @Override
            public void onUnavailable() {
                setOnlineState(false);
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                setOnlineState(isValidated(networkCapabilities));
            }
        };
        try {
            connectivityManager.registerDefaultNetworkCallback(registeredCallback);
        } catch (Exception ignored) {
            registeredCallback = null;
        }
    }

    private static void setOnlineState(boolean online) {
        Boolean previous = onlineState;
        onlineState = online;
        if (previous != null && previous == online) {
            return;
        }
        for (ConnectivityListener listener : listeners) {
            listener.onConnectivityChanged(online);
        }
    }

    private static boolean hasValidatedInternet(Context context) {
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
        return capabilities != null
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }
}
