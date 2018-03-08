package com.example.graham.scripter;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.ArrayAdapter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Scanner;

/*Service used to keep track of script files and relevant data structures*/
public class ScriptFileTracker extends Service {
    //Create data structures
    private static ArrayList<ScripterScript> javaScriptScripts;
    private static ArrayList<String> scriptFilenames;
    private static ArrayAdapter<String> arrayAdapter;
    private ArrayList<String> urlList;
    private ArrayList<Boolean> enabledPreferences;
    //Used to create an ArrayAdapter
    private android.content.Context mainContext;
    //appDir will store the path to the app's storage directory
    private static String appDir;

    public ScriptFileTracker() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /*Called when the service is started*/
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //Initialize variables and data structures
        mainContext = getBaseContext();
        appDir = getExternalFilesDir(null) + "";
        enabledPreferences = new ArrayList<>();
        urlList = new ArrayList<>();

        //If the scripts directory doesn't exist, create it
        File scriptDir = new File(appDir + "/scripts/");
        if (!scriptDir.exists()) {
            scriptDir.mkdir();
        }

        //If the scriptinfo directory doesn't exist, create it
        File prefsDir = new File(appDir + "/scriptinfo/");
        if (!prefsDir.exists()) {
            //create the directory
            prefsDir.mkdir();
        }

