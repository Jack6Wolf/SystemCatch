package com.jack.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.jack.demo.ui.login.LoginActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.bt).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e("TAG", "执行下去1");
        mockNullPointer();
        Log.e("TAG", "还会继续执行下去吗1");
        Log.e("TAG", "还会继续执行下去吗2");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e("TAG", "执行下去1");
//        mockNullPointer();
        Log.e("TAG", "还会继续执行下去吗1");
        Log.e("TAG", "还会继续执行下去吗2");
    }

    /**
     * 模拟一个空指针错误
     */
    private void mockNullPointer() {
        String s = null;

        if (s.equals("ss")) {
            Toast.makeText(this, "模拟空指针", Toast.LENGTH_SHORT).show();
        }
    }

}