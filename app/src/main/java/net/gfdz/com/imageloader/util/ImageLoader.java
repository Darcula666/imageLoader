package net.gfdz.com.imageloader.util;

import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.LruCache;
import android.widget.ImageView;

import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by Administrator on 2015/12/24.
 * 图片加载类（单例模式）
 */
public class ImageLoader {
    private static ImageLoader mInstance;
    private LruCache<String,Bitmap> mLruCache;//图片缓存的核心对象
    private ExecutorService mThreadPool;//线程池
    private static final int DEFAULT_THREAD_COUNT=1;
    private Type mType=Type.LIFO;//队列的调度方式
    private LinkedList<Runnable> mTaskQueue;//任务队列
    private Thread mPoolThread;//后台轮询线程
    private Handler mPoolTheradHandler;
    private Handler mUIHandler;//UI线程中的Handler;
    public enum  Type{
        FIFO,LIFO
    }
    private ImageLoader(int mTheradCount,Type type){
        init(mTheradCount,type);
    }

    private void init(int threadCount,Type type) {
        mPoolThread=new Thread(){
            @Override
            public void run() {
                super.run();
                Looper.prepare();
                mPoolTheradHandler=new Handler(){
                    @Override
                    public void handleMessage(Message msg) {
                        //线程池去取出一个任务执行
                    }
                };
                Looper.loop();
            }
        };
        mPoolThread.start();
        //获取我们应用的最大可用内存
        int maxMemory= (int) Runtime.getRuntime().maxMemory();
        int cacheMemory=maxMemory/8;

        mLruCache=new LruCache<String,Bitmap>(cacheMemory){
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getRowBytes()*value.getHeight();
            }
        };

        mThreadPool= Executors.newFixedThreadPool(threadCount);
        mTaskQueue=new LinkedList<>();
        mType=type;
    }

    public static ImageLoader getInstance(){
        if (mInstance==null){
            synchronized (ImageLoader.class){//同步
                if (mInstance==null){
                    mInstance=new ImageLoader(DEFAULT_THREAD_COUNT,Type.LIFO);
                }
            }
        }
        return mInstance;
    }
    //通过path为imageView设置图片
    public void loadImae(String path,ImageView imageView){
        imageView.setTag(path);

        if (mUIHandler==null){
            mUIHandler=new Handler(){
                @Override
                public void handleMessage(Message msg) {
                  //获取得到的图片，为imageview回调设置图片

                }
            };
        }
        //根据path在缓存中获取bitmap
        Bitmap bm=getBitmapFromLruCache(path);
        if (bm!=null){
            Message message=Message.obtain();
            ImageHodler hodler=new ImageHodler();
            hodler.bitmap=bm;
            hodler.path=path;
            hodler.imageView=imageView;
            message.obj=hodler;
            mUIHandler.sendMessage(message);
        }
    }

    private Bitmap getBitmapFromLruCache(String key) {
        return mLruCache.get(key);
    }
    private class ImageHodler{
     Bitmap bitmap;
        ImageView imageView;
        String path;
    }


}
