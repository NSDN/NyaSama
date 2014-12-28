package com.nyasama.util;

import android.graphics.Bitmap;
import android.util.LruCache;

import com.android.volley.toolbox.ImageLoader;

/**
 * Created by oxyflour on 2014/11/19.
 * REF: http://blog.lemberg.co.uk/volley-part-3-image-loader
 * 
 *
 * 用来为ThisApp 中的 imageLoader 提供存储空间的 Cache
 * 其实用起来和 Hashmap 没什么两样
 * 
 * getBitmap 和 putBitmap 是 ImageCache 中的方法
 * getBitmap 是按URL 取出bitmap
 * putBitmap 是当下载完成后，放入Cache
 * 
 * 其他是LruCache中的方法，LruCache 的意思是 使用 Last Recent Used 算法的 Cache
 * 至于具体为什么默认要用 maxmemory/8 Kbytes 左右的空间，我也不明白
 * 
 * 
 */
public class BitmapLruCache
        extends LruCache<String, Bitmap>
        implements ImageLoader.ImageCache {

    public BitmapLruCache() {
        this(getDefaultLruCacheSize());
    }

    public BitmapLruCache(int sizeInKiloBytes) {
        super(sizeInKiloBytes);
    }

    @Override
    protected int sizeOf(String key, Bitmap value) {
        return value.getRowBytes() * value.getHeight() / 1024;
    }

    @Override
    public Bitmap getBitmap(String url) {
        return get(url);
    }

    @Override
    public void putBitmap(String url, Bitmap bitmap) {
        put(url, bitmap);
    }

    public static int getDefaultLruCacheSize() {
        final int maxMemory =
                (int) (Runtime.getRuntime().maxMemory() / 1024);

        return maxMemory / 8;
    }
}
