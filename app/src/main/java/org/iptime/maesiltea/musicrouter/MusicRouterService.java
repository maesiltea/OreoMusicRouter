package org.iptime.maesiltea.musicrouter;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRouting;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

/**
 * MusicRouterService class
 * It checks the music is played or not
 * If music is played by other app, then routing service will working.
 * If music is stopped, then routing service will stopped.
 */
public class MusicRouterService extends Service {
    private String TAG = "MusicRouterService";
    private Looper mLooper;
    private ServiceHandler mHandler;
    private AudioManager mManager;
    private AudioManager.AudioPlaybackCallback mPlaybackCallback;
    private AudioDeviceCallback mDeviceCallback;
    private SparseArray<AudioDeviceInfo> mOutputDevices;
    private MusicRouterDeviceCallback mMusicDeviceCallback;
    private AudioRouting.OnRoutingChangedListener mRoutingChangedListener;
    private AudioDeviceInfo mRoutingDevice;

    /**
     *  Music playback related implementations
     */
    private AudioTrack mTrack;
    private int mBufferSize;
    private boolean mIsPlaying;
    private boolean mIsStopped;
    private boolean mPlaybackState;
    private byte[] mBuffer;
    private Thread mMutedMusicThread;

    private void createAudioTrack() {
        mIsPlaying = false;
        mIsStopped = true;
        mBufferSize = AudioTrack.getMinBufferSize(48000,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        Log.d(TAG, "createAudioTrack() mBufferSize " + mBufferSize);
        AudioAttributes attr = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build();
        AudioFormat format = new AudioFormat.Builder()
                .setSampleRate(48000)
                .setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build();
        mTrack = new AudioTrack.Builder()
                .setAudioAttributes(attr)
                .setAudioFormat(format)
                .setBufferSizeInBytes(mBufferSize)
                .build();
        mBuffer = new byte[mBufferSize];
        Arrays.fill(mBuffer, (byte) 0);
    }

    private void sleep(int mills) {
        try {
            Thread.sleep(mills);
        } catch (InterruptedException e) {
            // do nothing;
        }
    }

    private void playMutedMusic() {
        Log.d(TAG, "playMutedMusic()");
        if(mRoutingDevice != null) mTrack.setPreferredDevice(mRoutingDevice);
        if(mIsPlaying == false) {
            mTrack.play();
            if(mIsStopped) {
                mMutedMusicThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mIsPlaying = true;
                        mIsStopped = false;
                        while (mIsPlaying) {
                            Log.d(TAG, "write() offset 0 size " + mBufferSize);
                            mTrack.write(mBuffer, 0, mBufferSize);
                        }
                        stopMutedMusicThread();
                    }
                });
                mMutedMusicThread.start();
            }
        }
    }

    private void stopMutedMusicThread() {
        Log.d(TAG, "stopMutedMusicThread()");
        mIsPlaying = false;
        mTrack.setPreferredDevice(null);
        mTrack.stop();
        mIsStopped = true;
        mMutedMusicThread = null;
    }

    private void stopMutedMusic() {
        Log.d(TAG, "stopMutedMusic()");
        mIsPlaying = false;
    }

    /**
     * Handler related implements
     */
    // Command list of ServiceHandler
    private final int MSG_PLAY_MUSIC = 0;
    private final int MSG_STOP_MUSIC = 1;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.arg1) {
                case MSG_PLAY_MUSIC:
                    playMutedMusic();
                    break;
                case MSG_STOP_MUSIC:
                    stopMutedMusic();
                    break;
                default:
                    Log.e(TAG, "Wrong message!");
            }
        }
    }

    /**
     * Binder related implements
     */
    private final IBinder mBinder = new LocalBinder();
    public class LocalBinder extends Binder {
        MusicRouterService getService() {
            return MusicRouterService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind()");
        return mBinder;
    }

    public void registerMusicDeviceCallback(MusicRouterDeviceCallback callback) {
        mMusicDeviceCallback = callback;
        if(mMusicDeviceCallback == null) {
            Log.d(TAG, "registerMusicDeviceCallback() callback is null");
        } else {
            Log.d(TAG, "registerMusicDeviceCallback() ");
        }
    }

    public void setPreferredDevice(int type) {
        if(mOutputDevices.get(type) != null) {
            mTrack.setPreferredDevice(mOutputDevices.get(type));
            Toast.makeText(this, type + "으로 설정되었습니다.", Toast.LENGTH_SHORT).show();
            mRoutingDevice = mOutputDevices.get(type);
        } else {
            mTrack.setPreferredDevice(null);
            Toast.makeText(this, "라우팅을 초기화 합니다.", Toast.LENGTH_SHORT).show();
            mRoutingDevice = null;
        }
    }

    public AudioDeviceInfo getRoutedDevice() {
        return mTrack.getRoutedDevice();
    }

    public AudioManager getAudioManager() {
        if(mManager == null) {
            Log.d(TAG, "create AudioManager");
            mManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        }
        return mManager;
    }

    /**
     * Other override methods
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand()");
        Toast.makeText(this, "MusicRouterService starting", Toast.LENGTH_SHORT).show();

        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.w(TAG, "onCreate");
        // 1. register handler thread
        HandlerThread thread = new HandlerThread("MusicRouterSerivceHandler");
        thread.start();

        mLooper = thread.getLooper();
        mHandler = new ServiceHandler(mLooper);

        // 2. initialize variables
        mPlaybackState = false;
        mManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mPlaybackCallback = new AudioManager.AudioPlaybackCallback() {
            @Override
            public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                super.onPlaybackConfigChanged(configs);
                int configCount = 0;  // for filter myself
                for(AudioPlaybackConfiguration config : configs) {
                    AudioAttributes attr = config.getAudioAttributes();
                    if((attr.getContentType() == AudioAttributes.CONTENT_TYPE_MOVIE
                            || attr.getContentType() == AudioAttributes.CONTENT_TYPE_MUSIC)
                            && (attr.getUsage() == AudioAttributes.USAGE_MEDIA
                            || attr.getUsage() == AudioAttributes.USAGE_GAME)) {
                        Log.d(TAG, "Music is being played");
                        configCount++;
                    }
                }
                // should consider mutedMusic state
                if(mIsPlaying && configCount > 1) {
                    mPlaybackState = true;
                } else if (mIsPlaying == false && configCount > 0) {
                    mPlaybackState = true;
                } else {
                    mPlaybackState = false;
                }
                Log.d(TAG, "onPlaybackConfigChanged() mPlaybackState " + mPlaybackState);
                if(mMusicDeviceCallback != null) {
                    mMusicDeviceCallback.onMusicPlaybackStatusChanged(mPlaybackState == true
                                                            ? MusicRouterDeviceCallback.STATE_PLAY
                                                            : MusicRouterDeviceCallback.STATE_STOP);
                }

                Message msg = new Message();
                if(mPlaybackState) {
                    msg.arg1 = MSG_PLAY_MUSIC;
                    mHandler.sendMessage(msg);
                } else {
                    msg.arg1 = MSG_STOP_MUSIC;
                    mHandler.sendMessage(msg);
                }
            }
        };
        mDeviceCallback = new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                super.onAudioDevicesAdded(addedDevices);
                Log.d(TAG, "onAudioDevicesAdded()");
                for(AudioDeviceInfo device: addedDevices) {
                    if(device.isSink()) {
                        mOutputDevices.put(device.getType(), device);
                    }
                }
                // Notify to client(Activity)
                if(mMusicDeviceCallback != null) {
                    Log.d(TAG, "onAudioDevicesAdded() calls callback");
                    mMusicDeviceCallback.onDeviceAdded(addedDevices);
                }
            }

            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                super.onAudioDevicesRemoved(removedDevices);
                Log.d(TAG, "onAudioDevicesRemoved()");
                for(AudioDeviceInfo device : removedDevices) {
                    if(device.isSink()) {
                        mOutputDevices.delete(device.getType());
                    }
                }
                // Notify to client (Activity)
                if(mMusicDeviceCallback != null) {
                    Log.d(TAG, "onAudioDevicesRemoved() calls callback");
                    mMusicDeviceCallback.onDeviceDeleted(removedDevices);
                }
            }
        };

        // 3. register listeners
        mManager.registerAudioPlaybackCallback(mPlaybackCallback, mHandler);
        mManager.registerAudioDeviceCallback(mDeviceCallback, mHandler);
        mOutputDevices = null;
        updateOutputDevices();

        // 4. create AudioTrack
        createAudioTrack();

        // 5. process when music is on playing
        processMutedMusicFirstTime();

        // 6. register routingChangedListener
        mRoutingDevice = null;
        mRoutingChangedListener = new AudioRouting.OnRoutingChangedListener() {
            @Override
            public void onRoutingChanged(AudioRouting router) {
                Log.d(TAG, "onRoutingChanged() ");
                AudioDeviceInfo device = router.getRoutedDevice();
                if(mRoutingDevice == null) {
                    mTrack.setPreferredDevice(null);
                } else if(mTrack.getRoutedDevice() == null || mTrack.getRoutedDevice().getType() != mRoutingDevice.getType()) {
                    mTrack.setPreferredDevice(mRoutingDevice);
                }
            }
        };
        mTrack.addOnRoutingChangedListener(mRoutingChangedListener, mHandler);
    }

    private void processMutedMusicFirstTime() {
        if(mManager.isMusicActive()) {
            Log.d(TAG, "processMutedMusicFirstTime() music is on playing, track start");
            mPlaybackState = true;
            Message msg = new Message();
            msg.arg1 = MSG_PLAY_MUSIC;
            mHandler.sendMessage(msg);
        }
    }

    private void updateOutputDevices() {
        if(mOutputDevices != null) mOutputDevices.clear();
        else mOutputDevices = new SparseArray<>();
        AudioDeviceInfo[] devices = mManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
        for(AudioDeviceInfo device : devices) {
            mOutputDevices.put(device.getType(), device);
        }
    }

    @Override
    public void onDestroy() {
        Log.w(TAG, "OnDestroy");
        /*mTrack.setPreferredDevice(null);
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // do nothing;
        }*/
        mManager.unregisterAudioPlaybackCallback(mPlaybackCallback);
        mManager.unregisterAudioDeviceCallback(mDeviceCallback);
        stopMutedMusic();
        mTrack.release();
        mTrack = null;
    }
}
