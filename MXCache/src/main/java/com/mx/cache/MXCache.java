package com.mx.cache;

import android.content.Context;
import android.text.TextUtils;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 缓存工具类
 * Created by zmx on 2015/11/30.
 */
public class MXCache {
    private static final String TAG = MXCache.class.getSimpleName();
    private static MXCache INSTANCE;
    private static Context mContext;
    private static CacheParams cacheParams;
    /**
     * 互斥操作锁
     */
    private static final Object SYNC_WRITE = new Object();
    private StringCache stringCache;
    private AtomicInteger READ_SIZE = new AtomicInteger(0);

    public static void init(Context context, CacheParams params) {
        mContext = context;
        cacheParams = params;
    }

    private MXCache(Context context) {
        File cache = context.getCacheDir();
        if (cache != null && !cache.exists()) {
            cache.mkdirs();
        }

        if (cacheParams == null) {
            cacheParams = new CacheParams(new File(cache, "MXCache"));
            cacheParams.setMemCacheSizePercent(context, 0.06f);//使用内存缓存
            cacheParams.setDiskCacheSize(100 * 1024 * 1024);//100M的数据缓存
            cacheParams.setRecycleImmediately(true);
        }
        stringCache = new StringCache(cacheParams);
    }

    /**
     * 单例
     *
     * @return
     */
    public synchronized static MXCache getInstance() {
        if (INSTANCE == null)
            INSTANCE = new MXCache(mContext);
        return INSTANCE;
    }

    /**
     * 获取缓存字符串
     *
     * @param key
     * @return
     */
    public String getCache(String key) {
        long timeDelay = 60 * 24 * 15;//默认为15天
        return getCache(key, timeDelay);
    }

    /**
     * 缓存
     *
     * @param key
     * @param timeDelay 缓存有效期 分钟
     * @return
     */
    public String getCache(String key, long timeDelay) {
        if (TextUtils.isEmpty(key)) return null;
        READ_SIZE.incrementAndGet();
        String value = stringCache.getCache(key, timeDelay);
        READ_SIZE.decrementAndGet();
        return value;
    }

    /**
     * 重置缓存，会清空所有数据
     */
    public void reset() {
        synchronized (SYNC_WRITE) {
            stringCache.clearCache();
            stringCache.close();
            stringCache = null;
            stringCache = new StringCache(cacheParams);
        }
    }

    /**
     * 插入js缓存文件
     *
     * @param key
     * @param value
     */
    public void addCache(String key, String value) {
        if (TextUtils.isEmpty(key)) return;
        stringCache.addCache(key, value);
    }
}
