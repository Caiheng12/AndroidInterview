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

        // 三项多项式测试卡片点击事件
        findViewById(R.id.cardMoreFeatures).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FunctionDerivativeActivity.class);
            intent.putExtra("test_function", "x^2+x+1");
            startActivity(intent);
        });
        
        // 四项多项式测试卡片点击事件
        findViewById(R.id.cardPolynomialTest).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FunctionDerivativeActivity.class);
            intent.putExtra("test_function", "x^3+x^2+x+1");
            startActivity(intent);
        });
        
        // 乘法法则测试卡片点击事件
        findViewById(R.id.cardMultiplicationTest).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FunctionDerivativeActivity.class);
            intent.putExtra("test_function", "cos(x)*x");
            startActivity(intent);
        });
        
        // 复杂表达式测试卡片点击事件
        findViewById(R.id.cardComplexTest).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FunctionDerivativeActivity.class);
            intent.putExtra("test_function", "cos(x)*x+sin(x)*x");
            startActivity(intent);
        });
    }
}

