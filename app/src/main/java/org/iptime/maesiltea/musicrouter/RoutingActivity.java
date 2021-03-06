package org.iptime.maesiltea.musicrouter;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import java.util.HashMap;
import java.util.Objects;

import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdListener;

public class RoutingActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private final String TAG = "RoutingActivity";
    private final boolean DEBUG = false;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private PlaceholderFragment mRoutingPage;
    private Context mContext;
    private final int PAGE_ROUTING = 0;
    private final int PAGE_SETTINGS = 1;

    /**
     *  Audio related variables
     */
    protected MusicRouterDeviceCallback mMusicRouterDeviceCallback;
    private int mPlaybackState;

    /**
     *  Service related variables
     */
    private MusicRouterService mService;
    private MusicRouterService getService() { return mService; }
    private Context getContext() { return mContext; }
    private boolean mBound;
    private boolean getBound() { return mBound; }
    private void setBound(boolean bound) { mBound = bound; }
    private ServiceConnection getConnection() { return mConnection; }
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            if(DEBUG) Log.d(TAG, "onServiceConnected()");
            MusicRouterService.LocalBinder binder = (MusicRouterService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            // set last device
            mService.setPreferredDevice(getPreferencesInt("routing_device_type", -1));

            // Implements callback
            mMusicRouterDeviceCallback = new MusicRouterDeviceCallback() {
                @Override
                public void onDeviceAdded(AudioDeviceInfo[] addedDevices) {
                    if(DEBUG) Log.v(TAG, "onDeviceAdded()");
                    if(mRoutingPage == null) {
                        Log.w(TAG, "mRouingPage is null");
                        return;
                    }
                    for(AudioDeviceInfo info : addedDevices) {
                        if(info.isSink()) {
                            mRoutingPage.setSwitchEnabled(info.getType(), true);
                        }
                    }
                    AudioDeviceInfo device = mService.getRoutedDevice();
                    if(device != null && mPlaybackState == MusicRouterDevice.STATE_PLAY) {
                        mRoutingPage.setSwitchChecked(device.getType(), true, false);
                    }
                    mRoutingPage.updateSwitchEnabledState();
                }

                @Override
                public void onDeviceDeleted(AudioDeviceInfo[] removedDevices) {
                    if(DEBUG) Log.d(TAG, "onDeviceDeleted()");
                    if(mRoutingPage == null) {
                        Log.w(TAG, "mRouingPage is null");
                        return;
                    }
                    for(AudioDeviceInfo info : removedDevices) {
                        if(info.isSink()) {
                            mRoutingPage.setSwitchEnabled(info.getType(), false);
                        }
                    }
                    AudioDeviceInfo device = mService.getRoutedDevice();
                    if(device != null && mPlaybackState == MusicRouterDevice.STATE_PLAY) {
                        mRoutingPage.setSwitchChecked(device.getType(), true, false);
                    }
                    mRoutingPage.updateSwitchEnabledState();
                }

                @Override
                public void onMusicPlaybackStatusChanged(int status) {
                    // TODO: update playback state to Activity
                    switch(status) {
                        case MusicRouterDevice.STATE_PLAY:
                            mPlaybackState = MusicRouterDevice.STATE_PLAY;
                            break;
                        case MusicRouterDevice.STATE_STOP:
                            mPlaybackState = MusicRouterDevice.STATE_STOP;
                            break;
                    }
                }
            };
            mService.registerMusicDeviceCallback(mMusicRouterDeviceCallback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "onServiceDisconnected");
            setBound(false);
            mService = null;
        }
    };

    @Override
    protected  void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    public void setRoutingPage(PlaceholderFragment pf) {
        mRoutingPage = pf;
    }

    public String getPreferences(String name, String defVal) {
        SharedPreferences pf = mContext.getSharedPreferences("routing_activity", MODE_PRIVATE);
        return pf.getString(name, defVal);
    }

    public int getPreferencesInt(String name, int defVal) {
        SharedPreferences pf = mContext.getSharedPreferences("routing_activity", MODE_PRIVATE);
        return pf.getInt(name, defVal);
    }

    private void refreshService() {
        if (!getBound()) {
            if (DEBUG) Log.d(TAG, "refreshService() start Service...");
            Intent intent = new Intent(this, MusicRouterService.class);
            startService(intent);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onStart() {
        if(DEBUG) Log.v(TAG, "onStart()");
        super.onStart();
        refreshService();
    }

    @Override
    protected void onResume() {
        if(DEBUG) Log.v(TAG, "onResume()");
        super.onStart();
        refreshService();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.v(TAG, "onCreate() mBound " + mBound);
        setContentView(R.layout.activity_routing);
        mContext = this;

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        /*FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        // initialize default variables
        mRoutingPage = null;
        mPlaybackState = MusicRouterDevice.STATE_STOP;
        mMusicRouterDeviceCallback = null;
        refreshService();
    }

    /*
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_routing, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    */

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static final String TAG = "PlaceholderFragment";
        private final boolean DEBUG = false;
        private Switch mServiceSwitch;
        private Context mContext;

        private AdView mAdView;  // AdMob

        /**
         *  Audio related implementations
         */
        private AudioManager mManager;
        private HashMap<Integer, Switch> mSwitches;
        private boolean mDisableState;
        private boolean mWithoutRoutingChangeFlag;

        public PlaceholderFragment() {
        }

        public String getPreferences(String name, String defVal) {
            SharedPreferences pf = mContext.getSharedPreferences("routing_activity", MODE_PRIVATE);
            return pf.getString(name, defVal);
        }

        public int getPreferencesInt(String name, int defVal) {
            SharedPreferences pf = mContext.getSharedPreferences("routing_activity", MODE_PRIVATE);
            return pf.getInt(name, defVal);
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            if(DEBUG) Log.v(TAG, "onCreateView Section " + getArguments().getInt(ARG_SECTION_NUMBER));
            View rootView = null;

            assert getArguments() != null;
            int sectionNumber;
            sectionNumber = getArguments().getInt(ARG_SECTION_NUMBER);
            switch(sectionNumber) {
                case 1:
                    rootView = inflater.inflate(R.layout.fragment_routing, container, false);
                    if(getActivity() != null) ((RoutingActivity) getActivity()).setRoutingPage(this);
                    mManager = (AudioManager) Objects.requireNonNull(getContext()).getSystemService(Context.AUDIO_SERVICE);
                    mSwitches = null;
                    mDisableState = false;
                    mWithoutRoutingChangeFlag = false;
                    mServiceSwitch = null;
                    mContext = rootView.getContext();
                    initializeDeviceList(rootView);
                    initializeServiceSwitch(rootView);
                    initializeRoutingDevice(rootView);
                    initializeAdMob(rootView);
                    setSwitchChecked(getPreferencesInt("routing_device_type", -1)
                            , true
                            , true);
                    break;
                case 2:
                    rootView = inflater.inflate(R.layout.music_router_settings, container, false);
                    break;
            }

            return rootView;
        }

        private void initializeAdMob(View v) {
            // AdMob
            MobileAds.initialize(mContext, "");
            mAdView = v.findViewById(R.id.adView);
            AdRequest adRequest = new AdRequest.Builder()
                    .build();
            mAdView.loadAd(adRequest);
            mAdView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    // Code to be executed when an ad finishes loading.
                    Log.d(TAG, "onAdLoaded()");
                }

                @Override
                public void onAdFailedToLoad(int errorCode) {
                    // Code to be executed when an ad request fails.
                    Log.w(TAG, "onAdFailedToLoad() errorCode " + errorCode);
                }

                @Override
                public void onAdOpened() {
                    // Code to be executed when an ad opens an overlay that
                    // covers the screen.
                    Log.d(TAG, "onAdOpened");
                }

                @Override
                public void onAdLeftApplication() {
                    // Code to be executed when the user has left the app.
                    Log.d(TAG, "onAdLeftApplication()");
                }

                @Override
                public void onAdClosed() {
                    // Code to be executed when when the user is about to return
                    // to the app after tapping on an ad.
                    Log.d(TAG, "onAdClosed()");
                }
            });
        }

        private void initializeRoutingDevice(View v) {
            RoutingActivity ra = (RoutingActivity) getActivity();
            assert ra != null;
            MusicRouterService service = ra.getService();
            if(service != null) {
                AudioDeviceInfo info = service.getPreferredDevice();
                if(info != null) {
                    Switch sw = mSwitches.get(info.getType());
                    if(sw != null) sw.setChecked(true);
                }
            } else {
                int type = getPreferencesInt("routing_device_type", MusicRouterDevice.TYPE_NULL);
                if (type > AudioDeviceInfo.TYPE_UNKNOWN) {
                    Switch sw = mSwitches.get(type);
                    if (sw != null) sw.setChecked(true);
                }
            }
        }

        private void initializeServiceSwitch(View v) {
            if(DEBUG) Log.v(TAG, "initializeServiceSwitch()");
            mServiceSwitch = v.findViewById(R.id.switch_service);

            // set default switch check state before register listener
            RoutingActivity ra = (RoutingActivity) getActivity();
            assert ra != null;
            MusicRouterService service = ra.getService();
            if(service != null) {
                mServiceSwitch.setChecked(service.getBackgroundPlayback());
            } else {
                boolean backgroundPlayback = "true".equals(getPreferences("background_playback", "false"));
                mServiceSwitch.setChecked(backgroundPlayback);
            }

            // register listener
            mServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(DEBUG) Log.d(TAG, "onCheckedChanged() service switch");
                    RoutingActivity ra = (RoutingActivity) getActivity();
                    MusicRouterService service;
                    service = ra.getService();
                    if(isChecked) {
                        service.setBackgroundPlayback(true);
                    } else {
                        service.setBackgroundPlayback(false);
                    }
                }
            });
        }

        private void uncheckOtherSwitches(Switch s) {
            mDisableState = true;
            for(int key : mSwitches.keySet()) {
                if(mSwitches.get(key) != s) {
                    mSwitches.get(key).setChecked(false);
                }
            }
            mDisableState = false;
        }

        private void initializeDeviceList(View v) {
            LinearLayout layout = v.findViewById(R.id.linear_layout);

            mSwitches = new HashMap<Integer, Switch>();
            Switch s = v.findViewById(R.id.switch_aux_line);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // AUX_LINE
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(!mWithoutRoutingChangeFlag) {
                        if (isChecked && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_AUX_LINE);
                            uncheckOtherSwitches((Switch) buttonView);
                        } else if (!mDisableState && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(-1);
                        }
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_AUX_LINE, s);
            s = v.findViewById(R.id.switch_bluetooth_a2dp);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // Bluetooth A2DP
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(!mWithoutRoutingChangeFlag) {
                        if (isChecked && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);
                            uncheckOtherSwitches((Switch) buttonView);
                        } else if (!mDisableState && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(-1);
                        }
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, s);
            s = v.findViewById(R.id.switch_earpiece);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // Builtin earpiece
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(!mWithoutRoutingChangeFlag) {
                        if (isChecked && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE);
                            uncheckOtherSwitches((Switch) buttonView);
                        } else if (!mDisableState && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(-1);
                        }
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE, s);
            s = v.findViewById(R.id.switch_speaker);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // Builtin earpiece
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(!mWithoutRoutingChangeFlag) {
                        if (isChecked && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
                            uncheckOtherSwitches((Switch) buttonView);
                        } else if (!mDisableState && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(-1);
                        }
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, s);
            s = v.findViewById(R.id.switch_hdmi);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // HDMI
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(!mWithoutRoutingChangeFlag) {
                        if (isChecked && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_HDMI);
                            uncheckOtherSwitches((Switch) buttonView);
                        } else if (!mDisableState && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(-1);
                        }
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_HDMI, s);
            s = v.findViewById(R.id.switch_usb_accessory);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // USB Accessory
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(!mWithoutRoutingChangeFlag) {
                        if (isChecked && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_USB_ACCESSORY);
                            uncheckOtherSwitches((Switch) buttonView);
                        } else if (!mDisableState && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(-1);
                        }
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_USB_ACCESSORY, s);
            s = v.findViewById(R.id.switch_usb_device);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // USB Device
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(!mWithoutRoutingChangeFlag) {
                        if (isChecked && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_USB_DEVICE);
                            uncheckOtherSwitches((Switch) buttonView);
                        } else if (!mDisableState && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(-1);
                        }
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_USB_DEVICE, s);
            // USB headset visibility
            s = v.findViewById(R.id.switch_usb_headset);
            if(Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) {
                Log.i(TAG, "USB Headset type is not supported under 8.1");
                layout.removeView(s);
            } else {
                s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        // USB Headset
                        RoutingActivity a = (RoutingActivity) getActivity();
                        if(!mWithoutRoutingChangeFlag) {
                            if (isChecked && a != null && a.getService() != null) {
                                a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_USB_HEADSET);
                                uncheckOtherSwitches((Switch) buttonView);
                            } else if (!mDisableState && a != null && a.getService() != null) {
                                a.getService().setPreferredDevice(-1);
                            }
                        }
                    }
                });
                mSwitches.put(AudioDeviceInfo.TYPE_USB_HEADSET, s);
            }
            s = v.findViewById(R.id.switch_wired_headphones);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // Wired Headphones
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(!mWithoutRoutingChangeFlag) {
                        if (isChecked && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_WIRED_HEADPHONES);
                            uncheckOtherSwitches((Switch) buttonView);
                        } else if (!mDisableState && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(-1);
                        }
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, s);
            s = v.findViewById(R.id.switch_wired_headset);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // Wired Headset
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(!mWithoutRoutingChangeFlag) {
                        if (isChecked && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_WIRED_HEADSET);
                            uncheckOtherSwitches((Switch) buttonView);
                        } else if (!mDisableState && a != null && a.getService() != null) {
                            a.getService().setPreferredDevice(-1);
                        }
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_WIRED_HEADSET, s);

            updateSwitchEnabledState();
        }

        public void updateSwitchEnabledState() {
            AudioDeviceInfo[] devices = mManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for(AudioDeviceInfo device : devices) {
                if(mSwitches.containsKey(device.getType())) {
                    mSwitches.get(device.getType()).setEnabled(true);
                }
            }
        }

        public void setSwitchEnabled(int key, boolean enabled) {
            Switch sw = mSwitches.get(key);
            if(null != mSwitches && sw != null) {
                sw.setEnabled(enabled);
            } else {
                Log.w(TAG, "setSwitchEnabled() mSwitches is null");
            }
        }

        public void setSwitchChecked(int key, boolean checked, boolean withoutRoutingChange) {
            if(null != mSwitches) {
                Switch sw;
                sw = mSwitches.get(key);
                if(sw != null) {
                    if(withoutRoutingChange) mWithoutRoutingChangeFlag = true;
                    sw.setChecked(checked);
                    if(withoutRoutingChange) mWithoutRoutingChangeFlag = false;
                }
            } else {
                Log.w(TAG, "setSwitchChecked() mSwitches is null");
            }
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }
    }
}
