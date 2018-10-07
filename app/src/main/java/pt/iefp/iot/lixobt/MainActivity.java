package pt.iefp.iot.lixobt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Referência para o bluetooth http://android-er.blogspot.com/2015/10/android-communicate-with-arduino-hc-06.html

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Definir objectos do ecrã
        btnLigar = findViewById(R.id.btnLigar);
        btnLigar.setOnClickListener(btnLigar_click);
        btnDesligar = findViewById(R.id.btnDesligar);
        btnDesligar.setOnClickListener(btnDesligar_click);
        imgLixo = findViewById(R.id.imageView);
        imgLixo.setImageResource(R.drawable.trash0);
        txtEstado = findViewById(R.id.txtEstado);

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
    private Button btnLigar;
    private Button btnDesligar;
    private TextView txtEstado;



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
                .setMessage("Aplicação Android:\nTiago Cabral\n\nProgramação e Montagem do Arduino:\nAntídio Costa")
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
                .setMessage("Lixeira Bluetooth\nVersão 1.0\n\nTurma IoT-Tarde 2018 - IEFP Aveiro")
                .setNeutralButton("Fechar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    // Variaveis



    // Funções

    private void ligar(){
        //TODO isto pode ser feito na função de escolher dispositivo, na secção de successo, é mais garantido!
        btnDesligar.setVisibility(View.VISIBLE);
        btnLigar.setVisibility(View.GONE);
        txtEstado.setText(R.string.estado_on);
    }

    private void desligar(){
        btnDesligar.setVisibility(View.GONE);
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

    private void atualizarLixo(int num){
        //TODO pode-se adicionar alertas a cada nivel! para explorar mais tarde...

        int nivelLixo = num;
        if(nivelLixo<25){
            imgLixo.setImageResource(R.drawable.trash0);
        }
        if(nivelLixo>=25 & nivelLixo<50){
            imgLixo.setImageResource(R.drawable.trash25);
        }
        if(nivelLixo>=50 & nivelLixo<75){
            imgLixo.setImageResource(R.drawable.trash50);
        }
        if(nivelLixo>=75 & nivelLixo<100){
            imgLixo.setImageResource(R.drawable.trash75);
        }
        if(nivelLixo==100){
            imgLixo.setImageResource(R.drawable.trash100);
        }

    }

    public void escolherDispositivo(){

        //TODO A LISTA TEM DE SER DE TODOS OS DISPOSITIVOS E NÃO APENAS DOS EMPARELHADOS! VERIFICAR!!!

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

            //TODO só para testes! apagar mais tarde!!!!!!
            Toast.makeText(MainActivity.this,
                    "Name: " + device.getName() + "\n"
                            + "Address: " + device.getAddress() + "\n"
                            + "BondState: " + device.getBondState() + "\n"
                            + "BluetoothClass: " + device.getBluetoothClass() + "\n"
                            + "Class: " + device.getClass(),
                    Toast.LENGTH_LONG).show();

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
                Toast.makeText(getApplicationContext(),"Ponto 1", Toast.LENGTH_LONG).show();
                // TODO IMPLEMENTAR ISTO textStatus.setText("bluetoothSocket: \n" + bluetoothSocket);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                Toast.makeText(getApplicationContext(),"Erro 1", Toast.LENGTH_LONG).show();
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
                        Toast.makeText(getApplicationContext(),"Erro 2 \n"+ eMessage, Toast.LENGTH_LONG).show();
                    }
                });

                try {
                    bluetoothSocket.close();
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    Toast.makeText(getApplicationContext(),"Erro 3", Toast.LENGTH_LONG).show();
                    e1.printStackTrace();
                }
            }

            if(success){
                //connect successful
                //TODO mudar esta mensagem
                final String msgconnected = "connect successful:\n"
                        + "BluetoothSocket: " + bluetoothSocket + "\n"
                        + "BluetoothDevice: " + bluetoothDevice;

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        /* TODO IMPLEMENTAR
                        textStatus.setText("");
                        textByteCnt.setText(""); */
                        // TODO mudar esta mensagem
                        Toast.makeText(MainActivity.this, msgconnected, Toast.LENGTH_LONG).show();
                        /* TODO IMPLEMENTAR
                        listViewPairedDevice.setVisibility(View.GONE);
                        inputPane.setVisibility(View.VISIBLE);
                        */
                    }
                });

                startThreadConnected(bluetoothSocket);

            }else{
                //TODO implementar aqui um desligar tambem!!!
            }
        }

        public void cancel() {

            Toast.makeText(getApplicationContext(),
                    "Ligação Bluetooth terminada",
                    Toast.LENGTH_LONG).show();

            try {
                bluetoothSocket.close();
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
            byte[] buffer = new byte[1024];
            int bytes;

            //desnecessario?
            String strRx = "";

            while (true) {
                try {
                    bytes = connectedInputStream.read(buffer);
                    final String strReceived = new String(buffer, 0, bytes);
                    //desnecessario final String strByteCnt = String.valueOf(bytes) + " bytes received.\n";

                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {
                            /* TODO IMPLEMENTAR
                               TODO aqui vem todo o codigo executado quando é obtida alguma informação
                            textStatus.append(strReceived);
                            textByteCnt.append(strByteCnt);
                            */
                            try {
                                int nr = Integer.parseInt(strReceived);
                                atualizarLixo(nr);
                            } catch(NumberFormatException nfe) {
                                //TODO provavelmente seria melhor desligar a este momento pois estamos a receber informações erradas! verificar!
                            }
                        }});

                } catch (IOException e) {
                    // TODO Auto-generated catch block

                    // TODO aqui PROVAVELMENTE é a perda de ligação, adicionar toast e verificar a necessidade de correr desligar()! (quase certamente necessário)
                    e.printStackTrace();

                    final String msgConnectionLost = "Connection lost:\n"
                            + e.getMessage();
                    runOnUiThread(new Runnable(){

                        @Override
                        public void run() {
                            /* TODO IMLPEMENTAR
                            textStatus.setText(msgConnectionLost);
                            */
                        }});
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
