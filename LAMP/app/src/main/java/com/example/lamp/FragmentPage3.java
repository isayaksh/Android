package com.example.lamp;

import static android.content.Context.MODE_PRIVATE;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class FragmentPage3 extends Fragment {

    public SharedPreferences pref; // 앱 종료후 현재 상태를 저장
    public String fireStation = "tel:000100010009"; // 소방서 전화번호
    public String policeStation = "tel:000100010002"; // 경찰서 전화번호
    public Dialog dlg; // 전화 확인용 Dialog
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        dlg = new Dialog(getContext());
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dlg.setContentView(R.layout.basic_dialog);

        pref = this.getActivity().getSharedPreferences("State",MODE_PRIVATE);

        View v = inflater.inflate(R.layout.fragment_page_3, container, false);
        View fs = v.findViewById(R.id.fire_station); // fire station 뷰
        fs.setOnClickListener(new View.OnClickListener() { // TODO fire station 뷰를 클릭했을 때
            @Override
            public void onClick(View view) {
                DialClick(view, "소방서");
            }
        });
        View ps = v.findViewById(R.id.police_station); // police station 뷰
        ps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DialClick(view, "경찰서");
            }
        });

        v.findViewById(R.id.emergency_bell).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean check = pref.getBoolean("emergencyBell",false);
                EmergencyDialClick(check);
            }
        });
        return v;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    public void DialClick(View view, String str) {
        TextView text = dlg.findViewById(R.id.notification);
        text.setText(str+" 전화하시겠습니까?"); // 내용
        TextView title = dlg.findViewById(R.id.info_title);
        title.setText(str);
        Button okButton = dlg.findViewById(R.id.ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(str == "소방서"){
                    Intent it = new Intent("android.intent.action.CALL", Uri.parse(fireStation));
                    Permission(it);
                    dlg.dismiss();
                } else {
                    Intent it = new Intent("android.intent.action.CALL", Uri.parse(policeStation));
                    Permission(it);
                    dlg.dismiss();
                }
            }
        });
        Button cancelButton = dlg.findViewById(R.id.cancle_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dlg.dismiss();
            }
        });

        dlg.show();
        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
    public void EmergencyDialClick(boolean check) {
        pref = this.getActivity().getSharedPreferences("State",MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        TextView text = dlg.findViewById(R.id.notification);
        TextView title = dlg.findViewById(R.id.info_title);
        title.setText("비상벨");
        if(check){
            text.setText("비상벨을 종료하시겠습니까?"); // 내용
        } else {
            text.setText("비상벨을 켜시겠습니까?"); // 내용
        }

        Button okButton = dlg.findViewById(R.id.ok_button);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(check){
                    editor.putBoolean("emergencyBell",false);
                    editor.commit();
                    dlg.dismiss();
                } else {
                    editor.putBoolean("emergencyBell",true);
                    editor.commit();
                    dlg.dismiss();
                }
            }
        });

        Button cancelButton = dlg.findViewById(R.id.cancle_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dlg.dismiss();
            }
        });

        dlg.show();
        dlg.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
    }
    public void Permission(Intent intent){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 현재 버전이 마시멜로 버전 이상이라면
            int permissionResult = getContext().checkSelfPermission(Manifest.permission.CALL_PHONE);
            if (permissionResult == PackageManager.PERMISSION_DENIED) {
                if (shouldShowRequestPermissionRationale(Manifest.permission.CALL_PHONE)) {
                    AlertDialog.Builder dialog = new AlertDialog.Builder(getContext());
                    dialog.setTitle("권한이 필요합니다.")
                            .setMessage("이 기능을 사용하기 위해서는 단말기의 \"전화걸기\" 권한이 필요합니다. 계속하시겠습니까?")
                            .setPositiveButton("네", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 1000);
                                    }
                                }
                            })
                            .setNegativeButton("아니오", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getContext(), "기능을 취소했습니다.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .create()
                            .show();
                }
                //최초로 권한을 요청할 때
                else {
                    // CALL_PHONE 권한을 Android OS 에 요청한다.
                    requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, 1000);
                }
            }
            /* CALL_PHONE의 권한이 있을 때 */
            else {
                startActivity(intent);
            }
        }
        /* 사용자의 OS 버전이 마시멜로우 이하일 떄 */
        else {
            startActivity(intent);
        }
    }

}
