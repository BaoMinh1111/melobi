
package vn.edu.ueh.melobi_nhom5.service;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.model.User;

public class UserSuggestionServiceTest {

    private User createUser(String id, String major, int count) {
        User user = new User(id, "User " + id, id + "@test.com");
        user.setMajorId(major);
        user.setFollowerCount(count);
        return user;
    }

    @Test
    public void testSuggestUsers() {
        User a = createUser("A", "IT", 30);
        User b = createUser("B", "IT", 250);
        User c = createUser("C", "IT", 120);
        User d = createUser("D", "Marketing", 500);

        UserSuggestionService service =
                new UserSuggestionService(
                        Arrays.asList(a, b, c, d)
                );

        List<User> result = service.suggestUsers("IT");

        assertEquals(3, result.size());
        assertEquals("B", result.get(0).getUserId());
        assertEquals("C", result.get(1).getUserId());
        assertEquals("A", result.get(2).getUserId());
    }
}
