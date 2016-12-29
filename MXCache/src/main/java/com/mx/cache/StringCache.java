/**
 * Copyright (c) 2012-2013, Michael Yang 杨福海 (www.yangfuhai.com).
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mx.cache;

import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

class StringCache {
    private static final BytesBufferPool BYTES_BUFFER_POOL = new BytesBufferPool(5, 100 * 1024);

    private static final String TIME_SEPARATE = "#@@#";
    private static final String TAG = StringCache.class.getSimpleName();

    private DiskCache mDiskCache;
    private IMemoryCache mMemoryCache;

    public StringCache(CacheParams cacheParams) {
        init(cacheParams);
    }

    /**
     * 初始化 图片缓存
     *
     * @param mCacheParams
     */
    private void init(CacheParams mCacheParams) {
        // 是否启用内存缓存
        if (mCacheParams.memoryCacheEnabled) {
            // 是否立即回收内存
            if (mCacheParams.recycleImmediately)
                mMemoryCache = new SoftMemoryCacheImpl(mCacheParams.memCacheSize);
            else
                mMemoryCache = new BaseMemoryCacheImpl(mCacheParams.memCacheSize);
        }

        // 是否启用sdcard缓存
        if (mCacheParams.diskCacheEnabled) {
            try {
                String path = mCacheParams.diskCacheDir.getAbsolutePath();
                mDiskCache = new DiskCache(path, mCacheParams.diskCacheCount, mCacheParams.diskCacheSize, false);
            } catch (IOException e) {
                // ignore.
            }
        }
    }

    public synchronized void addCache(String url, String value) {
        if (url == null || value == null) {
            return;
        }
        value = "" + System.currentTimeMillis() + TIME_SEPARATE + value;
        addToDiskCache(url, value);
        addToMemoryCache(url, value);
    }

    /**
     * 添加图片到内存缓存中
     *
     * @param url   Url 地址
     * @param value 数据
     */
    private void addToMemoryCache(String url, String value) {
        if (url == null || value == null) {
            return;
        }
        if (mMemoryCache != null)
            mMemoryCache.put(url, value);
    }

    /**
     * 添加 数据到sdcard缓存中
     *
     * @param url   url地址
     * @param value 数据信息
     */
    private void addToDiskCache(String url, String value) {
        if (url == null || value == null) {
            return;
        }
        addToDiskCache(url, value.getBytes());
    }

    /**
     * 添加 数据到sdcard缓存中
     *
     * @param url  url地址
     * @param data 数据信息
     */
    private void addToDiskCache(String url, byte[] data) {
        if (mDiskCache == null || url == null || data == null) {
            return;
        }
        // Add to disk cache
        byte[] key = Utils.makeKey(url);
        long cacheKey = Utils.crc64Long(key);
        ByteBuffer buffer = ByteBuffer.allocate(key.length + data.length);
        buffer.put(key);
        buffer.put(data);
        synchronized (TAG) {
            try {
                mDiskCache.insert(cacheKey, buffer.array());
            } catch (IOException ex) {
                // ignore.
            }
        }
    }

    /**
     * 获取缓存
     *
     * @param key
     * @param timeout 缓存的最长保存时间  单位：分钟
     * @return
     */
    public synchronized String getCache(String key, long timeout) {
        long st = System.currentTimeMillis();
        if (key == null) {
            return null;
        }
        String result = getStringFromMemoryCache(key);
        if (TextUtils.isEmpty(result)) {
            BytesBufferPool.BytesBuffer buffer = BYTES_BUFFER_POOL.get();
            if (getSDData(key, buffer)) {
                result = new String(buffer.data, buffer.offset, buffer.length);
            }

            BYTES_BUFFER_POOL.recycle(buffer);
        }

        try {
            if (result != null && result.indexOf(TIME_SEPARATE) < 15) {
                String t = result.split(TIME_SEPARATE)[0];
                if (!TextUtils.isEmpty(t)) {
                    float time = (System.currentTimeMillis() - Long.valueOf(t)) / ((float) 1000 * 60);//保存的时间长度 分钟
                    Log.v(TAG, "缓存:" + key + "   已缓存时间：" + time + "  超时时间点：" + timeout);
                    if (time > 0 && time < timeout) {
                        result = result.substring(t.length() + TIME_SEPARATE.length());
                    } else {
                        clearCache(key);
                        result = null;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            clearCache(key);
            result = null;
        }

        if (!TextUtils.isEmpty(result)) {
            float ts = (System.currentTimeMillis() - st) / (float) 1000;
//            Log.v(TAG, "缓存：" + key + "  ---读取的时间为：" + ts + " s");
        }
        return result;
    }

    /**
     * 从sdcard中获取内存缓存
     *
     * @param url    图片url地址
     * @param buffer 填充缓存区
     * @return 是否获得图片
     */
    private boolean getSDData(String url, BytesBufferPool.BytesBuffer buffer) {
        if (mDiskCache == null)
            return false;

        byte[] key = Utils.makeKey(url);
        long cacheKey = Utils.crc64Long(key);
        try {
            DiskCache.LookupRequest request = new DiskCache.LookupRequest();
            request.key = cacheKey;
            request.buffer = buffer.data;
            synchronized (TAG) {
                if (!mDiskCache.lookup(request))
                    return false;
            }
            if (Utils.isSameKey(key, request.buffer)) {
                buffer.data = request.buffer;
                buffer.offset = key.length;
                buffer.length = request.length - buffer.offset;
                return true;
            }
        } catch (IOException ex) {
            // ignore.
        }
        return false;
    }

    /**
     * 从内存缓存中获取bitmap.
     */
    private String getStringFromMemoryCache(String data) {
        if (mMemoryCache != null)
            return mMemoryCache.get(data);
        return null;
    }

    /**
     * 清空内存缓存和sdcard缓存
     */
    public void clearCache() {
        clearMemoryCache();
        clearDiskCache();
    }

    public void clearDiskCache() {
        if (mDiskCache != null)
            mDiskCache.delete();
    }

    public void clearMemoryCache() {
        if (mMemoryCache != null) {
            mMemoryCache.evictAll();
        }
    }

    public void clearCache(String key) {
        clearMemoryCache(key);
        clearDiskCache(key);
    }

    public void clearDiskCache(String url) {
        addToDiskCache(url, new byte[0]);
    }

    public void clearMemoryCache(String key) {
        if (mMemoryCache != null) {
            mMemoryCache.remove(key);
        }
    }

    /**
     * Closes the disk cache associated with this ImageCache object. Note that
     * this includes disk access so this should not be executed on the main/UI
     * thread.
     */
    public void close() {
        clearMemoryCache();
        if (mDiskCache != null)
            mDiskCache.close();
    }
}
