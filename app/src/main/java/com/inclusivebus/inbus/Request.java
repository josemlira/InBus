package com.inclusivebus.inbus;


import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ActivityNotFoundException;
import android.content.pm.PackageManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.speech.RecognizerIntent;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewDebug;
import android.widget.Button;
import android.widget.EditText;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.Manifest;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

public class Request extends AppCompatActivity {

    LocationManager locationManager;
    double longitudeNetwork = 0.0;
    double latitudeNetwork = -3.3;
    EditText txtmicro;
    private static String address = null;
    Button bgorec;
    String URL_paradero;
    String micro;
    TextView tv_loc;
    Button bback;
    private ConnectedThread MyConexionBT;
    public String parada_cercana;
    public double minima_distacia = 999999999.9;
    Handler bluetoothIn;
    final int handlerState = 0;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private int REQUEST_ENABLE_BT = 1;
    private final int REQ_CODE_SPEECH_INPUT = 100;
    private ImageButton btnSpeak;
    private ProgressDialog progress;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        //getActionBar().hide(); esconde la barra de arriba de la app
        txtmicro = (EditText) findViewById(R.id.query_micro);
        bgorec = (Button) findViewById(R.id.button_gorecorrido);
        bback = (Button) findViewById(R.id.button_back);
        tv_loc = (TextView) findViewById(R.id.text_query);
        btnSpeak = (ImageButton) findViewById(R.id.mic);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        VerificarBT();

        //Toast.makeText(getApplicationContext(), "HOLA", Toast.LENGTH_SHORT).show();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        toggleNetworkUpdates();
        //getLocation();

