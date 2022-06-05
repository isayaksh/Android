package com.example.lamp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class Splash extends AppCompatActivity {
    public boolean loginCheck = false;
    SharedPreferences pref; // 앱 종료후 현재 상태를 저장
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pref = this.getSharedPreferences("State",MODE_PRIVATE);
        loginCheck = pref.getBoolean("LoginCache",false); // 현재 LoginCache의 값을 loginCheck 변수에 할당
        // Log.d(TAG,   "LoginCache => " + loginCheck);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable(){
            @Override
            public void run(){
                if(loginCheck){
                    startActivity(new Intent(getApplication(), MainActivity.class));
                } else {
                    startActivity(new Intent(getApplication(), Login.class));
                }
                finish(); // 스플래시 액티비티를 스택에서 제거.
            }
        }, 1000);
    }
}
