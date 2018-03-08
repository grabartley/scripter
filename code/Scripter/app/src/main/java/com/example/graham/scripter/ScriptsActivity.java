package com.example.graham.scripter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

/*The Scripts page of Scripter*/
public class ScriptsActivity extends AppCompatActivity {
    //Create a ListView to display the script names in
    private ListView listView;
    //Used to interact with the UI
    private Context mainContext;

    //Starts the MainActivity
    public void goToMain(View view) {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }

    //Starts the HelpPage
    public void goToHelpPage(View view) {
        Intent i = new Intent(this, HelpPage.class);
        startActivity(i);
    }

    //Called when the Activity is started
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scripts);

        //Used for toast messages
        mainContext = getBaseContext();

        //Get the ListView from the layout
        listView = (ListView)findViewById(R.id.scriptList);
        //Set it's adapter to be the ArrayAdapter we created (Will contain script names)
        listView.setAdapter(ScriptFileTracker.getArrayAdapter());

        //initialize enabled states for each script
        for (int i = 0; i < ScriptFileTracker.getNumOfScripts(); i++) {
            listView.setItemChecked(i, ScriptFileTracker.getScriptByIndex(i).isEnabled());
        }

        //set a click listener to register when scripts are toggled
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //set enabled status
                ScriptFileTracker.updateScriptStatusByIndex(position, listView.getCheckedItemPositions().get(position));
            }
        });

        //When an item is long clicked, prompt the user with file options
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                //Build an AlertDialog
                final String filename = (String)parent.getItemAtPosition(position);
                //Give names to the options in the AlertDialog
                final CharSequence[] options = {"Re-download", "Rename", "View Script Online", "Delete"};
                final ArrayList<Integer> choice = new ArrayList<Integer>();
                choice.add(-1);
                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(ScriptsActivity.this);
                alertDialogBuilder.setTitle("Script Options");
                alertDialogBuilder.setCancelable(true);
                //Add radio buttons with none selected by default
                alertDialogBuilder.setSingleChoiceItems(options, -1, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //save which radio button was selected
                        choice.set(0, which);
                    }
                });

                //what happens when the user selects Confirm
                alertDialogBuilder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //If the user selected Re-download
                        if (choice.get(0) == 0) {
                            //Start the DownloadScript service with the script's filename and url
                            Intent i = new Intent(mainContext, DownloadScript.class);
                            i.putExtra("filename", filename);
                            i.putExtra("url", ScriptFileTracker.getScriptByIndex(position).getUrl());
                            startService(i);
                        //if the user selected rename
                        } else if (choice.get(0) == 1) {
                            //Create a second alert to get the new filename
                            AlertDialog.Builder renameAlertBuilder = new AlertDialog.Builder(ScriptsActivity.this);
                            renameAlertBuilder.setTitle("Rename Script");
                            renameAlertBuilder.setMessage("Please enter a new name for " + filename);
                            renameAlertBuilder.setCancelable(true);

                            //Add an EditText for text entry
                            final EditText filenameInput = new EditText(ScriptsActivity.this);
                            filenameInput.setHint("Enter a new filename...");
                            renameAlertBuilder.setView(filenameInput);

                            //What to do when the Rename button is pressed
                            renameAlertBuilder.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //Get the value for the new filename from the EditText
                                    String newFilename = filenameInput.getText().toString();
                                    //If a value has been supplied
                                    if (newFilename != "") {
                                        //ensure new filename ends in ".js"
                                        newFilename = DownloadScript.ensureJsInFilename(newFilename);

                                        //rename the script
                                        if(!ScriptFileTracker.renameScript(filename, newFilename)) {
                                            Toast.makeText(mainContext, newFilename + " already exists or could not be created", Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        //No filename was entered, cancel the dialog and inform the user of why
                                        dialog.cancel();
                                        Toast.makeText(mainContext, "Please enter a new filename", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });

                            //What to do when the Cancel button is pressed
                            renameAlertBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });

                            //Build and show the alert
                            renameAlertBuilder.create().show();
                        //if the user selected view script online
                        } else if (choice.get(0) == 2) {
                            //Open the script's url in the device's default browser
                            String url = ScriptFileTracker.getScriptByIndex(position).getUrl();
                            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            startActivity(i);
                        //if the user selected delete
                        } else if (choice.get(0) == 3) {
                            //Remove the script from data structures
                            ScriptFileTracker.removeScript(filename);
                            //Delete the file from the device's storage and inform the user that it has been deleted
                            new File(getExternalFilesDir(null) + "/scripts/" + filename).delete();
                            Toast.makeText(mainContext, filename + " deleted", Toast.LENGTH_SHORT).show();
                        //if no option selected
                        } else {
                            //Cancel the dialog and tell the user to make a selection
                            dialog.cancel();
                            Toast.makeText(mainContext, "Please make a selection", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                //what happens when the user selects Cancel
                alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                //create the alert, set it's title and show it
                AlertDialog alert = alertDialogBuilder.create();
                alert.show();

                //return true so as not to pass a regular click event after this one
                return true;
            }
        });
    }
}
