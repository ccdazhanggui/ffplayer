package com.rockcarry.ffplayer;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Toast;
import android.util.Log;

public class TestPlayerActivity extends Activity {
    private player       mPlayer    = null;
    private playerRoot   mRoot      = null;
    private SurfaceView  mView      = null;
    private SeekBar      mSeek      = null;
    private ImageView    mPause     = null;
    private boolean      mIsPlaying = false;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        String url    = "/sdcard/test.mp4";
        Intent intent = getIntent();
        String action = intent.getAction();
        if (intent.ACTION_VIEW.equals(action)) {
            Uri    uri    = (Uri) intent.getData();
            String scheme = uri.getScheme();
            if (scheme.equals("file")) {
                url = uri.getPath();
            } else if (  scheme.equals("http" )
                      || scheme.equals("https")
                      || scheme.equals("rtsp" )
                      || scheme.equals("rtmp" ) ) {
                url = uri.toString();
            } else if (scheme.equals("content")) {
                String[] proj = { MediaStore.Images.Media.DATA };
                Cursor cursor = managedQuery(uri, proj, null, null, null);
                int    colidx = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                cursor.moveToFirst();
                url = cursor.getString(colidx);
            }
        }

        mPlayer = new player();
        if (!mPlayer.open(url)) {
            String str = String.format(getString(R.string.open_video_failed), url);
            Toast.makeText(this, str, Toast.LENGTH_LONG).show();
//          finish(); return;
        }

        mPlayer.setPlayerEventCallback(new player.playerEventCallback() {
            @Override
            public void onPlayerEvent(int event, long param) {
//              android.util.Log.d("===ck===", "event = " + event + ", param = " + param);
                switch (event) {
                case player.EVENT_PLAY_PROGRESS:
                    break;
                case player.EVENT_PLAY_COMPLETED:
                    break;
                }
            }
        });

        mRoot = (playerRoot )findViewById(R.id.player_root);
        mRoot.setOnSizeChangedListener(new playerRoot.OnSizeChangedListener() {
            @Override
            public void onSizeChanged(int w, int h, int oldw, int oldh) {
                int rw = w; // root width
                int rh = h; // root height

                int vw = mPlayer.getParam(player.PARAM_VIDEO_WIDTH ); // video width
                int vh = mPlayer.getParam(player.PARAM_VIDEO_HEIGHT); // video height

                int sw, sh; // scale width & height
                if (rw * vh < vw * rh) {
                    sw = rw; sh = sw * vh / vw;
                } else {
                    sh = rh; sw = sh * vw / vh;
                }

                final int fw = sw;
                final int fh = sh;
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)mView.getLayoutParams();
                        lp.width  = fw;
                        lp.height = fh;
                        mView.setLayoutParams(lp);
                    }
                });
            }
        });

        mView = (SurfaceView)findViewById(R.id.video_view  );
        mView.getHolder().addCallback(
            new Callback() {
                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                }

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    mPlayer.setDisplayWindow(holder.getSurface());
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    mPlayer.setDisplayWindow(null);
                }
            }
        );

        mSeek = (SeekBar)findViewById(R.id.seek_bar);
        mSeek.setMax(mPlayer.getParam(player.PARAM_MEDIA_DURATION));
        mSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    mPlayer.seek(progress);
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        mPause = (ImageView)findViewById(R.id.btn_playpause);
        mPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testPlayerPlay(!mIsPlaying);
            }
        });

        // show buttons with auto hide
        showUIControls(true, true);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mPlayer.close();
    }

    @Override
    public void onResume() {
        super.onResume();
        testPlayerPlay(true );
    }

    @Override
    public void onPause() {
        super.onPause();
        testPlayerPlay(false);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
            showUIControls(true, true);
        }
        return super.dispatchTouchEvent(ev);
    }

    private void testPlayerPlay(boolean play) {
        if (play) {
            mPlayer.play();
            mIsPlaying = true;
            mPause  .setImageResource(R.drawable.icn_media_pause);
            mHandler.sendEmptyMessage(MSG_UPDATE_PROGRESS);
        } else {
            mPlayer.pause();
            mIsPlaying = false;
            mPause  .setImageResource(R.drawable.icn_media_play );
            mHandler.removeMessages  (MSG_UPDATE_PROGRESS);
        }
    }

    private void showUIControls(boolean show, boolean autohide) {
        mHandler.removeMessages(MSG_HIDE_BUTTONS);
        if (show) {
            mSeek .setVisibility(View.VISIBLE);
            mPause.setVisibility(View.VISIBLE);
            if (autohide) {
                mHandler.sendEmptyMessageDelayed(MSG_HIDE_BUTTONS, 5000);
            }
        }
        else {
            mSeek .setVisibility(View.INVISIBLE);
            mPause.setVisibility(View.INVISIBLE);
        }
    }

    private static final int MSG_UPDATE_PROGRESS = 1;
    private static final int MSG_HIDE_BUTTONS    = 2;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MSG_UPDATE_PROGRESS: {
                    mHandler.sendEmptyMessageDelayed(MSG_UPDATE_PROGRESS, 200);
                    mSeek.setProgress(mPlayer.getParam(player.PARAM_MEDIA_POSITION));
                    Log.d("===ck===", "media progress: " + mPlayer.getParam(player.PARAM_MEDIA_POSITION));
                }
                break;
            case MSG_HIDE_BUTTONS: {
                    mSeek .setVisibility(View.INVISIBLE);
                    mPause.setVisibility(View.INVISIBLE);
                }
                break;
            }
        }
    };
}
