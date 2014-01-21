package com.loopj.android.image;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.Environment;

public class WebImageCache {
    private static final String DISK_CACHE_PATH = "/web_image_cache/";

    private LruBitmapCache memoryCache;
    private String diskCachePath;
    private boolean diskCacheEnabled = false;
    private ExecutorService writeThread;
    
    private void resetCachePath(Context context){
        Context appContext = context.getApplicationContext();
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
        	diskCachePath = appContext.getExternalCacheDir().getAbsolutePath() + DISK_CACHE_PATH;
        } else {
        	diskCachePath = appContext.getCacheDir().getAbsolutePath() + DISK_CACHE_PATH;
        }
    }

    public WebImageCache(Context context) {
        // Set up in-memory cache store
        memoryCache = new LruBitmapCache();

        // Set up disk cache store
        resetCachePath(context);

        File outFile = new File(diskCachePath);
        outFile.mkdirs();

        diskCacheEnabled = outFile.exists();

        // Set up threadpool for image fetching tasks
        writeThread = Executors.newSingleThreadExecutor();
    }

    public Bitmap get(final String url) {
        Bitmap bitmap = null;

        // Check for image in memory
        bitmap = getBitmapFromMemory(url);

        // Check for image on disk cache
        if(bitmap == null) {
            bitmap = getBitmapFromDisk(url);

            // Write bitmap back into memory cache
            if(bitmap != null) {
            //	Log.i("WebImageCache", "Hit file cache");
                cacheBitmapToMemory(url, bitmap);
            }
        }

        return bitmap;
    }

    public void put(String url, Bitmap bitmap) {
        cacheBitmapToDisk(url, bitmap);
        cacheBitmapToMemory(url, bitmap);
    }

    public void remove(String url) {
        if(url == null){
            return;
        }

        // Remove from memory cache
        memoryCache.remove(getCacheKey(url));

        // Remove from file cache
        File f = new File(diskCachePath, getCacheKey(url));
        if(f.exists() && f.isFile()) {
            f.delete();
        }
    }

    public void clear() {
        // Remove everything from memory cache
        memoryCache.clear();

        // Remove everything from file cache
        File cachedFileDir = new File(diskCachePath);
        if(cachedFileDir.exists() && cachedFileDir.isDirectory()) {
            File[] cachedFiles = cachedFileDir.listFiles();
            for(File f : cachedFiles) {
                if(f.exists() && f.isFile()) {
                    f.delete();
                }
            }
        }
    }

    private void cacheBitmapToMemory(final String url, final Bitmap bitmap) {
        memoryCache.put(getCacheKey(url), bitmap);
    }

    private void cacheBitmapToDisk(final String url, final Bitmap bitmap) {
        writeThread.execute(new Runnable() {
            @Override
            public void run() {
                if(diskCacheEnabled) {
                    BufferedOutputStream ostream = null;
                    try {
                        ostream = new BufferedOutputStream(new FileOutputStream(new File(diskCachePath, getCacheKey(url))), 2*1024);
                        bitmap.compress(CompressFormat.PNG, 100, ostream);
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if(ostream != null) {
                                ostream.flush();
                                ostream.close();
                            }
                        } catch (IOException e) {}
                    }
                }
            }
        });
    }

    public Bitmap getBitmapFromMemory(String url) {
        return memoryCache.get(getCacheKey(url));
    }

    private Bitmap getBitmapFromDisk(String url) {
        Bitmap bitmap = null;
        if(diskCacheEnabled){
            String filePath = getFilePath(url);
            File file = new File(filePath);
            if(file.exists()) {
                bitmap = BitmapFactory.decodeFile(filePath);
            }
        }
        return bitmap;
    }

    private String getFilePath(String url) {
        return diskCachePath + getCacheKey(url);
    }

    private String getCacheKey(String url) {
        if(url == null){
            throw new RuntimeException("Null url passed in");
        } else {
            return url.replaceAll("[.:/,%?&=]", "+").replaceAll("[+]+", "+");
        }
    }
}
