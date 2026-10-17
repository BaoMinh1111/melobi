
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
/*Đây là 1 lớp ko có trong file doc, 1 số lỗi sau:
Lớp chỉ lọc danh sách có sẵn. Để dùng thật, ai đó phải tải toàn bộ collection users từ Firestore về máy rồi truyền vào. 
Cách này không ổn khi người dùng nhiều (tốn mạng, tốn RAM, tốn lượt đọc Firestore). Nên để Firestore làm việc lọc và sắp xếp
* Tham số currentUserId: Gợi ý cả chính người đang đăng nhập (“gợi ý follow chính mình”)
* Loại những người đã follow: Người dùng thấy gợi ý người mình đã follow rồi.
* Loại tài khoản bị khóa (status = LOCKED): Gợi ý tài khoản không đăng nhập được
* Loại tài khoản Admin: Admin xuất hiện trong gợi ý
* Giới hạn số lượng (limit)	: Trả về toàn bộ người cùng chuyên ngành*/
