//
//  CleverTapPlugin.java
//  Copyright (C) 2015 CleverTap 
//
//  This code is provided under a commercial License.
//  A copy of this license has been distributed in a file called LICENSE
//  with this source code.
//
//

package com.clevertap.cordova;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.location.Location;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;

import com.clevertap.android.sdk.ActivityLifecycleCallback;
import com.clevertap.android.sdk.CleverTapAPI;
import com.clevertap.android.sdk.SyncListener;
import com.clevertap.android.sdk.InAppNotificationListener;
import com.clevertap.android.sdk.EventDetail;
import com.clevertap.android.sdk.UTMDetail;
import com.clevertap.android.sdk.exceptions.CleverTapMetaDataNotFoundException;
import com.clevertap.android.sdk.exceptions.CleverTapPermissionsNotSatisfied;
import com.clevertap.android.sdk.exceptions.InvalidEventNameException;


public class CleverTapPlugin extends CordovaPlugin implements SyncListener, InAppNotificationListener {

    private static final String LOG_TAG = "CLEVERTAP_PLUGIN";
    private static String CLEVERTAP_API_ERROR;
    private static CleverTapAPI cleverTap;


    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        try {
            cleverTap = CleverTapAPI.getInstance(cordova.getActivity().getApplicationContext());
            cleverTap.setSyncListener(this);
            cleverTap.setInAppNotificationListener(this);
        } catch (CleverTapMetaDataNotFoundException e) {
            CLEVERTAP_API_ERROR = e.getLocalizedMessage();
            //Log.d(LOG_TAG, e.getLocalizedMessage());
        } catch (CleverTapPermissionsNotSatisfied e) {
            CLEVERTAP_API_ERROR = e.getLocalizedMessage();
            //Log.d(LOG_TAG, e.getLocalizedMessage());
        }

