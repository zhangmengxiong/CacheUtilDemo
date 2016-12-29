package com.mx.cache;

import android.app.ActivityManager;
import android.content.Context;

import java.io.File;

/**
 * cache 的配置信息.
 * 创建人： zhangmengxiong
 * 创建时间： 2016-12-29.
 * 联系方式: zmx_final@163.com
 */

public class CacheParams {
    // 默认的内存缓存大小
    private static final int DEFAULT_MEM_CACHE_SIZE = 1024 * 1024 * 3; // 3MB

    // 默认的磁盘缓存大小
    private static final int DEFAULT_DISK_CACHE_SIZE = 1024 * 1024 * 50; // 50MB
    private static final int DEFAULT_DISK_CACHE_COUNT = 1000 * 50; // 缓存的图片数量

    // BitmapCache的一些默认配置
    private static final boolean DEFAULT_MEM_CACHE_ENABLED = true;
    private static final boolean DEFAULT_DISK_CACHE_ENABLED = true;

    int memCacheSize = DEFAULT_MEM_CACHE_SIZE;
    int diskCacheSize = DEFAULT_DISK_CACHE_SIZE;
    int diskCacheCount = DEFAULT_DISK_CACHE_COUNT;
    File diskCacheDir;
    boolean memoryCacheEnabled = DEFAULT_MEM_CACHE_ENABLED;
    boolean diskCacheEnabled = DEFAULT_DISK_CACHE_ENABLED;
    boolean recycleImmediately = true;

    public CacheParams(File diskCacheDir) {
        this.diskCacheDir = diskCacheDir;
    }

    public CacheParams(String diskCacheDir) {
        this.diskCacheDir = new File(diskCacheDir);
    }

    /**
     * 设置缓存大小
     *
     * @param context
     * @param percent 百分比，值的范围是在 0.05 到 0.8之间
     */
    public void setMemCacheSizePercent(Context context, float percent) {
        if (percent < 0.05f || percent > 0.8f) {
            throw new IllegalArgumentException(
                    "setMemCacheSizePercent - percent must be " + "between 0.05 and 0.8 (inclusive)");
        }
        memCacheSize = Math.round(percent * getMemoryClass(context) * 1024 * 1024);
    }

    public void setMemCacheSize(int memCacheSize) {
        this.memCacheSize = memCacheSize;
    }

    public void setDiskCacheSize(int diskCacheSize) {
        this.diskCacheSize = diskCacheSize;
    }

    private static int getMemoryClass(Context context) {
        return ((ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass();
    }

    public void setDiskCacheCount(int diskCacheCount) {
        this.diskCacheCount = diskCacheCount;
    }

    public void setRecycleImmediately(boolean recycleImmediately) {
        this.recycleImmediately = recycleImmediately;
    }

}
