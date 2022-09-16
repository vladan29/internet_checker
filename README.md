# internet_checker

minSdkVersion 21 -> Android 5.0 (API level 21)

This code does check a network state using a Network state object.
[![](https://jitpack.io/v/vladan29/internet_checker.svg)](https://jitpack.io/#vladan29/internet_checker)

~~~
class NetworkState(
      var isConnected: Boolean,
      var connectionType: Int,
      var maxMsToLive: Int,
      var signalStrenght: Int,
      var linkDnBandwidth: Int,
      var linkUpBandwidth: Int
      )
~~~
## How to use:

* Set permissions in AndroidManifest file.

~~~
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
~~~

* Set the connection to jitpack.io into the Gradle file

~~~
allprojects {
    repositories {
        google()
        jcenter()
        maven { url 'https://jitpack.io' }
    }
}
~~~

* Set next dependency
~~~
dependencies {
implementation 'com.github.vladan29:internet_checker:1.0.3'
}
~~~
* Owerride Application class and set next code

~~~
public class MyApplication extends Application {
    InternetManager internetManager;
   
    @Override
    public void onCreate() {
        super.onCreate();
        internetManager = InternetManager.getInternetManager(this);
        internetManager.registerInternetMonitor();

    }
}
~~~
* For now, code can be used in any view class as an Activity or Fragment

~~~
public class MainActivity extends AppCompatActivity{
private boolean isConnected;
private NetworkLiveData networkLiveData = NetworkLiveData.get();

@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         networkLiveData.observe(this, networkState -> {
            isConnected = networkState.isConnected();
            Here can be set all variables from the NetworkState object.
            All those values will be changed automatically every time when NetworkManager detects changing of NetworkState.
        });
        }
}
~~~

Mail address internet.checker.vladan29@gmail.com
