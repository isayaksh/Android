package com.example.lamp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class Login extends AppCompatActivity {

    // ↓ Access a Cloud Firestore instance from your Activity
    FirebaseFirestore db = FirebaseFirestore.getInstance();
    List<String> serials = new ArrayList<String>(); // Database에서 읽어온 serial number를 저장할 ArrayList
    String serial; // serial number를 임시로 저장할 공간
    SharedPreferences pref; // 앱 종료후 현재 상태를 저장

    public FSDialog fsDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        Button LoginButton = findViewById(R.id.login_button);
        EditText SerialNumber = findViewById(R.id.serial_number);
        TextView findSerial = findViewById(R.id.find_serial_number);
        TextView purchase = findViewById(R.id.purchase);
        pref = this.getSharedPreferences("State", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        // Cloud Firestore에서 serial collection에 해당하는 모든 serial number를 읽어서 serials ArrayList에 저장
        db.collection("serial")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                serial = document.getData().values().toString();
                                serial = serial.substring(1,serial.length() - 1);
                                serials.add(serial);
                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
        // 로그인 버튼을 클릭했을 때
        LoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                serial = SerialNumber.getText().toString();
                if(serial.equals("")){
                    Toast.makeText(Login.this, "Serial 번호를 입력하세요.", Toast.LENGTH_SHORT).show();
                } else if(CheckSerial(serial,serials)){ // 만약 Firestore에 serial 번호가 존재한다면
                    editor.putBoolean("LoginCache", true); // 로그인 캐시 = true
                    editor.putString("SerialNumber", serial); // 시리얼 번호 = 현재 로그인한 serial 번호
                    editor.commit();
                    Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(intent); // 메인으로 이동
                    finish();
                } else { // Firestore에 serial 번호가 존재하지 않는다면
                    Toast.makeText(Login.this, "Serial 번호를 다시 한번 확인하세요.", Toast.LENGTH_SHORT).show();
                }

            }
        });

        purchase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO 구매 이미지뷰를 클릭했을 때 ACTION
                // 구현하는건 에바겠지?
            }
        });

        findSerial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO 시리얼 번호 찾기 이미지뷰를 클릭했을 때 ACTION
                fsDialog = new FSDialog(Login.this);
                fsDialog.show();
                fsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }
        });
    }
    public boolean CheckSerial(String serial, List<String> serials){
        boolean check = false;
        for(String s : serials) { //for문을 통한 serials ArrayList 탐색
            if(s.equals(serial)){ // EditText에 입력한 serial과 같은 값이 존재한다면
                check = true; // check 변수에 true 할당
                break; // 반복문 종료
            }
        }
        return check; // check 변수 반환
    }
}