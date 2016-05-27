CleverTap Cordova Plugin
========

## Supported Versions

Tested on Cordova 6.1.1

- [CleverTap Android SDK version 2.1.0](https://github.com/CleverTap/clevertap-android-sdk/releases/tag/2.1.0)
- [CleverTap iOS SDK version 2.1.0, Xcode 7 only](https://github.com/CleverTap/clevertap-ios-sdk/releases/tag/2.1.0)

## Install

To integrate CleverTap for Cordova:

1. Sign up 

2. Set up and register for Push notifications, if required.

3. Integrate your Javascript with the CleverTap Plugin.

### 1. Sign up

[Sign up](http://www.clevertap.com/sign-up/) for a free account.  

When you create your CleverTap account, you will also automatically get a -Test account.  Use the -Test account for development and the main account for production.

### Install the Plugin

Grab the Account ID and Token values from your CleverTap [Dashboard](https://dashboard.clevertap.com) -> Settings.

#### Using Cordova  

- iOS:
```
cordova plugin add https://github.com/CleverTap/clevertap-cordova.git --variable CLEVERTAP_ACCOUNT_ID=YOUR CLEVERTAP ACCOUNT ID --variable CLEVERTAP_TOKEN=YOUR CELVERTAP ACCOUNT TOKEN 
```

- Android:
```
cordova plugin add https://github.com/CleverTap/clevertap-cordova.git --variable CLEVERTAP_ACCOUNT_ID=YOUR CLEVERTAP ACCOUNT ID --variable CLEVERTAP_TOKEN=YOUR CELVERTAP ACCOUNT TOKEN --variable GCM_PROJECT_NUMBER=YOUR GCM PROJECT NUMBER
```

#### Using Ionic  

- iOS:
```
ionic plugin add https://github.com/CleverTap/clevertap-cordova.git --variable CLEVERTAP_ACCOUNT_ID=YOUR CLEVERTAP ACCOUNT ID --variable CLEVERTAP_TOKEN=YOUR CELVERTAP ACCOUNT TOKEN 
```

- Android:
```
ionic plugin add https://github.com/CleverTap/clevertap-cordova.git --variable CLEVERTAP_ACCOUNT_ID=YOUR CLEVERTAP ACCOUNT ID --variable CLEVERTAP_TOKEN=YOUR CELVERTAP ACCOUNT TOKEN --variable GCM_PROJECT_NUMBER=YOUR GCM PROJECT NUMBER
```

#### Using PhoneGap Build

Add the following to your `www/config.xml` file:

```
<preference name="android-build-tool" value="gradle" />

<gap:plugin name="clevertap-cordova" source="npm">
    <param name="CLEVERTAP_ACCOUNT_ID" value="YOUR CLEVERTAP ACCOUNT ID" />
    <param name="CLEVERTAP_TOKEN" value="YOUR CLEVERTAP ACCOUNT TOKEN" />
    <param name="GCM_PROJECT_NUMBER" value="YOUR GCM PROJECT NUMBER" />
</gap:plugin>
```            

*For PhoneGap Build Android projects*:  **Extremely Important**:  add `CleverTap.notifyDeviceReady();` to your onDeviceReady callback in `www/js/index.js`:

```
onDeviceReady: function() {
    app.receivedEvent('deviceready');
    CleverTap.notifyDeviceReady();
    ...
},
```

#### iOS

Check your .plist file:

![plist account values](http://staging.support.wizrocket.com.s3-website-eu-west-1.amazonaws.com/images/integration/plist-account.png)

#### Android

Check that the following is inside the `<application></application>` tags of your AndroidManifest.xml:  

    <meta-data  
        android:name="CLEVERTAP_ACCOUNT_ID"  
        android:value="Your CleverTap Account ID"/>  
    <meta-data  
        android:name="CLEVERTAP_TOKEN"  
        android:value="Your CleverTap Account Token"/>

Replace "Your CleverTap Account ID" and "Your CleverTap Account Token" with actual values from your CleverTap [Dashboard](https://dashboard.clevertap.com) -> Settings.

**Set the Lifecycle Callback** 

IMPORTANT!

Check the `android:name` property of the `<application>` tag of our AndroidManifest.xml:

    <application
        android:name="com.clevertap.android.sdk.Application">

**Note:** The above step is **extremely important** and enables CleverTap to track notification opens, display in-app notifications, track deep links, and other important **user behaviour**.

**Add Permissions**

Please ensure that you're requesting the following permissions for your app:

    <!-- Required to retrieve a unique identifier for the device, see note below -->
    <uses-permission android:name="android.permission.READ_PHONE_STATE"/>
    <!-- Required to allow the app to send events -->
    <uses-permission android:name="android.permission.INTERNET"/>
    <!-- Recommended so that we can be smart about when to send the data -->
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <!-- Recommended so that we can get the user's primary e-mail address -->
    <uses-permission android:name="android.permission.GET_ACCOUNTS"/>
    <!-- Recommended so that we can get the user's location -->
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION"/>
    <!-- Recommended -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

[Please see the example AndroidManifest.xml here](https://github.com/CleverTap/clevertap-cordova/blob/master/Starter/platforms/android/AndroidManifest.xml).

**Add Dependencies**

Make sure your build.gradle file includes the play-services and support library dependencies:

    dependencies {
        compile fileTree(dir: 'libs', include: '*.jar'  )
        // SUB-PROJECT DEPENDENCIES START  
        debugCompile project(path: "CordovaLib", configuration: "debug")  
        releaseCompile project(path: "CordovaLib", configuration: "release")  
        compile "com.google.android.gms:play-services-base:9.0.0"
        compile "com.google.android.gms:play-services-basement:9.0.0"
        compile "com.google.android.gms:play-services-gcm:9.0.0"
        compile "com.google.android.gms:play-services-location:9.0.0"
        compile "com.android.support:support-v4:23.4.0"
        // SUB-PROJECT DEPENDENCIES END   


### 2. Set up and register for push notifications and deep links

#### iOS

[Follow Steps 1 and 2 in these instructions to set up push notifications for your app.](https://support.clevertap.com/messaging/push-notifications/#ios)

If you plan on using deep links, [please register your custom url scheme as described here](https://support.clevertap.com/messaging/deep-linking/#step-1-register-your-custom-scheme).  

Afterwards, call the following from your Javascript.

    CleverTap.notifyDeviceReady(); // to be notified on push notifications and deep links that launch your app.
    CleverTap.registerPush();


[See the included example Starter Cordova project](https://github.com/CleverTap/clevertap-cordova/blob/master/Starter/platforms/ios/CleverTapStarter/Classes/AppDelegate.m).


#### Android

[Follow these instructions to set up push notifications for your app.](https://support.clevertap.com/messaging/push-notifications/#android)

     
### 3. Integrate Javascript with the Plugin

After integrating, all calls to the CleverTap SDK should be made from your Javascript.

Start by adding the following listeners to your Javascript:

    document.addEventListener('deviceready', this.onDeviceReady, false);
    document.addEventListener('onCleverTapProfileSync', this.onCleverTapProfileSync, false); // optional: to be notified of CleverTap user profile synchronization updates 
    document.addEventListener('onCleverTapProfileDidInitialize', this.onCleverTapProfileDidInitialize, false); // optional, to be notified when the CleverTap user profile is initialized
    document.addEventListener('onCleverTapInAppNotificationDismissed', this.onCleverTapInAppNotificationDismissed, false); // optional, to be receive a callback with custom in-app notification click data
    document.addEventListener('onDeepLink', this.onDeepLink, false); // optional, register to receive deep links.
    document.addEventListener('onPushNotification', this.onPushNotification, false); // optional, register to receive push notification payloads.


   // deep link handling
    onDeepLink: function(e) {
        console.log(e.deeplink);
    },

    // push notification data handling
    onPushNotification: function(e) {
        console.log(JSON.stringify(e.notification));
    }, 


Then:  

- [see the included iOS Starter Cordova project for example usage](https://github.com/CleverTap/clevertap-cordova/blob/master/Starter/platforms/ios/www/js/index.js).   

- [see the included Android Starter Cordova project for example usage](https://github.com/CleverTap/clevertap-cordova/blob/master/Starter/platforms/android/assets/www/js/index.js).  

