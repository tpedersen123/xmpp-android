package com.teletronics.XMPP;

import android.util.Log;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

public class RCTXMPPModule extends ReactContextBaseJavaModule {

    private static final String TAG = "RCTXMPPModule";

    private final ReactApplicationContext _reactContext;

    public RCTXMPPModule(ReactApplicationContext reactContext) {
        super(reactContext);
        _reactContext = reactContext;
    }

    @Override
    public String getName() {
        return TAG;
    }

    @ReactMethod
    public void connect(String userName, String password) {
        Log.d(TAG, "connect called with" + userName + " - " + password);
    }
}
