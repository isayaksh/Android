package com.example.lamp;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class InfoDialog extends Dialog{
    private Context context; // 객체
    private TextView info_text; // Dialog Message 부분
    private TextView info_close; // Dialog 닫기 버튼
    private String message = "1. 캠지 앱을 실행시키고 홈 화면 상단\n    '블루투스로 제품 연결하기' 버튼을 눌러\n    Cam.G를 선택하여 연결하여 주십시오.\n    캠지플러스의 경우, 초기 비밀번호를\n" +
            "    잃어버렸을 경우, 고객센터로 연락해\n    초기화코드를 받아서 입력해야 합니다.\n    초기화 코드는 매일 변경되어 해당 일에만\n    유효합니다(캠지미니와 캠지에어는\n    비밀번호가 필요 없습니다.";
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.info_dialog);

        info_text = findViewById(R.id.info_text);
        info_text.setText(message);

        info_close = findViewById(R.id.info_close);
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
