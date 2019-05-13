package com.example.admin.gongzelong;

import android.Manifest;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.admin.gongzelong.permission.KbPermission;
import com.example.admin.gongzelong.permission.KbPermissionListener;
import com.example.admin.gongzelong.permission.KbPermissionUtils;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int COMPRESS_START = 1;
    private static final int COMPRESS_PROGRESS = 2;
    private static final int COMPRESS_FINISH = 3;
    private static final int COMPRESS_FAILURE = 4;

    public static String URL = "http://ipv4.download.thinkbroadband.com/50MB.zip";

    private static final String TAG = "MainActivity";

    private TextView mDownloadingProgress;
    private TextView mCompressProgress;

    private Handler mMainThreadHandler;
    private Handler mWorkerThreadHandler;
    private Runnable mStartingDownloadingRunnable;
    private Runnable mDownloadingProgressRunnable;
    private Runnable mDownloadingFinishRunnable;
    private Runnable mDownloadingFailureRunnable;
    private MainActivity mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        TextView mDownloadingButton = findViewById(R.id.btn_download);
        mDownloadingProgress = findViewById(R.id.tv_progress);
        mCompressProgress = findViewById(R.id.tv_compress_progress);

        mDownloadingButton.setOnClickListener(this);

        // Handle message from main thread message queue.
        mMainThreadHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                Log.i("MAIN_THREAD", "Receive message from child thread.");
                if (msg.what == COMPRESS_START) {
                    // If task one button is clicked.
                    mCompressProgress.setText("Compress Start");
                } else if (msg.what == COMPRESS_PROGRESS) {
                    // If task two button is clicked.
                    int currentProgress = msg.arg1;
                    mCompressProgress.setText("Compress " + currentProgress);
                } else if (msg.what == COMPRESS_FAILURE) {

                } else if (msg.what == COMPRESS_FINISH) {
                    mCompressProgress.setText("Compress Finish.");
                }
            }
        };

    }

    @Override
    public void onClick(View v) {
        if (KbPermissionUtils.needRequestPermission()) {
            KbPermission.with(this)
                    .requestCode(100)
                    .permission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .callBack(new KbPermissionListener() {
                        @Override
                        public void onPermit(int requestCode, String... permission) {
                            downloadPicture();
                        }

                        @Override
                        public void onCancel(int requestCode, String... permission) {
                            KbPermissionUtils.goSetting(mContext);
                        }
                    })
                    .send();
        } else {
            downloadPicture();
        }
    }

    private void downloadPicture() {
        //下载相关
        DownloadUtil downloadUtil = new DownloadUtil();
        downloadUtil.downloadFile(URL, new DownloadListener() {
            @Override
            public void onStart() {
                Log.e(TAG, "onStart: ");
                mStartingDownloadingRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mDownloadingProgress.setText("Start downloading");
                    }
                };
                mMainThreadHandler.post(mStartingDownloadingRunnable);
            }

            @Override
            public void onProgress(final int currentLength) {
                Log.e(TAG, "onLoading: " + currentLength);
                mDownloadingProgressRunnable = new Runnable() {
                    @Override
                    public void run() {
                        mDownloadingProgress.setText(currentLength);
                    }
                };
                mMainThreadHandler.post(mDownloadingProgressRunnable);
            }

            @Override
            public void onFinish(final String localPath) {
                Log.e(TAG, "onFinish: " + localPath);

                // Prepare child thread Lopper object.
                if (Looper.myLooper() == null) {
                    Log.d(TAG, "child thread looper");
                    Looper.prepare();
                }

                // Create child thread Handler.
                mWorkerThreadHandler = new Handler(Looper.myLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        // When child thread handler get message from child thread message queue.
                        Log.i("CHILD_THREAD", "Receive message from main thread.");
                        Message message = new Message();
                        message.what = msg.what;
                        // Send the message back to main thread message queue use main thread message Handler.
                        mMainThreadHandler.sendMessage(message);
                    }
                };

                // Loop the child thread message queue.
                Looper.loop();


                File file = new File(localPath);
                CompressListener compressListener = new CompressListener() {
                    @Override
                    public void onStart() {
                        Message msg = new Message();
                        msg.what = COMPRESS_START;
                        // Use worker thread message Handle
                    }

                    @Override
                    public void onProgress(int currentLength) {
                        Message msg = new Message();
                        msg.what = COMPRESS_PROGRESS;
                        msg.arg1 = currentLength;
                        // Use worker thread message Handle
                    }

                    @Override
                    public void onFinish(String localPath) {
                        Message msg = new Message();
                        msg.what = COMPRESS_FINISH;
                        // Use worker thread message Handle
                    }

                    @Override
                    public void onFailure(String erroInfo) {
                        Message msg = new Message();
                        msg.what = COMPRESS_FAILURE;
                        // Use worker thread message Handle
                    }
                };
                compressFile(file, compressListener);
                mDownloadingFinishRunnable = new Runnable() {
                    @Override
                    public void run() {
                    }
                };
                mMainThreadHandler.post(mDownloadingFinishRunnable);
            }

            @Override
            public void onFailure(final String erroInfo) {
                Log.e(TAG, "onFailure: " + erroInfo);
                mDownloadingFailureRunnable = new Runnable() {
                    @Override
                    public void run() {
                    }
                };
                mMainThreadHandler.post(mDownloadingFailureRunnable);
            }
        });
    }

    private void compressFile(File file, CompressListener compressListener) {
        CompressUtil compressUtil = new CompressUtil();
        compressUtil.compress(file, compressListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMainThreadHandler.removeCallbacks(mDownloadingFailureRunnable);
        mMainThreadHandler.removeCallbacks(mStartingDownloadingRunnable);
        mMainThreadHandler.removeCallbacks(mDownloadingProgressRunnable);
        mMainThreadHandler.removeCallbacks(mDownloadingFinishRunnable);
    }
}
