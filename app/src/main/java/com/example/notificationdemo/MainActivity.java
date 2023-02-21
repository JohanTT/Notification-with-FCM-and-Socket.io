package com.example.notificationdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.example.notificationdemo.Socket.SocketHandler;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class MainActivity extends AppCompatActivity {

    private static final int CHAT_NOTIFICATION_ID = 1;
    private static final int MATCH_NOTIFICATION_ID = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Mình để lên trên cùng để thuận tiện cho việc sử dụng
        Button btnSendChatNotification = findViewById(R.id.btn_send_chatNotification); // Lấy btn chat noti
        Button btnSendMatchNotification = findViewById(R.id.btn_send_matchNotification); // Lấy btn match noti
        EditText textMessage = findViewById(R.id.text_message); // Lấy input message

        Socket mSocket = new SocketHandler().getSocket();
        mSocket.connect(); // Kết nối với máy chủ

        btnSendChatNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (textMessage.getText().toString().trim() != null) {
                    sendChatNotification(String.valueOf(textMessage.getText()).trim());
                    textMessage.setText("");
                }
            }
        });

        btnSendMatchNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              //sendMatchNotification(mSocket);
              mSocket.emit("matchingNotification"); // Bắt sự kiện khi nhấn nút sẽ phát ra sự kiện matching
              // Khi này bên server.js của Socket.io sẽ nhận thấy có tín hiệu phát sự kiện nên
              // Máy chủ socket.io sẽ phát tín hiệu còn lại tới các thiết bị còn lại
            }
        });

        textMessage.setRawInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        textMessage.setOnKeyListener(new View.OnKeyListener() {
            @Override
            // Hàm này để nhận biết được khi nào người dùng nhập văn bản
            // Truyền vào 3 tham số, tham số đầu tiên là hộp text được chọn
            // Tham số thứ 2 là action id tức là loại hành động
            // Tham số thứ 3 là sự kiện khi nhấn phím
            public boolean onKey(View v, int i, KeyEvent keyEvent) {
                if (textMessage.getText().toString().trim() != null && i == KeyEvent.KEYCODE_ENTER && i != KeyEvent.KEYCODE_SHIFT_LEFT && i != KeyEvent.KEYCODE_SHIFT_RIGHT && keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    sendChatNotification(String.valueOf(textMessage.getText()).trim());
                    textMessage.setText("");
                    return true;
                }
                return false;
            }
        });

//        ---------------------- TOKEN FIREBASE --------------------------------------
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            System.out.println("Fetching FCM registration token failed");
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Log and toast
                        System.out.println(token);
                        textMessage.setText(token);
                        Toast.makeText(MainActivity.this, "TOKEN: " + token, Toast.LENGTH_SHORT).show();
                    }
                });
        // Ta tiến hành bắt lấy sự kiện dựa vào tên sự kiện và hàm Emitter listener.
        mSocket.on("matchingNotification", onNewMessage);

    }
    private void sendChatNotification(String textMessage) {
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.big_ic_notification_custom);
        Uri notiSound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.message_notification_sound); // Mô tả âm thanh thông báo cho kênh chat

        Notification notification = new NotificationCompat.Builder(this, ApplicationChannel.CHAT_CHANNEL_ID)
//              Chỉnh tiêu đề cho thông báo
                .setContentTitle("Hey you, new message here!")
//              Chỉnh nội dung thông báo đến người dùng
                .setContentText(textMessage)
//              Chỉnh cái ảnh nhỏ để hiện lên một cái logo đặc trưng
                .setSmallIcon(R.drawable.small_ic_notification_custom)
//              Set icon to để khi người dùng mở thông báo ra sẽ thấy nó
//              Ở đây sẽ sử dụng tham số truyền vào là một bitmap
                .setLargeIcon(largeIcon)
//              Tùy chỉnh để khi gửi tin nhắn dài thì sẽ hiển thị hết
                .setStyle(new NotificationCompat.BigTextStyle().bigText(textMessage))
//              Chỉnh âm thanh cho thông báo
                .setSound(notiSound) // Dành cho android 8.0 trở xuống
                .build();

//      Sử dụng hàm này để đăng thông báo vào bên trong thanh trạng thái và khi mà các thông báo có id bị trùng thì và chưa bị hủy thì sẽ được thay thế bằng thông tin cập nhật
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(CHAT_NOTIFICATION_ID, notification);
//      Truyền vào 2 tham số tham số đầu tiên
//      NOTIFICATION_ID là để xác định mã định danh cho thông báo của ứng dụng và việc ta cài cứng id cho noti là để chỉ số định danh của nó không bị thay đổi
//      Và tham số tiếp theo là thông tin thông báo gửi đến người dùng
    }

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
//        bên trong Emitter Listener sẽ có một hàm call và ta sẽ viết đè lên hàm call này của nó để thực hiện lệnh runOnUiThread
//        Lệnh runOnUiThread ngăn chặn các cuộc gọi lại, chúng luôn được gọi lại như một chuỗi dây chuyền lặp lại
//        Do đó mình phải đặt một cái hàm bên trong nữa để tránh việc nó sẽ luôn thực thi đi lại một hàm mà không có điểm dừng
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    sendMatchNotification();
                }
            });
        }
    };

    // Phần này mình thấy nó tương tự như phần trên nên mình sẽ không ghi chú thêm
    private void sendMatchNotification() {
        Bitmap largePicture = BitmapFactory.decodeResource(getResources(), R.drawable.big_pic_notifacation_custom);
        Uri matchSound = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.match_notification_sound); // Mô tả âm thanh thông báo cho kênh match

        Notification notification = new NotificationCompat.Builder(this, ApplicationChannel.CHAT_CHANNEL_ID)
                .setContentTitle("MATCHINGGG! Check this out")
                .setContentText("Your habbit matching with Giraffe")
                .setSmallIcon(R.drawable.small_ic_notification_custom)
                .setStyle(new NotificationCompat.BigPictureStyle().bigPicture(largePicture).bigLargeIcon(null))
                .setSound(matchSound) // Dành cho android 8.0 trở xuống
                .build();
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(MATCH_NOTIFICATION_ID, notification);
    }
}