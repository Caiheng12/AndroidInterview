package com.example.androidinterview;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.ArrayList;
import java.util.List;

import com.example.androidinterview.module.derivative.core.SymbolicDerivative;
import com.example.androidinterview.module.mapper.FunctionMapper;
import com.example.androidinterview.module.plotter.FunctionPlotter;

public class FunctionDerivativeActivity extends AppCompatActivity {

    private EditText etFunctionInput;
    private EditText etXMin, etXMax;
    private TextView tvOriginalFunction, tvDerivativeFunction;
    private LinearLayout llResults;
    private LineChart chart;
    private Button btnCalculate, btnApplyDomain, btnUpdateChart;
    private ImageButton btnBack;
    private ChipGroup chipGroupDisplay;
    private Chip chipShowOriginal, chipShowDerivative;

    private double currentXMin = 0.1;
    private double currentXMax = 10;
    private String currentFunction = "";
    private String currentDerivative = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_function_derivative);

        initViews();
        setupChart();
        setupExampleChips();
        setupListeners();

        // 检查是否有测试函数
        String testFunction = getIntent().getStringExtra("test_function");
        if (testFunction != null && !testFunction.isEmpty()) {
            etFunctionInput.setText(testFunction);
            calculateDerivative(); // 自动计算
        }
    }

    private void initViews() {
        etFunctionInput = findViewById(R.id.etFunctionInput);
        etXMin = findViewById(R.id.etXMin);
        etXMax = findViewById(R.id.etXMax);
        tvOriginalFunction = findViewById(R.id.tvOriginalFunction);
        tvDerivativeFunction = findViewById(R.id.tvDerivativeFunction);
        llResults = findViewById(R.id.llResults);
        chart = findViewById(R.id.chart);
        btnCalculate = findViewById(R.id.btnCalculate);
        btnApplyDomain = findViewById(R.id.btnApplyDomain);
        btnUpdateChart = findViewById(R.id.btnUpdateChart);
        btnBack = findViewById(R.id.btnBack);
        chipGroupDisplay = findViewById(R.id.chipGroupDisplay);
        chipShowOriginal = findViewById(R.id.chipShowOriginal);
        chipShowDerivative = findViewById(R.id.chipShowDerivative);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish()); // 返回上一页

        btnCalculate.setOnClickListener(v -> calculateDerivative());
        btnApplyDomain.setOnClickListener(v -> applyDomain());
        btnUpdateChart.setOnClickListener(v -> updateChart());

        // 设置默认选中状态
        chipShowOriginal.setChecked(true);
        chipShowDerivative.setChecked(true);

        // 为每个Chip单独设置监听器
        chipShowOriginal.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!currentFunction.isEmpty()) {
                updateChart();
            }
        });

        chipShowDerivative.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!currentFunction.isEmpty()) {
                updateChart();
            }
        });
    }

    private void setupChart() {
        // 使用模块化图表设置
        FunctionPlotter.setupChartStyle(chart);
    }

    private void setupExampleChips() {
        findViewById(R.id.chipExample1).setOnClickListener(v ->
                etFunctionInput.setText("sin(x)"));

        findViewById(R.id.chipExample2).setOnClickListener(v ->
                etFunctionInput.setText("x^2 + 2*x"));

        findViewById(R.id.chipExample3).setOnClickListener(v ->
                etFunctionInput.setText("log(x)"));

        findViewById(R.id.chipExample4).setOnClickListener(v ->
                etFunctionInput.setText("exp(x)"));

        findViewById(R.id.chipExample5).setOnClickListener(v ->
                etFunctionInput.setText("cos(x) * x"));
    }

    private void applyDomain() {
        try {
            double xMin = Double.parseDouble(etXMin.getText().toString());
            double xMax = Double.parseDouble(etXMax.getText().toString());

            if (xMin >= xMax) {
                Toast.makeText(this, "最小值必须小于最大值", Toast.LENGTH_SHORT).show();
                return;
            }

            currentXMin = xMin;
            currentXMax = xMax;

            if (!currentFunction.isEmpty()) {
                updateChart();
            }

            Toast.makeText(this, "定义域已更新", Toast.LENGTH_SHORT).show();

        } catch (NumberFormatException e) {
            Toast.makeText(this, "请输入有效的数字", Toast.LENGTH_SHORT).show();
        }
    }

    private void calculateDerivative() {
        String functionStr = etFunctionInput.getText().toString().trim();

        if (functionStr.isEmpty()) {
            Toast.makeText(this, "请输入函数表达式", Toast.LENGTH_SHORT).show();
            return;
        }

        currentFunction = functionStr;

        try {
            // 计算符号微分
            String symbolicDerivative = SymbolicDerivative.differentiate(functionStr);
            currentDerivative = symbolicDerivative;

            // 调试信息
            android.util.Log.d("FunctionDerivative", "原函数: " + functionStr);
            android.util.Log.d("FunctionDerivative", "符号微分结果: " + symbolicDerivative);

            // 测试几个常见函数
            if (functionStr.equals("x^2+2*x")) {
                android.util.Log.d("FunctionDerivative", "测试: x^2+2*x 的导数应该是 2*x+2");
            } else if (functionStr.equals("x^2")) {
                android.util.Log.d("FunctionDerivative", "测试: x^2 的导数应该是 2*x");
            } else if (functionStr.equals("sin(x)")) {
                android.util.Log.d("FunctionDerivative", "测试: sin(x) 的导数应该是 cos(x)");
            } else if (functionStr.equals("log(x)")) {
                android.util.Log.d("FunctionDerivative", "测试: log(x) 的导数应该是 1/x (自然对数)");
            } else if (functionStr.equals("exp(x)")) {
                android.util.Log.d("FunctionDerivative", "测试: exp(x) 的导数应该是 exp(x)");
            }

            if (functionStr.equals("cos(x)*x")) {
                android.util.Log.d("FunctionDerivative", "测试: cos(x)*x 的导数应该是 -sin(x)*x+cos(x)");
            }

            if (functionStr.equals("x^2/x")) {
                android.util.Log.d("FunctionDerivative", "测试: x^2/x 的导数应该是 1");
            }

            if (functionStr.equals("cos(x)*x+sin(x)*x")) {
                android.util.Log.d("FunctionDerivative", "测试: cos(x)*x+sin(x)*x 的导数应该是 (-sin(x)*x+cos(x))+(cos(x)*x+sin(x))");
            }

            // 显示函数信息
            tvOriginalFunction.setText("原函数: f(x) = " + functionStr);
            tvDerivativeFunction.setText("微分函数: f'(x) = " + symbolicDerivative);
            llResults.setVisibility(View.VISIBLE);

            // 确保显示选项有默认选择并强制更新图表
            chipShowOriginal.setChecked(true);
            chipShowDerivative.setChecked(true);

            // 延迟更新图表，确保UI状态已设置
            chipShowOriginal.post(() -> {
                updateChart();
            });

        } catch (Exception e) {
            android.util.Log.e("FunctionDerivative", "求导失败", e);
            Toast.makeText(this, "函数解析错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void updateChart() {
        if (currentFunction.isEmpty()) {
            return;
        }

        boolean showOriginal = chipShowOriginal.isChecked();
        boolean showDerivative = chipShowDerivative.isChecked();

        // 调试信息
        android.util.Log.d("FunctionDerivative", "showOriginal: " + showOriginal + ", showDerivative: " + showDerivative);

        // 确保至少有一个选项被选中
        if (!showOriginal && !showDerivative) {
            Toast.makeText(this, "请至少选择一个显示选项", Toast.LENGTH_SHORT).show();
            return;
        }

        // 创建绘图配置
        FunctionPlotter.PlotConfig config = new FunctionPlotter.PlotConfig(currentXMin, currentXMax)
                .setShowFunctions(showOriginal, showDerivative);

        // 使用模块化绘图
        FunctionPlotter.PlotResult result = FunctionPlotter.plotFunctions(
                chart, currentFunction, currentDerivative, config);

        if (!result.success) {
            android.util.Log.e("FunctionDerivative", "绘图失败: " + result.errorMessage);
            Toast.makeText(this, "绘图错误: " + result.errorMessage, Toast.LENGTH_SHORT).show();
        } else {
            android.util.Log.d("FunctionDerivative", "绘图成功 - 原函数点数: " + result.originalPointsCount + 
                    ", 导数点数: " + result.derivativePointsCount);
        }
    }

    private double calculateNumericalDerivative(String functionStr, double x, double h) {
        try {
            Expression expression = new ExpressionBuilder(functionStr)
                    .variable("x")
                    .build();
            return calculateNumericalDerivative(expression, x, h);
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    private double calculateNumericalDerivative(Expression expression, double x, double h) {
        try {
            // 使用五点中心差分公式提高精度
            // f'(x) ≈ (f(x-2h) - 8f(x-h) + 8f(x+h) - f(x+2h)) / (12h)
            expression.setVariable("x", x - 2*h);
            double f_x_minus_2h = expression.evaluate();

            expression.setVariable("x", x - h);
            double f_x_minus_h = expression.evaluate();

            expression.setVariable("x", x + h);
            double f_x_plus_h = expression.evaluate();

            expression.setVariable("x", x + 2*h);
            double f_x_plus_2h = expression.evaluate();

            // 检查所有值都有效
            if (!Double.isNaN(f_x_minus_2h) && !Double.isInfinite(f_x_minus_2h) &&
                    !Double.isNaN(f_x_minus_h) && !Double.isInfinite(f_x_minus_h) &&
                    !Double.isNaN(f_x_plus_h) && !Double.isInfinite(f_x_plus_h) &&
                    !Double.isNaN(f_x_plus_2h) && !Double.isInfinite(f_x_plus_2h)) {

                return (f_x_minus_2h - 8*f_x_minus_h + 8*f_x_plus_h - f_x_plus_2h) / (12*h);
            }

            // 如果五点法失败，使用中心差分
            expression.setVariable("x", x - h);
            double yMinus = expression.evaluate();
            expression.setVariable("x", x + h);
            double yPlus = expression.evaluate();

            if (!Double.isNaN(yMinus) && !Double.isInfinite(yMinus) &&
                    !Double.isNaN(yPlus) && !Double.isInfinite(yPlus)) {
                return (yPlus - yMinus) / (2 * h);
            }

        } catch (Exception e) {
            // 忽略错误
        }
        return Double.NaN;
    }

}