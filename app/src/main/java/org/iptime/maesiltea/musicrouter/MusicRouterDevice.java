package org.iptime.maesiltea.musicrouter;

import android.content.Context;
import android.media.AudioDeviceInfo;

public class MusicRouterDevice {
    public static final int TYPE_NULL = -1;
    public static final int STATE_STOP = 0;
    public static final int STATE_PLAY = 1;

    public static String getDeviceNameByType(Context ctx, int type) {
        switch(type) {
            case AudioDeviceInfo.TYPE_UNKNOWN:
                return ctx.getString(R.string.type_unknown);
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                return ctx.getString(R.string.builtin_earpiece);
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                return ctx.getString(R.string.builtin_speaker);
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                return ctx.getString(R.string.wired_headset);
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                return ctx.getString(R.string.wired_headphones);
            case AudioDeviceInfo.TYPE_LINE_ANALOG:
                return ctx.getString(R.string.line_analog);
            case AudioDeviceInfo.TYPE_LINE_DIGITAL:
                return ctx.getString(R.string.line_digital);
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
                return ctx.getString(R.string.bluetooth_sco);
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
                return ctx.getString(R.string.bluetooth_a2dp);
            case AudioDeviceInfo.TYPE_HDMI:
                return ctx.getString(R.string.hdmi);
            case AudioDeviceInfo.TYPE_HDMI_ARC:
                return ctx.getString(R.string.hdmi_arc);
            case AudioDeviceInfo.TYPE_USB_DEVICE:
                return ctx.getString(R.string.usb_device);
            case AudioDeviceInfo.TYPE_USB_ACCESSORY:
                return ctx.getString(R.string.usb_accessory);
            case AudioDeviceInfo.TYPE_USB_HEADSET:
                return ctx.getString(R.string.usb_headset);
            case AudioDeviceInfo.TYPE_DOCK:
                return ctx.getString(R.string.dock);
            case AudioDeviceInfo.TYPE_AUX_LINE:
                return ctx.getString(R.string.aux_line);
            case TYPE_NULL:
                return "null";
            default:
                return "Not Defined";
        }
    }
}
