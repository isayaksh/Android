package com.example.lamp;

import static android.content.Context.MODE_PRIVATE;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import org.w3c.dom.Text;

public class FragmentPage4 extends Fragment {

    static String RING_TIME = "ringTime";

    public SharedPreferences pref; // 앱 종료후 현재 상태를 저장

    public Dialog RTD; // 소리 지속시간 Dialog
    static String[] time = new String[]{"OFF","1초", "2초", "3초"}; // 울림 시간 목록
    public int ringTime; // 현재 설정된 울림 시간

    private static Handler mHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        Log.e("TAG","onCreateView()");
        View v = inflater.inflate(R.layout.fragment_page_4, container, false);
        setState(v);

        View ring_time_box = v.findViewById(R.id.fire_station); // 울림 시간 조절 칸
        View logOut = v.findViewById(R.id.logout); // 현재 기기 연결 해제

        TextView RV = v.findViewById(R.id.ring_time); // RV

        RTD = new Dialog(getContext());
        RTD.requestWindowFeature(Window.FEATURE_NO_TITLE);
        RTD.setContentView(R.layout.dialog);

        pref = this.getActivity().getSharedPreferences("State",MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                RV.setText(time[pref.getInt(RING_TIME,0)]);
            }
        };

        // CO 값 화면에 변환
        class NewRunnable implements Runnable {
            @Override
            public void run(){
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread( ).interrupt( );

                    } catch (Exception e) {
                        e.printStackTrace() ;
                    }
                    mHandler.sendEmptyMessage(0);
                }
            }
        }
        NewRunnable nr = new NewRunnable();
        Thread t1 = new Thread(nr);
        t1.start();

        ring_time_box.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                RingTimeClick(view);
            }
        });

        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                editor.putBoolean("LoginCache",false);
                editor.putString("SerialNumber",null);
                editor.commit();
                Intent intent = new Intent(getContext(), Login.class);
                startActivity(intent);
                ActivityCompat.finishAffinity(getActivity()); // TODO 현재 존재하는 모든 Activity Stack 제거!
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

        TextView title = RTD.findViewById(R.id.info_title);
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
    public void setState(View v){
        pref = this.getActivity().getSharedPreferences("State",MODE_PRIVATE);
        ringTime = pref.getInt(RING_TIME,0);

        TextView ring_time_text = v.findViewById(R.id.ring_time);
        ring_time_text.setText(time[ringTime]);
    }
}
