package com.example.lamp;

import android.graphics.Color;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.OvalShape;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FragmentPage1 extends Fragment {
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View v = inflater.inflate(R.layout.fragment_page_1, container, false);

        TextView led = v.findViewById(R.id.led); // led percentage 텍스트
        ImageView led_image = v.findViewById(R.id.led_image); // led image
        SeekBar led_seekbar = v.findViewById(R.id.led_seekBar); // led seek bar

        TextView co = v.findViewById(R.id.co); // 일산화탄소 On/Off 텍스트
        Switch co_switch = v.findViewById(R.id.co_switch); // 일산화탄소 Switch

        TextView detection = v.findViewById(R.id.detection); // 침입 감지 On/Off 텍스트
        Switch detection_switch = v.findViewById(R.id.detection_switch); // 침입 감지 Switch

        TextView mosquito = v.findViewById(R.id.mosquito); // 모기 퇴치 On/Off 텍스트
        Switch mosquito_switch = v.findViewById(R.id.mosquito_switch); // 모기 퇴치 Switch

        // TODO led 밝기 조절 seek bar를 조정할 때 발생하는 EventHandler
        led_seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) { // seek bar를 조작하고 있는 중
                if(i != 0){ // 만약 현재의 값이 0이 아니라면
                    led.setText(Integer.toString(i)+"%"); // seek bar가 움직일 때 마다 percentage text의 값을 변경해 줍니다.
                } else{ // 만약 현재의 값이 0 이라면
                    led.setText("OFF"); // percentage text의 값을 "Off"로 변경해 줍니다.
                }
                // 현재 led 밝기 percentage에 따라 led image backgroundColor를 변경해 줍니다.
                i *= 2.5;
                if(i<17){
                    String BGC = "#0" + Integer.toHexString(i) + "ffff00";
                    led_image.setBackgroundColor(Color.parseColor(BGC));
                } else{
                    String BGC = "#" + Integer.toHexString(i) + "ffff00";
                    led_image.setBackgroundColor(Color.parseColor(BGC));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { // seek bar를 처음 터치했을 때
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { // seek bar 터치가 끝났을 때
                int brightness = seekBar.getProgress(); // 현재의 밝기값
                if(brightness < 31){
                    led_image.setBackgroundColor(Color.parseColor("#00ffff00"));
                } else if(30 < brightness && brightness < 61){
                    led_image.setBackgroundColor(Color.parseColor("#99ffff00"));
                } else{
                    led_image.setBackgroundColor(Color.parseColor("#ffffff00"));
                }


                // TODO 블루투스를 통해 현재의 밝기값(brightness)을 랜턴에 전달!

            }
        });

        // TODO 일산화탄소 감지 스위치 On/Off시 발생하는 EventHandler
        co_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){ // 일산화탄소 감지 스위치가 On일 경우


                    co.setText("수정!!"); // 램프의 일산화탄소 감지센서에서 얻은 값을 블루투스를 통해 전달받으면 전달받은 값을 <Thread>를 통해 현재 화면의 값을 바꿔 줘야 함


                    Toast.makeText(getContext(),"일산화탄소 감지 기능을 실행합니다.",Toast.LENGTH_SHORT).show();
                }
                else{ // 일산화탄소 감지 스위치가 Off일 경우
                    co.setText("OFF"); // 일산화탄소 감지 On/Off 텍스트 "Off"으로 변경
                    Toast.makeText(getContext(),"일산화탄소 감지 기능을 종료합니다.",Toast.LENGTH_SHORT).show();
                }
            }
        });

        // TODO 침입 감지 스위치 On/Off시 발생하는 EventHandler
        detection_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){ // 침입 감지 스위치가 On일 경우
                    detection.setText("ON");  // 침입 감지 On/Off 텍스트 "ON"으로 변경
                    Toast.makeText(getContext(),"침입 감지 기능을 실행합니다.",Toast.LENGTH_SHORT).show();
                }
                else{ // 침입 감지 스위치가 Off일 경우
                    detection.setText("OFF"); // 침입 감지 On/Off 텍스트 "Off"으로 변경
                    Toast.makeText(getContext(),"침입 감지 기능을 종료합니다.",Toast.LENGTH_SHORT).show();
                }
            }
        });

        // TODO 모기 퇴치 스위치 On/Off시 발생하는 EventHandler
        mosquito_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b){ // 모기 퇴치 스위치가 On일 경우
                    mosquito.setText("ON"); // 모기 퇴치 On/Off 텍스트 "ON"으로 변경
                    Toast.makeText(getContext(),"모기 퇴치 기능을 실행합니다.",Toast.LENGTH_SHORT).show();


                    // TODO 블루투스를 통해 램프의 초음파 스피커가 작동되도록 하는 코드를 작성!!


                }
                else { // 모기 퇴치 스위치가 Off일 경우
                    mosquito.setText("OFF"); // 모기 퇴치 On/Off 텍스트 "OFF"로 변경
                    Toast.makeText(getContext(),"모기 퇴치 기능을 종료합니다.",Toast.LENGTH_SHORT).show();
                }
            }
        });


        return v; // inflater.inflate(R.layout.fragment_page_1, container, false);를 반환하는데 왜 반환하는지는 몰름;;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

}
