package com.inclusivebus.inbus;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Intermediate extends AppCompatActivity {

    Button btest;
    Button bapp;
    private static String address = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intermediate);

        Intent intent = getIntent();
        address = intent.getStringExtra(MainActivity.EXTRA_DEVICE_ADDRESS);
        btest = (Button) findViewById(R.id.button_test);
        bapp = (Button) findViewById(R.id.button_app);
        btest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent go = new Intent(Intermediate.this, Search.class);
                go.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, address);
                startActivity(go);
                finish();
            }
        });
        //Apretar el bapp y cambiar de layout
        bapp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent go = new Intent(Intermediate.this, Request.class);
                // Esto es para pasar la dirección MAC del dispositivo con el que nos estamos conectando al otro layout
                go.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, address);
                startActivity(go);
                finish();
            }
        });

    }
}
