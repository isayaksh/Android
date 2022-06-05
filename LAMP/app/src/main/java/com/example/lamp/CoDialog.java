package com.example.lamp;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class CoDialog extends Dialog {
    private Context context; // 객체
    private TextView info_text; // Dialog Message 부분
    private TextView info_close; // Dialog 닫기 버튼
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.co_dialog);

        info_close = findViewById(R.id.volume_ok);
        info_close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss(); // Dialog 종료
            }
        });
    }

    public CoDialog(Context context) {
        super(context);
        this.context = context;
    }
}
