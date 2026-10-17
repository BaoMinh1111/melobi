package vn.edu.ueh.melobi_nhom5.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.NetworkCapabilities;

public final class NetworkUtils {
    private static Context appContext;
    private static NetworkCallback registeredCallback;

    private NetworkUtils() {
    }

    public static void initialize(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
    }

    /**
     * Đăng ký lắng nghe mạng theo tài liệu: khi có mạng trở lại thì kích hoạt đồng bộ.
     */
    public static NetworkCallback registerNetworkListener() {
        return registerNetworkListener(() -> {
            // SyncManager.syncAll() sẽ được gọi tại đây khi lớp đó đã có trong project.
        });
    }

    public static boolean isOnline() {
        return appContext != null && isOnline(appContext);
    }

    public static boolean isOnline(Context context) {
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

        NetworkCapabilities capabilities =
                connectivityManager.getNetworkCapabilities(activeNetwork);
        if (capabilities == null) {
            return false;
        }

        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
                || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN);
    }

    public static NetworkCallback registerNetworkListener(Runnable onNetworkAvailable) {
        if (appContext == null) {
            return null;
        }
        return registerNetworkListener(appContext, onNetworkAvailable);
    }

    public static NetworkCallback registerNetworkListener(Context context, Runnable onNetworkAvailable) {
        if (context == null) {
            return null;
        }

        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return null;
        }

        if (registeredCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(registeredCallback);
            } catch (Exception ignored) {
                // Callback cũ có thể đã bị hủy.
            }
            registeredCallback = null;
        }

        NetworkCallback callback = new NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (onNetworkAvailable != null) {
                    onNetworkAvailable.run();
                }
            }
        };
        connectivityManager.registerDefaultNetworkCallback(callback);
        registeredCallback = callback;
        return callback;
    }
}
