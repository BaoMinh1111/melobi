package vn.edu.ueh.melobi_nhom5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.model.Answer;
import vn.edu.ueh.melobi_nhom5.repository.AnswerRepository;
import vn.edu.ueh.melobi_nhom5.service.AnswerService;

/**
 * Unit test kiểm tra thứ tự xếp hạng câu trả lời (Answer Ranking).
 * Quy tắc xếp hạng của dự án:
 * 1. Số lượt thích (likes / likeCount) giảm dần (câu trả lời nhiều lượt thích nhất đứng đầu).
 * 2. Nếu số lượt thích bằng nhau, sắp xếp theo thời gian tạo (createdAt) giảm dần (mới nhất đứng đầu).
 */
public class AnswerRankingUnitTest {

    @Test
    public void testSortAnswers_EmptyList() {
        List<Answer> list = new ArrayList<>();
        AnswerRepository.sortAnswers(list);
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void testSortAnswers_SingleAnswer() {
        List<Answer> list = new ArrayList<>();
        Answer ans = createAnswer("A1", 10, 1000L);
        list.add(ans);

        AnswerRepository.sortAnswers(list);

        assertEquals(1, list.size());
        assertEquals("A1", list.get(0).getId());
    }

    @Test
    public void testSortAnswers_ByLikesDescending() {
        List<Answer> list = new ArrayList<>();
        Answer a1 = createAnswer("A1", 5, 1000L);
        Answer a2 = createAnswer("A2", 20, 1000L);
        Answer a3 = createAnswer("A3", 12, 1000L);

        list.add(a1);
        list.add(a2);
        list.add(a3);

        AnswerRepository.sortAnswers(list);

        assertEquals(3, list.size());
        assertEquals("A2", list.get(0).getId()); // 20 likes
        assertEquals("A3", list.get(1).getId()); // 12 likes
        assertEquals("A1", list.get(2).getId()); // 5 likes
    }

    @Test
    public void testSortAnswers_EqualLikes_SortedByCreatedAtDescending() {
        List<Answer> list = new ArrayList<>();
        // Tất cả đều 10 likes, nhưng khác thời gian tạo
        Answer aOld = createAnswer("A_OLD", 10, 1000L);
        Answer aNew = createAnswer("A_NEW", 10, 3000L);
        Answer aMedium = createAnswer("A_MED", 10, 2000L);

        list.add(aOld);
        list.add(aNew);
        list.add(aMedium);

        AnswerRepository.sortAnswers(list);

        assertEquals(3, list.size());
        assertEquals("A_NEW", list.get(0).getId()); // createdAt = 3000
        assertEquals("A_MED", list.get(1).getId()); // createdAt = 2000
        assertEquals("A_OLD", list.get(2).getId()); // createdAt = 1000
    }

    @Test
    public void testSortAnswers_EqualLikesAndEqualCreatedAt() {
        List<Answer> list = new ArrayList<>();
        Answer a1 = createAnswer("A1", 15, 5000L);
        Answer a2 = createAnswer("A2", 15, 5000L);

        list.add(a1);
        list.add(a2);

        AnswerRepository.sortAnswers(list);

        assertEquals(2, list.size());
        // Không bị crash hoặc mất phần tử khi bằng nhau cả 2 tiêu chí
        assertTrue(list.contains(a1));
        assertTrue(list.contains(a2));
    }

    @Test
    public void testOfflineAndOnlineRankingEquivalence() {
        // Giả lập danh sách nhận từ Firestore (Online)
        List<Answer> onlineList = new ArrayList<>();
        onlineList.add(createAnswer("A1", 3, 100L));
        onlineList.add(createAnswer("A2", 10, 200L));
        onlineList.add(createAnswer("A3", 10, 500L));

        // Giả lập cùng danh sách lấy từ bộ nhớ đệm / DB Offline
        List<Answer> offlineList = new ArrayList<>();
        offlineList.add(createAnswer("A1", 3, 100L));
        offlineList.add(createAnswer("A2", 10, 200L));
        offlineList.add(createAnswer("A3", 10, 500L));

        // Tiến hành sắp xếp bằng quy tắc của AnswerRepository / AnswerService
        AnswerRepository.sortAnswers(onlineList);
        AnswerService.sortAnswers(offlineList);

        assertEquals(onlineList.size(), offlineList.size());
        for (int i = 0; i < onlineList.size(); i++) {
            assertEquals("Thứ tự phần tử " + i + " giữa Online và Offline phải trùng khớp",
                    onlineList.get(i).getId(), offlineList.get(i).getId());
            assertEquals("Số like phần tử " + i + " phải bằng nhau",
                    onlineList.get(i).getLikes(), offlineList.get(i).getLikes());
            assertEquals("Thời gian tạo phần tử " + i + " phải bằng nhau",
                    onlineList.get(i).getCreatedAt(), offlineList.get(i).getCreatedAt());
        }

        // Kết quả mong đợi: A3 (10 likes, 500L), A2 (10 likes, 200L), A1 (3 likes, 100L)
        assertEquals("A3", onlineList.get(0).getId());
        assertEquals("A2", onlineList.get(1).getId());
        assertEquals("A1", onlineList.get(2).getId());
    }

    @Test
    public void testSortAnswers_NullListHandledGracefully() {
        // Không được văng NullPointerException
        AnswerRepository.sortAnswers(null);
        AnswerService.sortAnswers(null);
    }

    private Answer createAnswer(String id, long likes, long createdAt) {
        Answer answer = new Answer();
        answer.setId(id);
        answer.setAnswerId(id);
        answer.setQuestionId("Q_TEST");
        answer.setUserId("U_TEST");
        answer.setUserName("Test User");
        answer.setContent("Nội dung câu trả lời " + id);
        answer.setLikes(likes);
        answer.setCreatedAt(createdAt);
        return answer;
    }
}
