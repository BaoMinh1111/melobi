package vn.edu.ueh.melobi_nhom5.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.widget.Toast;

import vn.edu.ueh.melobi_nhom5.service.ModerationService;

public class ReportDialogHelper {

    /**
     * Hàm tĩnh (static) để hiển thị hộp thoại báo cáo ở bất kỳ màn hình nào.
     *
     * @param context       Context của màn hình hiện tại (Activity chứa nút bấm)
     * @param targetId      ID của đối tượng bị báo cáo (questionId, answerId, hoặc userId)
     * @param targetType    Loại đối tượng: "QUESTION", "ANSWER", hoặc "USER"
     * @param currentUserId ID của người đang dùng app (người bấm nút báo cáo)
     */
    public static void showReportDialog(Context context, String targetId, String targetType, String currentUserId) {

        // 1. Chuẩn bị danh sách các lý do vi phạm phổ biến (Giúp người dùng chọn nhanh)
        String[] reasons = {
                "Nội dung phản cảm, không phù hợp",
                "Spam, quảng cáo rác",
                "Thông tin sai lệch, lừa đảo",
                "Ngôn từ đả kích, gây hấn",
                "Lý do khác"
        };

        // Kỹ thuật Java: Phải dùng mảng 1 phần tử để lưu vị trí đang được chọn
        // vì biến này sẽ bị thay đổi bên trong hàm callback (lambda) ở dưới.
        final int[] checkedItem = {0}; // Mặc định chọn dòng đầu tiên (index 0)

        // 2. Xây dựng hộp thoại bằng AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Báo cáo vi phạm");

        // 3. Cài đặt danh sách chọn (Radio Buttons)
        builder.setSingleChoiceItems(reasons, checkedItem[0], (dialog, which) -> {
            // Khi người dùng click vào một dòng khác, ta cập nhật lại vị trí đã chọn
            checkedItem[0] = which;
        });

        // 4. Xử lý nút "Gửi"
        builder.setPositiveButton("Gửi báo cáo", (dialog, which) -> {
            // Lấy ra lý do dạng chữ dựa trên vị trí người dùng vừa chốt
            String selectedReason = reasons[checkedItem[0]];

            // Khởi tạo ModerationService để gọi API
            ModerationService moderationService = new ModerationService();

            // Gọi hàm submitReport mà em vừa sửa thành công lúc nãy
            moderationService.submitReport(currentUserId, targetId, targetType, selectedReason)
                    .addOnSuccessListener(aVoid -> {
                        // Thành công: Firebase đã lưu xong. Hiện thông báo cho người dùng yên tâm.
                        Toast.makeText(context, "Cảm ơn bạn. Báo cáo đã được gửi cho Quản trị viên!", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // Thất bại: Có thể do rớt mạng mạng hoặc Firebase bị lỗi.
                        Toast.makeText(context, "Lỗi khi gửi báo cáo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        // 5. Xử lý nút "Hủy"
        builder.setNegativeButton("Hủy bỏ", (dialog, which) -> {
            dialog.dismiss(); // Tắt hộp thoại, không làm gì cả
        });

        // 6. Ra lệnh tạo và hiển thị hộp thoại lên màn hình
        builder.create().show();
    }
}