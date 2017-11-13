package com.inclusivebus.inbus;


import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
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
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

import javax.net.ssl.HttpsURLConnection;

public class Request extends AppCompatActivity {

    LocationManager locationManager;
    double longitudeNetwork, latitudeNetwork;
    EditText txtmicro;
    private static String address = null;
    Button bgorec;
    String URL_paradero;
    String URL_micro;
    String cod_paradero;
    String micro;
    TextView tv_loc;
    Button bback;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_request);

        txtmicro = (EditText) findViewById(R.id.query_micro);
        bgorec = (Button) findViewById(R.id.button_gorecorrido);
        bback = (Button) findViewById(R.id.button_back);
        tv_loc = (TextView) findViewById(R.id.tv_loc);
        Intent intent = getIntent();
        address = intent.getStringExtra(MainActivity.EXTRA_DEVICE_ADDRESS);
        toggleNetworkUpdates();
        getLocation();

        //boton para leer la micro
        bgorec.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                micro = txtmicro.getText().toString();
            }
        });
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);


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
                Toast.makeText(this, "Network provider started running", Toast.LENGTH_LONG).show();
            }
        }
    }

    //método que busca lat y lon
    private final LocationListener locationListenerNetwork = new LocationListener() {
        public void onLocationChanged(Location location) {
            longitudeNetwork = location.getLongitude();
            latitudeNetwork = location.getLatitude();

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

    public String getLocation(){
        String lat = String.valueOf(latitudeNetwork);
        String lon = String.valueOf(longitudeNetwork);
        URL_paradero = "http://www.transantiago.cl/restservice/rest/getpuntoparada?lat=" + lat +
                "&lon=" + lon + "&bip=1";
        String resultado_paradero = null;
        GetRequest getreq = new GetRequest();

        try {
            resultado_paradero = getreq.execute(URL_paradero).get();
            Toast.makeText(getBaseContext(),resultado_paradero,Toast.LENGTH_LONG).show();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        return resultado_paradero;


    };

    public void searchMicro(){

    };

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
}


