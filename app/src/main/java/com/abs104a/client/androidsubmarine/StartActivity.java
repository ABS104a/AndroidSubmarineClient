package com.abs104a.client.androidsubmarine;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class StartActivity extends AppCompatActivity {
    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private Activity mActivity = this;

    private boolean isGetting = false;
    private final static int GET_IMAGE_INTERVAL = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_start);

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        viewInit();
    }

    @Override
    protected void onDestroy() {
        doUnbindService();
        mHandler.removeCallbacks(mRunnable);
        super.onDestroy();
    }

    private void viewInit(){

        {
            final ImageButton startButton = (ImageButton) mActivity.findViewById(R.id.startImageButton);
            final ImageButton startControlButton = (ImageButton) mActivity.findViewById(R.id.startControlImageButton);
            View.OnClickListener listener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // StartButton がクリックされた時
                    doBindService();
                    final ImageButton startButton = (ImageButton)findViewById(R.id.startImageButton);
                    final View settingButton = findViewById(R.id.settings_button);
                    if(startButton != null){
                        if(isGetting) {
                            startButton.setImageResource(android.R.drawable.ic_media_pause);
                            startButton.setVisibility(View.VISIBLE);
                            settingButton.setVisibility(View.INVISIBLE);
                            hide();
                        }else {
                            startButton.setImageResource(android.R.drawable.ic_media_play);
                            startButton.setVisibility(View.INVISIBLE);
                            settingButton.setVisibility(View.VISIBLE);
                            show();
                        }
                    }
                }
            };
            startButton.setOnClickListener(listener);
            startControlButton.setOnClickListener(listener);
        }

        {
            final ImageButton settingsButton = (ImageButton) mActivity.findViewById(R.id.settings_button);
            settingsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // settingsButtonがクリックされた時，設定を開く
                    Intent settingsIntent = new Intent(mActivity,SettingsActivity.class);
                    mActivity.startActivity(settingsIntent);
                }
            });

        }

        {
            // 右ボタンの設定
            final View rightButton = mActivity.findViewById(R.id.button_right);
            rightButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // rightClick
                    if(mBoundService != null){
                        mBoundService.kickTurnRight();
                    }
                }
            });

        }

        {
            // 左ボタンの設定
            final View leftButton = mActivity.findViewById(R.id.button_left);
            leftButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // leftClick
                    if(mBoundService != null){
                        mBoundService.kickTurnLeft();
                    }
                }
            });

        }

        {
            // ライトの設定
            final View leftButton = mActivity.findViewById(R.id.light_button);
            leftButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // leftClick
                    if(mBoundService != null){
                        mBoundService.kickLightOn();
                    }
                }
            });

        }

        {
            // 画像保存の設定
            final View leftButton = mActivity.findViewById(R.id.camera_button);
            leftButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // 画像を保存する．
                    if(mBoundService != null){
                        mBoundService.saveImage(new ImageService.OnGetImageListener() {
                            @Override
                            public void onGetImage(final Bitmap image) {
                                if(image != null){
                                    mActivity.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            ImageView imageView = getMainImageView();
                                            if(imageView != null){
                                                imageView.setImageBitmap(image);
                                            }
                                        }
                                    });
                                    try {
                                        ImageView toastImage = new ImageView(mActivity);
                                        toastImage.setImageBitmap(ImageUtils.copy(mActivity, image));
                                        Toast toast = new Toast(mActivity);
                                        toast.setDuration(Toast.LENGTH_LONG);
                                        toast.setView(toastImage);
                                        toast.show();
                                    }catch (Exception e){
                                        e.printStackTrace();
                                    }
                                }
                            }
                        });
                    }
                }
            });

        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        //delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


    private final Handler mHandler = new Handler();

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            if(mBoundService != null && mIsBound && isGetting) {
                mBoundService.getImage(new ImageService.OnGetImageListener() {
                    @Override
                    public void onGetImage(final Bitmap image) {
                        if(image != null){
                            mActivity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ImageView imageView = getMainImageView();
                                    if(imageView != null){
                                        imageView.setImageBitmap(image);
                                    }
                                }
                            });
                        }
                        mHandler.postDelayed(mRunnable,GET_IMAGE_INTERVAL);
                    }
                });
            }
        }
    };

    private ImageView getMainImageView(){
        return (ImageView) mActivity.findViewById(R.id.imageView);
    }

    private ImageService mBoundService = null;

    private boolean mIsBound;

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((ImageService.LocalBinder)service).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService = null;
        }
    };

    void doBindService() {
        Intent mServiceIntent = new Intent(this, ImageService.class);
        bindService(mServiceIntent, mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        kick();
    }

    void doUnbindService() {
        if (mIsBound) {
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    void kick(){
        if(!isGetting)
            mHandler.postDelayed(mRunnable,GET_IMAGE_INTERVAL);
        isGetting = !isGetting;
    }
}
