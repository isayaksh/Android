package com.example.lamp;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class InfoDialog extends Dialog{
    private Context context; // 객체
    private TextView info_text1; // Dialog Message1 부분
    private TextView info_text2; // Dialog Message1 부분
    private TextView info_text3; // Dialog Message1 부분
    private TextView info_text4; // Dialog Message1 부분
    private TextView info_text5; // Dialog Message1 부분
    private TextView info_text6; // Dialog Message1 부분
    private TextView info_close; // Dialog 닫기 버튼

    private String message1 =
            "1. LAMP 앱을 실행시키고 홈 화면 상단\n" +
            "    '블루투스로 제품 연결하기' 버튼을 눌러\n" +
            "    Camping SHIELD를 선택하여 연결하여\n" +
            "    주십시오.";
    private String message2 =
            "2. 날씨 정보는 현재 위치를 기반으로\n" +
            "    온도와 날씨 정보를 제공합니다.";
    private String message3 =
            "3. 일산화탄소 View를 클릭할 시\n" +
            "    단계 별 농도 정보를 확인할 수 있습니다.";
    private String message4 =
            "4. 하단 우측의 마이크 모양의 버튼을\n" +
            "    클릭하면 음성인식 기능을 사용할 수\n" +
            "    있습니다.";
    private String message5 =
            "5. 이퀄라이저 기능과 LED 기능을 함께\n"+
            "    사용할 수 없습니다.";
    private String message6 =
            "6. 소방서 & 경찰서 전화하기 기능은\n"+
            "    즉시 전화로 연결됩니다.";

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_dialog);

        info_text1 = findViewById(R.id.info_text1);
        info_text2 = findViewById(R.id.info_text2);
        info_text3 = findViewById(R.id.info_text3);
        info_text4 = findViewById(R.id.info_text4);
        info_text5 = findViewById(R.id.info_text5);
        info_text6 = findViewById(R.id.info_text6);

        info_text1.setText(message1);
        info_text2.setText(message2);
        info_text3.setText(message3);
        info_text4.setText(message4);
        info_text5.setText(message5);
        info_text6.setText(message6);

        info_close = findViewById(R.id.volume_ok);
        info_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss(); // Dialog 종료
            }
        });
    }

    public InfoDialog(Context context) {
        super(context);
        this.context = context;
    }

}
