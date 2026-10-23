package vn.edu.ueh.melobi_nhom5.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Handler;
import android.os.Looper;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

import vn.edu.ueh.melobi_nhom5.R;

/**
 * Gọi widget reCAPTCHA v2 "Tôi không phải là robot" của Google.
 */
public final class RecaptchaHelper {
    public interface Callback {
        void onSuccess(String token);
        void onError(Exception e);
    }

    private RecaptchaHelper() {
    }

    public static void verify(Activity activity, Callback callback) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            notifyError(callback, new IllegalStateException("Activity khong hop le"));
            return;
        }

        String siteKey = activity.getString(R.string.recaptcha_site_key);
        if (siteKey == null || siteKey.trim().isEmpty()) {
            notifyError(callback, new IllegalStateException("Chua cau hinh recaptcha_site_key"));
            return;
        }

        String html = loadHtml(activity, siteKey.trim());
        if (html == null) {
            notifyError(callback, new IllegalStateException("Khong tai duoc mau reCAPTCHA"));
            return;
        }

        AtomicBoolean finished = new AtomicBoolean(false);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        final String[] successToken = {null};
        final Exception[] errorHolder = {null};
        WebView webView = createWebView(activity);

        FrameLayout container = new FrameLayout(activity);
        int padding = (int) (16 * activity.getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);
        container.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (int) (420 * activity.getResources().getDisplayMetrics().density)));

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.label_recaptcha)
                .setView(container)
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        RecaptchaJsBridge bridge = new RecaptchaJsBridge(mainHandler, token -> {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            successToken[0] = token;
            dismissQuietly(dialog);
        }, error -> {
            if (!finished.compareAndSet(false, true)) {
                return;
            }
            errorHolder[0] = error;
            dismissQuietly(dialog);
        });
        webView.addJavascriptInterface(bridge, "RecaptchaBridge");
        dialog.setOnDismissListener(d -> {
            boolean cancelled = finished.compareAndSet(false, true);
            if (cancelled) {
                errorHolder[0] = new IllegalStateException("Da huy reCAPTCHA");
            }
            String token = successToken[0];
            Exception error = errorHolder[0];
            detachWebView(webView);
            mainHandler.post(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }
                if (token != null && !token.trim().isEmpty()) {
                    if (callback != null) {
                        callback.onSuccess(token);
                    }
                } else {
                    notifyError(callback, error != null
                            ? error
                            : new IllegalStateException("Xac minh reCAPTCHA that bai"));
                }
            });
            mainHandler.postDelayed(() -> destroyWebView(webView), 400);
        });
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        webView.loadDataWithBaseURL(
                "https://www.google.com",
                html,
                "text/html",
                "UTF-8",
                null
        );
    }

    private static void dismissQuietly(AlertDialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    private static void notifyError(Callback callback, Exception error) {
        if (callback != null) {
            callback.onError(error);
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private static WebView createWebView(Activity activity) {
        WebView webView = new WebView(activity);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        return webView;
    }

    private static void detachWebView(WebView webView) {
        if (webView == null) {
            return;
        }
        try {
            webView.stopLoading();
            webView.onPause();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(new WebViewClient());
            webView.removeJavascriptInterface("RecaptchaBridge");
            ViewParent parent = webView.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup) parent).removeView(webView);
            }
        } catch (Exception ignored) {
            // WebView co the da bi go khoi dialog.
        }
    }

    private static void destroyWebView(WebView webView) {
        if (webView == null) {
            return;
        }
        try {
            detachWebView(webView);
            webView.destroy();
        } catch (Exception ignored) {
            // WebView co the da bi huy khi dialog dong.
        }
    }

    private static String loadHtml(Activity activity, String siteKey) {
        try (InputStream inputStream = activity.getAssets().open("recaptcha.html");
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString().replace("SITE_KEY", siteKey);
        } catch (Exception e) {
            return null;
        }
    }

    private static final class RecaptchaJsBridge {
        interface TokenListener {
            void onToken(String token);
        }

        interface ErrorListener {
            void onError(Exception error);
        }

        private final Handler handler;
        private final TokenListener tokenListener;
        private final ErrorListener errorListener;

        RecaptchaJsBridge(Handler handler, TokenListener tokenListener, ErrorListener errorListener) {
            this.handler = handler;
            this.tokenListener = tokenListener;
            this.errorListener = errorListener;
        }

        @JavascriptInterface
        public void onVerified(String token) {
            if (token == null || token.trim().isEmpty()) {
                onFailed("Token reCAPTCHA trong");
                return;
            }
            handler.post(() -> tokenListener.onToken(token));
        }

        @JavascriptInterface
        public void onExpired() {
            onFailed("reCAPTCHA het han, vui long thu lai");
        }

        @JavascriptInterface
        public void onFailed(String message) {
            handler.post(() -> errorListener.onError(new IllegalStateException(
                    message != null ? message : "Xac minh reCAPTCHA that bai")));
        }
    }
}
