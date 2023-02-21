package com.example.notificationdemo.FCM;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.notificationdemo.ApplicationChannel;
import com.example.notificationdemo.MainActivity;
import com.example.notificationdemo.R;
import com.example.notificationdemo.Socket.SocketHandler;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Map;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

// Config một service để nhận thông tin từ firebase gửi xuống
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    public static final String TAG = MyFirebaseMessagingService.class.getName();

    public class PayLoad {
        private String sender;
        private String reciver;
        private String content;

        public PayLoad() {
            sender = "";
            reciver = "";
            content = "";
        }

        public String getContent() {
            return content;
        }

        public String getReciver() {
            return reciver;
        }

        public String getSender() {
            return sender;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public void setReciver(String reciver) {
            this.reciver = reciver;
        }

        public void setSender(String sender) {
            this.sender = sender;
        }

        public void setPayLoad (String sender, String reciver, String content) {
            this.sender = sender;
            this.reciver = reciver;
            this.content = content;
        }
    }

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        // Tạo một hàm để hứng giá trị được gửi xuống
        // RemoteMessage là một hàm của firebase
        // Rằng một tin nhắn có thể có một thông báo,
        // Nếu một ứng dụng đang mở thì thông báo sẽ được thông báo và hiển thị trên khay thông báo
        // Hoặc không tùy thuộc vào cài đặt của người dùng mà thông báo vẫn sẽ được hiển thị trên khay
        RemoteMessage.Notification notification = remoteMessage.getNotification();
        if (notification == null) { // Nếu giá trị gửi xuống là null thì cần phải kiểm tra lại, ở bước này sau này sẽ phải phát triển thêm
            System.out.println("Null Notification");
            return;
        }
        else if (notification.getTitle().equals("Matching Notification")) {
            System.out.println(notification);
//        ---------------------------Notification Message-----------------------------------
            // Sau khi hứng giá trị xong thì mình sẽ lược ra 2 giá trị chính trong một notification message
            String strTitle = notification.getTitle(); // Lấy tiêu đề
            String strMessage = notification.getBody(); // Lấy nội dung
            String strAction = "Matching Notification"; // Phân biệt loại thông báo

            // Khi nhận thông tin từ trên xuống xong thì chúng ta sẽ gửi thông báo này đến người dùng
            // Thông báo dạng Notification Message mình sẽ sử dụng cho thông báo matching
            sendMatchNotification(strTitle, strMessage, strAction);
        }
        else {
//        -----------------------------Data Message-----------------------------------
            // Thông báo dạng Data Message mình sẽ sử dụng thông báo message
            // Vì là data message nên sẽ có nhiều dòng thông tin
            Map<String, String> stringMap = remoteMessage.getData();

            // Ở đây anh An yêu cầu 4 tham số cơ bản và mình sẽ thêm 2 tham số cơ bản nữa
            String action = "Message Notification";
            PayLoad dataMessage = new PayLoad();
            dataMessage.setPayLoad(stringMap.get("sender"), stringMap.get("reciver"), stringMap.get("content"));
            sendMessageNotification(action, dataMessage);
        }
    }

    private void sendMessageNotification(String action, PayLoad dataMessage) {
        // Theo tìm hiểu, Intent sử dụng để yêu cầu hành động từ các nguồn khác, ở đây là firebase yêu cầu android hành động.
        // Ở đây mình hiểu là tạo một Intent để thực thi lớp Main thay vì dựa vào hệ thống để tìm một lớp thích hợp.
        // Tham số 1 là context (bối cảnh) triển khai lớp này
        // Tham số 2 là lớp sẽ được thực thi
        Intent intent = new Intent(this, MainActivity.class);

        // Theo tìm hiểu, Sử dụng PendingIntent để mô tả hành động thực thi khi Intent kích hoạt.
        // Truyền vào bên trong là 4 tham số
        // Tham số 1 Mô tả context (bối cảnh) để nó bắt đầu hoạt động
        // Tham số 2 Mã yêu cầu riêng cho người gửi
        // Tham số 3 Intent của hoạt động
        // Tham số 4 Đặt cờ hiệu, nếu thông báo hiện tại đã tồn tại rồi thì thông báo sẽ được giữ lại và chỉ thay đổi nội dung mới cập nhật
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.big_ic_notification_custom);
        // Cấu hình thông báo
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ApplicationChannel.CHAT_CHANNEL_ID)
                .setContentTitle(dataMessage.getReciver())
                .setContentText(dataMessage.getContent())
                .setContentInfo(action)
                .setSmallIcon(R.drawable.small_ic_notification_custom)
                .setLargeIcon(largeIcon)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(dataMessage.getContent()))
                .setContentIntent(pendingIntent);
        // Sử dụng NotificationManager để thông báo tới người dùng kể cả khi ứng dụng chạy nền
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Từ Android 8.0 trở lên phải tạo channel riêng
        NotificationChannel firebaseChannel = new NotificationChannel(ApplicationChannel.CHAT_CHANNEL_ID,
                "Firebase Match Channel", NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(firebaseChannel);

        if (notificationManager != null) {
            notificationManager.notify(0, notificationBuilder.build());
        }
    }

    private void sendMatchNotification(String strTitle, String strMessage, String strAction) {
        // Vì nó giống hàm trên nên mình sẽ không ghi chú thêm
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        Bitmap largePicture = BitmapFactory.decodeResource(getResources(), R.drawable.big_pic_notifacation_custom);
        // Tạo thông báo
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ApplicationChannel.MATCH_CHANNEL_ID)
                .setContentTitle(strTitle)
                .setContentText(strMessage)
                .setContentInfo(strAction)
                .setSmallIcon(R.drawable.small_ic_notification_custom)
                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(largePicture).bigLargeIcon(null))
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel firebaseChannel = new NotificationChannel(ApplicationChannel.MATCH_CHANNEL_ID, "Firebase Match Channel", NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(firebaseChannel);

        if (notificationManager != null) {
            notificationManager.notify(0, notificationBuilder.build());
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        /**
         - onNewToken: Sử dụng để cung cấp mã token truyền lên server để khi đó server biết phải đẩy thông tin cho máy nào.
         -	Có hai tình huống khi cần tạo mã Token mới:
            o	1) Khi mã thông báo mới được tạo khi khởi động ứng dụng ban đầu.
            o	2) Bất cứ khi nào mã thông báo hiện tại bị thay đổi.
            o	Trong mục #2, có ba trường hợp xảy ra khi mã thông báo hiện tại bị thay đổi:
                	A) Ứng dụng được khôi phục trên thiết bị mới
                	B) Người dùng gỡ cài đặt/cài đặt lại ứng dụng
                	C) Người dùng xóa dữ liệu ứng dụng
         */
        super.onNewToken(token);
    }
}
