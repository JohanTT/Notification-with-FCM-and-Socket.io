package com.example.notificationdemo;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class ApplicationChannel extends Application {
    public static final String CHAT_CHANNEL_ID = "CHAT_CHANNEL";
    public static final String MATCH_CHANNEL_ID = "MATCH_CHANNEL";

    @Override
//    Tạo một channel id cho ứng dụng
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // Kiểm tra xem phiên bản hiện tại có phải là phiên bản API từ 26 trở lên hay không
            Uri notiSound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.message_notification_sound); // Mô tả âm thanh thông báo cho kênh chat
            Uri matchSound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.match_notification_sound); // Mô tả âm thanh thông báo cho kênh match
//          Sử dụng AudioAttribute để đóng gói các thuộc tính về âm thanh
//          Để phân biệt được cho hệ thống biết rằng đây là dạng âm thanh nghe lại (media) hay chỉ là âm thanh thông báo (notification)
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION) // cài đặt cách sử dụng là dạng thông báo
                    .build();
//            ---------------------------Channel Chat-----------------------------------------------
            // Config Channel Chat
            CharSequence chatName = getString(R.string.CHAT_Channel_name); // Tên của Channel
            String chatDescription = getString(R.string.CHAT_Channel_description); // Mô tả thêm cho Channel
            int chatImportance = NotificationManager.IMPORTANCE_HIGH; // Chỉnh mức độ ưu tiên của dòng thông báo là cao
            // Notification Channel này có 3 tham số truyền vào
            // Tham số đầu tiên Channel_id là id của kênh để đảm bảo rằng sẽ không bị trùng với channel nào nên sẽ được gán cố định
            // Tham số thứ hai là tên hiển thị của kênh
            // Tham số thứ ba là hiển thị mức độ quan trọng của thông báo
            NotificationChannel chatChannel = new NotificationChannel(CHAT_CHANNEL_ID, chatName, chatImportance);
            chatChannel.setDescription(chatDescription);
            chatChannel.setSound(notiSound, audioAttributes);
//            ---------------------------Channel Match------------------------------------------------
            // Config Channel Match
            // Tạo ra 2 channel để thông báo nhiều dạng
            CharSequence matchName = getString(R.string.MATCH_Channel_name); // Tên của Channel
            String matchDescription = getString(R.string.MATCH_Channel_description); // Mô tả thêm cho Channel
            int matchImportance = NotificationManager.IMPORTANCE_HIGH; // Chỉnh mức độ ưu tiên của dòng thông báo là cao
            NotificationChannel matchChannel = new NotificationChannel(MATCH_CHANNEL_ID, matchName, matchImportance);
            matchChannel.setDescription(matchDescription);

            matchChannel.setSound(matchSound, audioAttributes);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(chatChannel);
                notificationManager.createNotificationChannel(matchChannel);
            }
        }
    }
}
