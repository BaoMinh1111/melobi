package vn.edu.ueh.melobi_nhom5;

import android.app.Application;

import vn.edu.ueh.melobi_nhom5.util.NetworkUtils;

public class MelobiApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NetworkUtils.initialize(this);
    }
}
