package com.inclusivebus.inbus;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Intermediate extends AppCompatActivity {

    Button btest;
    Button bapp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intermediate);

        btest = (Button) findViewById(R.id.button_test);
        bapp = (Button) findViewById(R.id.button_app);
        btest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent go = new Intent(Intermediate.this, Search.class);
                startActivity(go);
                finish();
            }
        });



        // Por ahora el bapp no hace nada
    }
}
