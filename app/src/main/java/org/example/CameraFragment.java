package org.example;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.view.PreviewView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider; 

public class CameraFragment extends Fragment {

    private static final String TAG = "CameraFragment";
    private PreviewView previewView;
    private CameraViewModel viewModel; 

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // fragment_camera.xml をインフレート
        return inflater.inflate(R.layout.fragment_camera, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // ViewModelのインスタンスを取得
        viewModel = new ViewModelProvider(this).get(CameraViewModel.class); 

        previewView = view.findViewById(R.id.previewView);

        // カメラの初期化ロジックはViewModelに委譲
        viewModel.startCamera(
            requireContext(),
            getViewLifecycleOwner(), // FragmentのViewのライフサイクルにバインド
            previewView
        );
    }
}