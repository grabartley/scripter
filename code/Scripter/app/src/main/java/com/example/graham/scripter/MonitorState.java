package com.example.graham.scripter;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.telephony.SmsManager;
import android.widget.Toast;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.atomic.AtomicBoolean;

/*Service used to store state values and functions available to scripts and run scripts when the state changes*/
public class MonitorState extends Service {
    //used to control the service thread
    private AtomicBoolean running = new AtomicBoolean(true);
    //names of each of the states stored in state with the corresponding indices are stored in stateLookupStrings
    private static final String[] stateLookupStrings = {"dayOfMonth", "month", "year", "hour", "minute", "dayOfWeek",
            "headphonesPluggedIn", "latitude", "longitude", "wifiConnected", "SSID"};
    //state contains values for each of the states provided for scripts
    private static Object[] state = {-1, -1, -1, -1, -1, "", false, -1, -1, false, ""};
    //previousState is used to hold the values of the previous state
    private static Object[] previousState = new Object[state.length];
    //changedStates stores the names of any states which have changed during a particular run of scripts in order to decide which scripts should be run
    private ArrayList<String> changedStates = new ArrayList<>();
    //variable used to hold IDs used in notifications created by Scripter
    private int notificationID = 1;
    //used to interact with UI
    private android.content.Context mainContext;

