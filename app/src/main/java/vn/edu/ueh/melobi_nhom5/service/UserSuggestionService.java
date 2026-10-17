
package vn.edu.ueh.melobi_nhom5.service;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.model.User;

public class UserSuggestionService {

    private final List<User> users = new ArrayList<>();

    public UserSuggestionService(List<User> initialUsers) {
        setUsers(initialUsers);
    }

    public void setUsers(List<User> newUsers) {
        users.clear();

        if (newUsers != null) {
            users.addAll(newUsers);
        }
    }

    // De xuat user cung chuyen nganh
    // Sap xep followerCount giam dan
    public List<User> suggestUsers(String majorId) {
        if (majorId == null || majorId.isBlank()) {
            throw new IllegalArgumentException(
                    "majorId khong duoc de trong"
            );
        }

        List<User> result = new ArrayList<>();

        for (User user : users) {
            if (user != null
                    && user.getMajorId() != null
                    && majorId.trim().equalsIgnoreCase(
                    user.getMajorId().trim())) {
                result.add(user);
            }
        }

        result.sort((a, b) ->
                Integer.compare(
                        b.getFollowerCount(),
                        a.getFollowerCount()
                )
        );

        return result;
    }
}
