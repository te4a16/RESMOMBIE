package org.example;

import android.app.Dialog;          /*androidの「ダイアログ」を扱うためのクラス 
                                        今回は DialogFragment が内部で生成するダイアログの基本クラスにあたる。*/
import android.content.Context;     /* Android の コンテキスト（環境情報） を扱うためのクラス。
                                       Toast や Ringtone を取得する際に必要となるアプリの情報を提供する。*/
import android.content.DialogInterface; /* ダイアログ上でのクリックイベント（OK/Cancel）などに関するインターフェース。
                                            例：onDismiss() の引数で使われる。*/
import android.media.Ringtone;          /*着信音・通知音などの音を再生するクラス。
                                        今回はアラーム音（ブザー）を鳴らすために使用。 */
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * ブザー音を伴うアラートダイアログを表示するためのDialogFragment。
 * Ringtoneの再生と停止、およびダイアログのライフサイクルを管理します。
 */
public class AlertFragment extends DialogFragment {

    private static final String TAG = "AlertFragment";
    private Ringtone currentRingtone;

    /**
     * ダイアログの作成とブザー音の再生を開始します。
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // 1. ブザー音の準備と再生
        currentRingtone = prepareAndPlayRingtone(requireContext());

        // 2. アラートダイアログのビルダーを作成
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity());

        // 3. アラートダイアログの設定
        builder.setTitle("⚡ 警告！アラート発生 ⚡")
                .setMessage("ブザーが鳴動中です。\n[OK]で停止、[キャンセル]でダイアログを閉じます。")

                // OKボタン: ブザーを停止し、ダイアログを閉じる
                .setPositiveButton("OK (ブザー停止)", (dialog, id) -> {
                    stopRingtone(); 
                    Toast.makeText(requireContext(), "ブザーを停止しました。", Toast.LENGTH_SHORT).show();
                    // ダイアログは自動的に閉じられます
                })

                // キャンセルボタン: 音は止めずにダイアログを閉じる
                .setNegativeButton("キャンセル", (dialog, which) -> {
                    // 何もしない (音は鳴り続ける)
                })

                // ダイアログ外タップでのキャンセルを禁止
                .setCancelable(false);

        return builder.create();
    }
    
    /**
     * RingtoneのURIを取得し、再生します。
     */
    private Ringtone prepareAndPlayRingtone(Context context) {
        try {
            // OSのデフォルトアラーム音を取得
            Uri notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (notificationUri == null) {
                // アラーム音がなければ通知音を試す
                notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            Ringtone ringtone = RingtoneManager.getRingtone(context, notificationUri);
            
            if (ringtone != null) {
                if (!ringtone.isPlaying()) {
                    ringtone.play();
                }
                return ringtone;
            }
        } catch (Exception e) {
            Log.e(TAG, "ブザー音の再生に失敗しました。", e);
            Toast.makeText(context, "ブザー音の再生に失敗しました。", Toast.LENGTH_SHORT).show();
        }
        return null;
    }

    /**
     * Ringtoneの停止処理をまとめます。
     */
    private void stopRingtone() {
        if (currentRingtone != null && currentRingtone.isPlaying()) {
            currentRingtone.stop();
        }
    }

    // 【重要】Fragmentが閉じられたり、ホストActivityが停止したときにブザーを停止します。
    
    @Override
    public void onPause() {
        super.onPause();
        // Activityが一時停止するとき（例: ホームボタンを押す）、音を停止
        stopRingtone();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        // ダイアログが閉じたとき（例: OK/キャンセルボタンを押す）、音を停止
        stopRingtone();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Viewが破棄されるとき、念のため音を停止し参照を解放
        stopRingtone(); 
        currentRingtone = null;
    }
}