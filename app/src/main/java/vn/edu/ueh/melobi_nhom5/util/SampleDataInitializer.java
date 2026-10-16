package vn.edu.ueh.melobi_nhom5.util;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

import vn.edu.ueh.melobi_nhom5.model.Major;
import vn.edu.ueh.melobi_nhom5.model.Question;

public class SampleDataInitializer {
    private static final String TAG = "SampleDataInitializer";
    private static final String COLLECTION_MAJORS = "majors";
    private static final String COLLECTION_QUESTIONS = "questions";

    public interface InitCallback {
        void onSuccess(String message);
        void onError(Exception e);
    }

    public static List<Major> getSampleMajors() {
        List<Major> sampleMajors = new ArrayList<>();
        sampleMajors.add(new Major("it", "Công nghệ thông tin", "Lập trình, dữ liệu, AI, mạng máy tính và phát triển phần mềm."));
        sampleMajors.add(new Major("business", "Kinh tế", "Kinh tế học, quản trị kinh doanh và phân tích thị trường."));
        sampleMajors.add(new Major("finance", "Tài chính", "Tài chính doanh nghiệp, đầu tư, ngân hàng và chứng khoán."));
        sampleMajors.add(new Major("marketing", "Marketing", "Digital marketing, thương hiệu, truyền thông và chiến lược nội dung."));
        sampleMajors.add(new Major("languages", "Ngoại ngữ", "Tiếng Anh, tiếng Trung, giao tiếp quốc tế và biên phiên dịch."));
        sampleMajors.add(new Major("education", "Giáo dục", "Phương pháp giảng dạy, công nghệ giáo dục và phát triển người học."));
        sampleMajors.add(new Major("science", "Khoa học", "Nghiên cứu khoa học, phân tích dữ liệu và tư duy thực nghiệm."));
        sampleMajors.add(new Major("design", "Thiết kế", "UI/UX, đồ họa, mỹ thuật số và tư duy sáng tạo."));
        sampleMajors.add(new Major("other", "Khác", "Các lĩnh vực liên ngành hoặc chủ đề quan tâm khác."));
        return sampleMajors;
    }

