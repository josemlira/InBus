package com.inclusivebus.inbus;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class Search extends AppCompatActivity {

    Button bgoback;
    Button bsignal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        bgoback = (Button) findViewById(R.id.button_goback);
        bsignal = (Button) findViewById(R.id.button_signal);
        bgoback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent go = new Intent(Search.this, Intermediate.class);
                startActivity(go);
                finish();
            }
        });
    }
}
