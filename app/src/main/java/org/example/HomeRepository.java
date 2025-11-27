package org.example;

// Model層: データ操作の責務を持つ
public class HomeRepository {

    // 例: カメラ機能の状態や設定を永続化する場合などに使用します。
    // 今回はカメラ権限のチェック自体はView/ViewModelで行うため、ロジックは最小限です。
    public boolean isCameraSupported() {
        // 将来的に、デバイスの機能チェックなどをここで行うことができます。
        return true; 
    }

    // 将来的に、ユーザー設定の読み込みなどのビジネスロジックを追加できます。
    public String getHomeMessage() {
        return "RESMOMBIE App - Ready to start camera.";
    }
}