        onNewIntent(cordova.getActivity().getIntent());

    }

    /**
     * Called when the activity receives a new intent.
     */
    public void onNewIntent(Intent intent) {
        if (intent == null) return;

        // deeplink
        if(intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
            Uri data = intent.getData();
            if (data != null) {
                final String json = "{'deeplink':'" + data.toString() + "'}";

                cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadUrl("javascript:cordova.fireDocumentEvent('onDeepLink'," + json + ");");
                    }
                });

            }
        }
        // push notification
        else {
            Bundle extras = intent.getExtras();
            Boolean isPushNotification = (extras != null && extras.get("wzrk_pn") != null);
            if (isPushNotification) {
                JSONObject data = new JSONObject();

                for (String key : extras.keySet()) {
                    try {
                        Object value = extras.get(key);
                        if (value instanceof Map) {
                            JSONObject jsonObject = new JSONObject((Map) value);
                            data.put(key, jsonObject);
                        } else if (value instanceof List) {
                            JSONArray jsonArray = new JSONArray((List) value);
                            data.put(key, jsonArray);
                        } else {
                            data.put(key, extras.get(key));
                        }
                    } catch (Throwable t) {
                        // no-op
                    }
                }

                final String json = "{'notification':" + data.toString() + "}";
                cordova.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadUrl("javascript:cordova.fireDocumentEvent('onPushNotification'," + json + ");");
                    }
                });

            }
        }
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {

        Log.d(LOG_TAG, "handling action " + action);

        boolean haveError = false;
        String errorMsg = "unhandled CleverTapPlugin action";

        PluginResult result = null;

        if (!checkCleverTapInitialized()) {
            result = new PluginResult(PluginResult.Status.ERROR, "CleverTap API not initialized");
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        }

        // manually start application life cycle
        else if (action.equals("notifyDeviceReady")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    ActivityLifecycleCallback.register(cordova.getActivity().getApplication());
                    CleverTapAPI.setAppForeground(true);
                    cleverTap.activityResumed(cordova.getActivity());
                    PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                    _result.setKeepCallback(true);
                    callbackContext.sendPluginResult(_result);
                }
            });

            return true;
        }

        // not required for Android here but handle as its in the JS interface
        else if (action.equals("registerPush")) {
            result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        }

        // not required for Android here but handle as its in the JS interface
        else if (action.equals("setPushTokenAsString")) {
            result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        }

        // not required for Android here but handle as its in the JS interface
        else if (action.equals("recordScreenView")) {
            result = new PluginResult(PluginResult.Status.NO_RESULT);
            result.setKeepCallback(true);
            callbackContext.sendPluginResult(result);
            return true;
        }

        else if (action.equals("setDebugLevel")) {
            int level = (args.length() == 1 ? args.getInt(0) : -1);
            if (level >= 0) {
                CleverTapAPI.setDebugLevel(level);
                result = new PluginResult(PluginResult.Status.NO_RESULT);
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);
                return true;
            }

        }

        else if (action.equals("enablePersonalization")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    cleverTap.enablePersonalization();
                    PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                    _result.setKeepCallback(true);
                    callbackContext.sendPluginResult(_result);
                }
            });
            return true;

        }

        else if (action.equals("recordEventWithName")) {
            final String eventName = (args.length() == 1 ? args.getString(0) : null);
            if (eventName != null) {
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        cleverTap.event.push(eventName);
                        PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                });
                return true;

            } else {
                errorMsg = "eventName cannot be null";
            }
        }

        else if (action.equals("recordEventWithNameAndProps")) {
            String eventName = null;
            JSONObject jsonProps;
            HashMap<String, Object> _props = null;

            if (args.length() == 2) {
                if (!args.isNull(0)) {
                    eventName = args.getString(0);
                } else {
                    haveError = true;
                    errorMsg = "eventName cannot be null";
                }
                if (!args.isNull(1)) {
                    jsonProps = args.getJSONObject(1);
                    try {
                        _props = toMap(jsonProps);
                    } catch (JSONException e) {
                        haveError = true;
                        errorMsg = "Error parsing event properties";
                    }
                } else {
                    haveError = true;
                    errorMsg = "Arg cannot be null";
                }
            } else {
                haveError = true;
                errorMsg = "Expected 2 arguments";
            }

            if (!haveError) {
                final String _eventName = eventName;
                final HashMap<String, Object> props = _props;
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        cleverTap.event.push(_eventName, props);
                        PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                });
                return true;
            }
        }

        else if (action.equals("recordChargedEventWithDetailsAndItems")) {
            JSONObject jsonDetails;
            JSONArray jsonItems;
            HashMap<String, Object> _details = null;
            ArrayList<HashMap<String, Object>> _items = null;

            if (args.length() == 2) {
                if (!args.isNull(0)) {
                    jsonDetails = args.getJSONObject(0);
                    try {
                        _details = toMap(jsonDetails);
                    } catch (JSONException e) {
                        haveError = true;
                        errorMsg = "Error parsing arg " + e.getLocalizedMessage();
                    }
                } else {
                    haveError = true;
                    errorMsg = "Arg cannot be null";
                }
                if (!args.isNull(1)) {
                    jsonItems = args.getJSONArray(1);
                    try {
                        _items = toArrayListOfStringObjectMaps(jsonItems);
                    } catch (JSONException e) {
                        haveError = true;
                        errorMsg = "Error parsing arg " + e.getLocalizedMessage();
                    }
                } else {
                    haveError = true;
                    errorMsg = "Arg cannot be null";
                }
            } else {
                haveError = true;
                errorMsg = "Expected 2 arguments";
            }

            if (!haveError) {
                final HashMap<String, Object> details = _details;
                final ArrayList<HashMap<String, Object>> items = _items;
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        try {
                            cleverTap.event.push(CleverTapAPI.CHARGED_EVENT, details, items);
                            PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                            _result.setKeepCallback(true);
                            callbackContext.sendPluginResult(_result);
                        } catch (InvalidEventNameException e) {
                            PluginResult _result = new PluginResult(PluginResult.Status.ERROR, e.getLocalizedMessage());
                            _result.setKeepCallback(true);
                            callbackContext.sendPluginResult(_result);
                        }
                    }
                });
                return true;
            }
        }

        else if (action.equals("pushInstallReferrer")) {
            String source = null;
            String campaign = null;
            String medium = null;

            if (args.length() == 3) {
                if (!args.isNull(0)) {
                    source = args.getString(0);
                } else {
                    haveError = true;
                    errorMsg = "source cannot be null";
                }
                if (!args.isNull(1)) {
                    medium = args.getString(1);
                } else {
                    haveError = true;
                    errorMsg = "medium cannot be null";
                }
                if (!args.isNull(2)) {
                    campaign = args.getString(2);
                } else {
                    haveError = true;
                    errorMsg = "campaign cannot be null";
                }
            } else {
                haveError = true;
                errorMsg = "Expected 3 arguments";
            }

            if (!haveError) {
                final String _source = source;
                final String _medium = medium;
                final String _campaign = campaign;
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        cleverTap.pushInstallReferrer(_source, _medium, _campaign);
                        PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                });
                return true;
            }
        }

        else if (action.equals("eventGetFirstTime")) {
            final String eventName = (args.length() == 1 ? args.getString(0) : null);
            if (eventName != null) {
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        double first = cleverTap.event.getFirstTime(eventName);
                        PluginResult _result = new PluginResult(PluginResult.Status.OK, (float) first);
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                });
                return true;

            } else {
                errorMsg = "eventName cannot be null";
            }
        }

        else if (action.equals("eventGetLastTime")) {
            final String eventName = (args.length() == 1 ? args.getString(0) : null);
            if (eventName != null) {
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        double lastTime = cleverTap.event.getLastTime(eventName);
                        PluginResult _result = new PluginResult(PluginResult.Status.OK, (float) lastTime);
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                });
                return true;

            } else {
                errorMsg = "eventName cannot be null";
            }
        }

        else if (action.equals("eventGetOccurrences")) {
            final String eventName = (args.length() == 1 ? args.getString(0) : null);
            if (eventName != null) {
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        int num = cleverTap.event.getCount(eventName);
                        PluginResult _result = new PluginResult(PluginResult.Status.OK, num);
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                });
                return true;

            } else {
                errorMsg = "eventName cannot be null";
            }
        }

        else if (action.equals("eventGetDetails")) {
            final String eventName = (args.length() == 1 ? args.getString(0) : null);
            if (eventName != null) {
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        EventDetail details = cleverTap.event.getDetails(eventName);
                        try {
                            JSONObject jsonDetails = CleverTapPlugin.eventDetailsToJSON(details);
                            PluginResult _result = new PluginResult(PluginResult.Status.OK, jsonDetails);
                            _result.setKeepCallback(true);
                            callbackContext.sendPluginResult(_result);
                        } catch (JSONException e ) {
                            PluginResult _result = new PluginResult(PluginResult.Status.ERROR, e.getLocalizedMessage());
                            _result.setKeepCallback(true);
                            callbackContext.sendPluginResult(_result);
                        }
                    }
                });
                return true;

            } else {
                errorMsg = "eventName cannot be null";
            }
        }

        else if (action.equals("getEventHistory")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    Map<String, EventDetail> history = cleverTap.event.getHistory();
                    try {
                        JSONObject jsonDetails = CleverTapPlugin.eventHistoryToJSON(history);
                        PluginResult _result = new PluginResult(PluginResult.Status.OK, jsonDetails);
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    } catch (JSONException e) {
                        PluginResult _result = new PluginResult(PluginResult.Status.ERROR, e.getLocalizedMessage());
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                }
            });
            return true;
        }

        else if (action.equals("setLocation")) {
            Double lat = null;
            Double lon = null;

            if (args.length() == 2) {
                if (!args.isNull(0)) {
                    lat = args.getDouble(0);
                } else {
                    haveError = true;
                    errorMsg = "lat cannot be null";
                }
                if (!args.isNull(1)) {
                    lon = args.getDouble(1);
                } else {
                    haveError = true;
                    errorMsg = "lon cannot be null";
                }
            } else {
                haveError = true;
                errorMsg = "Expected 2 arguments";
            }

            if (!haveError) {
                final Location location = new Location("CleverTapPlugin");
                location.setLatitude(lat);
                location.setLongitude(lon);
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        cleverTap.setLocation(location);
                        PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                });
                return true;
            }
        }

        else if (action.equals("getLocation")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    Location location = cleverTap.getLocation();
                    PluginResult _result = null;
                    try {
                        if (location != null) {
                            JSONObject jsonLoc = new JSONObject();
                            jsonLoc.put("lat", location.getLatitude());
                            jsonLoc.put("lon", location.getLongitude());
                            _result = new PluginResult(PluginResult.Status.OK, jsonLoc);
                        }
                    } catch (Throwable t) {
                        // no-op
                    }

                    if (_result == null) {
                        _result = new PluginResult(PluginResult.Status.ERROR, "Unable to get location");
                    }
                    _result.setKeepCallback(true);
                    callbackContext.sendPluginResult(_result);
                }
            });
            return true;
        }

        else if (action.equals("profileSet")) {
            JSONObject jsonProfile = null;

            if (args.length() == 1) {
                if (!args.isNull(0)) {
                    jsonProfile = args.getJSONObject(0);
                } else {
                    haveError = true;
                    errorMsg = "profile cannot be null";
                }

            } else {
                haveError = true;
                errorMsg = "Expected 1 argument";
            }

            if (!haveError) {
                final JSONObject _jsonProfile = jsonProfile;
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        try {
                            HashMap<String, Object> profile = formatProfile(_jsonProfile);
                            cleverTap.profile.push(profile);
                        } catch (Exception e) {
                            Log.d(LOG_TAG, "Error setting profile " + e.getLocalizedMessage());
                        }
                        PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                });

                return true;
            }
        }

        else if (action.equals("onUserLogin")) {
            JSONObject jsonProfile = null;

            if (args.length() == 1) {
                if (!args.isNull(0)) {
                    jsonProfile = args.getJSONObject(0);
                } else {
                    haveError = true;
                    errorMsg = "profile cannot be null";
                }

            } else {
                haveError = true;
                errorMsg = "Expected 1 argument";
            }

            if (!haveError) {
                final JSONObject _jsonProfile = jsonProfile;
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        try {
                            HashMap<String, Object> profile = formatProfile(_jsonProfile);
                            cleverTap.onUserLogin(profile);
                        } catch (Exception e) {
                            Log.d(LOG_TAG, "Error in onUserLogin " + e.getLocalizedMessage());
                        }
                        PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                });

                return true;
            }
        }

        else if (action.equals("profileSetGraphUser")) {
            JSONObject jsonGraphUser = null;

            if (args.length() == 1) {
                if (!args.isNull(0)) {
                    jsonGraphUser = args.getJSONObject(0);
                } else {
                    haveError = true;
                    errorMsg = "profile cannot be null";
                }

            } else {
                haveError = true;
                errorMsg = "Expected 1 argument";
            }

            if (!haveError) {
                final JSONObject _jsonGraphUser = jsonGraphUser;
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        cleverTap.profile.pushFacebookUser(_jsonGraphUser);
                        PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                });
                return true;
            }
        }

        else if (action.equals("profileSetGooglePlusUser")) {
            JSONObject jsonGooglePlusUser;
            HashMap<String, Object> _profile = null;

            if (args.length() == 1) {
                if (!args.isNull(0)) {
                    jsonGooglePlusUser = args.getJSONObject(0);
                    try {
                        _profile = toMapFromGooglePlusUser(jsonGooglePlusUser);
                    } catch (JSONException e) {
                        haveError = true;
                        errorMsg = "Error parsing arg " + e.getLocalizedMessage();
                    }
                } else {
                    haveError = true;
                    errorMsg = "profile cannot be null";
                }

            } else {
                haveError = true;
                errorMsg = "Expected 1 argument";
            }

            if (!haveError) {
                final HashMap<String, Object> profile = _profile;
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        cleverTap.profile.push(profile);
                        PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                });
                return true;
            }
        }

        else if (action.equals("profileGetProperty")) {
            final String propertyName = (args.length() == 1 ? args.getString(0) : null);
            if (propertyName != null) {
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        PluginResult _result;
                        Object prop = cleverTap.profile.getProperty(propertyName);

                        if (prop instanceof JSONArray) {
                            JSONArray _prop = (JSONArray) prop;
                            _result = new PluginResult(PluginResult.Status.OK, _prop);

                        } else {
                            String _prop;
                            if (prop != null) {
                                 _prop = prop.toString();
                            } else {
                                _prop = null;
                            }
                            _result = new PluginResult(PluginResult.Status.OK, _prop);
                        }

                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                });
                return true;

            } else {
                errorMsg = "propertyName cannot be null";
            }
        }

        else if (action.equals("profileGetCleverTapID")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    String CleverTapID = cleverTap.getCleverTapID();
                    PluginResult _result = new PluginResult(PluginResult.Status.OK, CleverTapID);
                    _result.setKeepCallback(true);
                    callbackContext.sendPluginResult(_result);
                }
            });
            return true;
        }

        else if (action.equals("profileGetCleverTapAttributionIdentifier")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    String attributionID = cleverTap.getCleverTapAttributionIdentifier();
                    PluginResult _result = new PluginResult(PluginResult.Status.OK, attributionID);
                    _result.setKeepCallback(true);
                    callbackContext.sendPluginResult(_result);
                }
            });
            return true;
        }

        else if (action.equals("profileRemoveValueForKey")) {
            final String key = (args.length() == 1 ? args.getString(0) : null);
            if (key != null) {
                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        cleverTap.profile.removeValueForKey(key);
                        PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                });
                return true;

            } else {
                errorMsg = "property key cannot be null";
            }
        }

        else if (action.equals("profileSetMultiValues")) {
            String key = null;
            JSONArray values = null;

            if (args.length() == 2) {
                if (!args.isNull(0)) {
                    key = args.getString(0);
                } else {
                    haveError = true;
                    errorMsg = "key cannot be null";
                }
                if (!args.isNull(1)) {
                    values = args.getJSONArray(1);
                    if (values == null) {
                        haveError = true;
                        errorMsg = "values cannot be null";
                    }
                } else {
                    haveError = true;
                    errorMsg = "values cannot be null";
                }
            } else {
                haveError = true;
                errorMsg = "Expected 2 arguments";
            }

            if (!haveError) {
                final String _key = key;
                final ArrayList<String> _values = new ArrayList<String>();
                try {
                    for (int i = 0; i < values.length(); i++) {
                        _values.add(values.get(i).toString());
                    }

                    cordova.getThreadPool().execute(new Runnable() {
                        public void run() {
                            cleverTap.profile.setMultiValuesForKey(_key, _values);
                            PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                            _result.setKeepCallback(true);
                            callbackContext.sendPluginResult(_result);
                        }
                    });

                    return true;

                } catch (Exception e) {
                    // no-op
                }
            }
        }

        else if (action.equals("profileAddMultiValues")) {
            String key = null;
            JSONArray values = null;

            if (args.length() == 2) {
                if (!args.isNull(0)) {
                    key = args.getString(0);
                } else {
                    haveError = true;
                    errorMsg = "key cannot be null";
                }
                if (!args.isNull(1)) {
                    values = args.getJSONArray(1);
                    if (values == null) {
                        haveError = true;
                        errorMsg = "values cannot be null";
                    }
                } else {
                    haveError = true;
                    errorMsg = "values cannot be null";
                }
            } else {
                haveError = true;
                errorMsg = "Expected 2 arguments";
            }

            if (!haveError) {
                final String _key = key;
                final ArrayList<String> _values = new ArrayList<String>();
                try {
                    for (int i = 0; i < values.length(); i++) {
                        _values.add(values.get(i).toString());
                    }

                    cordova.getThreadPool().execute(new Runnable() {
                        public void run() {
                            cleverTap.profile.addMultiValuesForKey(_key, _values);
                            PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                            _result.setKeepCallback(true);
                            callbackContext.sendPluginResult(_result);
                        }
                    });

                    return true;

                } catch (Exception e) {
                    // no-op
                }
            }
        }
        else if (action.equals("profileRemoveMultiValues")) {
            String key = null;
            JSONArray values = null;

            if (args.length() == 2) {
                if (!args.isNull(0)) {
                    key = args.getString(0);
                } else {
                    haveError = true;
                    errorMsg = "key cannot be null";
                }
                if (!args.isNull(1)) {
                    values = args.getJSONArray(1);
                    if (values == null) {
                        haveError = true;
                        errorMsg = "values cannot be null";
                    }
                } else {
                    haveError = true;
                    errorMsg = "values cannot be null";
                }
            } else {
                haveError = true;
                errorMsg = "Expected 2 arguments";
            }

            if (!haveError) {
                final String _key = key;
                final ArrayList<String> _values = new ArrayList<String>();
                try {
                    for (int i = 0; i < values.length(); i++) {
                        _values.add(values.get(i).toString());
                    }

                    cordova.getThreadPool().execute(new Runnable() {
                        public void run() {
                            cleverTap.profile.removeMultiValuesForKey(_key, _values);
                            PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                            _result.setKeepCallback(true);
                            callbackContext.sendPluginResult(_result);
                        }
                    });

                    return true;

                } catch (Exception e) {
                    // no-op
                }
            }
        }

        else if (action.equals("profileAddMultiValue")) {
            String key = null;
            String value = null;

            if (args.length() == 2) {
                if (!args.isNull(0)) {
                    key = args.getString(0);
                } else {
                    haveError = true;
                    errorMsg = "key cannot be null";
                }
                if (!args.isNull(1)) {
                    value = args.getString(1);
                } else {
                    haveError = true;
                    errorMsg = "value cannot be null";
                }
            } else {
                haveError = true;
                errorMsg = "Expected 2 arguments";
            }

            if (!haveError) {
                final String _key = key;
                final String _value = value;

                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        cleverTap.profile.addMultiValueForKey(_key, _value);
                        PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                });
                return true;
            }
        }

        else if (action.equals("profileRemoveMultiValue")) {
            String key = null;
            String value = null;

            if (args.length() == 2) {
                if (!args.isNull(0)) {
                    key = args.getString(0);
                } else {
                    haveError = true;
                    errorMsg = "key cannot be null";
                }
                if (!args.isNull(1)) {
                    value = args.getString(1);
                } else {
                    haveError = true;
                    errorMsg = "value cannot be null";
                }
            } else {
                haveError = true;
                errorMsg = "Expected 2 arguments";
            }

            if (!haveError) {
                final String _key = key;
                final String _value = value;

                cordova.getThreadPool().execute(new Runnable() {
                    public void run() {
                        cleverTap.profile.removeMultiValueForKey(_key, _value);
                        PluginResult _result = new PluginResult(PluginResult.Status.NO_RESULT);
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                });
                return true;
            }
        }

        else if (action.equals("sessionGetTimeElapsed")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    int time = cleverTap.session.getTimeElapsed();
                    PluginResult _result = new PluginResult(PluginResult.Status.OK, time);
                    _result.setKeepCallback(true);
                    callbackContext.sendPluginResult(_result);
                }
            });
            return true;
        }

        else if (action.equals("sessionGetTotalVisits")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    int count = cleverTap.session.getTotalVisits();
                    PluginResult _result = new PluginResult(PluginResult.Status.OK, count);
                    _result.setKeepCallback(true);
                    callbackContext.sendPluginResult(_result);
                }
            });
            return true;
        }

        else if (action.equals("sessionGetScreenCount")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    int count = cleverTap.session.getScreenCount();
                    PluginResult _result = new PluginResult(PluginResult.Status.OK, count);
                    _result.setKeepCallback(true);
                    callbackContext.sendPluginResult(_result);
                }
            });
            return true;
        }

        else if (action.equals("sessionGetPreviousVisitTime")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    int time = cleverTap.session.getPreviousVisitTime();
                    PluginResult _result = new PluginResult(PluginResult.Status.OK, time);
                    _result.setKeepCallback(true);
                    callbackContext.sendPluginResult(_result);
                }
            });
            return true;
        }

        else if (action.equals("sessionGetUTMDetails")) {
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    UTMDetail details = cleverTap.session.getUTMDetails();
                    try {
                        JSONObject jsonDetails = CleverTapPlugin.utmDetailsToJSON(details);
                        PluginResult _result = new PluginResult(PluginResult.Status.OK, jsonDetails);
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    } catch (JSONException e ) {
                        PluginResult _result = new PluginResult(PluginResult.Status.ERROR, e.getLocalizedMessage());
                        _result.setKeepCallback(true);
                        callbackContext.sendPluginResult(_result);
                    }
                }
            });
            return true;
        }

        result = new PluginResult(PluginResult.Status.ERROR, errorMsg);
        result.setKeepCallback(true);
        callbackContext.sendPluginResult(result);
        return true;
    }


    // InAppNotificationListener

    public boolean beforeShow(Map<String, Object> var1) {
        return true;
    }

    public void onDismissed(Map<String, Object> var1, @Nullable Map<String, Object> var2) {
        if(var1 == null && var2 == null) {
            return ;
        }

        JSONObject extras = var1 != null ? new JSONObject(var1) : new JSONObject();
        String _json = "{'extras':"+extras.toString()+",";

        JSONObject actionExtras = var2 != null ? new JSONObject(var2) : new JSONObject();
        _json += "'actionExtras':"+actionExtras.toString()+"}";

        final String json = _json;
        webView.getView().post(new Runnable() {
            public void run() {
                webView.loadUrl("javascript:cordova.fireDocumentEvent('onCleverTapInAppNotificationDismissed'," + json + ");");
            }
        });
    }

    // SyncListener
    public void profileDataUpdated(JSONObject updates) {

        if(updates == null) {
            return ;
        }

        final String json = "{'updates':"+updates.toString()+"}";
        webView.getView().post(new Runnable() {
            public void run() {
                webView.loadUrl("javascript:cordova.fireDocumentEvent('onCleverTapProfileSync'," + json + ");");
            }
        });
    }

    public void profileDidInitialize (String CleverTapID) {

        if (CleverTapID == null) {
            return;
        }

        final String json = "{'CleverTapID':"+ "'"+CleverTapID+"'"+"}";
        webView.getView().post(new Runnable() {
            public void run() {
                webView.loadUrl("javascript:cordova.fireDocumentEvent('onCleverTapProfileDidInitialize',"+json+");");
            }
        });
    }

    /*******************
     * Private Methods
     ******************/

    private static boolean checkCleverTapInitialized() {
        boolean initialized = (cleverTap != null);
        if(!initialized) {
            Log.d(LOG_TAG, "CleverTap API not initialized: " + CLEVERTAP_API_ERROR);
        }
        return initialized;
    }

    private static HashMap<String, Object> formatProfile(JSONObject jsonProfile) {
        try {
            HashMap<String, Object> profile = toMap(jsonProfile);
            String dob = (String)profile.get("DOB");
            if(dob != null) {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                try {
                    Date date = format.parse(dob);
                    profile.put("DOB", date);
                } catch (ParseException e) {
                    profile.remove("DOB");
                    Log.d(LOG_TAG, "invalid DOB format in profileSet");
                }
            }

            return profile;

        } catch (Throwable t) {
            return null;
        }
    }

    private static Object fromJson(Object json) throws JSONException {
        if (json == JSONObject.NULL) {
            return null;
        } else if (json instanceof JSONObject) {
            return toMap((JSONObject) json);
        } else {
            return json;
        }
    }

    private static HashMap<String, Object> toMap(JSONObject object) throws JSONException {
        HashMap<String, Object> map = new HashMap<String, Object>();
        Iterator keys = object.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            map.put(key, fromJson(object.get(key)));
        }
        return map;
    }

    private static HashMap<String, Object> toMapFromGooglePlusUser(JSONObject object) throws JSONException {

        HashMap<String, Object> map = new HashMap<String, Object>();

        JSONObject nameObj = (JSONObject)object.get("name");
        if(nameObj != null) {
            String name = (String)nameObj.get("formatted");
            if(name != null) {
                map.put("gpName", name);
            }
        }

        String id1 = (String)object.get("id");
        if(id1 != null) {
            map.put("gpID", id1);
        }

        String gender1 = (String)object.get("gender");
        if(gender1 != null) {
            if (gender1.toLowerCase().startsWith("m")) {
                gender1 = "M";
            } else if (gender1.toLowerCase().startsWith("f")) {
                gender1 = "F";
            } else {
                gender1 = "";
            }
            map.put("gpGender", gender1);
        }

        JSONArray organizations = (JSONArray)object.get("organizations");
        if(organizations != null) {
            String work1 = "N";
            for(int i=0; i < organizations.length(); i++) {
                JSONObject org = organizations.getJSONObject(i);
                String type = (String)org.get("type");
                if(type.equals("work")) {
                    work1 = "Y";
                    break;
                }
            }
            map.put("gpEmployed", work1);
        }

        String birthday = (String)object.get("birthday");
        if(birthday != null) {
            Date DOB = null;
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            try {
                DOB = format.parse(birthday);
            } catch (ParseException e) {
                Log.d(LOG_TAG, "invalid DOB format");
            }
            if(DOB != null) {
                map.put("gpDOB", DOB);
            }
        }

        String relationshipStatus = (String)object.get("relationshipStatus");
        if(relationshipStatus != null) {
            String married1 = relationshipStatus.equals("married") ? "Y" : "N";
            map.put("gpRS", married1);
        }

        return map;
    }

    private static ArrayList<HashMap<String, Object>> toArrayListOfStringObjectMaps(JSONArray array) throws JSONException {
        ArrayList<HashMap<String, Object>> aList = new ArrayList<HashMap<String, Object>>();

        for (int i = 0; i < array.length(); i++) {
            aList.add(toMap((JSONObject) array.get(i)));
        }

        return aList;
    }

    private static JSONObject eventDetailsToJSON(EventDetail details) throws JSONException {

        JSONObject json = new JSONObject();

        if(details != null) {
            json.put("name", details.getName());
            json.put("firstTime", details.getFirstTime());
            json.put("lastTime", details.getLastTime());
            json.put("count", details.getCount());
        }

        return json;
    }

    private static JSONObject utmDetailsToJSON(UTMDetail details) throws JSONException {

        JSONObject json = new JSONObject();

        if(details != null) {
            json.put("campaign", details.getCampaign());
            json.put("source", details.getSource());
            json.put("medium", details.getMedium());
        }

        return json;
    }

    private static JSONObject eventHistoryToJSON( Map<String, EventDetail> history) throws JSONException {

        JSONObject json = new JSONObject();

        if(history != null) {
            for (Object key : history.keySet()) {
                json.put(key.toString(), eventDetailsToJSON(history.get((String)key)));
            }
        }

        return json;
    }
}


