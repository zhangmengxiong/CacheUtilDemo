package com.mx.demo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.mx.cache.CacheParams;
import com.mx.cache.MXCache;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private ExecutorService executorService;    // = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        CacheParams cacheParams = new CacheParams("/sdcard/cache");
        cacheParams.setDiskCacheCount(100);
        cacheParams.setMemCacheSizePercent(this, 0.06f);

        MXCache.init(this, cacheParams);

        executorService = Executors.newSingleThreadExecutor();
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                int count = 1;
                long start = System.currentTimeMillis();
                while (count < 1000000) {
                    MXCache.getInstance().addCache("aaa" + count, "123" + System.currentTimeMillis());
                    count++;
                }
                long end = System.currentTimeMillis();

                Log.v("aa", "" + (end - start));
            }
        });
    }

    @Override
    protected void onDestroy() {
        executorService.shutdownNow();
        super.onDestroy();
    }
}
