package com.example.admin.gongzelong;

import java.io.File;

public class CompressUtil {
    public int compress(File file, CompressListener compressListener) {
        compressListener.onStart();
        compressListener.onFailure("");
        int currentLength = 0;
        compressListener.onProgress(currentLength);
        String localPath = "xasdfasdfa";
        compressListener.onFinish(localPath);
        return 0;
    }
}
