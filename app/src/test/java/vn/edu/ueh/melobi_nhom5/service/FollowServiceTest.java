
package vn.edu.ueh.melobi_nhom5.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class FollowServiceTest {

    @Test
    public void cannotFollowSelf() {
        FollowService service = new FollowService();

        try {
            service.followUser("U01", "U01");
            fail("Phai chan follow chinh minh");
        } catch (IllegalArgumentException expected) {
            // Dung nhu mong doi
        }
    }

    @Test
    public void followAndUnfollow() {
        FollowService service = new FollowService();

        assertTrue(service.followUser("U01", "U02"));
        assertTrue(service.isFollowing("U01", "U02"));

        assertFalse(service.followUser("U01", "U02"));

        assertTrue(service.unfollowUser("U01", "U02"));
        assertFalse(service.isFollowing("U01", "U02"));

        assertFalse(service.unfollowUser("U01", "U02"));
    }

    @Test
    public void testFollowersAndFollowing() {
        FollowService service = new FollowService();

        service.followUser("A", "B");
        service.followUser("C", "B");
        service.followUser("B", "D");

        // Kiem tra danh sach followers
        assertEquals(2, service.getFollowers("B").size());
        assertTrue(service.getFollowers("B").contains("A"));
        assertTrue(service.getFollowers("B").contains("C"));

        // Kiem tra danh sach following
        assertEquals(1, service.getFollowing("B").size());
        assertTrue(service.getFollowing("B").contains("D"));

        // Kiem tra so luong
        assertEquals(2, service.getFollowerCount("B"));
        assertEquals(1, service.getFollowingCount("B"));

        // Follow trung khong lam tang so luong
        assertFalse(service.followUser("A", "B"));
        assertEquals(2, service.getFollowerCount("B"));

        // Unfollow phai giam so luong
        assertTrue(service.unfollowUser("A", "B"));
        assertEquals(1, service.getFollowerCount("B"));
    }

    @Test
    public void testFollowQuestion() {
        FollowService service = new FollowService();

        assertTrue(service.followQuestion("U01", "Q01"));
        assertTrue(service.followQuestion("U01", "Q02"));

        // Khong follow trung
        assertFalse(service.followQuestion("U01", "Q01"));

        assertEquals(2,
                service.getFollowedQuestions("U01").size());

        // Khong anh huong den user following
        assertEquals(0,
                service.getFollowingCount("U01"));

        // Huy theo doi
        assertTrue(service.unfollowQuestion("U01", "Q01"));

        assertEquals(1,
                service.getFollowedQuestions("U01").size());

        assertFalse(service.unfollowQuestion("U01", "Q01"));
    }


}
/*sửa lại theo lớp đã sửa*/
