package org.iptime.maesiltea.musicrouter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioTrack;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
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
    private final boolean DEBUG = false;
    private Looper mLooper;
    private ServiceHandler mHandler;
    private AudioManager mManager;
    private AudioManager.AudioPlaybackCallback mPlaybackCallback;
    private AudioDeviceCallback mDeviceCallback;
    private SparseArray<AudioDeviceInfo> mOutputDevices;
    private MusicRouterDeviceCallback mMusicDeviceCallback;
    private AudioDeviceInfo mRoutingDevice;

    /**
     *  Foreground related implementations
     */
    private final int SERVICE_NOTIFICATION_ID = 1;
    private final String SERVICE_CHANNEL_NAME = "MusicRouterService";
    private final String SERVICE_CHANNEL_ID = "MusicRouter";
    private NotificationManager mNotificationManager;

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
    private boolean mBackgroundPlayback;

    private void createAudioTrack() {
        if(DEBUG) Log.v(TAG, "createAudioTrack() mBufferSize " + mBufferSize);
        mIsPlaying = false;
        mIsStopped = true;
        mBufferSize = AudioTrack.getMinBufferSize(48000,
                AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
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
        if(DEBUG) Log.v(TAG, "playMutedMusic()");
        if(!mIsPlaying) {
            mTrack.play();
            if(mIsStopped && mMutedMusicThread == null) {
                mMutedMusicThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mIsPlaying = true;
                        mIsStopped = false;
                        while (mIsPlaying) {
                            //Log.d(TAG, "write() offset 0 size " + mBufferSize);
                            mTrack.write(mBuffer, 0, mBufferSize);
                        }
                        stopMutedMusicThread();
                    }
                });
                mMutedMusicThread.start();
            }
        } else {
            Log.i(TAG, "playMutedMusic() muted music still playing...");
        }
        // check whether the routing is initialized.
        if(mRoutingDevice != null) {
            if(DEBUG) Log.v(TAG, "playMutedMusic() change routing to " + mRoutingDevice.getType());
            Message msg = new Message();
            msg.arg1 = MSG_SET_PREFERRED_DEVICE;
            msg.arg2 = mRoutingDevice.getType();
            mHandler.sendMessage(msg);
        }
    }

    private void stopMutedMusicThread() {
        if(DEBUG) Log.d(TAG, "stopMutedMusicThread()");
        mIsPlaying = false;
        mTrack.setPreferredDevice(null);
        mTrack.stop();
        mIsStopped = true;
        mMutedMusicThread = null;
    }

    private void stopMutedMusic() {
        if(DEBUG) Log.d(TAG, "stopMutedMusic()");
        mIsPlaying = false;
    }

    /**
     * Handler related implements
     */
    // Command list of ServiceHandler
    private final int MSG_PLAY_MUSIC = 0;
    private final int MSG_STOP_MUSIC = 1;
    private final int MSG_SET_PREFERRED_DEVICE = 2;

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        private ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.arg1) {
                case MSG_PLAY_MUSIC:
                    if(mBackgroundPlayback) playMutedMusic();
                    break;
                case MSG_STOP_MUSIC:
                    stopMutedMusic();
                    break;
                case MSG_SET_PREFERRED_DEVICE:
                    setPreferredDevice(msg.arg2);
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
        if(DEBUG) Log.d(TAG, "onBind()");
        return mBinder;
    }

    public void registerMusicDeviceCallback(MusicRouterDeviceCallback callback) {
        if(DEBUG) Log.d(TAG, "registerMusicDeviceCallback()");
        if(mMusicDeviceCallback == null) {
            Log.w(TAG, "registerMusicDeviceCallback() callback is null");
        }
        mMusicDeviceCallback = callback;
        if(mRoutingDevice != null && mPlaybackState && mIsPlaying) {
            mMusicDeviceCallback.onFirstRoutingDevice(mRoutingDevice);
        }
    }

    public void setPreferredDevice(int type) {
        if(DEBUG) Log.v(TAG, "setPreferredDevice() " + MusicRouterDevice.getDeviceNameByType(this, type));
        if(mOutputDevices.get(type) != null) {
            mTrack.setPreferredDevice(mOutputDevices.get(type));
            AudioDeviceInfo info = mTrack.getRoutedDevice();
            if((info == null || info.getType() != type) && mBackgroundPlayback) {
                Toast.makeText(this, this.getString(R.string.msg_selected_device)
                        + MusicRouterDevice.getDeviceNameByType(this, type), Toast.LENGTH_SHORT)
                        .show();
            }
            mRoutingDevice = mOutputDevices.get(type);
            setPreferencesInt("routing_device_type", type);
        } else {
            AudioDeviceInfo info = mTrack.getPreferredDevice();
            if(info != null && mBackgroundPlayback) {
                Toast.makeText(this, this.getString(R.string.msg_initialize_routing), Toast.LENGTH_SHORT)
                        .show();
            }
            mTrack.setPreferredDevice(null);
            mRoutingDevice = null;
            setPreferencesInt("routing_device_type", MusicRouterDevice.TYPE_NULL);
        }
        if(!mBackgroundPlayback) {
            Toast.makeText(this, this.getString(R.string.msg_plz_service_enable), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    public AudioDeviceInfo getRoutedDevice() {
        return mTrack.getRoutedDevice();
    }

    public AudioDeviceInfo getPreferredDevice() {
        return mTrack.getPreferredDevice();
    }

    public void setBackgroundPlayback(boolean enable) {
        if(DEBUG) Log.d(TAG, "setBackgroundPlayback() enable " + enable + " mPlaybackState " + mPlaybackState);
        Message msg = new Message();
        if(enable && mPlaybackState) {
            msg.arg1 = MSG_PLAY_MUSIC;
            mHandler.sendMessage(msg);
        }

        mBackgroundPlayback = enable;
        setPreferences("background_playback", enable ? "true" : "false");
        if(enable) {
            initializeNotification();
            Notification notification = makeNotification(this
                    , getString(R.string.notification_title)
                    , getString(R.string.notification_content));
            mNotificationManager.notify(SERVICE_NOTIFICATION_ID, notification);
        } else {
            mNotificationManager.deleteNotificationChannel(SERVICE_CHANNEL_ID);
        }

        if (!enable && mPlaybackState) {
            msg.arg1 = MSG_STOP_MUSIC;
            mHandler.sendMessage(msg);
        }
    }

    public boolean getBackgroundPlayback() { return mBackgroundPlayback; }

    public String getPreferences(String name, String defVal) {
        SharedPreferences pf = getSharedPreferences("routing_activity", MODE_PRIVATE);
        return pf.getString(name, defVal);
    }

    public void setPreferences(String name, String value) {
        SharedPreferences pf = getSharedPreferences("routing_activity", MODE_PRIVATE);
        SharedPreferences.Editor ed = pf.edit();
        ed.putString(name, value);
        ed.apply();
    }

    public int getPreferencesInt(String name, int defVal) {
        SharedPreferences pf = getSharedPreferences("routing_activity", MODE_PRIVATE);
        return pf.getInt(name, defVal);
    }

    public void setPreferencesInt(String name, int value) {
        SharedPreferences pf = getSharedPreferences("routing_activity", MODE_PRIVATE);
        SharedPreferences.Editor ed = pf.edit();
        ed.putInt(name, value);
        ed.apply();
    }

    /**
     * Other override methods
     * It doesn't work.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(DEBUG) Log.v(TAG, "onStartCommand()");

        super.onStartCommand(intent, flags, startId);
        Notification notification = makeNotification(this
                , getString(R.string.notification_title)
                , getString(R.string.notification_content));
        startForeground(SERVICE_NOTIFICATION_ID, notification);
        if(!mBackgroundPlayback) mNotificationManager.deleteNotificationChannel(SERVICE_CHANNEL_ID);
        return START_STICKY;
    }

    private void initializeNotification() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(SERVICE_CHANNEL_ID
                , SERVICE_CHANNEL_NAME
                , NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(getString(R.string.notification_channel_description));
        channel.setName(getString(R.string.notification_channel_name));
        channel.enableVibration(false);
        mNotificationManager.createNotificationChannel(channel);
    }

    private Notification makeNotification(Context ctx, String title, String text) {
        return new NotificationCompat.Builder(ctx, SERVICE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_headset)
                .setContentTitle(title)
                .setContentText(text)
                .build();
    }

    @Override
    public void onCreate() {
        if(DEBUG) Log.w(TAG, "onCreate");

        // 0. set this service to foreground. (this service should not be killed.)
        initializeNotification();
        Notification notification = makeNotification(this
                , getString(R.string.notification_title)
                , getString(R.string.notification_content));
        startForeground(SERVICE_NOTIFICATION_ID, notification);
        if(!mBackgroundPlayback) mNotificationManager.deleteNotificationChannel(SERVICE_CHANNEL_ID);

        // 1. register handler thread
        HandlerThread thread = new HandlerThread("MusicRouterSerivceHandler");
        thread.start();

        mLooper = thread.getLooper();
        mHandler = new ServiceHandler(mLooper);

        // 2. initialize variables
        mBackgroundPlayback = "true".equals(getPreferences("background_playback", "false"));
        mPlaybackState = false;
        mManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mPlaybackCallback = new AudioManager.AudioPlaybackCallback() {
            @Override
            public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
                super.onPlaybackConfigChanged(configs);
                int configCount = 0;  // for filter myself
                for (AudioPlaybackConfiguration config : configs) {
                    AudioAttributes attr = config.getAudioAttributes();
                    if (attr.getUsage() == AudioAttributes.USAGE_MEDIA
                            || attr.getUsage() == AudioAttributes.USAGE_GAME
                            || attr.getUsage() == AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
                            || attr.getUsage() == AudioAttributes.USAGE_ASSISTANT
                            || attr.getUsage() == AudioAttributes.USAGE_UNKNOWN) {
                        if (DEBUG) Log.d(TAG, "Music is being played");
                        configCount++;
                    }
                }

                // should consider mutedMusic state
                mPlaybackState = mIsPlaying && configCount > 1 || !mIsPlaying && configCount > 0;

                Message msg = new Message();
                if (mPlaybackState) {
                    msg.arg1 = MSG_PLAY_MUSIC;
                    mHandler.sendMessage(msg);
                } else {
                    msg.arg1 = MSG_STOP_MUSIC;
                    mHandler.sendMessage(msg);
                }

                if (DEBUG) Log.d(TAG, "onPlaybackConfigChanged() mPlaybackState " + mPlaybackState);
                if (mMusicDeviceCallback != null) {
                    mMusicDeviceCallback.onMusicPlaybackStatusChanged(mPlaybackState
                            ? MusicRouterDevice.STATE_PLAY
                            : MusicRouterDevice.STATE_STOP);
                }
            }
        };
        mDeviceCallback = new AudioDeviceCallback() {
            @Override
            public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
                super.onAudioDevicesAdded(addedDevices);
                if(DEBUG) Log.d(TAG, "onAudioDevicesAdded()");
                for(AudioDeviceInfo device: addedDevices) {
                    if(device.isSink()) {
                        mOutputDevices.put(device.getType(), device);
                    }
                }
                // Notify to client(Activity)
                if(mMusicDeviceCallback != null) {
                    mMusicDeviceCallback.onDeviceAdded(addedDevices);
                }
            }

            @Override
            public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
                super.onAudioDevicesRemoved(removedDevices);
                if(DEBUG) Log.d(TAG, "onAudioDevicesRemoved()");
                for(AudioDeviceInfo device : removedDevices) {
                    if(device.isSink()) {
                        mOutputDevices.delete(device.getType());
                    }
                }
                // Notify to client (Activity)
                if(mMusicDeviceCallback != null) {
                    mMusicDeviceCallback.onDeviceDeleted(removedDevices);
                }
            }
        };

        // 3. register listeners and list up output devices.
        mManager.registerAudioPlaybackCallback(mPlaybackCallback, mHandler);
        mManager.registerAudioDeviceCallback(mDeviceCallback, mHandler);
        mOutputDevices = null;
        updateOutputDevices();

        // 4. create AudioTrack
        createAudioTrack();

        // 5. process when music is on playing
        processMutedMusicFirstTime();
        mRoutingDevice = null;
    }

    private void processMutedMusicFirstTime() {
        if(mManager.isMusicActive()) {
            if(DEBUG) Log.v(TAG, "processMutedMusicFirstTime() music is on playing, starts track!");
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
        if(DEBUG) Log.v(TAG, "OnDestroy");
        mManager.unregisterAudioPlaybackCallback(mPlaybackCallback);
        mManager.unregisterAudioDeviceCallback(mDeviceCallback);
        stopMutedMusic();
        mTrack.release();
        mTrack = null;
    }
}