    public static void initializeSampleMajors(InitCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        List<Major> sampleMajors = getSampleMajors();

        WriteBatch batch = db.batch();
        for (Major major : sampleMajors) {
            batch.set(
                    db.collection(COLLECTION_MAJORS).document(major.getMajorId()),
                    major,
                    SetOptions.merge()
            );
        }

        batch.commit()
                .addOnSuccessListener(unused -> {
                    Log.d(TAG, "Khởi tạo thành công " + sampleMajors.size() + " chuyên ngành mẫu");
                    if (callback != null) {
                        callback.onSuccess("Đã khởi tạo " + sampleMajors.size() + " chuyên ngành mẫu.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tạo danh sách chuyên ngành mẫu: ", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }

    public static void initializeSampleQuestions(InitCallback callback) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        List<Question> sampleQuestions = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        long dayInMillis = 86400000L;

        sampleQuestions.add(new Question(
                "q_sample_01",
                "Nên học lập trình Android bằng Java hay Kotlin cho sinh viên UEH?",
                "Em đang bắt đầu học phát triển ứng dụng di động, các anh chị cho em xin lời khuyên nên học Java trước hay vào thẳng Kotlin luôn ạ?",
                "Công nghệ thông tin",
                "user_minh_anh",
                "Minh Anh",
                "",
                currentTime - (dayInMillis * 7),
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_02",
                "Phương pháp quản lý tài chính cá nhân hiệu quả cho sinh viên xa nhà?",
                "Mọi người có kinh nghiệm chia sẻ chi tiêu hàng tháng không ạ? Em hay bị hụt tiền vào tuần cuối tháng quá.",
                "Kinh tế",
                "user_hoang_nam",
                "Hoàng Nam",
                "",
                currentTime - (dayInMillis * 6),
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_03",
                "Lộ trình học Digital Marketing từ con số 0?",
                "Các kỹ năng quan trọng nhất cần nắm khi bắt đầu làm quen với Performance Marketing và Social Media là gì vậy ạ?",
                "Marketing",
                "user_thu_ha",
                "Thu Hà",
                "",
                currentTime - (dayInMillis * 6),
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_04",
                "Kinh nghiệm phỏng vấn thực tập vị trí Frontend Developer?",
                "Cần lưu ý những kiến thức ReactJS hoặc Jetpack Compose nào để ghi điểm với nhà tuyển dụng công nghệ?",
                "Công nghệ thông tin",
                "user_gia_huy",
                "Gia Huy",
                "",
                currentTime - (dayInMillis * 5),
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_05",
                "Đầu tư chứng khoán sinh viên: Nên bắt đầu từ đâu với số vốn nhỏ?",
                "Em có một khoản tiết kiệm nhỏ khoảng 2 triệu đồng, muốn tìm hiểu về thị trường tài chính thì nên đọc sách gì và bắt đầu thế nào ạ?",
                "Kinh tế",
                "user_khanh_linh",
                "Khánh Linh",
                "",
                currentTime - (dayInMillis * 5),
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_06",
                "Thiết kế cơ sở dữ liệu quan hệ (SQL) cho ứng dụng thương mại điện tử?",
                "Khi thiết kế bảng Order và Product, chúng ta nên xử lý quan hệ nhiều-nhiều thế nào để tối ưu hiệu năng truy vấn?",
                "Công nghệ thông tin",
                "user_duc_anh",
                "Đức Anh",
                "",
                currentTime - (dayInMillis * 4),
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_07",
                "Bí quyết đạt điểm cao môn Kinh tế vi mô cho tân sinh viên?",
                "Môn này có nhiều đồ thị và bài tập tối ưu hóa lợi nhuận, anh chị có mẹo gì ôn thi hiệu quả chia sẻ em với ạ.",
                "Kinh tế",
                "user_ngoc_mai",
                "Ngọc Mai",
                "",
                currentTime - (dayInMillis * 4),
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_08",
                "Cách viết Content SEO thu hút khách hàng mục tiêu?",
                "Làm thế nào để tối ưu từ khóa tự nhiên trong bài viết blog mà không bị gượng gạo hay nhồi nhét từ khóa ạ?",
                "Marketing",
                "user_tuan_kiet",
                "Tuấn Kiệt",
                "",
                currentTime - (dayInMillis * 3),
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_09",
                "Mạng máy tính: Phân biệt TCP và UDP trong lập trình socket?",
                "Khi nào chúng ta nên ưu tiên dùng TCP và khi nào nên dùng UDP cho các ứng dụng thời gian thực?",
                "Công nghệ thông tin",
                "user_phuong_thao",
                "Phương Thảo",
                "",
                currentTime - (dayInMillis * 3),
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_10",
                "Kỹ năng mềm cần thiết khi tham gia các CLB sinh viên UEH?",
                "Em vừa đậu phỏng vấn vào CLB, làm sao để cân bằng giữa việc hoạt động ngoại khóa và kết quả học tập trên lớp ạ?",
                "Khác",
                "user_quang_huy",
                "Quang Huy",
                "",
                currentTime - (dayInMillis * 2),
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_11",
                "Học tiếng Anh chuyên ngành Kinh tế/IT như thế nào hiệu quả?",
                "Tài liệu đọc thuật ngữ chuyên ngành công nghệ và tài chính ở đâu là chuẩn xác và dễ tiếp cận nhất ạ?",
                "Khác",
                "user_lan_huong",
                "Lan Hương",
                "",
                currentTime - (dayInMillis * 2),
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_12",
                "Xu hướng Brand Marketing năm nay có gì đổi mới?",
                "Các chiến dịch viral marketing trên TikTok hiện nay đòi hỏi agency và nhãn hàng những yếu tố cốt lõi nào?",
                "Marketing",
                "user_thanh_dat",
                "Thành Đạt",
                "",
                currentTime - dayInMillis,
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_13",
                "Sử dụng Firebase Authentication và Firestore trong Android Studio?",
                "Em đang làm đồ án tốt nghiệp cần tích hợp Realtime Database và Auth, có lưu ý gì về bảo mật quy tắc Firestore không ạ?",
                "Công nghệ thông tin",
                "user_hong_nhung",
                "Hồng Nhung",
                "",
                currentTime - dayInMillis,
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_14",
                "Trí tuệ nhân tạo (AI) thay đổi thị trường lao động ngành Kinh tế ra sao?",
                "Các bạn học tài chính - ngân hàng cần trang bị thêm kỹ năng gì để không bị thay thế bởi AI và Automation?",
                "Kinh tế",
                "user_duy_khanh",
                "Duy Khánh",
                "",
                currentTime - 43200000L,
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_15",
                "Kinh nghiệm viết CV và chuẩn bị phỏng vấn Business Analyst (BA)?",
                "Vị trí BA đòi hỏi những kỹ năng phân tích nghiệp vụ và sử dụng công cụ nào là cốt lõi đối với sinh viên mới ra trường?",
                "Khác",
                "user_quynh_chi",
                "Quỳnh Chi",
                "",
                currentTime - 28800000L,
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_16",
                "Tối ưu hóa câu lệnh truy vấn SQL trong các dự án phần mềm lớn?",
                "Khi bảng dữ liệu lên tới hàng triệu records, việc sử dụng Index và Store Procedure đem lại hiệu quả thế nào ạ?",
                "Công nghệ thông tin",
                "user_dinh_trong",
                "Đình Trọng",
                "",
                currentTime - 21600000L,
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_17",
                "Quản trị chuỗi cung ứng (Supply Chain) trong thời đại số có gì thách thức?",
                "Bài toán logistics và quản lý kho hàng thông minh hiện đang áp dụng những công nghệ gì phổ biến nhất?",
                "Kinh tế",
                "user_my_duyen",
                "Mỹ Duyên",
                "",
                currentTime - 10800000L,
                0
        ));

        sampleQuestions.add(new Question(
                "q_sample_18",
                "Chiến lược định giá sản phẩm (Pricing Strategy) cho Startup công nghệ?",
                "Giữa mô hình Freemium và Subscription, loại nào phù hợp hơn khi ra mắt một ứng dụng SaaS mới trên thị trường?",
                "Marketing",
                "user_gia_bao",
                "Gia Bảo",
                "",
                currentTime,
                0
        ));

        WriteBatch batch = db.batch();
        for (Question q : sampleQuestions) {
            batch.set(db.collection(COLLECTION_QUESTIONS).document(q.getId()), q, SetOptions.merge());
        }

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Cập nhật thành công 18 câu hỏi mẫu đa lĩnh vực!");
                    if (callback != null) {
                        callback.onSuccess("Đã khởi tạo và cập nhật 18 câu hỏi mẫu đa lĩnh vực với nhiều tác giả!");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Lỗi tạo dữ liệu mẫu: ", e);
                    if (callback != null) {
                        callback.onError(e);
                    }
                });
    }
}
