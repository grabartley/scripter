package com.example.graham.scripter;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.webkit.URLUtil;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;


/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * Used for downloading files onto the device
 */
public class DownloadScript extends IntentService {
    //used to interact with UI
    private Context mainContext;

    public DownloadScript() {
        super("DownloadScript");
    }

    //Called when the service is started
    @Override
    protected void onHandleIntent(Intent intent) {
        //used to interact with UI
        mainContext = getBaseContext();

        //get filename and url from the Intent
        String urlString = ensureHttpInURL(intent.getStringExtra("url"));
        String filename = intent.getStringExtra("filename");

        //if no filename supplied, guess the filename
        if (filename.equals("")) {
            filename = URLUtil.guessFileName(urlString, null, null);
        }

        //ensure the filename ends in .js
        ensureJsInFilename(filename);

        try {
            //Open a connection to the URL
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            connection.connect();

            //Create input/output objects
            InputStream input = new BufferedInputStream(connection.getInputStream());
            File file = new File(getExternalFilesDir(null) + "/scripts/" + filename);
            OutputStream output = new FileOutputStream(file);

            //respond to the user
            toastMessage("Downloading...");

            byte[] data = new byte[1024];
            long total = 0;
            int count;

            //While there's data to read, read it and write it to the file
            while ((count = input.read(data)) != -1) {
                total += count;
                output.write(data, 0, count);
            }

            //respond to the user
            toastMessage("Your script has been downloaded");

            //add the script to our tracker
            ScriptFileTracker.addScript(filename, new String(data), urlString);

            //close streams
            output.flush();
            output.close();
            input.close();
        } catch (IOException e) {
            //Inform the user that an error occurred
            toastMessage("An error occurred");
            e.printStackTrace();
        }
    }

    //checks if a URL includes http:// or https:// and if not, it adds http://
    public static String ensureHttpInURL(String url) {
        //check if URL contains at least 8 characters
        if (url.length() > 7) {
            //check if URL begins with http:// or https://, if not add http://
            if (!url.substring(0, 7).equals("http://") && !url.substring(0, 8).equals("https://")) {
                url = "http://" + url;
            }
        } else {
            //it's not long enough to contain one so add it
            url = "http://" + url;
        }

        return url;
    }

    //check if .js is at the end of the given filename ad if not, add it
    public static String ensureJsInFilename(String filename) {
        //if the length is at least 3 then it may contain ".js"
        if (filename.length() >= 3) {
            if (!(filename.substring(filename.length() - 3).equals(".js"))) {
                filename += ".js";
            }
        } else {
            filename += ".js";
        }

        return filename;
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
}
