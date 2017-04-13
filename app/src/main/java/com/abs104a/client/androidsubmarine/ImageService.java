package com.abs104a.client.androidsubmarine;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageService extends Service {

    // upstreamから取得するURL
    private final static String DEFAULT_SERVER = "http://192.168.38.58:8080/cam.jpg?pwd=";
    private String urlString = "";
    private String leftKickString = "";
    private String rightKickString = "";
    private String lightKickString = "";
    private String rightOffKickString = "";
    private String leftOffKickString = "";

    private Looper mServiceLooper;

    private Service mService = this;
    private ServiceHandler mServiceHandler;


    public interface OnGetImageListener{
        void onGetImage(Bitmap image);
    }

    public ImageService() {
    }


    @Override
    public void onCreate() {
        // onCleate
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        mServiceLooper = thread.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        urlString = prefs.getString(this.getString(R.string.server_url_image_string),DEFAULT_SERVER);
        rightKickString = prefs.getString(this.getString(R.string.server_url_right_string),DEFAULT_SERVER);
        leftKickString = prefs.getString(this.getString(R.string.server_url_left_string),DEFAULT_SERVER);
        lightKickString = prefs.getString(this.getString(R.string.server_url_light_string),DEFAULT_SERVER);
        rightOffKickString = prefs.getString(this.getString(R.string.server_url_right_off_string),DEFAULT_SERVER);
        leftOffKickString = prefs.getString(this.getString(R.string.server_url_left_off_string),DEFAULT_SERVER);
    }


    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            // Normally we would do some work here, like download a file.
            // For our sample, we just sleep for 5 seconds.
            long endTime = System.currentTimeMillis() + 5*1000;
            while (System.currentTimeMillis() < endTime) {
                synchronized (this) {
                    try {
                        wait(endTime - System.currentTimeMillis());
                    } catch (Exception e) {
                    }
                }
            }
            // Stop the service using the startId, so that we don't stop
            // the service in the middle of handling another job
            stopSelf(msg.arg1);
        }
    }



    public class LocalBinder extends Binder {
        public ImageService getService() {
            return ImageService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e("LocalService", "Received start id " + startId + ": " + intent);
        Toast.makeText(this, "Start", Toast.LENGTH_SHORT).show();
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.

        // Message msg = mServiceHandler.obtainMessage();
        // msg.arg1 = startId;
        // mServiceHandler.sendMessage(msg);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        // Tell the user we stopped.
        Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // バインドしたとき
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
        // バインドが外れたとき
    }

    // This is the object that receives interactions from clients.  See
    // RemoteService for a more complete example.
    private final IBinder mBinder = new LocalBinder();

    public void getImage(final OnGetImageListener listener) {

        Log.v("ImageService","addQueue");
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                Bitmap image = getUpstreamImage(urlString);
                // listenerが無いときはimageを破棄して終了
                // listenerがあるときはコールバックする．
                if(listener == null){
                    image.recycle();
                }else {
                    listener.onGetImage(image);
                }
            }
        });
    }

    public void saveImage(final OnGetImageListener listener) {
        Log.v("ImageService","addQueue");
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                Bitmap image = getUpstreamImage(urlString);
                try {
                    new ImageUtils().saveBitmap(mService,image);
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(mService,"Can't save picture.",Toast.LENGTH_SHORT).show();
                }
                // listenerが無いときはimageを破棄して終了
                // listenerがあるときはコールバックする．
                if(listener == null){
                    image.recycle();
                }else {
                    listener.onGetImage(image);
                }
            }
        });
    }

    /**
     * 右ボタンをキックする関数
     * toggle true : ON　、false: OFF
     */
    public void kickTurnRight(final boolean toggle){
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                //右のAPIをコール
                if (toggle){
                  postKickServer(rightKickString);
                  Log.v("ImageService","kickONRight");
                }else{
                  postKickServer(rightOffKickString);
                  Log.v("ImageService","kickOFFRight");
                }
            }
        });
    }

    /**
     * 左ボタンをキックする関数
     * toggle true : ON　、false: OFF
     */
    public void kickTurnLeft(final boolean toggle){
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                //左のAPIをコール
                if(toggle){
                  postKickServer(leftKickString);
                  Log.v("ImageService","kickONLeft");
                }else {
                  postKickServer(leftOffKickString);
                  Log.v("ImageService","kickOFFLeft");
                }
            }
        });
    }

    public void kickLightOn() {
        mServiceHandler.post(new Runnable() {
            @Override
            public void run() {
                //左のAPIをコール
                getKickServer(lightKickString);
                Log.v("ImageService","kickLeft");
            }
        });
    }

    public void setServerUrl(String url) {
        urlString = url;
    }

    public String getServerUrl() {
        return urlString;
    }

    /**
     * サーバーのイメージを持ってくる.
     */
    private Bitmap getUpstreamImage(String urlString){
        // 受け取ったbuilderでインターネット通信する
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        Bitmap bitmap = null;

        try{

            URL url = new URL(urlString);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            inputStream = connection.getInputStream();

            bitmap = BitmapFactory.decodeStream(inputStream);
            Log.v("ImageService","GetBitmap" + (bitmap.getConfig() != null ? bitmap.getConfig().name() : "bitmap"));
        }catch (Exception e){
            Log.d("ImageService","Connect Failed");
        }finally
         {
            if (connection != null){
                connection.disconnect();
            }
            try{
                if (inputStream != null){
                    inputStream.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        return bitmap;
    }

    private boolean getKickServer(String urlString){
        // 受け取ったbuilderでインターネット通信する
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        boolean flag = false;
        try{

            URL url = new URL(urlString);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            inputStream = connection.getInputStream();
            Log.d("KickServer","Connected");
            flag = true;
        }catch (Exception e){
            Log.d("KickServer","Connect Failed");
        }finally
        {
            if (connection != null){
                connection.disconnect();
            }
            try{
                if (inputStream != null){
                    inputStream.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        return flag;
    }

    private boolean postKickServer(String urlString){
        // 受け取ったbuilderでインターネット通信する
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        boolean flag = false;
        try{

            URL url = new URL(urlString);
            connection = (HttpURLConnection)url.openConnection();
            connection.setRequestMethod("POST");
            connection.connect();
            inputStream = connection.getInputStream();
            Log.d("KickServer","Connected");
            flag = true;
        }catch (Exception e){
            Log.d("KickServer","Connect Failed");
        }finally
        {
            if (connection != null){
                connection.disconnect();
            }
            try{
                if (inputStream != null){
                    inputStream.close();
                }
            }catch (IOException e){
                e.printStackTrace();
            }
        }

        return flag;
    }
}
