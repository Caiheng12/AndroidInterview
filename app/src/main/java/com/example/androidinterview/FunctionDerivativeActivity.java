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
        chart.setTouchEnabled(true);
        chart.getLegend().setEnabled(true);
        chart.getDescription().setEnabled(false);
        chart.setDrawGridBackground(false);
        
        // 设置坐标轴样式
        chart.getXAxis().setDrawGridLines(false);
        chart.getAxisLeft().setDrawGridLines(true);
        chart.getAxisRight().setEnabled(false);
        
        // 设置坐标轴标签格式
        chart.getXAxis().setLabelCount(8, false);
        chart.getAxisLeft().setLabelCount(8, false);
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
            String simplifiedDerivative = SymbolicDerivative.simplifyExpression(symbolicDerivative);
            currentDerivative = simplifiedDerivative;

            // 调试信息
            android.util.Log.d("FunctionDerivative", "原函数: " + functionStr);
            android.util.Log.d("FunctionDerivative", "符号微分结果: " + symbolicDerivative);
            android.util.Log.d("FunctionDerivative", "简化后: " + simplifiedDerivative);
            
            // 测试几个常见函数
            if (functionStr.equals("x^2+2*x")) {
                android.util.Log.d("FunctionDerivative", "测试: x^2+2*x 的导数应该是 2*x+2");
            } else if (functionStr.equals("x^2")) {
                android.util.Log.d("FunctionDerivative", "测试: x^2 的导数应该是 2*x");
            } else if (functionStr.equals("sin(x)")) {
                android.util.Log.d("FunctionDerivative", "测试: sin(x) 的导数应该是 cos(x)");
            } else if (functionStr.equals("log(x)")) {
                android.util.Log.d("FunctionDerivative", "测试: log(x) 的导数应该是 pow(x,-1) (自然对数)");
            } else if (functionStr.equals("exp(x)")) {
                android.util.Log.d("FunctionDerivative", "测试: exp(x) 的导数应该是 exp(x)");
            }
            
            // 测试exp4j解析
            if (functionStr.equals("log(x)") && !simplifiedDerivative.contains("pow")) {
                android.util.Log.e("FunctionDerivative", "ERROR: log(x)的导数应该是pow(x,-1)，但得到: " + simplifiedDerivative);
            }
            
            // 测试exp4j是否能解析不同格式
            if (functionStr.equals("log(x)")) {
                try {
                    Expression testExp1 = new ExpressionBuilder("1/x").variable("x").build();
                    testExp1.setVariable("x", 1.0);
                    double result1 = testExp1.evaluate();
                    android.util.Log.d("FunctionDerivative", "exp4j测试 1/x 在x=1时 = " + result1);
                } catch (Exception e) {
                    android.util.Log.e("FunctionDerivative", "exp4j无法解析1/x: " + e.getMessage());
                }
                
                try {
                    Expression testExp2 = new ExpressionBuilder("x^-1").variable("x").build();
                    testExp2.setVariable("x", 1.0);
                    double result2 = testExp2.evaluate();
                    android.util.Log.d("FunctionDerivative", "exp4j测试 x^-1 在x=1时 = " + result2);
                } catch (Exception e) {
                    android.util.Log.e("FunctionDerivative", "exp4j无法解析x^-1: " + e.getMessage());
                }
                
                try {
                    Expression testExp3 = new ExpressionBuilder("pow(x,-1)").variable("x").build();
                    testExp3.setVariable("x", 1.0);
                    double result3 = testExp3.evaluate();
                    android.util.Log.d("FunctionDerivative", "exp4j测试 pow(x,-1) 在x=1时 = " + result3);
                } catch (Exception e) {
                    android.util.Log.e("FunctionDerivative", "exp4j无法解析pow(x,-1): " + e.getMessage());
                }
            }

            // 显示函数信息
            tvOriginalFunction.setText("原函数: f(x) = " + functionStr);
            tvDerivativeFunction.setText("微分函数: f'(x) = " + simplifiedDerivative);
            llResults.setVisibility(View.VISIBLE);

            // 确保显示选项有默认选择并强制更新图表
            chipShowOriginal.setChecked(true);
            chipShowDerivative.setChecked(true);
            
            // 延迟更新图表，确保UI状态已设置
            chipShowOriginal.post(() -> {
                updateChart();
            });
            
        } catch (Exception e) {
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

        plotChart(currentFunction, currentDerivative, showOriginal, showDerivative);
    }

    private void plotChart(String functionStr, String derivativeStr, boolean showOriginal, boolean showDerivative) {
        List<ILineDataSet> dataSets = new ArrayList<>();

        // 使用更密集的采样点
        double step = (currentXMax - currentXMin) / 1000; // 1000个采样点
        double h = Math.min(0.0001, step / 10); // 自适应步长

        try {
            // 绘制原函数
            if (showOriginal) {
                List<Entry> functionEntries = new ArrayList<>();
                Expression expression = new ExpressionBuilder(functionStr)
                        .variable("x")
                        .build();

                for (double x = currentXMin; x <= currentXMax; x += step) {
                    try {
                        expression.setVariable("x", x);
                        double y = expression.evaluate();
                        
                        // 对于对数函数，只显示定义域内的值
                        if (functionStr.contains("log") && x <= 0) {
                            android.util.Log.d("FunctionDerivative", "跳过x=" + x + " (对数函数定义域外)");
                            continue; // 跳过负x和0
                        }
                        
                        if (!Double.isNaN(y) && !Double.isInfinite(y)) {
                            functionEntries.add(new Entry((float) x, (float) y));
                        }
                    } catch (Exception e) {
                        continue;
                    }
                }

                LineDataSet functionDataSet = new LineDataSet(functionEntries, "原函数 f(x)");
                functionDataSet.setColor(android.graphics.Color.BLUE);
                functionDataSet.setLineWidth(2f);
                functionDataSet.setDrawCircles(false);
                functionDataSet.setDrawValues(false);
                dataSets.add(functionDataSet);
            }

            // 绘制微分函数
            if (showDerivative) {
                List<Entry> derivativeEntries = new ArrayList<>();
                
                // 优先使用符号微分结果，如果解析失败则使用数值微分
                try {
                    Expression derivativeExpression = new ExpressionBuilder(derivativeStr)
                            .variable("x")
                            .build();

                    for (double x = currentXMin; x <= currentXMax; x += step) {
                        try {
                            derivativeExpression.setVariable("x", x);
                            double y = derivativeExpression.evaluate();
                            
                            // 对于对数函数的导数，只显示定义域内的值
                            if (functionStr.contains("log") && x <= 0) {
                                android.util.Log.d("FunctionDerivative", "跳过导数x=" + x + " (对数函数定义域外)");
                                continue; // 跳过负x和0
                            }
                            
                            if (!Double.isNaN(y) && !Double.isInfinite(y)) {
                                android.util.Log.d("FunctionDerivative", "添加导数点: x=" + x + ", y=" + y);
                                derivativeEntries.add(new Entry((float) x, (float) y));
                            }
                        } catch (Exception e) {
                            // 如果符号微分失败，使用数值微分
                            double numericalDerivative = calculateNumericalDerivative(functionStr, x, h);
                            
                            // 对于对数函数的导数，只显示定义域内的值
                            if (functionStr.contains("log") && x <= 0) {
                                continue; // 跳过负x和0
                            }
                            
                            if (!Double.isNaN(numericalDerivative) && !Double.isInfinite(numericalDerivative)) {
                                derivativeEntries.add(new Entry((float) x, (float) numericalDerivative));
                            }
                        }
                    }
                } catch (Exception e) {
                    // 符号微分表达式解析失败，完全使用数值微分
                    Expression originalExpression = new ExpressionBuilder(functionStr)
                            .variable("x")
                            .build();

                    for (double x = currentXMin; x <= currentXMax; x += step) {
                        try {
                            double numericalDerivative = calculateNumericalDerivative(originalExpression, x, h);
                            
                            // 对于对数函数的导数，只显示定义域内的值
                            if (functionStr.contains("log") && x <= 0) {
                                continue; // 跳过负x和0
                            }
                            
                            if (!Double.isNaN(numericalDerivative) && !Double.isInfinite(numericalDerivative)) {
                                derivativeEntries.add(new Entry((float) x, (float) numericalDerivative));
                            }
                        } catch (Exception ex) {
                            continue;
                        }
                    }
                }

                LineDataSet derivativeDataSet = new LineDataSet(derivativeEntries, "微分函数 f'(x)");
                derivativeDataSet.setColor(android.graphics.Color.RED);
                derivativeDataSet.setLineWidth(2f);
                derivativeDataSet.setDrawCircles(false);
                derivativeDataSet.setDrawValues(false);
                dataSets.add(derivativeDataSet);
            }

            LineData data = new LineData(dataSets);
            chart.setData(data);
            chart.notifyDataSetChanged();
            
            // 设置初始视图范围
            chart.getXAxis().setAxisMinimum((float) currentXMin);
            chart.getXAxis().setAxisMaximum((float) currentXMax);
            
            // 自动调整Y轴范围
            chart.setAutoScaleMinMaxEnabled(true);
            
            chart.invalidate();
            
            // 初始调整刻度
           // adjustAxisLabels();

        } catch (Exception e) {
            Toast.makeText(this, "计算错误: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