        //boton para leer la micro
        bgorec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                parada_cercana = "PA343";
                micro = txtmicro.getText().toString();
                if (isCorrecta(parada_cercana, micro)) {
                    bgorec.setEnabled(false);
                    progress = new ProgressDialog(Request.this);
                    progress.setTitle("Esperando bus " + micro);
                    progress.setCancelable(false);
                    progress.show();
                    /*
                    Consultar Cons = new Consultar(micro);
                    Cons.start();
                    */
                    Timer timer = new Timer(true);
                    timer.scheduleAtFixedRate(new Tarea(0), 0, 5000);
                } else {
                    Toast.makeText(getBaseContext(), "La micro indicada no pasa por este paradero", Toast.LENGTH_SHORT).show();
                }
                txtmicro.setText("");
            }
        });

        //boton para volver
        bback.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent go = new Intent(Request.this, Intermediate.class);
                go.putExtra(MainActivity.EXTRA_DEVICE_ADDRESS, address);
                startActivity(go);
                finish();
            }
        });

        btnSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                promptSpeechInput();
            }
        });


    }

    public void onResume() {
        super.onResume();
        Intent intent = getIntent();
        address = intent.getStringExtra(MainActivity.EXTRA_DEVICE_ADDRESS);
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Hubo un problema", Toast.LENGTH_SHORT).show();
        }
        try {
            btSocket.connect();
        } catch (IOException e) {
            try {
                btSocket.close();
            } catch (IOException e2) { }
        }
        if (btSocket != null) {
            MyConexionBT = new ConnectedThread(btSocket);
            MyConexionBT.start();
        }
    }

    public void onPause() {
        super.onPause();
        try {
            btSocket.close();
        } catch (IOException e) { }
    }

    //probar que esta activada la localización
    private boolean checkLocation() {
        if (!isLocationEnabled())
            showAlert();
        return isLocationEnabled();
    }

    //Mostrar alerta en caso de no tener la ubicación activada
    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Enable Location")
                .setMessage("Su ubicación esta desactivada.\npor favor active su ubicación ")
                .setPositiveButton("Configuración de ubicación", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(myIntent);
                    }
                })
                .setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    }
                });
        dialog.show();
    }

    //método que detecta si está activa la ubicación
    private boolean isLocationEnabled() {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public void toggleNetworkUpdates() {
        if (!checkLocation())
            return;
        else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            } else {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER, 20 * 1000, 10, locationListenerNetwork);
                //Toast.makeText(this, "Network provider started running", Toast.LENGTH_SHORT).show();
                //String lat = String.valueOf(latitudeNetwork);
                Toast.makeText(getBaseContext(), parada_cercana, Toast.LENGTH_SHORT).show();
            }
        }
    }

    //método que busca lat y lon
    private final LocationListener locationListenerNetwork = new LocationListener() {
        public void onLocationChanged(Location location) {
            longitudeNetwork = location.getLongitude();
            latitudeNetwork = location.getLatitude();
            JSONArray result;
            result = getLocation();
            String aux = "";
            for (int i =0; i < result.length(); i++){
                JSONObject paradero = null;
                try {
                    paradero = result.getJSONObject(i);
                    double dist = paradero.getDouble("distancia");
                    if(dist < minima_distacia){
                        minima_distacia = dist;
                        aux = paradero.getString("cod");

                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            parada_cercana = aux;
            //Toast.makeText(getBaseContext(), parada_cercana, Toast.LENGTH_SHORT).show();
            /*
            //no se si este codigo nos sirve a nosotros, los override de abajo tampoco
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    longitudeValueNetwork.setText(longitudeNetwork + "");
                    latitudeValueNetwork.setText(latitudeNetwork + "");
                }

            }); */
        }
            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {

            }
            @Override
            public void onProviderDisabled(String s) {
            }
        };

    public JSONArray getLocation(){
        String lat = String.valueOf(latitudeNetwork);
        String lon = String.valueOf(longitudeNetwork);
        URL_paradero = "http://www.transantiago.cl/restservice/rest/getpuntoparada?lat=" + lat +
                "&lon=" + lon + "&bip=1";
        String resultado_paradero = null;
        GetRequest getreq = new GetRequest();
        JSONArray json;

        try {
            resultado_paradero = getreq.execute(URL_paradero).get();
            //Toast.makeText(getBaseContext(),resultado_paradero,Toast.LENGTH_SHORT).show();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        try {
            json = new JSONArray(resultado_paradero);
        } catch (JSONException e) {
            e.printStackTrace();
            json = null;
        }
        return json;


    };

    public Integer searchMicro(String paradero, String micro) {
        String url = "http://www.transantiago.cl/predictor/prediccion?codsimt=" + paradero + "&codser=" + micro; //NO
        Integer distancia; //NO
        GetRequest getreq = new GetRequest(); //NO
        JSONObject json;
        String result_get = null;
        try {
            result_get = getreq.execute(url).get();
        } catch (InterruptedException e) { } catch (ExecutionException e) { }
        try {
            json = new JSONObject(result_get);
            distancia = json.getJSONObject("servicios").getJSONArray("item").getJSONObject(0).getInt("distanciabus1");
            if (distancia == 0) {
                return 999999;
            }
            return distancia;
        } catch (JSONException e) {
            e.printStackTrace();
            return 999999;
        }


    };

    public boolean isCorrecta(String paradero, String micro) {
        String url = "http://www.transantiago.cl/restservice/rest/getservicios/parada?codsimt=" + paradero;
        GetRequest getreq = new GetRequest();
        JSONArray array;
        boolean correct = false;
        String result_get = null;
        try {
            result_get = getreq.execute(url).get();
        } catch (InterruptedException e) { } catch (ExecutionException e) { }
        //Toast.makeText(getBaseContext(),result_get, Toast.LENGTH_SHORT).show();
        try {
            array = new JSONArray(result_get);
            for (int i = 0; i < array.length(); i++) {
                JSONObject ii = array.getJSONObject(i);
                if (ii.getString("cod").equals(micro)) {
                    correct = true;
                }
            }
            return correct;
        } catch (JSONException e) {
            return correct;
        }

    }

    private void VerificarBT() {
        if (btAdapter == null) {
            Toast.makeText(getBaseContext(), "El dispositivo no es compatible con bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (!btAdapter.isEnabled()) {
                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBT, REQUEST_ENABLE_BT);
            }
        }
    }

    public static class GetRequest extends AsyncTask<String, Void, String>
    {
        private static final String req_meth = "GET";
        private static final int read_to = 15000;
        private static final int connect_to = 15000;

        @Override
        protected String doInBackground(String... params){
            String stringurl = params[0];
            String result;
            String inputLine;

            try {
                URL myUrl = new URL(stringurl);
                HttpURLConnection connection = (HttpURLConnection) myUrl.openConnection();

                connection.setRequestMethod(req_meth);
                connection.setReadTimeout(read_to);
                connection.setConnectTimeout(connect_to);

                connection.connect();

                InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());
                BufferedReader reader = new BufferedReader(streamReader);
                StringBuilder stringBuilder = new StringBuilder();

                while((inputLine = reader.readLine()) != null){
                    stringBuilder.append(inputLine);
                }

                reader.close();
                streamReader.close();

                result = stringBuilder.toString();
            }

            catch (IOException e){
                e.printStackTrace();
                result = null;
            }

            return result;
        }

        @Override
        protected void onPostExecute(String result){
            super.onPostExecute(result);
        }

    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }

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

        public void write(String input) throws ConnectException {
            try {
                mmOutStream.write(input.getBytes());
            } catch (IOException e) {
                throw new ConnectException("Fallo");
            }
        }
    }


    private class Tarea extends TimerTask {
        int distance;
        int distancia_minima = 300;
        int buffer;

        public Tarea(Integer buf) {
            buffer = buf;
        }

        @Override
        public void run() {
            distance = searchMicro(parada_cercana, micro);
            if (distance == 999999) {
                Request.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setMessage("Distancia: " + String.valueOf(buffer) + " (conectando)");
                    }
                });
                Reconnect other_thread = new Reconnect(buffer);
                other_thread.start();
                this.cancel();
            } else if (distance <= distancia_minima) {
                Vibrar vib = new Vibrar();
                vib.start();
                progress.dismiss();
                this.cancel();
            } else {
                Request.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progress.setMessage("Distancia: " + String.valueOf(distance));
                    }
                });
                buffer = distance;
            }
        }
    }



    private class Reconnect extends Thread {

        int buffer;

        public Reconnect(Integer buff) {
            buffer = buff;
            this.setDaemon(true);
        }

        public void run() {
            try {
                this.sleep(7000);
                Timer tim = new Timer(true);
                tim.scheduleAtFixedRate(new Tarea(buffer), 0, 5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }



    private class Vibrar extends Thread {

        private boolean success = true;

        public void run() {
            try {
                for (int i = 0; i < 2; i++) {
                    MyConexionBT.write("A");
                    this.sleep(500);
                    MyConexionBT.write("B");
                    this.sleep(500);
                }
                Request.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        bgorec.setEnabled(true);
                    }
                });
            } catch (InterruptedException e) { } catch (ConnectException e) {
                Request.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"La conexión falló", Toast.LENGTH_SHORT).show();
                    }
                });
                success = false;
                Intent again = new Intent(Request.this, MainActivity.class);
                startActivity(again);
                finish();
            } finally {
                if (success) {
                    Request.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Ha llegado tu bus", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }
    }


    /*
    private class Consultar extends Thread {

        private final String mic;
        private final Integer distancia_minima;
        private int distance;
        private boolean success = true;

        public Consultar(String micro) {
            mic = micro;
            distancia_minima = 500;
        }

        class Correr implements Runnable {

            final private int val;

            public Correr(Integer value) {
                val = value;
            }
            public void run() {
                progress.setMessage(String.valueOf(val));
            }
        }

        public void run() {
            distance = searchMicro(parada_cercana, mic);
            if (distance == 999999) {
                Request.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getBaseContext(), "No hay micro", Toast.LENGTH_SHORT).show();
                    }
                });
                progress.dismiss();
                return;
            }
            int v = 1;
            while (distance > distancia_minima) {
                try {
                    this.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                distance = searchMicro(parada_cercana, mic);
                Request.this.runOnUiThread(new Correr(v));
                v++;
            }
            try {
                for (int i = 0; i<2; i++) {
                    MyConexionBT.write("A");
                    this.sleep(500);
                    MyConexionBT.write("B");
                    this.sleep(500);
                }
                bgorec.setEnabled(true);
                progress.dismiss();
            } catch (InterruptedException e) { } catch (ConnectException e) {
                Request.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"La conexión falló", Toast.LENGTH_SHORT).show();
                    }
                });
                success = false;
                progress.dismiss();
                Intent again = new Intent(Request.this, MainActivity.class);
                startActivity(again);
                finish();
            } finally {
                if (success) {
                    Request.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Ha llegado tu bus", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        }
    } */

    private void promptSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Habla ...");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(), "Tu dispositivo no soporta lector de voz", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if ((resultCode == RESULT_OK) && (data != null)) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    txtmicro.setText(result.get(0).replace(" ",""));
                }
            }
        }
    }
}


