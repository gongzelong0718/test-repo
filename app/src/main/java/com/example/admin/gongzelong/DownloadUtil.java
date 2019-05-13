package com.example.admin.gongzelong;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DownloadUtil {
    private static final String TAG = "DownloadUtil";
    private static final String PATH_DOWNLOAD = Environment.getExternalStorageDirectory() + "/DownloadFile";

    protected ApiInterface mApi;
    private Call<ResponseBody> mCall;
    private File mFile;
    private Thread mThread;
    private String mFilePath;

    public DownloadUtil() {
        if (mApi == null) {
            mApi = ApiHelper.getInstance().buildRetrofit("https://sapi.daishumovie.com/")
                    .createService(ApiInterface.class);
        }
    }

    public void downloadFile(String url, final DownloadListener downloadListener) {
        //get the name
        String name = url;
        if (FileUtils.createOrExistsDir(PATH_DOWNLOAD)) {
            int i = name.lastIndexOf('/');//get the file name
            if (i != -1) {
                name = name.substring(i);
                mFilePath = PATH_DOWNLOAD +
                        name;
            }
        }
        if (TextUtils.isEmpty(mFilePath)) {
            Log.e(TAG, "download: saving path is null");
            return;
        }
        //建立一个文件
        mFile = new File(mFilePath);
        if (!FileUtils.isFileExists(mFile) && FileUtils.createOrExistsFile(mFile)) {
            if (mApi == null) {
                Log.e(TAG, "download: downloading api is null");
                return;
            }
            mCall = mApi.downloadFile(url);
            mCall.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(@NonNull Call<ResponseBody> call, @NonNull final Response<ResponseBody> response) {
                    //child thread
                    mThread = new Thread() {
                        @Override
                        public void run() {
                            super.run();
                            //save to the local
                            Log.i(TAG, "starting download");
                            writeFile2Disk(response, mFile, downloadListener);
                        }
                    };
                    mThread.start();
                }

                @Override
                public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                    downloadListener.onFailure("Network error");
                }
            });
        } else {
            downloadListener.onFinish(mFilePath);
        }
    }

    private void writeFile2Disk(Response<ResponseBody> response, File file, DownloadListener downloadListener) {
        downloadListener.onStart();
        long currentLength = 0;
        OutputStream os = null;

        if (response.body() == null) {
            downloadListener.onFailure("resource error！");
            return;
        }
        InputStream is = response.body().byteStream();
        long totalLength = response.body().contentLength();

        try {
            os = new FileOutputStream(file);
            int len;
            byte[] buff = new byte[1024];
            while ((len = is.read(buff)) != -1) {
                os.write(buff, 0, len);
                currentLength += len;
                Log.e(TAG, "Current progress: " + currentLength);
                downloadListener.onProgress((int) (100 * currentLength / totalLength));
                if ((int) (100 * currentLength / totalLength) == 100) {
                    downloadListener.onFinish(mFilePath);
                }
            }
        } catch (FileNotFoundException e) {
            downloadListener.onFailure("file not found！");
            e.printStackTrace();
        } catch (IOException e) {
            downloadListener.onFailure("IO error！");
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}