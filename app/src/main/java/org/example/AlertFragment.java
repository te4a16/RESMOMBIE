package org.example;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi; // ★ requiresApi のために必要

public class AlertFragment extends Fragment {

    private static final String CHANNEL_ID = "alert_channel";
    private static final String CHANNEL_NAME = "警告アラート";
    private static final int NOTIFICATION_ID = 100;

    /**
     * 通知を表示するスタティックメソッド。
     * どのContextからでも呼び出せるように設計されています。
     */
    public static void showNotification(@NonNull Context context) {
        // NotificationManagerはContextから取得する
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // API 26 (Oreo) 以上でのみチャンネルを作成する
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Contextを渡してチャンネル作成ロジックを実行
            createNotificationChannel(context, notificationManager);
        }

        // 通知タップ時のIntent設定
        Intent intent = new Intent(context, App.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        
        // PendingIntentのフラグ設定
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        
        // Notificationの構築
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // 仮のアイコンリソース
            .setContentTitle("⚡ 警告！アラート発生 ⚡")
            .setContentText("ブザーが鳴動中です。タップして確認してください。")
            .setPriority(NotificationCompat.PRIORITY_HIGH) 
            .setAutoCancel(true)
            .setContentIntent(pendingIntent); 

        // 通知の表示
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
    
    /**
     * 通知チャンネルを作成し、NotificationManagerに登録します。
     * staticメソッドなので、ContextとNotificationManagerを引数として受け取ります。
     */
    @RequiresApi(Build.VERSION_CODES.O) // ★ Lintエラー解消のための修正 (API 26未満での呼び出しを禁止)
    private static void createNotificationChannel(@NonNull Context context, @NonNull NotificationManager notificationManager) {
        // 1. サウンドと属性の定義
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_ALARM) 
            .build();
        
        // 2. NotificationChannelの作成 
        // @RequiresApiアノテーションにより、LintはここがAPI 26以上で実行されることを理解します。
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH // 警告なのでHIGHを使用
        );

        // 3. チャンネルの設定
        channel.setDescription("ブザー音付きの重大な警告通知");
        channel.setSound(alarmSound, audioAttributes);
        channel.enableVibration(true);

        // 4. チャンネルの登録
        notificationManager.createNotificationChannel(channel);
    }
}