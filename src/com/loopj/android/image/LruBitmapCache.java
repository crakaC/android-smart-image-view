package com.loopj.android.image;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.util.Log;

public class LruBitmapCache {
	private LruCache<String, Bitmap> mMemoryCache;	
	
	public LruBitmapCache(){
		int maxMemory = (int) Runtime.getRuntime().maxMemory();
		int cacheSize = maxMemory / 4;
		mMemoryCache = new LruCache<String, Bitmap>(cacheSize){
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				return bitmap.getHeight() * bitmap.getRowBytes();
			};
		};
		Log.v("LruBitmapCache", "set cache size:" + cacheSize + "bytes");
	}
	
	public Bitmap get(String key){
		return mMemoryCache.get(key);
	}
	
	public void put(String key, Bitmap bitmap){
		if(get(key) == null){
			mMemoryCache.put(key, bitmap);
		}
	}
	
	public void remove(String key){
		Bitmap removed = mMemoryCache.remove(key);
		if(removed != null){
			removed.recycle();
			removed = null;
		}
	}
	
	public void clear(){
		mMemoryCache.evictAll();
	}
}
