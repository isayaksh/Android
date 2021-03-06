package com.example.lamp;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private SoundPool soundPool;
    private int emergencySound;

    static String LED_ENABLE = "led_enable";
    static String EQUALIZER_ENABLE = "equalizer_enable";

    public Dialog dlg;

    final int HOME_FRAG = 2131231043;
    final int EMG_FRAG = 2131231044;
    final int OPT_FRAG = 2131231045;

    final int PERMISSION = 1;
    Intent sttIntent;
    SpeechRecognizer mRecognizer;
    private RecognitionListener listener;

    private static Handler mHandler;

    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevices;
    ArrayList<String> mListPairedDevices;

    Handler mBluetoothHandler;
    ConnectedBluetoothThread mThreadConnectedBluetooth;
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public SharedPreferences pref; // ??? ????????? ?????? ????????? ??????
    private BottomNavigationView mBottomNV; // Bottom Navigation ??? ??????
    public InfoDialog infoDialog;
    public ListView deviceList; // dialog??? ????????? ???????????? ?????? ?????? listview
    public String bluetoothState;

    public LocationManager LM;
    public SharedPreferences.Editor editor;
    public int emergencyCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // NOTE CALL_PHONE ?????? ??????
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},200);
        }
        // NOTE ACCESS_FINE_LOCATION ?????? ??????
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            // ??????????????? ?????????????????? ????????? ????????????
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // ?????? ??????(READ_PHONE_STATE??? requestCode??? 1000?????? ??????
                requestPermissions( new String[]{Manifest.permission.CALL_PHONE}, 1000);
            }
        }
        // NOTE ??????????????? 6.0?????? ???????????? ???????????? ????????? ??????
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) == PackageManager.PERMISSION_DENIED
                    || ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED ) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.INTERNET,
                                Manifest.permission.RECORD_AUDIO},PERMISSION);
            }
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            soundPool = new SoundPool.Builder()
                    .setMaxStreams(6)
                    .setAudioAttributes(audioAttributes)
                    .build();
        } else {
            soundPool = new SoundPool(6, AudioManager.STREAM_MUSIC, 0);
        }

        emergencySound = soundPool.load(this, R.raw.emergency, 1);

        sttIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        sttIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName()); // ????????? ???
        sttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR"); // ?????? ??????

        dlg = new Dialog(MainActivity.this);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlg.setContentView(R.layout.basic_dialog);

        // NOTE ?????? ?????? ??? ???????????? ?????? ??????!
        findViewById(R.id.stt_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRecognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this); // ??? SpeechRecognizer ??? ????????? ????????? ?????????
                mRecognizer.setRecognitionListener(listener); // ????????? ??????
                mRecognizer.startListening(sttIntent); // ?????? ??????
            }
        });

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // TODO IoT??? ??????????????? ?????? ????????? ??????
            }
        };

        class NewRunnable implements Runnable {
            @Override
            public void run(){
                while (true) {
                    pref = getBaseContext().getSharedPreferences("State",MODE_PRIVATE);
                    bluetoothState = pref.getString("bluetoothState"," ");
                    String message = "";
                    int led = pref.getInt("led_value",0);
                    int ringTime = pref.getInt("ringTime",0);
                    boolean inv = pref.getBoolean("invasion_boolean",false);
                    boolean mos = pref.getBoolean("mosquito_boolean",false);
                    boolean eql = pref.getBoolean("equalizer_boolean", false);
                    boolean emg = pref.getBoolean("emergencyBell",false);

                    // led ??? ??????
                    if(led == 0) message += "q";
                    else if(led == 1) message += "w";
                    else if(led == 2) message += "e";
                    else if(led == 3) message += "r";
                    else if(led == 4) message += "t";

                    // ringTime ??? ??????
                    if(ringTime == 0) message += "z";
                    else if(ringTime == 1) message += "x";
                    else if(ringTime == 2) message += "c";
                    else if(ringTime == 3) message += "v";

                    // ?????? ??? ??????
                    if(inv) message += "Y";
                    else message += "y";

                    // ?????? ??? ??????
                    if(mos) message += "U";
                    else message += "u";

                    // ????????? ??? ??????
                    if(emg){
                        if(emergencyCheck >= ringTime * 10){
                            editor.putBoolean("emergencyBell", false);
                            editor.commit();
                            message += "i";
                            emergencyCheck = 0;
                        } else {
                            message += "I";
                            emergencyCheck += 1;
                        }
                    }
                    else message += "i";

                    if(eql) message += "S";
                    else message += "s";
                    Log.d(TAG, "???????????? ?????? =>> " + message);
                    if(mThreadConnectedBluetooth!=null){
                        if(!bluetoothState.equals(message)) {
                            Log.d(TAG, "???????????? ?????? =>> " + message);
                            mThreadConnectedBluetooth.write(message);
                            editor.putString("bluetoothState", message);
                            editor.commit();
                        }
                    }
                    /*
                    if(mThreadConnectedBluetooth!=null){
                        Log.d(TAG,"???????????? ?????? =>> success");
                        mThreadConnectedBluetooth.write(message);
                    }
                       */
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread( ).interrupt( );

                    } catch (Exception e) {
                        e.printStackTrace() ;
                    }
                    //mHandler.sendEmptyMessage(0);
                }
            }
        }
        NewRunnable nr = new NewRunnable() ;
        Thread t = new Thread(nr);
        t.start();


        LM = (LocationManager) getSystemService(Context.LOCATION_SERVICE); // LocationManager ?????? ??????

        pref = this.getSharedPreferences("State",MODE_PRIVATE);
        editor = pref.edit();

        editor.putString("meanavg_value", "0,0,0,0");
        editor.commit();

        // TODO ???????????? https://bugwhale.tistory.com/11
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // NOTE ??????????????? ?????? ?????????????????? ???????????? ?????? ???????????? handler!!!
        mBluetoothHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == BT_MESSAGE_READ){
                    String readMessage = "";
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    // NOTE 0: LED OFF, 1: LED 1??????, 2: LED 2??????, 3: LED 3??????, 4: LED ?????????
                    // NOTE h: ??????????????? ??????, j: ??????????????? ??????, k: ??????????????? ??????, l: ??????????????? ??????
                    // NOTE 5: ???????????? ??????
                    Log.d(TAG, "???????????? ?????? ??? : "+readMessage);
                    if(readMessage.contains("q")){
                        editor.putInt("led_value", 0);
                        editor.commit();
                    }
                    if(readMessage.contains("w")){
                        editor.putInt("led_value", 1);
                        editor.commit();
                    }
                    if(readMessage.contains("e")){
                        editor.putInt("led_value", 2);
                        editor.commit();
                    }
                    if(readMessage.contains("r")){
                        editor.putInt("led_value", 3);
                        editor.commit();
                    }
                    if(readMessage.contains("t")){
                        editor.putInt("led_value", 4);
                        editor.commit();
                    }
                    if(readMessage.contains("h")){
                        editor.putInt("co_value", 0);
                        editor.commit();
                    }
                    if(readMessage.contains("j")){
                        editor.putInt("co_value", 1);
                        editor.commit();
                    }
                    if(readMessage.contains("k")){
                        editor.putInt("co_value", 2);
                        editor.commit();
                    }
                    if(readMessage.contains("l")){
                        editor.putInt("co_value", 3);
                        editor.commit();
                        TextView text = dlg.findViewById(R.id.notification);
                        text.setText("?????? ?????????????????? ????????? ?????? ????????????.\n???????????? ?????????????????????????"); // ??????
                        TextView title = dlg.findViewById(R.id.info_title);
                        title.setText("??????????????? ?????? ??????");
                        Button okButton = dlg.findViewById(R.id.ok_button);
                        okButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent it = new Intent("android.intent.action.CALL", Uri.parse("tel:111199"));
                                startActivity(it);
                                dlg.dismiss();
                            }
                        });
                        Button cancelButton = dlg.findViewById(R.id.cancle_button);
                        cancelButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dlg.dismiss();
                            }
                        });
                        dlg.show();
                        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    }
                    if(readMessage.contains("M")){
                        // NOTE ?????? Dialog ??????!
                        soundPool.play(emergencySound,1,1,0,0,1);
                        TextView text = dlg.findViewById(R.id.notification);
                        text.setText("?????? ???????????? ????????? ?????????????????????.\n???????????? ?????????????????????????"); // ??????
                        TextView title = dlg.findViewById(R.id.info_title);
                        title.setText("????????? ?????? ??????");
                        Button okButton = dlg.findViewById(R.id.ok_button);
                        okButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                Intent it = new Intent("android.intent.action.CALL", Uri.parse("tel:111199"));
                                startActivity(it);
                                dlg.dismiss();
                            }
                        });
                        Button cancelButton = dlg.findViewById(R.id.cancle_button);
                        cancelButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                dlg.dismiss();
                            }
                        });

                        dlg.show();
                        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                    }
                }
            }
        };

        //======

        TextView curDate = findViewById(R.id.date); // ?????? ????????? ?????? ?????? TextView
        curDate.setText(getTime()); // curDate??? ?????? ?????? ????????? set

        mBottomNV = findViewById(R.id.nav_view);
        mBottomNV.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() { //NavigationItemSelecte
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                BottomNavigate(menuItem.getItemId());
                // NOTE ??? ??????????????? ??? id
                // ??? : 2131231043, ????????? : 2131231044, ??????: 2131231045

                return true;
            }
        });
        mBottomNV.setSelectedItemId(R.id.navigation_1);

        findViewById(R.id.info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {// ?????? ????????? ?????? Information
                //Toast.makeText(this,"info",Toast.LENGTH_SHORT).show();
                // ???????????? ????????? AlertDialog ?????????
                infoDialog = new InfoDialog(MainActivity.this);
                infoDialog.show();
                infoDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });

        findViewById(R.id.bluetooth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO ???????????? ????????? ?????? ??????
                bluetoothOn();
            }
        });
        // NOTE ???????????? listner!!!
        listener = new RecognitionListener() {
            FragmentPage1 mf = (FragmentPage1) getSupportFragmentManager().findFragmentById(R.id.mosquito_switch);
            @Override
            public void onReadyForSpeech(Bundle params) {
                // ????????? ????????? ??????????????? ??????
                Toast.makeText(getApplicationContext(), "???????????? ??????", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBeginningOfSpeech() {
                // ????????? ???????????? ??? ??????

            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // ???????????? ????????? ????????? ?????????
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // ?????? ???????????? ????????? ??? ????????? buffer??? ??????
            }

            @Override
            public void onEndOfSpeech() {
                // ???????????? ???????????? ??????
            }

            @Override
            public void onError(int error) {
                // ???????????? ?????? ?????? ????????? ???????????? ??? ??????
                String message;

                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        message = "????????? ??????";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        message = "??????????????? ??????";
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        message = "????????? ??????";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        message = "???????????? ??????";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        message = "????????? ????????????";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        message = "?????? ??? ??????";
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        message = "RECOGNIZER ??? ??????";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        message = "????????? ?????????";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        message = "????????? ????????????";
                        break;
                    default:
                        message = "??? ??? ?????? ?????????";
                        break;
                }
                Toast.makeText(getApplicationContext(), "?????? ?????? : " + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                // ?????? ????????? ???????????? ??????
                // ?????? ?????? ArrayList??? ????????? ?????? textView??? ????????? ?????????
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String order = "";
                for (int i = 0; i < matches.size(); i++) {
                    Toast.makeText(MainActivity.this,matches.get(i) , Toast.LENGTH_SHORT).show();
                    order += matches.get(i);
                }

                Log.d(TAG,"voice value : "+ order);
                switch (order){
                    case "LED ??? ???":
                        editor.putInt("led_value", 0);
                        editor.putBoolean(EQUALIZER_ENABLE,true);
                        editor.commit();
                        break;
                    case "LED 1??????":
                        if(pref.getBoolean("equalizer_boolean", true)){
                            Toast.makeText(MainActivity.this, "??????????????? ??????????????????.", Toast.LENGTH_SHORT).show();
                        } else {
                            editor.putInt("led_value", 1);
                            editor.putBoolean(EQUALIZER_ENABLE, false);
                            editor.commit();
                        }
                        break;
                    case "LED 2??????":
                        if(pref.getBoolean("equalizer_boolean", true)){
                            Toast.makeText(MainActivity.this, "??????????????? ??????????????????.", Toast.LENGTH_SHORT).show();
                        } else {
                            editor.putInt("led_value", 2);
                            editor.putBoolean(EQUALIZER_ENABLE, false);
                            editor.commit();
                        }
                        break;
                    case "LED 3??????":
                        if(pref.getBoolean("equalizer_boolean", true)){
                            Toast.makeText(MainActivity.this, "??????????????? ??????????????????.", Toast.LENGTH_SHORT).show();
                        } else {
                            editor.putInt("led_value", 3);
                            editor.putBoolean(EQUALIZER_ENABLE, false);
                            editor.commit();
                        }
                        break;
                    case "LED ?????????":
                        if(pref.getBoolean("equalizer_boolean", true)){
                            Toast.makeText(MainActivity.this, "??????????????? ??????????????????.", Toast.LENGTH_SHORT).show();
                        } else {
                            editor.putInt("led_value", 4);
                            editor.putBoolean(EQUALIZER_ENABLE, false);
                            editor.commit();
                        }
                        break;

                    case "?????? ?????? ?????? ??? ???":
                        editor.putBoolean("invasion_boolean", false);
                        editor.commit();
                        break;
                    case "?????? ?????? ?????? ??? ???":
                        editor.putBoolean("invasion_boolean", true);
                        editor.commit();
                        break;
                    case "?????? ?????? ?????? ??? ???":
                        editor.putBoolean("mosquito_boolean", false);
                        editor.commit();
                        break;
                    case "?????? ?????? ?????? ??? ???":
                        editor.putBoolean("mosquito_boolean", true);
                        editor.commit();
                        break;
                    case "??????????????? ??? ???":
                        editor.putBoolean("equalizer_boolean", false);
                        editor.putBoolean(LED_ENABLE, true);
                        editor.commit();
                        break;
                    case "??????????????? ??? ???":
                        if(pref.getInt("led_value", 0) != 0){
                            Toast.makeText(MainActivity.this, "LED??? ??????????????????.", Toast.LENGTH_SHORT).show();
                        } else {
                            editor.putBoolean("equalizer_boolean", true);
                            editor.putBoolean(LED_ENABLE, false);
                            editor.commit();
                        }
                        break;
                    case "?????? ?????? ??? ???":
                        editor.putInt("ringTime", 0);
                        editor.commit();
                        break;
                    case "?????? ?????? 5???":
                        editor.putInt("ringTime", 1);
                        editor.commit();
                        break;
                    case "?????? ?????? 10???":
                        editor.putInt("ringTime", 2);
                        editor.commit();
                        break;
                    case "?????? ?????? 15???":
                        editor.putInt("ringTime", 3);
                        editor.commit();
                        break;
                    default:
                        break;
                }
            }
            @Override
            public void onPartialResults(Bundle partialResults) {
                // ?????? ?????? ????????? ????????? ??? ?????? ??? ??????
            }
            @Override
            public void onEvent(int eventType, Bundle params) {
                // ?????? ???????????? ???????????? ?????? ??????
            }
        };
    }
    private void BottomNavigate(int id) {  //BottomNavigation ????????? ??????
        String tag = String.valueOf(id);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment currentFragment = fragmentManager.getPrimaryNavigationFragment();
        if (currentFragment != null) {
            fragmentTransaction.hide(currentFragment);
        }
        Fragment fragment = fragmentManager.findFragmentByTag(tag);
        if (fragment == null) {
            if (id == R.id.navigation_1) {
                fragment = new FragmentPage1();
            } else if (id == R.id.navigation_3){
                fragment = new FragmentPage3();
            }
            else if (id == R.id.navigation_4){
                fragment = new FragmentPage4();
            }
            fragmentTransaction.add(R.id.content_layout, fragment, tag);
        } else {
            fragmentTransaction.show(fragment);
        }
        fragmentTransaction.setPrimaryNavigationFragment(fragment);
        fragmentTransaction.setReorderingAllowed(true);
        // ??? setReorderingAllowed()??? ?????????????????? ????????? ???????????? ??????????????? ??????????????? ????????? ?????????????????? ?????? ????????? ??????????????????.
        fragmentTransaction.commit();
    }


    private String getTime(){ // ????????? ????????? String ???????????? ???????????? ??????
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
        String getTime = dateFormat.format(date);
        return getTime;
    }
    // NOTE ??????????????? ???????????? ?????? ?????????
    void bluetoothOn() {
        if(mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "??????????????? ???????????? ?????? ???????????????.", Toast.LENGTH_LONG).show();
        }
        else {
            if (mBluetoothAdapter.isEnabled()) {
                // Toast.makeText(getApplicationContext(), "??????????????? ?????? ????????? ?????? ????????????.", Toast.LENGTH_LONG).show();
                listPairedDevices();
            }
            else {
                //Toast.makeText(getApplicationContext(), "??????????????? ????????? ?????? ?????? ????????????.", Toast.LENGTH_LONG).show();
                Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intentBluetoothEnable, BT_REQUEST_ENABLE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BT_REQUEST_ENABLE:
                if (resultCode == RESULT_OK) { // ???????????? ???????????? ????????? ??????????????????
                    Toast.makeText(getApplicationContext(), "???????????? ?????????", Toast.LENGTH_LONG).show();
                    listPairedDevices();
                } else if (resultCode == RESULT_CANCELED) { // ???????????? ???????????? ????????? ??????????????????
                    Toast.makeText(getApplicationContext(), "??????", Toast.LENGTH_LONG).show();
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    void listPairedDevices() {
        if (mBluetoothAdapter.isEnabled()) {
            mPairedDevices = mBluetoothAdapter.getBondedDevices();

            if (mPairedDevices.size() > 0) {
                Dialog dlg = new Dialog(this);
                dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dlg.setContentView(R.layout.list_dialog);

                TextView title = dlg.findViewById(R.id.info_title);
                title.setText("?????? ??????");

                mListPairedDevices = new ArrayList<String>();
                for (BluetoothDevice device : mPairedDevices) {
                    mListPairedDevices.add(device.getName());
                }

                ArrayAdapter<String> adpater = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mListPairedDevices);
                deviceList = dlg.findViewById(R.id.list);
                deviceList.setAdapter(adpater);

                CharSequence[] items = mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);
                mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);

                // connectSelectedDevice(serialNumber); //TDOD ???????????? ?????? ?????????!

                deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                        boolean check = connectSelectedDevice(items[i].toString());
                        if(check){
                            dlg.dismiss();
                        }
                    }
                });
                dlg.show();
                dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            } else {
                Toast.makeText(getApplicationContext(), "???????????? ????????? ????????????.", Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "??????????????? ???????????? ?????? ????????????.", Toast.LENGTH_SHORT).show();
        }
    }
    boolean connectSelectedDevice(String selectedDeviceName) {
        for(BluetoothDevice tempDevice : mPairedDevices) {
            if (selectedDeviceName.equals(tempDevice.getName())) {
                mBluetoothDevice = tempDevice;
                break;
            }
        }
        try {
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            mBluetoothSocket.connect();
            mThreadConnectedBluetooth = new ConnectedBluetoothThread(mBluetoothSocket);
            mThreadConnectedBluetooth.start();
            mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();
            return true;
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "???????????? ?????? ??? ????????? ??????????????????.", Toast.LENGTH_LONG).show();
            return false;
        }
    }

    private class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "?????? ?????? ??? ????????? ??????????????????.", Toast.LENGTH_LONG).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            int bytes;
            byte[] buffer = {0,};

            while (true) {
                try {
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        buffer = new byte[1024]; // ?????? ?????????
                        Arrays.fill(buffer,(byte)0);
                        SystemClock.sleep(100);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes);
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }
        public void write(String str) {
            byte[] bytes = str.getBytes();
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "????????? ?????? ??? ????????? ??????????????????.", Toast.LENGTH_LONG).show();
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "?????? ?????? ??? ????????? ??????????????????.", Toast.LENGTH_LONG).show();
            }
        }
    }
}