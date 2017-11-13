package com.inclusivebus.inbus;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Search extends AppCompatActivity {

    Button bgoback;
    Button bsignal;
    //Identificador de la conexion
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //Direccion MAC
    private static String address = null;
    Handler bluetoothIn;
    private ConnectedThread MyConexionBT;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder DataStringIN = new StringBuilder();
    final int handlerState = 0;
    private int REQUEST_ENABLE_BT = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        bgoback = (Button) findViewById(R.id.button_goback);
        bsignal = (Button) findViewById(R.id.button_signal);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        VerificarBT();


        bgoback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent go = new Intent(Search.this, Intermediate.class);
                go.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, address);
                startActivity(go);
                finish();
            }
        });

        bsignal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    MyConexionBT.doble();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

    public void onResume() {
        super.onResume();
        Intent intent = getIntent();
        address = intent.getStringExtra(MainActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Hubo un problema", Toast.LENGTH_LONG).show();
        }
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) { }
        }
        MyConexionBT = new ConnectedThread(btSocket);
        MyConexionBT.start();
    }

    public void onPause() {
        super.onPause();
        try {
            btSocket.close();
        } catch (IOException e) { }
    }

    private void VerificarBT() {
        if (btAdapter == null) {
            Toast.makeText(getBaseContext(), "El dispositivo no es compatible con bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (!btAdapter.isEnabled()) {
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT, REQUEST_ENABLE_BT);
            }
        }
    }

// PRIMERO PROBAR QUE FUNCIONE LA VIBRACION
/*
    private class SendMessage extends Thread {

        private final ConnectedThread writter;

        public SendMessage(ConnectedThread thr) {
            writter = thr;
        }

        public void run() {
            try {
                for (int i = 0; i < 2; i++) {
                    writter.write("A");
                    this.sleep(500);
                    writter.write("B");
                    this.sleep(500);
                }
            } catch (InterruptedException e) { }
        }
    }
*/
    private class ConnectedThread extends Thread {

        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket sock) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = sock.getInputStream();
                tmpOut = sock.getOutputStream();
            } catch (IOException e) { }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    String readMessage = new String(buffer, 0, bytes);
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(String input) {
            try {
                mmOutStream.write(input.getBytes());
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "La conexión falló", Toast.LENGTH_LONG).show();
                finish();
            }
        }

        public void doble() throws InterruptedException {
            for(int i = 0; i<2; i++){
                this.write("A");
                this.sleep(500);
                this.write("B");
                this.sleep(500);
            }
        }
    }
}