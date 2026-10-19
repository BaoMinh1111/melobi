
package vn.edu.ueh.melobi_nhom5.adapter;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import vn.edu.ueh.melobi_nhom5.R;
import vn.edu.ueh.melobi_nhom5.entity.Message;

public class MessageAdapter
        extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private final List<Message> messages = new ArrayList<>();
    private final String currentUserId;

    public MessageAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    // Cap nhat danh sach tin nhan
    public void setMessages(List<Message> newMessages) {
        messages.clear();

        if (newMessages != null) {
            messages.addAll(newMessages);
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(
                        R.layout.item_chat_message,
                        parent,
                        false
                );

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder,
            int position) {

        Message message = messages.get(position);

        // Hien thi noi dung
        holder.tvBody.setText(message.getContent());

        // Hien thi thoi gian
        if (message.getSentAt() != null) {

            SimpleDateFormat format =
                    new SimpleDateFormat(
                            "HH:mm",
                            Locale.getDefault()
                    );

            holder.tvTime.setText(
                    format.format(message.getSentAt())
            );

        } else {
            holder.tvTime.setText("");
        }

        // Kiem tra tin nhan co phai cua minh
        boolean isMine = currentUserId != null
                && currentUserId.equals(
                message.getSenderId()
        );

        if (isMine) {

            // Tin nhan cua minh: ben phai
            holder.rowMessage.setGravity(Gravity.END);

            holder.cardMessage.setCardBackgroundColor(
                    ContextCompat.getColor(
                            holder.itemView.getContext(),
                            R.color.melobi_primary
                    )
            );

            holder.tvBody.setTextColor(
                    ContextCompat.getColor(
                            holder.itemView.getContext(),
                            R.color.white
                    )
            );

            holder.tvTime.setTextColor(
                    ContextCompat.getColor(
                            holder.itemView.getContext(),
                            R.color.white
                    )
            );

            // Hien thi trang thai da doc
            holder.tvStatus.setVisibility(View.VISIBLE);

            holder.tvStatus.setText(
                    message.getIsRead()
                            ? "Đã xem"
                            : "Đã gửi"
            );

            holder.tvStatus.setTextColor(
                    ContextCompat.getColor(
                            holder.itemView.getContext(),
                            R.color.white
                    )
            );

        } else {

            // Tin nhan nguoi khac: ben trai
            holder.rowMessage.setGravity(Gravity.START);

            holder.cardMessage.setCardBackgroundColor(
                    ContextCompat.getColor(
                            holder.itemView.getContext(),
                            R.color.white
                    )
            );

            holder.tvBody.setTextColor(
                    ContextCompat.getColor(
                            holder.itemView.getContext(),
                            R.color.text_primary
                    )
            );

            holder.tvTime.setTextColor(
                    ContextCompat.getColor(
                            holder.itemView.getContext(),
                            R.color.text_secondary
                    )
            );

            // Tin nhan nhan duoc khong hien trang thai
            holder.tvStatus.setVisibility(View.GONE);
            holder.tvStatus.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    public static class ViewHolder
            extends RecyclerView.ViewHolder {

        LinearLayout rowMessage;
        MaterialCardView cardMessage;

        TextView tvBody;
        TextView tvTime;
        TextView tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            rowMessage = itemView.findViewById(
                    R.id.rowMessage
            );

            cardMessage = itemView.findViewById(
                    R.id.cardMessage
            );

            tvBody = itemView.findViewById(
                    R.id.tvMessageBody
            );

            tvTime = itemView.findViewById(
                    R.id.tvMessageTime
            );

            tvStatus = itemView.findViewById(
                    R.id.tvMessageStatus
            );
        }
    }
}
