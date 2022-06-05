package com.example.lamp;

import static android.content.ContentValues.TAG;
import static android.content.Context.LOCATION_SERVICE;
import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class FragmentPage1 extends Fragment {

    static String EQUALIZER = "equalizer_boolean"; // 이퀄라이저 String
    static String LED_ENABLE = "led_enable";
    static String EQUALIZER_ENABLE = "equalizer_enable";

    public SharedPreferences pref; // 앱 종료후 현재 상태를 저장
    private static Handler mHandler;
    private static Handler locHandler;

    private static String[] coState = {"좋음", "주의", "경고", "위험"};
    private static String[] coColor = {"#00CC00", "#FFD400", "#FF7F00", "#FF0000"};

    public int led_value; // 빛의 값을 저장할 변수
    public boolean co_boolean; // 일산화탄소 스위치 ON/OFF 값
    public boolean invasion_boolean; // 침입 감지 스위치 ON/OFF 값
    public boolean mosquito_boolean; // 모기퇴치 스위치 ON/OFF 값
    public boolean equalizer_boolean; // 이퀄라이저 스위치 ON/OFF 값


    public double curLatitude; // 현재 위도
    public double curLongitude; // 현재 경도
    public String curLocation; // 현재 지역
    public String doname; // 현재 도
    public String d;

    public String baseDate = getDate(); // 오늘 날짜 "20220421"
    public String baseTime = getTime(); // 현재 시간 ""

    public String weatherTemperature; //
    public String curWeather; // 현재 날씨
    public String curTemperature; // 현재 기온

    public ImageView weatherImg; // 현재 날씨 이미지
    public TextView weatherText; // 현재 날씨 텍스트

    public SeekBar led_seekbar;
    public Switch invasion_switch;
    public Switch mosquito_switch;
    public Switch equalizer_switch;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View v = inflater.inflate(R.layout.fragment_page_1, container, false);

        //앱 종료시 UI 상태 정보를 저장한 pref
        pref = this.getActivity().getSharedPreferences("State",MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        ApiExplorer ae = new ApiExplorer(); // 기상청 정보 객체
        ApiFF apiFF = new ApiFF(); // 산림청 정보 객체



        TextView location = v.findViewById(R.id.location); // 현재 위치 텍스트
        TextView temperature = v.findViewById(R.id.temperature); // 현재 기온 텍스트

        TextView led = v.findViewById(R.id.led); // led percentage 텍스트
        led_seekbar = v.findViewById(R.id.seekBar); // led seek bar
        led_seekbar.setMax(4);

        TextView co = v.findViewById(R.id.co); // 일산화탄소 On/Off 텍스트

        TextView invasion = v.findViewById(R.id.detection); // 침입 감지 On/Off 텍스트
        invasion_switch = v.findViewById(R.id.detection_switch); // 침입 감지 Switch

        TextView mosquito = v.findViewById(R.id.mosquito); // 모기 퇴치 On/Off 텍스트
        mosquito_switch = v.findViewById(R.id.mosquito_switch); // 모기 퇴치 Switch

        TextView equalizer = v.findViewById(R.id.equalizer); // 이퀄라이저 ON/OFF 텍스트
        equalizer_switch = v.findViewById(R.id.equalizer_switch); // 이퀄라이저 Switch

        weatherImg = v.findViewById(R.id.weather_img); // 현재 날씨 이미지
        //weatherText = v.findViewById(R.id.weather_text); // 현재 날씨 텍스트

        setState(v);

        // NOTE CO BOX 클릭시 발생하는 이벤트
        View co_box = v.findViewById(R.id.co_box);
        co_box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CoDialog coDialog = new CoDialog(getContext());
                coDialog.show();
                coDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });


        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                int idx = pref.getInt("co_value",0);
                co.setText(coState[idx]);
                co.setTextColor(Color.parseColor(coColor[idx]));
                d = pref.getString("meanavg_value","0,0,0,0");
                // NOTE d1, d2, d3, d4에 대한 정보 set
                setProgressBar(v, d);

                mosquito_switch.setChecked(pref.getBoolean("mosquito_boolean",false));
                invasion_switch.setChecked(pref.getBoolean("invasion_boolean",false));
                equalizer_switch.setChecked(pref.getBoolean(EQUALIZER,false));
                equalizer_switch.setEnabled(pref.getBoolean(EQUALIZER_ENABLE, true));
                led_seekbar.setProgress(pref.getInt("led_value",0));
                led_seekbar.setEnabled(pref.getBoolean(LED_ENABLE,true));

            }
        };



        locHandler =  new Handler() {
            @Override
            public void handleMessage(Message msg) {
                location.setText(curLocation);
                temperature.setText(pref.getString("curTemperature","00℃"));
                setWeather(pref.getString("curWeather","1"));
            }
        };

        // CO 값 화면에 변환
        class NewRunnable implements Runnable {
            @Override
            public void run(){
                while (true) {
                    try {
                        d = apiFF.main(doname);
                        editor.putString("meanavg_value",d);
                        editor.commit();
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread( ).interrupt( );
                    } catch (Exception e) {
                        e.printStackTrace() ;
                        Log.d(TAG, "@R 원인 : "+e);
                    }
                    mHandler.sendEmptyMessage(0);
                }
            }
        }
        NewRunnable nr = new NewRunnable();
        Thread t1 = new Thread(nr);
        t1.start();

        // TODO 현재 위치 값 1초마다 확인
        class LocRunnable implements Runnable {
            @Override
            public void run(){
                int permissionCheck = ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION);
                if(permissionCheck == PackageManager.PERMISSION_DENIED){ //위치 권한 확인
                    //위치 권한 요청
                    ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                }
                /*
                LocationManager locationManager = (LocationManager)getActivity().getSystemService(Context.LOCATION_SERVICE); // 마지막 위치 받아오기
                Location locCurrent = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                */
                LocationManager locationManager = (LocationManager)getActivity().getSystemService(LOCATION_SERVICE);
                List<String> providers = locationManager.getProviders(true);
                Location bestLocation = null;
                for (String provider : providers) {
                    Location l = locationManager.getLastKnownLocation(provider);
                    if (l == null) {
                        continue;
                    }
                    if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                        // Found best last known location: %s", l);
                        bestLocation = l;
                    }
                }
                Location locCurrent = bestLocation;
                Geocoder g = new Geocoder(getContext());
                while (true) {
                    // TODO 현재 이용자의 위치 정보를 pref에 저장
                    curLatitude = locCurrent.getLatitude();
                    curLongitude = locCurrent.getLongitude();

                    List<Address> address=null;
                    // Log.d(TAG, "cur => "+curLatitude + ", "+curLongitude);
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        Thread.currentThread( ).interrupt( );

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    try {
                        //TODO 여기있다!
                        Log.d(TAG, "baseDate => "+baseDate+", baseTime => "+baseTime);
                        weatherTemperature = ae.main(baseDate, baseTime, Integer.toString((int)Math.round(curLatitude)), Integer.toString((int)Math.round(curLongitude)));
                        curWeather = weatherTemperature.substring(0,1);
                        curTemperature = weatherTemperature.substring(1);
                        editor.putString("curWeather", curWeather);
                        editor.putString("curTemperature", curTemperature);
                        editor.commit();
                        Log.d(TAG, "현재 날씨 => " + curWeather + ", 현재 기온 => "+curTemperature);
                        // Log.d(TAG,"기상 정보 => " + baseDate +" "+ baseTime +" "+ Integer.toString((int)curLatitude) +" "+ Integer.toString((int)curLongitude));
                    } catch (Exception e) {
                        e.printStackTrace() ;
                        Log.d(TAG,"기상 정보 => False");
                    }
                    try {
                        address = g.getFromLocation(curLatitude,curLongitude,10);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d(TAG,"입출력오류");
                    }
                    if(address!=null){
                        if(address.size()==0){
                            Log.d(TAG,"오류 => ");
                        }else{
                            //Log.d(TAG,"찾은 주소 => " + address.get(0).toString());
                            doname =  address.get(0).getAdminArea();
                            curLocation = address.get(0).getAdminArea() +" "+ address.get(0).getLocality() + " " + address.get(0).getThoroughfare();
                        }
                    }
                    locHandler.sendEmptyMessage(0);
                }
            }
        }
        // TODO 현재 위치의 위도, 경도 값 1초마다 갱신
        LocRunnable lc = new LocRunnable() ;
        Thread t = new Thread(lc);
        t.start();

        // TODO led 밝기 조절 seek bar를 조정할 때 발생하는 EventHandler
        led_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) { // seek bar를 조작하고 있는 중
                if(i == 4){
                    led.setText("무드등"); // seek bar가 움직일 때 마다 percentage text의 값을 변경해 줍니다.
                    equalizer_switch.setEnabled(false);
                    editor.putBoolean(EQUALIZER_ENABLE,false);
                    editor.commit();
                }
                else if(i != 0){ // 만약 현재의 값이 0이 아니라면
                    led.setText(String.valueOf(i)+"단계"); // seek bar가 움직일 때 마다 percentage text의 값을 변경해 줍니다.
                    equalizer_switch.setEnabled(false);
                    editor.putBoolean(EQUALIZER_ENABLE,false);
                    editor.commit();
                }
                else { // 만약 현재의 값이 0 이라면
                    led.setText("OFF"); // text의 값을 "OFF"로 변경해 줍니다.
                    equalizer_switch.setEnabled(true);
                    editor.putBoolean(EQUALIZER_ENABLE,true);
                    editor.commit();
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { // seek bar를 처음 터치했을 때
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { // seek bar 터치가 끝났을 때
                led_value = seekBar.getProgress(); // 현재의 seekbar 값을 led_value에 저장
                editor.putInt("led_value", led_value);
                editor.commit();
                // TODO 블루투스를 통해 현재의 밝기값(brightness)을 랜턴에 전달!
            }
        });

        // TODO 침입 감지 스위치 On/Off시 발생하는 EventHandler
        invasion_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){ // 침입 감지 스위치가 On일 경우
                    invasion_boolean = true; // 현재의 스위치 ON/OFF 상태를 invasion_boolean에 저장
                    invasion.setText("ON");  // 침입 감지 On/Off 텍스트 "ON"으로 변경
                    Toast.makeText(getContext(),"침입 감지 기능을 실행합니다.",Toast.LENGTH_SHORT).show();
                }
                else{ // 침입 감지 스위치가 Off일 경우
                    invasion_boolean = false; // 현재의 스위치 ON/OFF 상태를 invasion_boolean에 저장
                    invasion.setText("OFF"); // 침입 감지 On/Off 텍스트 "Off"으로 변경
                    Toast.makeText(getContext(),"침입 감지 기능을 종료합니다.",Toast.LENGTH_SHORT).show();
                }
                editor.putBoolean("invasion_boolean",b );
                editor.commit();
            }
        });

        // TODO 모기 퇴치 스위치 On/Off시 발생하는 EventHandler
        mosquito_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){ // 모기 퇴치 스위치가 On일 경우
                    mosquito_boolean = true; // 현재의 스위치 ON/OFF 상태를 mosquito_boolean에 저장
                    mosquito.setText("ON"); // 모기 퇴치 On/Off 텍스트 "ON"으로 변경
                    Toast.makeText(getContext(),"모기 퇴치 기능을 실행합니다.",Toast.LENGTH_SHORT).show();

                    // TODO 블루투스를 통해 램프의 초음파 스피커가 작동되도록 하는 코드를 작성!!

                }
                else { // 모기 퇴치 스위치가 Off일 경우
                    mosquito_boolean = false; // 현재의 스위치 ON/OFF 상태를 mosquito_boolean에 저장
                    mosquito.setText("OFF"); // 모기 퇴치 On/Off 텍스트 "OFF"로 변경
                    Toast.makeText(getContext(),"모기 퇴치 기능을 종료합니다.",Toast.LENGTH_SHORT).show();
                }
                editor.putBoolean("mosquito_boolean", b);
                editor.commit();
            }
        });

        // NOTE 이퀄라이저 스위치 ON/OFF시 발생하는 EventHandler
        equalizer_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){ // 모기 퇴치 스위치가 On일 경우
                    equalizer_boolean = true; // 현재의 스위치 ON/OFF 상태를 mosquito_boolean에 저장
                    equalizer.setText("ON"); // 모기 퇴치 On/Off 텍스트 "ON"으로 변경
                    led_seekbar.setEnabled(false);
                    editor.putBoolean(LED_ENABLE,false);
                    editor.commit();
                    Toast.makeText(getContext(),"이퀄라이저 기능을 실행합니다.",Toast.LENGTH_SHORT).show();
                }
                else { // 모기 퇴치 스위치가 Off일 경우
                    equalizer_boolean = false; // 현재의 스위치 ON/OFF 상태를 mosquito_boolean에 저장
                    equalizer.setText("OFF"); // 모기 퇴치 On/Off 텍스트 "OFF"로 변경
                    led_seekbar.setEnabled(true);
                    editor.putBoolean(LED_ENABLE,true);
                    editor.commit();
                    Toast.makeText(getContext(),"이퀄라이저 기능을 종료합니다.",Toast.LENGTH_SHORT).show();
                }
                editor.putBoolean(EQUALIZER, b);
                editor.commit();
            }
        });
        return v; // inflater.inflate(R.layout.fragment_page_1, container, false);를 반환
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }
    
    // 앱 종료시 UI 상태를 저장한 값을 앱 재실행 시 UI에 적용하는 함수
    public void setState(View v){

        TextView led = v.findViewById(R.id.led); // led percentage 텍스트
        SeekBar led_seekbar = v.findViewById(R.id.seekBar); // led seek bar

        TextView co = v.findViewById(R.id.co); // 일산화탄소 On/Off 텍스트

        TextView detection = v.findViewById(R.id.detection); // 침입 감지 On/Off 텍스트
        Switch detection_switch = v.findViewById(R.id.detection_switch); // 침입 감지 Switch

        TextView mosquito = v.findViewById(R.id.mosquito); // 모기 퇴치 On/Off 텍스트
        Switch mosquito_switch = v.findViewById(R.id.mosquito_switch); // 모기 퇴치 Switch

        TextView equalizer = v.findViewById(R.id.equalizer); // 이퀄라이저 ON/OFF 텍스트
        Switch equalizer_switch = v.findViewById(R.id.equalizer_switch); // 이퀄라이저 Switch

        // 앱 종료시 UI 상태 정보를 저장한 pref
        pref = this.getActivity().getSharedPreferences("State",MODE_PRIVATE);

        // pref에 저장된 값을 반환
        led_value = pref.getInt("led_value",0);
        co_boolean = pref.getBoolean("co_boolean",false);
        invasion_boolean = pref.getBoolean("invasion_boolean",false);
        mosquito_boolean = pref.getBoolean("mosquito_boolean",false);
        equalizer_boolean = pref.getBoolean(EQUALIZER, false);

        // 반환받은 값을 현재 UI에 set!

        // led 파트
        led_seekbar.setProgress(led_value);
        if(led_value == 4){
            led.setText("무드등");
        } else {
            led.setText(led_value+"단계");
        }


        // 침입 감지 파트
        detection_switch.setChecked(invasion_boolean);
        if(invasion_boolean){
            detection.setText("ON");
        } else {
            detection.setText("OFF");
        }

        // 모기 퇴치 파트
        mosquito_switch.setChecked(mosquito_boolean);
        if(mosquito_boolean){
            mosquito.setText("ON");
        } else {
            mosquito.setText("OFF");
        }

        // 이퀄라이저 파트
        equalizer_switch.setChecked(equalizer_boolean);
        if(equalizer_boolean){
            equalizer.setText("ON");
        } else {
            equalizer.setText("OFF");
        }
    }

    // TODO 현재의 날짜를 String 형식으로 반환하는 함수
    private String getDate(){
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        String getTime = dateFormat.format(date);
        return getTime;
    }
    // TODO 현재의 시간을 String 형식으로 반환하는 함수
    private String getTime(){
        String dataTime = new SimpleDateFormat("HH").format(new Date(System.currentTimeMillis()));
        int check = (Integer.parseInt(dataTime)-1);
        dataTime = check + "30";
        return dataTime;
    }
    // TODO 현재 날씨 정보를 입력 받아 weatherImg와 weather Text의 값을 설정하는 함수
    private void setWeather(String curWeather){
        switch (curWeather){
            case "1": // 맑음
                weatherImg.setImageResource(R.drawable.sunny);
                //weatherText.setText("맑음");
                break;
            case "2": // 비
                weatherImg.setImageResource(R.drawable.raining);
                //weatherText.setText("비");
                break;
            case "3": // 구름
                weatherImg.setImageResource(R.drawable.cloud);
                //weatherText.setText("구름");
                break;
            case "4": // 흐린
                weatherImg.setImageResource(R.drawable.cloudy);
                //weatherText.setText("흐림");
                break;
        }
    }

    // NOTE 산불 예방 값 설정
    private void setProgressBar(View v, String d){
        ProgressBar pb1 = v.findViewById(R.id.pb1);
        ProgressBar pb2 = v.findViewById(R.id.pb2);
        ProgressBar pb3 = v.findViewById(R.id.pb3);
        ProgressBar pb4 = v.findViewById(R.id.pb4);

        TextView tv1 = v.findViewById(R.id.pb1_value);
        TextView tv2 = v.findViewById(R.id.pb2_value);
        TextView tv3 = v.findViewById(R.id.pb3_value);
        TextView tv4 = v.findViewById(R.id.pb4_value);

        String[] list = d.split(",");

        pb1.setProgress(Integer.parseInt(list[0]));
        pb2.setProgress(Integer.parseInt(list[1]));
        pb3.setProgress(Integer.parseInt(list[2]));
        pb4.setProgress(Integer.parseInt(list[3]));

        for(int i = 0; i < 4; i++){
            if(list[i].length() < 2){
                list[i] = "0" + list[i];
            }
        }

        tv1.setText(list[0]+"%");
        tv2.setText(list[1]+"%");
        tv3.setText(list[2]+"%");
        tv4.setText(list[3]+"%");
    }
}
