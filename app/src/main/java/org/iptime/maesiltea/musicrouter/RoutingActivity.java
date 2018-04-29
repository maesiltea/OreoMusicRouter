package org.iptime.maesiltea.musicrouter;

import android.app.ActionBar;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
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

import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    private PlaceholderFragment mRoutingPage;
    private final int PAGE_ROUTING = 0;
    private final int PAGE_SETTINGS = 1;

    /**
     *  Audio related variables
     */
    private MusicRouterDeviceCallback mMusicRouterDeviceCallback;
    private int mPlaybackState;

    /**
     *  Service related variables
     */
    private MusicRouterService mService;
    private MusicRouterService getService() { return mService; }
    private boolean mBound;
    private boolean getBound() { return mBound; }
    private ServiceConnection getConnection() { return mConnection; }
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected()");
            MusicRouterService.LocalBinder binder = (MusicRouterService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            /**
             *  Callback implementation
             */
            mMusicRouterDeviceCallback = new MusicRouterDeviceCallback() {
                @Override
                public void onDeviceAdded(AudioDeviceInfo[] addedDevices) {
                    Log.v(TAG, "onDeviceAdded()");
                    for(AudioDeviceInfo device : addedDevices) {
                        if(device.isSink()) {
                            mRoutingPage.setSwitchEnabled(device.getType(),true);
                        }
                    }
                }

                @Override
                public void onDeviceDeleted(AudioDeviceInfo[] removedDevices) {
                    for(AudioDeviceInfo device : removedDevices) {
                        if(device.isSink()) {
                            mRoutingPage.setSwitchEnabled(device.getType(),false);
                        }
                    }
                }

                @Override
                public void onMusicPlaybackStatusChanged(int status) {
                    // TODO: update playback state to Activity
                    switch(status) {
                        case MusicRouterDeviceCallback.STATE_PLAY:
                            mPlaybackState = MusicRouterDeviceCallback.STATE_PLAY;
                            break;
                        case MusicRouterDeviceCallback.STATE_STOP:
                            mPlaybackState = MusicRouterDeviceCallback.STATE_STOP;
                            break;
                    }
                }
            };
            mService.registerMusicDeviceCallback(mMusicRouterDeviceCallback);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.w(TAG, "onServiceDisconnected");
            mBound = false;
        }
    };

    /*
    @Override
    protected void onStart() {
        Log.d(TAG, "onStart()");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }*/

    @Override
    protected  void onDestroy() {
        Log.d(TAG, "onDestroy()");
        super.onDestroy();
        // Unbind from MusicRouterService (local service)
        /*if(mBound) {
            unbindService(mConnection);
            mBound = false;
        }*/
    }

    public void setRoutingPage(PlaceholderFragment pf) {
        mRoutingPage = pf;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.w(TAG, "onCreate() mBound " + mBound);
        setContentView(R.layout.activity_routing);

        // bind to MusicRouterService (local service)
        if(mBound == false) {
            Log.d(TAG, "onCreate() start Service...");
            Intent intent = new Intent(this, MusicRouterService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
            startService(intent);
            Log.d(TAG, "onCreate() start Service is done");
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        mRoutingPage = null;
        mPlaybackState = MusicRouterDeviceCallback.STATE_STOP;
    }

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

        /**
         *  Audio related implementations
         */
        private AudioManager mManager;
        private HashMap<Integer, Switch> mSwitches;
        private boolean mDisableState;

        public PlaceholderFragment() {
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
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Log.w(TAG, "onCreateView");
            View rootView = null;

            switch(getArguments().getInt(ARG_SECTION_NUMBER)) {
                case 1:
                    rootView = inflater.inflate(R.layout.fragment_routing, container, false);
                    if(getActivity() != null) ((RoutingActivity) getActivity()).setRoutingPage(this);
                    break;
                case 2:
                    rootView = inflater.inflate(R.layout.music_router_settings, container, false);
                    break;
            }

            mManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
            mSwitches = null;
            mDisableState = false;
            Log.d(TAG, "onCreateView() arg " + getArguments().getInt(ARG_SECTION_NUMBER));

            // update device list
            if(getArguments().getInt(ARG_SECTION_NUMBER) == 1) {
                Log.d(TAG, "First Page");
                initializeDeviceList(rootView);
            }

            return rootView;
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
            mSwitches = new HashMap<Integer, Switch>();
            Switch s = (Switch) v.findViewById(R.id.switch_aux_line);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // AUX_LINE
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(isChecked && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_AUX_LINE);
                        uncheckOtherSwitches((Switch) buttonView);
                    } else if(mDisableState == false && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(-1);
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_AUX_LINE, s);
            s = (Switch) v.findViewById(R.id.switch_bluetooth_a2dp);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // Bluetooth A2DP
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(isChecked && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP);
                        uncheckOtherSwitches((Switch) buttonView);
                    } else if(mDisableState == false && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(-1);
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
                    if(isChecked && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE);
                        uncheckOtherSwitches((Switch) buttonView);
                    } else if(mDisableState == false && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(-1);
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_BUILTIN_EARPIECE, s);
            s = (Switch) v.findViewById(R.id.switch_speaker);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // Builtin earpiece
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(isChecked && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER);
                        uncheckOtherSwitches((Switch) buttonView);
                    } else if(mDisableState == false && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(-1);
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER, s);
            s = (Switch) v.findViewById(R.id.switch_hdmi);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // HDMI
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(isChecked && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_HDMI);
                        uncheckOtherSwitches((Switch) buttonView);
                    } else if(mDisableState == false && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(-1);
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_HDMI, s);
            s = (Switch) v.findViewById(R.id.switch_usb_accessory);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // USB Accessory
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(isChecked && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_USB_ACCESSORY);
                        uncheckOtherSwitches((Switch) buttonView);
                    } else if(mDisableState == false && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(-1);
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_USB_ACCESSORY, s);
            s = (Switch) v.findViewById(R.id.switch_usb_device);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // USB Device
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(isChecked && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_USB_DEVICE);
                        uncheckOtherSwitches((Switch) buttonView);
                    } else if(mDisableState == false && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(-1);
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_USB_DEVICE, s);
            s = (Switch) v.findViewById(R.id.switch_usb_headset);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // USB Headset
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(isChecked && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_USB_HEADSET);
                        uncheckOtherSwitches((Switch) buttonView);
                    } else if(mDisableState == false && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(-1);
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_USB_HEADSET, s);
            s = (Switch) v.findViewById(R.id.switch_wired_headphones);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // Wired Headphones
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(isChecked && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_WIRED_HEADPHONES);
                        uncheckOtherSwitches((Switch) buttonView);
                    } else if(mDisableState == false && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(-1);
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_WIRED_HEADPHONES, s);
            s = (Switch) v.findViewById(R.id.switch_wired_headset);
            s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // Wired Headset
                    RoutingActivity a = (RoutingActivity) getActivity();
                    if(isChecked && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(AudioDeviceInfo.TYPE_WIRED_HEADSET);
                        uncheckOtherSwitches((Switch) buttonView);
                    } else if(mDisableState == false && a != null && a.getService() != null) {
                        a.getService().setPreferredDevice(-1);
                    }
                }
            });
            mSwitches.put(AudioDeviceInfo.TYPE_WIRED_HEADSET, s);

            AudioDeviceInfo[] devices = mManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for(AudioDeviceInfo device : devices) {
                if(mSwitches.containsKey(device.getType())) {
                    ((Switch)mSwitches.get(device.getType())).setEnabled(true);
                }
            }
        }

        public void setSwitchEnabled(int key, boolean enabled) {
            if(mSwitches != null) {
                Log.v(TAG, "setSwitchEnabled() enable key " + key);
                mSwitches.get(key).setEnabled(enabled);
            } else {
                Log.v(TAG, "setSwitchEnabled() mSwitches is null");
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
