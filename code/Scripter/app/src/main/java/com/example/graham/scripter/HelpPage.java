package com.example.graham.scripter;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

/*The Help page of Scripter*/
public class HelpPage extends AppCompatActivity {
    //Called when the Activity is started
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help_page);

        TextView textView = (TextView) findViewById(R.id.help_page_intro);
        textView.setMovementMethod(LinkMovementMethod.getInstance());
    }

    //starts the MainActivity
    public void goToMain(View view) {
        Intent i = new Intent(this, MainActivity.class);
        startActivity(i);
    }

    //starts the ScriptsActivity
    public void goToScriptsActivity(View view) {
        Intent i = new Intent(this, ScriptsActivity.class);
        startActivity(i);
    }
}