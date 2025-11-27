package org.example;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

// ViewModel層: ViewとModelの仲介役、ライフサイクルを超えてデータを保持
public class HomeViewModel extends ViewModel {

    // ナビゲーションイベントの定義（遷移先を識別）
    public enum NavigationEvent {
        TO_CAMERA_FRAGMENT,
    }

    private final MutableLiveData<NavigationEvent> _navigationEvent = 
        new MutableLiveData<>();
    public LiveData<NavigationEvent> navigationEvent = _navigationEvent;

    // Repositoryのインスタンス（Model層）
    private final HomeRepository repository = new HomeRepository();

    // Viewに公開する権限の状態
    private final MutableLiveData<Boolean> _cameraPermissionGranted = 
        new MutableLiveData<>(false);
    public LiveData<Boolean> cameraPermissionGranted = _cameraPermissionGranted;

    public HomeViewModel() {
        System.out.println(repository.getHomeMessage());
    }

    /**
     * Viewでカメラボタンがクリックされたときに呼び出されます。
     */
    public void onStartCameraClicked(boolean hasPermission) {
        if (hasPermission) {
            // 権限がある場合、カメラFragmentへの遷移イベントをトリガー
            _navigationEvent.setValue(NavigationEvent.TO_CAMERA_FRAGMENT);
            // イベントを消費済みにするためnullリセット（SingleLiveEventの代替）
            _navigationEvent.setValue(null); 
        } 
        // 権限がない場合は、Viewが ActivityCompat.requestPermissions() を実行する責務を持つ
    }

    /**
     * Viewが権限リクエストの結果を受け取ったときに呼び出されます。
     */
    public void onPermissionResult(boolean granted) {
        _cameraPermissionGranted.setValue(granted);
        if (granted) {
            // 権限が付与されたら、すぐにナビゲーションイベントをトリガー
            _navigationEvent.setValue(NavigationEvent.TO_CAMERA_FRAGMENT);
            _navigationEvent.setValue(null); 
        }
    }
    
    public String getHomeMessage() {
        return repository.getHomeMessage();
    }
}