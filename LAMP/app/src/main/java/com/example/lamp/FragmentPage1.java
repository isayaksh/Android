package com.example.lamp;

import static android.content.Context.MODE_PRIVATE;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FragmentPage1 extends Fragment {

    public SharedPreferences pref; // 앱 종료후 현재 상태를 저장
    private static Handler mHandler;

    public int led_value; // 빛의 값을 저장할 변수
    public boolean co_boolean; // 일산화탄소 스위치 ON/OFF 값
    public String co_value = "OFF"; // 일산화탄소 현재 값
    public boolean invasion_boolean; // 침입 감지 스위치 ON/OFF 값
    public boolean mosquito_boolean; // 모기퇴치 스위치 ON/OFF 값

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View v = inflater.inflate(R.layout.fragment_page_1, container, false);

        TextView led = v.findViewById(R.id.led); // led percentage 텍스트
        SeekBar led_seekbar = v.findViewById(R.id.seekBar); // led seek bar

        TextView co = v.findViewById(R.id.co); // 일산화탄소 On/Off 텍스트
        Switch co_switch = v.findViewById(R.id.co_switch); // 일산화탄소 Switch

        TextView invasion = v.findViewById(R.id.detection); // 침입 감지 On/Off 텍스트
        Switch invasion_switch = v.findViewById(R.id.detection_switch); // 침입 감지 Switch

        TextView mosquito = v.findViewById(R.id.mosquito); // 모기 퇴치 On/Off 텍스트
        Switch mosquito_switch = v.findViewById(R.id.mosquito_switch); // 모기 퇴치 Switch

        setState(v);
        //앱 종료시 UI 상태 정보를 저장한 pref
        pref = this.getActivity().getSharedPreferences("State",MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();


        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                co.setText(co_value);
            }
        };
        class NewRunnable implements Runnable {
            @Override
            public void run(){
                while (co_boolean) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread( ).interrupt( );

                    } catch (Exception e) {
                        e.printStackTrace() ;
                    }
                    co_value = pref.getString("co_value","444");
                    mHandler.sendEmptyMessage(0) ;
                }
            }
        }

        // TODO led 밝기 조절 seek bar를 조정할 때 발생하는 EventHandler
        led_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) { // seek bar를 조작하고 있는 중
                if(i != 0){ // 만약 현재의 값이 0이 아니라면
                    led.setText(Integer.toString(i)+"%"); // seek bar가 움직일 때 마다 percentage text의 값을 변경해 줍니다.
                } else{ // 만약 현재의 값이 0 이라면
                    led.setText("OFF"); // percentage text의 값을 "Off"로 변경해 줍니다.
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

        // TODO 일산화탄소 감지 스위치 On/Off시 발생하는 EventHandler
        co_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                NewRunnable nr = new NewRunnable() ;
                Thread t = new Thread(nr) ;
                if(b){ // 일산화탄소 감지 스위치가 On일 경우
                    co_boolean = true; // 현재의 스위치 ON/OFF 상태를 co_boolean에 저장
                    t.start();
                    Toast.makeText(getContext(),"일산화탄소 감지 기능을 실행합니다.",Toast.LENGTH_SHORT).show();
                }
                else{ // 일산화탄소 감지 스위치가 Off일 경우
                    co_boolean = false; // 현재의 스위치 ON/OFF 상태를 co_boolean에 저장
                    co_value = "OFF";
                    Toast.makeText(getContext(),"일산화탄소 감지 기능을 종료합니다.",Toast.LENGTH_SHORT).show();
                }
                editor.putBoolean("co_boolean",b );
                editor.commit();
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
        Switch co_switch = v.findViewById(R.id.co_switch); // 일산화탄소 Switch

        TextView detection = v.findViewById(R.id.detection); // 침입 감지 On/Off 텍스트
        Switch detection_switch = v.findViewById(R.id.detection_switch); // 침입 감지 Switch

        TextView mosquito = v.findViewById(R.id.mosquito); // 모기 퇴치 On/Off 텍스트
        Switch mosquito_switch = v.findViewById(R.id.mosquito_switch); // 모기 퇴치 Switch

        // 앱 종료시 UI 상태 정보를 저장한 pref
        pref = this.getActivity().getSharedPreferences("State",MODE_PRIVATE);

        // pref에 저장된 값을 반환
        led_value = pref.getInt("led_value",0);
        co_boolean = pref.getBoolean("co_boolean",false);
        invasion_boolean = pref.getBoolean("invasion_boolean",false);
        mosquito_boolean = pref.getBoolean("mosquito_boolean",false);

        // 반환받은 값을 현재 UI에 set!

        // led 파트
        led_seekbar.setProgress(led_value);
        led.setText(led_value+"%");

        // 일산화탄소 파트
        co_switch.setChecked(co_boolean);
        if(co_boolean){
            co.setText("ON");
        } else {
            co.setText("OFF");
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
    }
}
