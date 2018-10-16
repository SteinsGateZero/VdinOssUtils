package com.steinsgatezero.vdinossutils;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.steinsgatezero.vdinoss.OssCompleteCallback;
import com.steinsgatezero.vdinoss.OssProgressCallback;
import com.steinsgatezero.vdinoss.VdinOss;

public class MainActivity extends Activity implements OssCompleteCallback, OssProgressCallback {
    VdinOss vdinOss;
    TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.osstext);
        vdinOss = new VdinOss.Builder(this, this).isAsync(true).setProgressCallback(this).build();
    }

    @Override
    public void onSuccess(String fileUrl) {
        textView.setText(fileUrl);
        Log.d("osstest", fileUrl);
    }

    @Override
    public void onFailure(int err) {
        Log.d("osstest", err + "");
    }

    @Override
    public void onProgress(long currentSize, long totalSize) {
        Log.d("osstest", currentSize + "/" + totalSize);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        vdinOss.destroy();
    }

    public void upload(View view) {
        String url = vdinOss.upload(Environment.getExternalStorageDirectory() + "/IMG_20181016_110044.jpg");
        Log.d("osstest", url);
    }
}
