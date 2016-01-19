package com.whee.wheetalklollipop.fragment;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.whee.wheetalklollipop.Features;
import com.whee.wheetalklollipop.R;
import com.whee.wheetalklollipop.service.MyService;
import com.whee.wheetalklollipop.util.BackgroundUtil;

import java.util.ArrayList;


/**
 * Created by wenmingvs on 2016/1/14.
 */
public class OneFragment extends Fragment {

    private Context mContext;
    private View mView;

    private CheckBox checkBox1, checkBox2, checkBox3, checkBox4, checkBox5;
    private TextView mTextView;
    private ArrayList<String> reminderlist;
    private NotificationManager notificationManager;

    public OneFragment(Context context) {
        mContext = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        reminderlist = new ArrayList<String>();
        reminderlist.add(getResources().getString(R.string.reminder1));
        reminderlist.add(getResources().getString(R.string.reminder2));
        reminderlist.add(getResources().getString(R.string.reminder3));
        reminderlist.add(getResources().getString(R.string.reminder4));
        reminderlist.add(getResources().getString(R.string.reminder5));
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_one, container, false);
        initCheckBox();
        initTextView();
        layoutClick();
        return mView;
    }

    private void initTextView() {
        mTextView = (TextView) mView.findViewById(R.id.reminder);
    }

    private void startService() {
        Features.stopForeground = false;
        Intent intent = new Intent(mContext, MyService.class);
        mContext.startService(intent);
    }

    private void stopService() {
        Features.stopForeground = true;
        Intent intent = new Intent(mContext, MyService.class);
        mContext.stopService(intent);
    }

    private void initCheckBox() {
        checkBox1 = (CheckBox) mView.findViewById(R.id.checkbox1);
        checkBox2 = (CheckBox) mView.findViewById(R.id.checkbox2);
        checkBox3 = (CheckBox) mView.findViewById(R.id.checkbox3);
        checkBox4 = (CheckBox) mView.findViewById(R.id.checkbox4);
        checkBox5 = (CheckBox) mView.findViewById(R.id.checkbox5);
        checkBox1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {
                    startService();
                    deselectAll();
                    checkBox1.setChecked(true);
                    Features.BGK_METHOD = BackgroundUtil.BKGMETHOD_GETRUNNING_TASK;
                    mTextView.setText(reminderlist.get(Features.BGK_METHOD));
                } else {
                    stopService();
                }
            }
        });
        checkBox2.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {
                    startService();
                    deselectAll();
                    checkBox2.setChecked(true);
                    Features.BGK_METHOD = BackgroundUtil.BKGMETHOD_GETRUNNING_PROCESS;
                    mTextView.setText(reminderlist.get(Features.BGK_METHOD));
                } else {
                    stopService();
                }
            }
        });
        checkBox3.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {
                    startService();
                    deselectAll();
                    checkBox3.setChecked(true);
                    Features.BGK_METHOD = BackgroundUtil.BKGMETHOD_GETAPPLICATION_VALUE;
                    mTextView.setText(reminderlist.get(Features.BGK_METHOD));
                } else {
                    stopService();
                }
            }
        });
        checkBox4.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        startService();
                        deselectAll();
                        checkBox4.setChecked(true);
                        Features.BGK_METHOD = BackgroundUtil.BKGMETHOD_GETUSAGESTATS;
                        mTextView.setText(reminderlist.get(Features.BGK_METHOD));
                    } else {
                        Toast.makeText(mContext, "此方法需要在Android5.0以上才能使用！", Toast.LENGTH_SHORT).show();
                        checkBox4.setChecked(false);
                    }
                } else {
                    stopService();
                }
            }
        });

        checkBox5.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked == true) {
                    startService();
                    deselectAll();
                    checkBox5.setChecked(true);
                    Features.BGK_METHOD = BackgroundUtil.BKGMETHOD_GETLINUXPROCESS;
                    mTextView.setText(reminderlist.get(Features.BGK_METHOD));
                } else {
                    stopService();
                }
            }
        });


    }

    public void layoutClick() {
        RelativeLayout relativeLayout = (RelativeLayout) mView.findViewById(R.id.clearForeground);
        relativeLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Features.stopForeground = true;
                Intent intent = new Intent(mContext, MyService.class);
                mContext.stopService(intent);
                deselectAll();
            }
        });
    }


    private void deselectAll() {
        checkBox1.setChecked(false);
        checkBox2.setChecked(false);
        checkBox3.setChecked(false);
        checkBox4.setChecked(false);
        checkBox5.setChecked(false);
    }

}
