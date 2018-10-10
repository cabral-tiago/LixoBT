package pt.iefp.iot.lixobt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Referência para o bluetooth http://android-er.blogspot.com/2015/10/android-communicate-with-arduino-hc-06.html

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myUUID = UUID.fromString(UUID_STRING_WELL_KNOWN_SPP);

        //Definir objectos do ecrã
        btnLigar = findViewById(R.id.imgbtnLigar);
        btnLigar.setOnClickListener(btnLigar_click);
        btnDesligar = findViewById(R.id.imgbtnDesligar);
        btnDesligar.setOnClickListener(btnDesligar_click);
        btnAbrir = findViewById(R.id.imgbtnAbrir);
        btnAbrir.setOnClickListener(btnAbrir_click);
        btnFechar = findViewById(R.id.imgbtnFechar);
        btnFechar.setOnClickListener(btnFechar_click);
        imgLixo = findViewById(R.id.imageView);
        imgLixo.setImageResource(R.drawable.lixo0);
        txtEstado = findViewById(R.id.txtEstado);
        txtLixo = findViewById(R.id.txtLixo);
        progressBar = findViewById(R.id.progressBar);


        btnFechar.setClickable(false);

        //para xxhdpi com alturas superiores às standard... em especifico para todos os xxhdpi de 1080x2160. Não é uma solução muito boa!

        DisplayMetrics displayMetrics = getApplicationContext().getResources().getDisplayMetrics();
        float dpHeight = displayMetrics.heightPixels / displayMetrics.density;
        int dpInt = Math.round(dpHeight);

        if(dpInt==738){
            final ViewGroup.MarginLayoutParams mar =(ViewGroup.MarginLayoutParams)txtLixo.getLayoutParams();
            mar.setMargins(mar.leftMargin,mar.leftMargin,mar.rightMargin,190);
            txtLixo.setLayoutParams(mar);
        }


        progressBarDrawable = (LayerDrawable) progressBar.getProgressDrawable();
        backgroundDrawable = progressBarDrawable.getDrawable(0);
        progressDrawable = progressBarDrawable.getDrawable(1);
        backgroundDrawable.setColorFilter(Color.parseColor("#e6e6e6"), PorterDuff.Mode.SRC_IN);

        //Pedir para ligar o Bluetooth
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
    }

    //Desconectar ao sair
    @Override
    protected void onDestroy() {
        super.onDestroy();

        if(myThreadConnectBTdevice!=null){
            myThreadConnectBTdevice.cancel();
        }
    }

    // Fechar se o bluetooth não for ligado
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode==REQUEST_ENABLE_BT){
            if(resultCode != Activity.RESULT_OK){
                Toast.makeText(this,
                        "É necessário ativar o bluetooth",
                        Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    // Objectos
    private ImageView imgLixo;
    private ImageButton btnAbrir;
    private ImageButton btnFechar;
    private ImageButton btnLigar;
    private ImageButton btnDesligar;
    private TextView txtEstado;
    private TextView txtLixo;
    private ProgressBar progressBar;



    // Cliques de botão

    private View.OnClickListener btnLigar_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ligar();
        }
    };

    private View.OnClickListener btnDesligar_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            desligar();
        }
    };

    private View.OnClickListener btnAbrir_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            abrir();
        }
    };

    private View.OnClickListener btnFechar_click = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            fechar();
        }
    };

    // Menus

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case R.id.menuItem1:
                menu1();
                break;
            case R.id.menuItem2:
                menu2();
                break;
            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    private void menu1(){
        AlertDialog.Builder dialogoCreditos = new AlertDialog.Builder(MainActivity.this);
        dialogoCreditos.setTitle("Créditos")
                .setMessage("Aplicação Android:\nTiago Cabral\n\nDesenvolvimento em Arduino:\nAntídio Costa")
                .setNeutralButton("Fechar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    private void menu2(){
        AlertDialog.Builder dialogoSobre = new AlertDialog.Builder(MainActivity.this);
        dialogoSobre.setTitle("Sobre")
                .setMessage("Smart Dustbin\nVersão 1.0\n\nIoT Turma B 2018 - IEFP Aveiro")
                .setNeutralButton("Fechar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    // Variaveis

    LayerDrawable progressBarDrawable = null;
    Drawable backgroundDrawable = null;
    Drawable progressDrawable = null;



    // Funções

    private void abrir(){
        btnAbrir.setVisibility(View.GONE);
        btnFechar.setVisibility(View.VISIBLE);

        byte[] bytesToSend = "A".getBytes();
        myThreadConnected.write(bytesToSend);

    }

    private void fechar(){
        btnAbrir.setVisibility(View.VISIBLE);
        btnFechar.setVisibility(View.GONE);

        byte[] bytesToSend = "B".getBytes();
        myThreadConnected.write(bytesToSend);
    }

    private void ligar(){
        escolherDispositivo();
    }

    private void desligar(){
        btnDesligar.setVisibility(View.GONE);
        btnAbrir.setVisibility(View.GONE);
        btnFechar.setVisibility(View.GONE);
        btnLigar.setVisibility(View.VISIBLE);
        txtEstado.setText(R.string.estado_off);

        if(myThreadConnectBTdevice!=null){
            myThreadConnectBTdevice.cancel();
            myThreadConnectBTdevice = null;
        }
        else{
            //TODO já não deveria chegar aqui nesta versão da app, mas só por precaução e para testes...
            Toast.makeText(getApplicationContext(),"Erro: Não existe ligação", Toast.LENGTH_LONG).show();
        }
    }

    private void atualizarLixo(String str){
        str = str.trim();
        String straberto = str.substring(str.length()-1);
        String strnum = str.substring(0, str.length()-1);

        int num = 0;
        try{
            num = Integer.parseInt(strnum);
        }
        catch(NumberFormatException e){
            //nada
        }

        txtLixo.setText(num + " %");
        progressBar.setProgress(num);
        if(num<25){
            imgLixo.setImageResource(R.drawable.lixo0);
            progressDrawable.setColorFilter(Color.parseColor("#b9f6ca"), PorterDuff.Mode.SRC_IN);
        }
        if(num>=25 & num<50){
            imgLixo.setImageResource(R.drawable.lixo25);
            progressDrawable.setColorFilter(Color.parseColor("#b9f6ca"), PorterDuff.Mode.SRC_IN);
        }
        if(num>=50 & num<75){
            imgLixo.setImageResource(R.drawable.lixo50);
            progressDrawable.setColorFilter(Color.parseColor("#b9f6ca"), PorterDuff.Mode.SRC_IN);
        }
        if(num>=75 & num<100){
            imgLixo.setImageResource(R.drawable.lixo75);
            progressDrawable.setColorFilter(Color.parseColor("#ff9900"), PorterDuff.Mode.SRC_IN);
        }
        if(num==100){
            imgLixo.setImageResource(R.drawable.lixo100);
            progressDrawable.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
        }

        if(straberto.equals("A")){
            btnAbrir.setVisibility(View.GONE);
            btnFechar.setVisibility(View.VISIBLE);
        }
        if(straberto.equals("B")){
            btnFechar.setVisibility(View.GONE);
            btnAbrir.setVisibility(View.VISIBLE);
        }

    }

    public void escolherDispositivo(){

        //TODO se possivel mudar para encontrar todos os dispositivos, provavelmente trabalhoso

        //Limpar dispositivo seleccionado! é sempre feita a escolha!
        dispositivoSeleccionado = null;

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            List<String> lista = new ArrayList<String>();
            //TODO provavelmente vai ser preciso mais tarde List<String> listaNome = new ArrayList<String>();
            List<String> listaAddress = new ArrayList<String>();
            for (BluetoothDevice device : pairedDevices) {
                listaAddress.add(device.getAddress());
                //TODO já indicado listaNome.add(device.getName());
                lista.add(device.getName() + "\n" + device.getAddress());
            }

            final String[] arLista = lista.toArray(new String[lista.size()]);
            //TODO já indicado final String[] arListaNome = listaNome.toArray(new String[listaNome.size()]);
            final String[] arListaAddress = listaAddress.toArray(new String[listaAddress.size()]);

            AlertDialog.Builder dialogoA = new AlertDialog.Builder(MainActivity.this);
            dialogoA.setTitle("Escolher dispositivo")
                    .setItems(arLista, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dispositivoSeleccionado = mBluetoothAdapter.getRemoteDevice(arListaAddress[which]);
                            conectar();
                        }
                    })
                    .show();
        }
        else{
            AlertDialog.Builder dialogoA = new AlertDialog.Builder(MainActivity.this);
            dialogoA.setTitle("Erro")
                    .setMessage("Não há dispositivos emparelhados. Por favor emparelhe um dispositivo primeiro.")
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();

        }

    }

    private void conectar(){
        if (dispositivoSeleccionado == null){ //TODO não deveria chegar aqui nesta versão da app!, só por precaução
            AlertDialog.Builder dialogoA = new AlertDialog.Builder(MainActivity.this);
            dialogoA.setTitle("Erro")
                    .setMessage("Não foi escolhido nenhum dispositivo. Escolha um dispositivo primeiro.")
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
        }
        else{
            //TODO em construção!
            BluetoothDevice device = dispositivoSeleccionado;

            myThreadConnectBTdevice = new ThreadConnectBTdevice(device);
            myThreadConnectBTdevice.start();


        }
    }

    // Bluetooth

    private final int REQUEST_ENABLE_BT = 0;
    public BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public BluetoothDevice dispositivoSeleccionado = null;


    ThreadConnected myThreadConnected;
    ThreadConnectBTdevice myThreadConnectBTdevice;

    private UUID myUUID;

    private final String UUID_STRING_WELL_KNOWN_SPP = "00001101-0000-1000-8000-00805F9B34FB";


    //Called in ThreadConnectBTdevice once connect successed
    //to start ThreadConnected
    private void startThreadConnected(BluetoothSocket socket){

        myThreadConnected = new ThreadConnected(socket);
        myThreadConnected.start();
    }

    /*
    ThreadConnectBTdevice:
    Background Thread to handle BlueTooth connecting
    */
    private class ThreadConnectBTdevice extends Thread {

        private BluetoothSocket bluetoothSocket = null;
        private BluetoothDevice bluetoothDevice;


        private ThreadConnectBTdevice(BluetoothDevice device) {
            bluetoothDevice = device;

            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(myUUID);
                Toast.makeText(getApplicationContext(),"A tentar ligar", Toast.LENGTH_LONG).show();
                // TODO IMPLEMENTAR ISTO textStatus.setText("bluetoothSocket: \n" + bluetoothSocket);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Toast.makeText(getApplicationContext(),"Erro: Bluetooth foi desligado durante a operação!", Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            boolean success = false;
            try {
                bluetoothSocket.connect();
                success = true;
            } catch (IOException e) {
                e.printStackTrace();

                final String eMessage = e.getMessage();
                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"Erro de ligação: Dispositivo fora de alcance ou ocupado. Tente de novo.", Toast.LENGTH_LONG).show();
                    }
                });

                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }

            if(success){
                //connect successful

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {

                        btnDesligar.setVisibility(View.VISIBLE);
                        btnLigar.setVisibility(View.GONE);
                        btnAbrir.setVisibility(View.VISIBLE);
                        txtEstado.setText(R.string.estado_on);

                        Toast.makeText(MainActivity.this, "Ligação com sucesso!", Toast.LENGTH_LONG).show();
                    }
                });

                startThreadConnected(bluetoothSocket);

            }else{
                // nao deveria chegar aqui
            }
        }

        public void cancel() {
            try {
                bluetoothSocket.close();
                Toast.makeText(MainActivity.this, "Ligação terminada.", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

    }

    /*
    ThreadConnected:
    Background Thread to handle Bluetooth data communication
    after connected
     */
    private class ThreadConnected extends Thread {
        private final BluetoothSocket connectedBluetoothSocket;
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;

        public ThreadConnected(BluetoothSocket socket) {
            connectedBluetoothSocket = socket;
            InputStream in = null;
            OutputStream out = null;

            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() {
            final byte delimiter = 10;
            byte[] buffer = new byte[8];
            int readBufferPosition = 0;

            while (true) {
                if (connectedBluetoothSocket.isConnected()) {
                    try {
                        int bytesAvailable = connectedInputStream.available();
                        if (bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            connectedInputStream.read(packetBytes);
                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                if (b == delimiter) {
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(buffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;

                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            atualizarLixo(data);
                                        }
                                    });
                                } else {
                                    buffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException e) {
                        // nada, ou a aplicação encrava

                    }
                }else{
                    // ?
                }
            }
        }

        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        public void cancel() {
            try {
                connectedBluetoothSocket.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

}