        //if the urls file doesn't exist, create it
        File urlsFile = new File(appDir + "/scriptinfo/urls.txt");
        if (!urlsFile.exists()) {
            try {
                //create the file
                urlsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                //read in urls and store them
                Scanner urlsInput = new Scanner(urlsFile);

                //while there is input from the file
                while (urlsInput.hasNext()) {
                    //store it in the ArrayList
                    urlList.add(urlsInput.next());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //if the enabledPrefs.txt file doesn't exist, create it
        File enabledPrefsFile = new File(appDir + "/scriptinfo/enabledPrefs.txt");
        if (!enabledPrefsFile.exists()) {
            try {
                //create the file
                enabledPrefsFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                //read in enabled preferences and store them
                Scanner enabledPrefsInput = new Scanner(enabledPrefsFile);

                //while there is input from the file
                while (enabledPrefsInput.hasNext()) {
                    //store it in the ArrayList
                    enabledPreferences.add(Boolean.valueOf(enabledPrefsInput.next()));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //multithreading
        Runnable r = new Runnable() {
            @Override
            public void run() {
                /*Instantiate ArrayLists*/
                javaScriptScripts = new ArrayList<>();
                scriptFilenames = new ArrayList<>();

                //are there existing urls?
                boolean urlsExist = !urlList.isEmpty();

                //are there preferences in place for enabled status?
                boolean prefsExist = !enabledPreferences.isEmpty();

                //get the scripts and store them as ScripterScript objects
                Context cx = Context.enter();
                cx.setOptimizationLevel(-1);

                try {
                    //Get a list of all the scripts stored on the device
                    File[] files = new File(appDir + "/scripts/").listFiles();

                    //for each file found
                    for (int i = 0; i < files.length; i++) {
                        String javaScript = "";
                        String filename = new File(files[i].toString()).getName();
                        String url = "";
                        //assume not enabled unless a saved preference is found
                        boolean isEnabled = false;

                        //store the filename for display purposes
                        scriptFilenames.add(filename);

                        //Read in the script's contents
                        Scanner input = new Scanner(new FileInputStream(new File(files[i].toString())));
                        while (input.hasNext()) {
                            javaScript += input.nextLine();
                        }

                        //if a url exists, set it
                        if (urlsExist) {
                            url = urlList.get(i);
                        }

                        //if preferences exist for enabled state, set it
                        if (prefsExist) {
                            isEnabled = enabledPreferences.get(i);
                        }

                        //Run the script in order to get it's dependencies
                        Scriptable scope = cx.initStandardObjects();
                        try {
                            cx.evaluateString(scope, javaScript, "<cmd>", 1, null);
                        } catch (Exception e) {
                            System.out.println("Error in script!");
                        }

                        //Get the script's dependencies
                        Object tmpDependencies = scope.get("dependencies", scope);
                        //if dependencies were found
                        if (tmpDependencies != Scriptable.NOT_FOUND) {
                            Object dependenciesObject = Context.jsToJava(tmpDependencies, String[].class);
                            String[] dependencies = (String[])dependenciesObject;
                            //Add the script with dependencies
                            javaScriptScripts.add(new ScripterScript(javaScript, filename, url, dependencies, isEnabled));
                        } else {
                            //Add the script with no dependencies (none were found)
                            javaScriptScripts.add(new ScripterScript(javaScript, filename, url, isEnabled));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    Context.exit();
                }

                //set an ArrayAdapter to the ListView using our ArrayList
                arrayAdapter = new ArrayAdapter<>(mainContext, android.R.layout.simple_list_item_checked, scriptFilenames);

                //update the urls file
                updateUrlsFile();

                //update the enabledPrefs file
                updateEnabledPrefsFile();
            }
        };

        //start a new thread for the service
        Thread scriptFileTrackerThread = new Thread(r);
        scriptFileTrackerThread.start();
        return Service.START_STICKY;
    }

    //returns the number of scripts currently on the device
    public static int getNumOfScripts() {
        return javaScriptScripts.size();
    }

    //returns a ScripterScript object representing a script at a given index in the ArrayList
    public static ScripterScript getScriptByIndex(int index) {
        if (index < javaScriptScripts.size() && index >= 0) {
            return javaScriptScripts.get(index);
        } else {
            return null;
        }
    }

    //returns the ArrayAdapter for use in a ListView
    public static ArrayAdapter<String> getArrayAdapter() {
        return arrayAdapter;
    }

    /*adds a ScripterScript representation of the given parameters to our javaScriptScripts ArrayList
    * and a String representation to our scriptFilenames ArrayList*/
    public static void addScript(String filename, String code, String url) {
        Context cx = Context.enter();
        cx.setOptimizationLevel(-1);
        boolean scriptExists = false;
        String trimmedCode = code.trim();

        //If the script already exists, just update it's code!
        for (int i = 0; i < javaScriptScripts.size(); i++) {
            if (javaScriptScripts.get(i).getFilename().equals(filename)) {
                scriptExists = true;
                updateCode(javaScriptScripts.get(i).getFilename(), trimmedCode);
                break;
            }
        }

        //If the script doesn't already exist, create it
        if (!scriptExists) {
            //get dependencies
            try {
                Scriptable scope = cx.initStandardObjects();
                try {
                    cx.evaluateString(scope, trimmedCode, "<cmd>", 1, null);
                } catch (Exception e) {
                    System.out.println("Error in script!");
                }
                Object tmpDependencies = scope.get("dependencies", scope);
                if (tmpDependencies != Scriptable.NOT_FOUND) {
                    Object dependenciesObject = Context.jsToJava(tmpDependencies, String[].class);
                    String[] dependencies = (String[]) dependenciesObject;
                    //add script as ScripterScript object
                    javaScriptScripts.add(new ScripterScript(trimmedCode, filename, url, dependencies));
                } else {
                    //add script as ScripterScript object without dependencies
                    javaScriptScripts.add(new ScripterScript(trimmedCode, filename, url));
                }
            } finally {
                Context.exit();
            }

            //add the filename to scriptFilenames, update UI and save enabledPrefs and urls files
            scriptFilenames.add(filename);
            //Run on main UI Thread
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(new Runnable() {
                @Override
                public void run() {
                    arrayAdapter.notifyDataSetChanged();
                }
            });
            updateUrlsFile();
            updateEnabledPrefsFile();
        }
    }

    //removes a script from both of our ArrayLists by filename
    public static void removeScript(String filename) {
        //find the script and remove it
        for (int i = 0; i < javaScriptScripts.size(); i++) {
            if (javaScriptScripts.get(i).getFilename().equals(filename)) {
                javaScriptScripts.remove(i);
                break;
            }
        }

        //remove the filename from scriptFilenames, update UI and save enabledPrefs and urls files
        scriptFilenames.remove(filename);
        arrayAdapter.notifyDataSetChanged();
        updateUrlsFile();
        updateEnabledPrefsFile();
    }

    //updates the code for a given filename to the given code
    public static void updateCode(String filename, String code) {
        //find the object with this filename and update it
        for (int i = 0; i < javaScriptScripts.size(); i++) {
            if (javaScriptScripts.get(i).getFilename().equals(filename)) {
                javaScriptScripts.get(i).setCode(code);
                break;
            }
        }
    }

    //renames a given filename to a new filename
    public static boolean renameScript(String oldFilename, String newFilename) {
        boolean newFileExists = false;
        boolean fileRenamed = false;

        //If the newFilename already exists, don't rename the old one
        for (int i = 0; i < javaScriptScripts.size(); i++) {
            if (javaScriptScripts.get(i).getFilename().equals(newFilename)) {
                newFileExists = true;
            }
        }

        if (!newFileExists) {
            //find the object with the oldFilename and update it
            for (int j = 0; j < javaScriptScripts.size(); j++) {
                if (javaScriptScripts.get(j).getFilename().equals(oldFilename)) {
                    javaScriptScripts.get(j).setFilename(newFilename);
                    scriptFilenames.set(j, newFilename);
                    arrayAdapter.notifyDataSetChanged();
                    break;
                }
            }

            //rename the file in the device's storage
            File oldFile = new File(appDir + "/scripts/" + oldFilename);
            File newFile = new File(appDir + "/scripts/" + newFilename);

            fileRenamed = oldFile.renameTo(newFile);
        }

        return fileRenamed;
    }

    //update a script's enabled value to the given boolean
    public static void updateScriptStatusByIndex(int index, boolean enabled) {
        javaScriptScripts.get(index).setEnabled(enabled);
        updateUrlsFile();
        updateEnabledPrefsFile();
    }

    //updates the urls.txt file with script urls
    private static void updateUrlsFile() {
        /*Save url for each script*/
        File urlsFile = new File(appDir + "/scriptinfo/urls.txt");

        try {
            OutputStream outputStream = new FileOutputStream(urlsFile);
            OutputStreamWriter output = new OutputStreamWriter(outputStream);

            //for every script
            for (int i = 0; i < javaScriptScripts.size(); i++) {
                //write it's url
                output.write(javaScriptScripts.get(i).getUrl());
                output.write(" ");
            }

            //close streams
            output.flush();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //updates the enabledPrefs.txt file with current enabled preferences
    private static void updateEnabledPrefsFile() {
        /*Save enabled preferences for each script*/
        File enabledPrefsFile = new File(appDir + "/scriptinfo/enabledPrefs.txt");

        try {
            OutputStream outputStream = new FileOutputStream(enabledPrefsFile);
            OutputStreamWriter output = new OutputStreamWriter(outputStream);

            //for every script
            for (int i = 0; i < javaScriptScripts.size(); i++) {
                //write it's enabled status
                output.write(javaScriptScripts.get(i).isEnabled() + "");
                output.write(" ");
            }

            //close streams
            output.flush();
            output.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
