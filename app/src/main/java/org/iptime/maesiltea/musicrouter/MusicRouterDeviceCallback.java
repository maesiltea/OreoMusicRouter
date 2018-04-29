package org.iptime.maesiltea.musicrouter;

import android.media.AudioDeviceInfo;

public interface MusicRouterDeviceCallback {
    public final int STATE_STOP = 0;
    public final int STATE_PLAY = 1;
    void onDeviceAdded(AudioDeviceInfo[] deviceInfos);
    void onDeviceDeleted(AudioDeviceInfo[] deviceInfos);
    void onMusicPlaybackStatusChanged(int status);  // 0: stop, 1: play
}
