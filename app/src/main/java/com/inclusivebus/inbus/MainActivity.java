package com.inclusivebus.inbus;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.Set;
import java.util.concurrent.locks.Lock;


public class MainActivity extends AppCompatActivity {

    Button bconnect;
    private BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
    boolean enableBT = false;
    private int REQUEST_ENABLE_BT = 1;
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    public static String VOICE_ACTIVE = "active_voice";
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bconnect = (Button) findViewById(R.id.button_connect);
        bconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Establecer conexión BLUETOOTH
                VerificarBT();
            }
        });
    }

    private void Vincular() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Selecciona un dispositivo");
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        final CharSequence[] items = new CharSequence[pairedDevices.size()];
        if (pairedDevices.size() > 0) {
            int cont = 0;
            for (BluetoothDevice device : pairedDevices) {
                items[cont] = device.getName() + "\n" + device.getAddress();
                cont++;
                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        String info = items[i].toString();
                        String address = info.substring(info.length() - 17);
                        Intent try_connect = new Intent(MainActivity.this, Intermediate.class);
                        Toast msg = Toast.makeText(getApplicationContext(), "Vinculando con el dispositivo", Toast.LENGTH_SHORT);
                        msg.show();
                        try_connect.putExtra(EXTRA_DEVICE_ADDRESS, address);
                        startActivity(try_connect);
                        finish();
                    }
                });
            }
        } else {
            builder.setMessage("No hay dispositivos vinculados");
        }
        AlertDialog caption = builder.create();
        caption.show();
    }

    //REVISAR ESTO, NO ESTA CORRIENDO
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Vincular();
            }
        }
    }

    private void VerificarBT() {
        if (btAdapter == null) {
            Toast alerta = Toast.makeText(getApplicationContext(), "El dispositivo no es compatible con bluetooth", Toast.LENGTH_SHORT);
            alerta.show();
        } else {
            if (!btAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            } else {
                Vincular();
            }
        }
    }
}
