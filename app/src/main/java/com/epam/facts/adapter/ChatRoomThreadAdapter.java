package com.epam.facts.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.epam.facts.R;
import com.epam.facts.model.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;


public class ChatRoomThreadAdapter extends RecyclerView.Adapter<ChatRoomThreadAdapter.ViewHolder> {

    private static String TAG = ChatRoomThreadAdapter.class.getSimpleName();

    public int SELF = 100;

    private String userId;
    private static String today;

    public static String BOT_ID = "Bot";
    public static String SELF_ID = "Self";

    private Context mContext;
    private ArrayList<Message> messageArrayList;

    private final List<TextView> textViewPool = new LinkedList<>();

    public ChatRoomThreadAdapter(Context mContext, ArrayList<Message> messageArrayList, String userId) {
        this.mContext = mContext;
        this.messageArrayList = messageArrayList;
        this.userId = userId;

        Calendar calendar = Calendar.getInstance();
        today = String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final List<TextView> textViews = new ArrayList<>();

        TextView message, timestamp;
        private ViewGroup container;

        ViewHolder(View v) {
            super(v);
            container = itemView.findViewById(R.id.blocksCointainer);
            message = itemView.findViewById(R.id.message);
            timestamp = itemView.findViewById(R.id.timestamp);
        }

        void bind(Message message) {
            recycleImageViews();

            if (message.getJobs() != null) {
                for (int i = 0; i < message.getJobs().size(); ++i) {
                    final TextView jobTextView = getRecycledImageViewOrCreate();
                    textViews.add(jobTextView);
                    container.addView(jobTextView);

                    jobTextView.setText(message.getJobs().get(i).name);
                    jobTextView.setBackgroundResource(message.getJobs().get(i).status.equals("red") ? R.drawable.red_text_bg : R.drawable.gray_text_bg);
                }
            }
        }

        private TextView getRecycledImageViewOrCreate() {
            if (textViewPool.isEmpty()) {
                return (TextView) LayoutInflater.from(container.getContext()).inflate(R.layout.text_block, container, false);
            }
            return textViewPool.remove(0);
        }

        void recycleImageViews() {
            textViewPool.addAll(textViews);
            textViews.clear();
            if (container != null) {
                container.removeAllViews();
            }
        }
    }

    @Override
    public ChatRoomThreadAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        // view type is to identify where to render the chat message
        // left or right
        if (viewType == SELF) {
            // self message
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_item_self, parent, false);
        } else {
            // others message
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_item_other, parent, false);
        }


        return new ViewHolder(itemView);
    }


    @Override
    public int getItemViewType(int position) {
        Message message = messageArrayList.get(position);
        if (message.getUserId().equals(userId)) {
            return SELF;
        }

        return position;
    }

    @Override
    public void onBindViewHolder(final ChatRoomThreadAdapter.ViewHolder holder, int position) {
        Message message = messageArrayList.get(position);
        holder.message.setText(message.getMessage());

        String timestamp = getTimeStamp(Long.parseLong(message.getCreatedAt()));

        holder.timestamp.setText(timestamp);

        holder.bind(message);
    }
    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.recycleImageViews();
    }

    @Override
    public int getItemCount() {
        return messageArrayList.size();
    }

    private static String getTimeStamp(long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = "";

        today = today.length() < 2 ? "0" + today : today;

        Date date = new Date(time);
        SimpleDateFormat todayFormat = new SimpleDateFormat("dd");
        String dateToday = todayFormat.format(date);
        format = dateToday.equals(today) ? new SimpleDateFormat("hh:mm a") : new SimpleDateFormat("dd LLL, hh:mm a");
        String date1 = format.format(date);
        timestamp = date1.toString();

        return timestamp;
    }
}

