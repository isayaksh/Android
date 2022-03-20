package com.example.lamp;

import static android.content.Context.MODE_PRIVATE;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FragmentPage4 extends Fragment {

    static String RING_TIME = "ringTime";
    static String SOUND_VOLUME = "soundVolume";


    public SharedPreferences pref; // 앱 종료후 현재 상태를 저장
    public Dialog RTD; // 소리 지속시간 Dialog
    public Dialog SVD; // 음량 조절 Dialog
    public TextView RV;

    static String[] time = new String[]{"OFF","5초", "10초", "15초"}; // 울림 시간 목록
    static String[] level = new String[]{"OFF","작게", "보통", "크게"}; // 울림 시간 목록

    public int ringTime; // 현재 설정된 울림 시간
    public int soundVolume; // 비상벨 음량

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        Log.e("TAG","onCreateView()");
        View v = inflater.inflate(R.layout.fragment_page_4, container, false);

        setState(v);

        View ring_time_box = v.findViewById(R.id.fire_station); // 울림 시간 조절 칸
        View sound_volume_box = v.findViewById(R.id.police_station); // 비상벨 볼륨 조절 칸
        TextView RV = v.findViewById(R.id.sound_volume); // 비상벨 음량

        RTD = new Dialog(getContext());
        RTD.requestWindowFeature(Window.FEATURE_NO_TITLE);
        RTD.setContentView(R.layout.dialog);

        SVD = new Dialog(getContext());
        SVD.requestWindowFeature(Window.FEATURE_NO_TITLE);
        SVD.setContentView(R.layout.dialog);

        ring_time_box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RingTimeClick(view);
            }
        });

        sound_volume_box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SoundVolumeClick(view);
            }
        });

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    // 울림 시간 조절 메뉴 [Dialog]
    public void RingTimeClick(View view) {
        // 앱 종료 전 저장한 앱의 상태값
        pref = this.getActivity().getSharedPreferences("State",MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        ringTime = pref.getInt(RING_TIME, 0);

        TextView tv = view.findViewById(R.id.ring_time);
        TextView ringTimeText = RTD.findViewById(R.id.current_value);
        ringTimeText.setText(time[ringTime]);

        TextView title = RTD.findViewById(R.id.title);
        title.setText("Ring Time");
        


        Button okButton = RTD.findViewById(R.id.ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putInt(RING_TIME,ringTime);
                editor.commit();
                tv.setText(time[ringTime]);
                RTD.dismiss();
            }
        });
        Button cancelButton = RTD.findViewById(R.id.cancle_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RTD.dismiss();
            }
        });

        SeekBar seekBar = RTD.findViewById(R.id.seekbar);
        seekBar.setProgress(ringTime);
        seekBar.setMax(3);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                ringTimeText.setText(time[i]);
                ringTime = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        RTD.show();
        RTD.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }




    public void SoundVolumeClick(View view){
        // 앱 종료 전 저장한 앱의 상태값
        pref = this.getActivity().getSharedPreferences("State",MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        soundVolume = pref.getInt(SOUND_VOLUME, 0);

        TextView tv = view.findViewById(R.id.sound_volume);
        TextView soundVolumeText = SVD.findViewById(R.id.current_value);
        soundVolumeText.setText(level[soundVolume]);

        TextView title = SVD.findViewById(R.id.title);
        title.setText("Sound Volume");



        Button okButton = SVD.findViewById(R.id.ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putInt(SOUND_VOLUME,soundVolume);
                editor.commit();
                tv.setText(level[soundVolume]);
                SVD.dismiss();
            }
        });
        Button cancelButton = SVD.findViewById(R.id.cancle_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SVD.dismiss();
            }
        });

        SeekBar seekBar = SVD.findViewById(R.id.seekbar);
        seekBar.setProgress(soundVolume);
        seekBar.setMax(3);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                soundVolumeText.setText(level[i]);
                soundVolume = i;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        SVD.show();
        SVD.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }

    public void setState(View v){
        pref = this.getActivity().getSharedPreferences("State",MODE_PRIVATE);
        ringTime = pref.getInt(RING_TIME,0);
        soundVolume = pref.getInt(SOUND_VOLUME,0);

        TextView ring_time_text = v.findViewById(R.id.ring_time);
        ring_time_text.setText(time[ringTime]);

        TextView sound_volume_text = v.findViewById(R.id.sound_volume);
        sound_volume_text.setText(level[soundVolume]);

        // 울림 시간 조절 < 끄기, 5초, 10초, 15초 >
    }
}
