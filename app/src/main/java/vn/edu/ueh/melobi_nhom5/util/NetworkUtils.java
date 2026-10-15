package vn.edu.ueh.melobi_nhom5.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.ConnectivityManager.NetworkCallback;
import android.net.NetworkCapabilities;

public final class NetworkUtils {
    private static Context appContext;

    private NetworkUtils() {
    }

    public static void initialize(Context context) {
        if (context != null) {
            appContext = context.getApplicationContext();
        }
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

        NetworkCallback callback = new NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                if (onNetworkAvailable != null) {
                    onNetworkAvailable.run();
                }
            }
        };
        connectivityManager.registerDefaultNetworkCallback(callback);
        return callback;
    }
}