    public MonitorState() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*When the service is started*/
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //used to interact with UI
        mainContext = getBaseContext();
        //build persistent notification
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this);
        notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
        notificationBuilder.setContentTitle("Scripter is enabled!");
        notificationBuilder.setContentText("Click to open Scripter.");

        //Allow it to be clickable and open MainActivity
        Intent openMainActivity = new Intent(this, MainActivity.class);
        TaskStackBuilder taskStackBuilder = TaskStackBuilder.create(this);
        taskStackBuilder.addParentStack(MainActivity.class);
        taskStackBuilder.addNextIntent(openMainActivity);
        PendingIntent pendingIntent = taskStackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationBuilder.setContentIntent(pendingIntent);

        //start the constant notification for this Service (the Service will not be killed by Android)
        startForeground(notificationID, notificationBuilder.build());
        notificationID++;

        //show the user that their scripts have been started
        new MyScriptable().toastMessage("Scripter started");

        //multithreading
        Runnable r = new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    /* Initialize states */
                    Calendar cal = Calendar.getInstance();
                    state[0] = cal.get(Calendar.DAY_OF_MONTH);
                    state[1] = cal.get(Calendar.MONTH) + 1;
                    state[2] = cal.get(Calendar.YEAR);
                    state[3] = cal.get(Calendar.HOUR_OF_DAY);
                    state[4] = cal.get(Calendar.MINUTE);
                    state[5] = getDayOfWeek(cal.get(Calendar.DAY_OF_WEEK));
                    setEqualTo(previousState, state);
                    //while the service is running
                    while (running.get()) {
                        /*What the service does*/
                        //if the state has changed
                        if (!isEqualTo(state, previousState)) {
                            //call JavaScript scripts based on their dependencies
                            for (int i = 0; i < ScriptFileTracker.getNumOfScripts(); i++) {
                                ScripterScript currentScript = ScriptFileTracker.getScriptByIndex(i);
                                //if the current script is enabled
                                if (currentScript.isEnabled()) {
                                    //if the current script has specific dependencies
                                    if (currentScript.getNumOfDependencies() > 0) {
                                        //check if any of it's dependencies have changed
                                        for (int j = 0; j < currentScript.getNumOfDependencies(); j++) {
                                            //if one of it's dependencies have changed, run the script and stop looking
                                            if (changedStates.contains(currentScript.getDependencyByIndex(j))) {
                                                evaluateJS(currentScript.getCode(), currentScript.getFilename());
                                                break;
                                            }
                                        }
                                        //otherwise just run it whenever any state changes
                                    } else {
                                        evaluateJS(currentScript.getCode(), currentScript.getFilename());
                                    }
                                }
                            }
                            //set previous state & reset changed state indices
                            setEqualTo(previousState, state);
                            changedStates = new ArrayList<>();
                        }
                    }
                }
            }
        };

        //start a new thread for the service
        Thread monitorStateThread = new Thread(r);
        monitorStateThread.start();
        return Service.START_STICKY;
    }

    /*When the service is stopped*/
    @Override
    public void onDestroy() {
        //show the user that their scripts have been stopped
        new MyScriptable().toastMessage("Scripter stopped");
        running.set(false);
    }

    //uses Rhino to interpret code and return result as a String
    public String evaluateJS(String code, String filename) {
        Context cx = Context.enter();
        cx.setOptimizationLevel(-1);
        try {
            Scriptable scope = cx.initStandardObjects();
            //parentScope is needed in the case of calling instance methods existing in MyScriptable from JavaScript
            Scriptable parentScope = new MyScriptable();
            parentScope.setParentScope(scope);

            /* variables to add to scope */
            addVariableToScope(state[0], stateLookupStrings[0], scope);
            addVariableToScope(state[1], stateLookupStrings[1], scope);
            addVariableToScope(state[2], stateLookupStrings[2], scope);
            addVariableToScope(state[3], stateLookupStrings[3], scope);
            addVariableToScope(state[4], stateLookupStrings[4], scope);
            addVariableToScope(state[5], stateLookupStrings[5], scope);
            addVariableToScope(state[6], stateLookupStrings[6], scope);
            addVariableToScope(state[7], stateLookupStrings[7], scope);
            addVariableToScope(state[8], stateLookupStrings[8], scope);
            addVariableToScope(state[9], stateLookupStrings[9], scope);
            addVariableToScope(state[10], stateLookupStrings[10], scope);

            /* functions to add to scope */
            addFunctionToScope(MyScriptable.class.getMethod("toastMessage", String.class), "toastMessage", scope, parentScope);
            addFunctionToScope(MyScriptable.class.getMethod("openCamera"), "openCamera", scope, parentScope);
            addFunctionToScope(MyScriptable.class.getMethod("openMusicPlayer"), "openMusicPlayer", scope, parentScope);
            addFunctionToScope(MyScriptable.class.getMethod("isWeekday", String.class), "isWeekday", scope, parentScope);
            addFunctionToScope(MyScriptable.class.getMethod("isWeekend", String.class), "isWeekend", scope, parentScope);
            addFunctionToScope(MyScriptable.class.getMethod("openWebpage", String.class), "openWebpage", scope, parentScope);
            addFunctionToScope(MyScriptable.class.getMethod("displayNotification", String.class, String.class), "displayNotification", scope, parentScope);
            addFunctionToScope(MyScriptable.class.getMethod("displayNotification", String.class), "displayNotification", scope, parentScope);
            addFunctionToScope(MyScriptable.class.getMethod("withinMeterRadiusOf", double.class, double.class, double.class), "withinMeterRadiusOf", scope, parentScope);
            addFunctionToScope(MyScriptable.class.getMethod("turnOnSilent"), "turnOnSilent", scope, parentScope);
            addFunctionToScope(MyScriptable.class.getMethod("turnOnVibrate"), "turnOnVibrate", scope, parentScope);
            addFunctionToScope(MyScriptable.class.getMethod("turnOnSound"), "turnOnSound", scope, parentScope);
            addFunctionToScope(MyScriptable.class.getMethod("sendSMS", String.class, String.class), "sendSMS", scope, parentScope);
            addFunctionToScope(MyScriptable.class.getMethod("isOnSilent"), "isOnSilent", scope, parentScope);
            addFunctionToScope(MyScriptable.class.getMethod("isOnVibrate"), "isOnVibrate", scope, parentScope);
            addFunctionToScope(MyScriptable.class.getMethod("isOnSound"), "isOnSound", scope, parentScope);

            /* obtain and return result */
            Object result = cx.evaluateString(scope, code, "<cmd>", 1, null);
            return Context.toString(result);

        } catch (Exception e) {
            System.out.println("An error occurred in script " + filename + ": " + e.getMessage());
            System.out.println(code);
        } finally {
            Context.exit();
        }

        return "";
    }

    //adds the given variable to the JS scope with the given name
    public void addVariableToScope(Object variable, String name, Scriptable scope) {
        Object wrappedVariable = Context.javaToJS(variable, scope);
        ScriptableObject.putProperty(scope, name, wrappedVariable);
    }

    //adds the given function to the JS scope with the given name
    public void addFunctionToScope(Method method, String name, Scriptable boundScope, Scriptable parentScope) {
        FunctionObject function = new MyFunctionObject(name, method, parentScope);
        boundScope.put(name, boundScope, function);
    }

    //sets the first array equal to the second if they are of the same length
    public boolean setEqualTo(Object[] first, Object[] second) {
        if (first.length == second.length) {
            for (int i = 0; i < first.length; i++) {
                first[i] = second[i];
            }
            return true;
        } else {
            return false;
        }
    }

    //returns true if the two arrays contents are equal
    public boolean isEqualTo(Object[] first, Object[] second) {
        boolean isEqual = true;
        if (first.length == second.length) {
            for (int i = 0; i < first.length; i++) {
                if (!(first[i].equals(second[i]))) {
                    isEqual = false;
                    changedStates.add(stateLookupStrings[i]);
                }
            }
        } else {
            isEqual = false;
        }
        return isEqual;
    }

    //returns a String representing the day of the week at the given index
    public static String getDayOfWeek(int dayIndex) {
        if (dayIndex == 1) {
            return "Sunday";
        } else if (dayIndex == 2) {
            return "Monday";
        } else if (dayIndex == 3) {
            return "Tuesday";
        } else if (dayIndex == 4) {
            return "Wednesday";
        } else if (dayIndex == 5) {
            return "Thursday";
        } else if (dayIndex == 6) {
            return "Friday";
        } else if (dayIndex == 7) {
            return "Saturday";
        } else {
            return "";
        }
    }

    //update a state variable by index
    public static void updateStateByIndex(int index, Object newState) {
        state[index] = newState;
    }

    //returns a state variable's value by index
    public static Object getStateByIndex(int index) {
        return state[index];
    }

    /* class extending Scriptable object which is used as an interface between our JavaScript and the Android SDK functions */
    private class MyScriptable extends ScriptableObject {
        //required override when extending ScriptableObject
        @Override
        public String getClassName() {
            return getClass().getName();
        }

        //prints message on the screen as a toast notification
        public void toastMessage(String message) {
            if (mainContext != null) {
                final CharSequence text = message;
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        int duration = Toast.LENGTH_SHORT;
                        Toast toast = Toast.makeText(mainContext, text, duration);
                        toast.show();
                    }
                });
            }
        }

        //opens the device's camera
        public void openCamera() {
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            startActivity(intent);
        }

        //opens the device's default music app or displays a selection
        public void openMusicPlayer() {
            Intent i = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        }

        //returns true if the given day is a weekday
        public boolean isWeekday(String day) {
            return !(day.charAt(0) == 'S');
        }

        //returns true if the given day is on the weekend
        public boolean isWeekend(String day) {
            return day.charAt(0) == 'S';
        }

        //opens the device's web browser with a given URL
        public void openWebpage(String url) {
            url = DownloadScript.ensureHttpInURL(url);

            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(i);
        }

        //creates a notification on the device with the given title and content
        public void displayNotification(String title, String content) {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mainContext);
            notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
            notificationBuilder.setContentTitle(title);
            notificationBuilder.setContentText(content);

            NotificationManager notificationManager = (NotificationManager)getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            notificationManager.notify(notificationID, notificationBuilder.build());
        }

        //creates a notification on the device with the given title and no content
        public void displayNotification(String title) {
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(mainContext);
            notificationBuilder.setSmallIcon(R.mipmap.ic_launcher);
            notificationBuilder.setContentTitle(title);
            notificationBuilder.setContentText("");

            NotificationManager notificationManager = (NotificationManager)getSystemService(android.content.Context.NOTIFICATION_SERVICE);
            notificationManager.notify(notificationID, notificationBuilder.build());
        }

        //returns true if the current location is within a given metre radius of a given location latitude and longitude
        public boolean withinMeterRadiusOf(double meters, double latitude, double longitude) {
            //Create Location objects to represent the two locations
            Location currentLocation = new Location(LocationManager.GPS_PROVIDER);
            Location desiredLocation = new Location(LocationManager.GPS_PROVIDER);

            //Set current location data
            currentLocation.setLatitude((double)state[7]);
            currentLocation.setLongitude((double)state[8]);

            //Set desired location data
            desiredLocation.setLatitude(latitude);
            desiredLocation.setLongitude(longitude);

            return currentLocation.distanceTo(desiredLocation) <= meters;
        }

        //turns the device's silent mode on
        public void turnOnSilent() {
            AudioManager audioManager = (AudioManager)getSystemService(android.content.Context.AUDIO_SERVICE);
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
        }

        //turns the device's vibrate mode on
        public void turnOnVibrate() {
            AudioManager audioManager = (AudioManager)getSystemService(android.content.Context.AUDIO_SERVICE);
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
        }

        //turns the device's sound mode on
        public void turnOnSound() {
            AudioManager audioManager = (AudioManager)getSystemService(android.content.Context.AUDIO_SERVICE);
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        }

        //returns true if the device is on silent mode
        public boolean isOnSilent() {
            AudioManager audioManager = (AudioManager)getSystemService(android.content.Context.AUDIO_SERVICE);
            return audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT;
        }

        //returns true if the device is on vibrate mode
        public boolean isOnVibrate() {
            AudioManager audioManager = (AudioManager)getSystemService(android.content.Context.AUDIO_SERVICE);
            return audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE;
        }

        //returns true if the device is on sound mode
        public boolean isOnSound() {
            AudioManager audioManager = (AudioManager)getSystemService(android.content.Context.AUDIO_SERVICE);
            return audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
        }

        //sends an SMS with a given message to a given number
        public void sendSMS(String destinationNumber, String message) {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(destinationNumber, null, message, null, null);
        }
    }


    /* Overridden call() method in FunctionObject so that it can handle instance methods as well as static */
    private static class MyFunctionObject extends FunctionObject {
        private MyFunctionObject(String name, Member methodOrConstructor, Scriptable parentScope) {
            super(name, methodOrConstructor, parentScope);
        }

        @Override
        public Object call(Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
            return super.call(cx, scope, getParentScope(), args);
        }
    }
}
