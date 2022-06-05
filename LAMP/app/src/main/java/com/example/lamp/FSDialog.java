package com.example.lamp;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class FSDialog extends Dialog {
    private Context context; // 객체
    private TextView info_title; // Dialog Title 부분
    private TextView info_text; // Dialog Message 부분
    private TextView info_close; // Dialog 닫기 버튼
    private String message = "Camping SHIELD LAMP 제품의 하단 부분에 \n영어 4글자 + 숫자 4개로 구성된 시리얼 번호를\n확인하실 수 있습니다.";
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fsd_dialog);

        info_title = findViewById(R.id.info_title);
        info_title.setText("시리얼 번호 찾기");
        info_text = findViewById(R.id.info_text1);
        info_text.setText(message);

        info_close = findViewById(R.id.volume_ok);
        info_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss(); // Dialog 종료
            }
        });
    }
    public FSDialog(Context context) {
        super(context);
        this.context = context;
    }
}
