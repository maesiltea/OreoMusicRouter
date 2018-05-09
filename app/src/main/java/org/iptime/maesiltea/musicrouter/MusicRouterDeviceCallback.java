package org.iptime.maesiltea.musicrouter;

import android.media.AudioDeviceInfo;

public interface MusicRouterDeviceCallback {
    void onDeviceAdded(AudioDeviceInfo[] deviceInfos);
    void onDeviceDeleted(AudioDeviceInfo[] deviceInfos);
    void onMusicPlaybackStatusChanged(int status);  // 0: stop, 1: play
}
