package vn.edu.ueh.melobi_nhom5.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class UserNameLookup {
    private static final Map<String, String> names = new ConcurrentHashMap<>();

    private UserNameLookup() {
    }

    public static void put(String userId, String fullName) {
        if (userId == null || userId.trim().isEmpty()) {
            return;
        }
        if (fullName == null || fullName.trim().isEmpty()) {
            return;
        }
        names.put(userId, fullName.trim());
    }

    public static String resolve(String userId, String fallback) {
        if (userId != null) {
            String live = names.get(userId);
            if (live != null && !live.trim().isEmpty()) {
                return live;
            }
        }
        if (fallback != null && !fallback.trim().isEmpty()) {
            return fallback.trim();
        }
        return "Thành viên";
    }
}
