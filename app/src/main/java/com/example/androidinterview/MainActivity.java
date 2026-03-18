package com.example.androidinterview;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupMenuListeners();
    }

    private void setupMenuListeners() {
        // 函数微分计算器卡片点击事件
        findViewById(R.id.cardFunctionDerivative).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FunctionDerivativeActivity.class);
            startActivity(intent);
        });

        // 更多功能卡片点击事件
        findViewById(R.id.cardMoreFeatures).setOnClickListener(v -> {
            Toast.makeText(this, "更多功能即将推出", Toast.LENGTH_SHORT).show();
        });
    }
}

