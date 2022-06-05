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

    public SharedPreferences pref; // 앱 종료후 현재 상태를 저장
    private BottomNavigationView mBottomNV; // Bottom Navigation 뷰 객체
    public InfoDialog infoDialog;
    public ListView deviceList; // dialog의 연결할 블루투스 기기 목록 listview
    public String bluetoothState;

    public LocationManager LM;
    public SharedPreferences.Editor editor;
    public int emergencyCheck;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // NOTE CALL_PHONE 권한 설정
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION},200);
        }
        // NOTE ACCESS_FINE_LOCATION 권한 설정
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_DENIED) {
            // 마쉬멜로우 이상버전부터 권한을 물어본다
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 권한 체크(READ_PHONE_STATE의 requestCode를 1000으로 세팅
                requestPermissions( new String[]{Manifest.permission.CALL_PHONE}, 1000);
            }
        }
        // NOTE 안드로이드 6.0버전 이상인지 체크해서 퍼미션 체크
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
        sttIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName()); // 여분의 키
        sttIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR"); // 언어 설정

        dlg = new Dialog(MainActivity.this);
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlg.setContentView(R.layout.basic_dialog);

        // NOTE 버튼 클릭 시 음성인식 기능 시작!
        findViewById(R.id.stt_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRecognizer = SpeechRecognizer.createSpeechRecognizer(MainActivity.this); // 새 SpeechRecognizer 를 만드는 팩토리 메서드
                mRecognizer.setRecognitionListener(listener); // 리스너 설정
                mRecognizer.startListening(sttIntent); // 듣기 시작
            }
        });

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                // TODO IoT에 블루투스를 통한 데이터 전송
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

                    // led 값 변환
                    if(led == 0) message += "q";
                    else if(led == 1) message += "w";
                    else if(led == 2) message += "e";
                    else if(led == 3) message += "r";
                    else if(led == 4) message += "t";

                    // ringTime 값 변환
                    if(ringTime == 0) message += "z";
                    else if(ringTime == 1) message += "x";
                    else if(ringTime == 2) message += "c";
                    else if(ringTime == 3) message += "v";

                    // 침입 값 변환
                    if(inv) message += "Y";
                    else message += "y";

                    // 모기 값 변환
                    if(mos) message += "U";
                    else message += "u";

                    // 비상벨 값 변환
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
                    Log.d(TAG, "블루투스 전송 =>> " + message);
                    if(mThreadConnectedBluetooth!=null){
                        if(!bluetoothState.equals(message)) {
                            Log.d(TAG, "블루투스 전송 =>> " + message);
                            mThreadConnectedBluetooth.write(message);
                            editor.putString("bluetoothState", message);
                            editor.commit();
                        }
                    }
                    /*
                    if(mThreadConnectedBluetooth!=null){
                        Log.d(TAG,"블루투스 수신 =>> success");
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


        LM = (LocationManager) getSystemService(Context.LOCATION_SERVICE); // LocationManager 객체 생성

        pref = this.getSharedPreferences("State",MODE_PRIVATE);
        editor = pref.edit();

        editor.putString("meanavg_value", "0,0,0,0");
        editor.commit();

        // TODO 블루투스 https://bugwhale.tistory.com/11
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // NOTE 블루투스를 통해 랜턴으로부터 전달받은 값을 처리하는 handler!!!
        mBluetoothHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == BT_MESSAGE_READ){
                    String readMessage = "";
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    // NOTE 0: LED OFF, 1: LED 1단계, 2: LED 2단계, 3: LED 3단계, 4: LED 무드등
                    // NOTE h: 일산화탄소 좋음, j: 일산화탄소 주의, k: 일산화탄소 위험, l: 일산화탄소 경고
                    // NOTE 5: 침입감지 경고
                    Log.d(TAG, "블루투스 송신 값 : "+readMessage);
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
                        text.setText("현재 일산화탄소의 수치가 너무 높습니다.\n소방서에 전화하시겠습니까?"); // 내용
                        TextView title = dlg.findViewById(R.id.info_title);
                        title.setText("일산화탄소 위험 수치");
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
                        // NOTE 위험 Dialog 출력!
                        soundPool.play(emergencySound,1,1,0,0,1);
                        TextView text = dlg.findViewById(R.id.notification);
                        text.setText("현재 외부인의 침입이 감지되었습니다.\n소방서에 전화하시겠습니까?"); // 내용
                        TextView title = dlg.findViewById(R.id.info_title);
                        title.setText("외부인 침입 감지");
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

        TextView curDate = findViewById(R.id.date); // 날짜 정보를 담고 있는 TextView
        curDate.setText(getTime()); // curDate의 값을 현재 날짜로 set

        mBottomNV = findViewById(R.id.nav_view);
        mBottomNV.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() { //NavigationItemSelecte
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                BottomNavigate(menuItem.getItemId());
                // NOTE 각 프레그먼트 별 id
                // 홈 : 2131231043, 비상벨 : 2131231044, 설정: 2131231045

                return true;
            }
        });
        mBottomNV.setSelectedItemId(R.id.navigation_1);

        findViewById(R.id.info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {// 앱과 제품에 대한 Information
                //Toast.makeText(this,"info",Toast.LENGTH_SHORT).show();
                // 도움말을 제공할 AlertDialog 메시지
                infoDialog = new InfoDialog(MainActivity.this);
                infoDialog.show();
                infoDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });

        findViewById(R.id.bluetooth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO 블루투스 연결을 위한 작업
                bluetoothOn();
            }
        });
        // NOTE 음성인식 listner!!!
        listener = new RecognitionListener() {
            FragmentPage1 mf = (FragmentPage1) getSupportFragmentManager().findFragmentById(R.id.mosquito_switch);
            @Override
            public void onReadyForSpeech(Bundle params) {
                // 말하기 시작할 준비가되면 호출
                Toast.makeText(getApplicationContext(), "음성인식 시작", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onBeginningOfSpeech() {
                // 말하기 시작했을 때 호출

            }

            @Override
            public void onRmsChanged(float rmsdB) {
                // 입력받는 소리의 크기를 알려줌
            }

            @Override
            public void onBufferReceived(byte[] buffer) {
                // 말을 시작하고 인식이 된 단어를 buffer에 담음
            }

            @Override
            public void onEndOfSpeech() {
                // 말하기를 중지하면 호출
            }

            @Override
            public void onError(int error) {
                // 네트워크 또는 인식 오류가 발생했을 때 호출
                String message;

                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO:
                        message = "오디오 에러";
                        break;
                    case SpeechRecognizer.ERROR_CLIENT:
                        message = "클라이언트 에러";
                        break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                        message = "퍼미션 없음";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK:
                        message = "네트워크 에러";
                        break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                        message = "네트웍 타임아웃";
                        break;
                    case SpeechRecognizer.ERROR_NO_MATCH:
                        message = "찾을 수 없음";
                        break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                        message = "RECOGNIZER 가 바쁨";
                        break;
                    case SpeechRecognizer.ERROR_SERVER:
                        message = "서버가 이상함";
                        break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                        message = "말하는 시간초과";
                        break;
                    default:
                        message = "알 수 없는 오류임";
                        break;
                }
                Toast.makeText(getApplicationContext(), "에러 발생 : " + message, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResults(Bundle results) {
                // 인식 결과가 준비되면 호출
                // 말을 하면 ArrayList에 단어를 넣고 textView에 단어를 이어줌
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String order = "";
                for (int i = 0; i < matches.size(); i++) {
                    Toast.makeText(MainActivity.this,matches.get(i) , Toast.LENGTH_SHORT).show();
                    order += matches.get(i);
                }

                Log.d(TAG,"voice value : "+ order);
                switch (order){
                    case "LED 꺼 줘":
                        editor.putInt("led_value", 0);
                        editor.putBoolean(EQUALIZER_ENABLE,true);
                        editor.commit();
                        break;
                    case "LED 1단계":
                        if(pref.getBoolean("equalizer_boolean", true)){
                            Toast.makeText(MainActivity.this, "이퀄라이저 사용중입니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            editor.putInt("led_value", 1);
                            editor.putBoolean(EQUALIZER_ENABLE, false);
                            editor.commit();
                        }
                        break;
                    case "LED 2단계":
                        if(pref.getBoolean("equalizer_boolean", true)){
                            Toast.makeText(MainActivity.this, "이퀄라이저 사용중입니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            editor.putInt("led_value", 2);
                            editor.putBoolean(EQUALIZER_ENABLE, false);
                            editor.commit();
                        }
                        break;
                    case "LED 3단계":
                        if(pref.getBoolean("equalizer_boolean", true)){
                            Toast.makeText(MainActivity.this, "이퀄라이저 사용중입니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            editor.putInt("led_value", 3);
                            editor.putBoolean(EQUALIZER_ENABLE, false);
                            editor.commit();
                        }
                        break;
                    case "LED 무드등":
                        if(pref.getBoolean("equalizer_boolean", true)){
                            Toast.makeText(MainActivity.this, "이퀄라이저 사용중입니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            editor.putInt("led_value", 4);
                            editor.putBoolean(EQUALIZER_ENABLE, false);
                            editor.commit();
                        }
                        break;

                    case "외부 감지 기능 꺼 줘":
                        editor.putBoolean("invasion_boolean", false);
                        editor.commit();
                        break;
                    case "외부 감지 기능 켜 줘":
                        editor.putBoolean("invasion_boolean", true);
                        editor.commit();
                        break;
                    case "모기 퇴치 기능 꺼 줘":
                        editor.putBoolean("mosquito_boolean", false);
                        editor.commit();
                        break;
                    case "모기 퇴치 기능 켜 줘":
                        editor.putBoolean("mosquito_boolean", true);
                        editor.commit();
                        break;
                    case "이퀄라이저 꺼 줘":
                        editor.putBoolean("equalizer_boolean", false);
                        editor.putBoolean(LED_ENABLE, true);
                        editor.commit();
                        break;
                    case "이퀄라이저 켜 줘":
                        if(pref.getInt("led_value", 0) != 0){
                            Toast.makeText(MainActivity.this, "LED를 사용중입니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            editor.putBoolean("equalizer_boolean", true);
                            editor.putBoolean(LED_ENABLE, false);
                            editor.commit();
                        }
                        break;
                    case "울림 시간 꺼 줘":
                        editor.putInt("ringTime", 0);
                        editor.commit();
                        break;
                    case "울림 시간 5초":
                        editor.putInt("ringTime", 1);
                        editor.commit();
                        break;
                    case "울림 시간 10초":
                        editor.putInt("ringTime", 2);
                        editor.commit();
                        break;
                    case "울림 시간 15초":
                        editor.putInt("ringTime", 3);
                        editor.commit();
                        break;
                    default:
                        break;
                }
            }
            @Override
            public void onPartialResults(Bundle partialResults) {
                // 부분 인식 결과를 사용할 수 있을 때 호출
            }
            @Override
            public void onEvent(int eventType, Bundle params) {
                // 향후 이벤트를 추가하기 위해 예약
            }
        };
    }
    private void BottomNavigate(int id) {  //BottomNavigation 페이지 변경
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
        // ↑ setReorderingAllowed()는 애니메이션과 전환이 올바르게 작동하도록 트랜잭션과 관련된 프래그먼트의 상태 변경을 최적화합니다.
        fragmentTransaction.commit();
    }


    private String getTime(){ // 현재의 날짜를 String 형식으로 반환하는 함수
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd");
        String getTime = dateFormat.format(date);
        return getTime;
    }
    // NOTE 블루투스를 활성화를 위한 메서드
    void bluetoothOn() {
        if(mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_LONG).show();
        }
        else {
            if (mBluetoothAdapter.isEnabled()) {
                // Toast.makeText(getApplicationContext(), "블루투스가 이미 활성화 되어 있습니다.", Toast.LENGTH_LONG).show();
                listPairedDevices();
            }
            else {
                //Toast.makeText(getApplicationContext(), "블루투스가 활성화 되어 있지 않습니다.", Toast.LENGTH_LONG).show();
                Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intentBluetoothEnable, BT_REQUEST_ENABLE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BT_REQUEST_ENABLE:
                if (resultCode == RESULT_OK) { // 블루투스 활성화를 확인을 클릭하였다면
                    Toast.makeText(getApplicationContext(), "블루투스 활성화", Toast.LENGTH_LONG).show();
                    listPairedDevices();
                } else if (resultCode == RESULT_CANCELED) { // 블루투스 활성화를 취소를 클릭하였다면
                    Toast.makeText(getApplicationContext(), "취소", Toast.LENGTH_LONG).show();
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
                title.setText("장치 선택");

                mListPairedDevices = new ArrayList<String>();
                for (BluetoothDevice device : mPairedDevices) {
                    mListPairedDevices.add(device.getName());
                }

                ArrayAdapter<String> adpater = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mListPairedDevices);
                deviceList = dlg.findViewById(R.id.list);
                deviceList.setAdapter(adpater);

                CharSequence[] items = mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);
                mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);

                // connectSelectedDevice(serialNumber); //TDOD 테스트를 위해 추가함!

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
                Toast.makeText(getApplicationContext(), "페어링된 장치가 없습니다.", Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "블루투스가 비활성화 되어 있습니다.", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(getApplicationContext(), "블루투스 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
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
                Toast.makeText(getApplicationContext(), "소켓 연결 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
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
                        buffer = new byte[1024]; // 버퍼 초기화
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
                Toast.makeText(getApplicationContext(), "데이터 전송 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "소켓 해제 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show();
            }
        }
    }
}