package com.example.androidinterview.module.plotter;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import com.example.androidinterview.module.mapper.FunctionMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 函数画图模块
 * 负责将函数表达式绘制成图表
 */
public class FunctionPlotter {

    /**
     * 绘图配置类
     */
    public static class PlotConfig {
        public double xMin = 0.1;
        public double xMax = 10;
        public int samplePoints = 1000;
        public boolean showOriginal = true;
        public boolean showDerivative = true;
        public int originalColor = android.graphics.Color.BLUE;
        public int derivativeColor = android.graphics.Color.RED;
        public float lineWidth = 2f;
        public boolean drawCircles = false;
        public boolean drawValues = false;

        public PlotConfig() {}

        public PlotConfig(double xMin, double xMax) {
            this.xMin = xMin;
            this.xMax = xMax;
        }

        public PlotConfig setRange(double xMin, double xMax) {
            this.xMin = xMin;
            this.xMax = xMax;
            return this;
        }

        public PlotConfig setSamplePoints(int points) {
            this.samplePoints = points;
            return this;
        }

        public PlotConfig setShowFunctions(boolean showOriginal, boolean showDerivative) {
            this.showOriginal = showOriginal;
            this.showDerivative = showDerivative;
            return this;
        }

        public PlotConfig setColors(int originalColor, int derivativeColor) {
            this.originalColor = originalColor;
            this.derivativeColor = derivativeColor;
            return this;
        }
    }

    /**
     * 绘图结果类
     */
    public static class PlotResult {
        public boolean success;
        public String errorMessage;
        public int originalPointsCount;
        public int derivativePointsCount;

        public PlotResult(boolean success) {
            this.success = success;
        }

        public PlotResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
    }

    /**
     * 主绘图方法
     */
    public static PlotResult plotFunctions(LineChart chart, String originalFunction, 
                                         String derivativeFunction, PlotConfig config) {
        if (chart == null) {
            return new PlotResult(false, "图表对象为空");
        }

        if (config == null) {
            config = new PlotConfig();
        }

        try {
            List<ILineDataSet> dataSets = new ArrayList<>();
            int originalCount = 0, derivativeCount = 0;

            // 绘制原函数
            if (config.showOriginal && originalFunction != null && !originalFunction.isEmpty()) {
                LineDataSet originalDataSet = createFunctionDataSet(
                    originalFunction, config, "原函数 f(x)", config.originalColor);
                if (originalDataSet != null) {
                    dataSets.add(originalDataSet);
                    originalCount = originalDataSet.getEntryCount();
                }
            }

            // 绘制导数函数 - 基于原函数的数值微分
            if (config.showDerivative && originalFunction != null && !originalFunction.isEmpty()) {
                LineDataSet derivativeDataSet = createNumericalDerivativeDataSet(
                    originalFunction, config, "导数 f'(x)", config.derivativeColor);
                if (derivativeDataSet != null) {
                    dataSets.add(derivativeDataSet);
                    derivativeCount = derivativeDataSet.getEntryCount();
                }
            }

            if (dataSets.isEmpty()) {
                return new PlotResult(false, "没有可绘制的数据");
            }

            // 设置图表数据
            LineData data = new LineData(dataSets);
            chart.setData(data);
            chart.notifyDataSetChanged();

            // 设置坐标轴范围
            chart.getXAxis().setAxisMinimum((float) config.xMin);
            chart.getXAxis().setAxisMaximum((float) config.xMax);

            // 自动调整Y轴范围
            chart.setAutoScaleMinMaxEnabled(true);
            chart.invalidate();

            PlotResult result = new PlotResult(true);
            result.originalPointsCount = originalCount;
            result.derivativePointsCount = derivativeCount;
            return result;

        } catch (Exception e) {
            return new PlotResult(false, "绘图失败: " + e.getMessage());
        }
    }

    /**
     * 创建函数数据集
     */
    private static LineDataSet createFunctionDataSet(String functionStr, PlotConfig config, 
                                                    String label, int color) {
        List<Entry> entries = new ArrayList<>();
        
        // 计算采样步长
        double step = (config.xMax - config.xMin) / config.samplePoints;
        
        // 获取定义域检查器
        FunctionMapper.DomainChecker domainChecker = FunctionMapper.getDomainChecker(functionStr);
        
        for (double x = config.xMin; x <= config.xMax; x += step) {
            // 检查是否在定义域内
            if (!domainChecker.isInDomain(x)) {
                continue;
            }
            
            try {
                double y = FunctionMapper.evaluateFunction(functionStr, x);
                
                // 检查结果是否有效
                if (!Double.isNaN(y) && !Double.isInfinite(y)) {
                    entries.add(new Entry((float) x, (float) y));
                }
            } catch (Exception e) {
                // 跳过无法计算的点
                continue;
            }
        }

        if (entries.isEmpty()) {
            return null;
        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setLineWidth(config.lineWidth);
        dataSet.setDrawCircles(config.drawCircles);
        dataSet.setDrawValues(config.drawValues);
        
        return dataSet;
    }

    /**
     * 创建数值微分数据集
     */
    private static LineDataSet createNumericalDerivativeDataSet(String functionStr, PlotConfig config, 
                                                         String label, int color) {
        List<Entry> entries = new ArrayList<>();
        
        // 计算采样步长
        double step = (config.xMax - config.xMin) / config.samplePoints;
        
        // 获取定义域检查器
        FunctionMapper.DomainChecker domainChecker = FunctionMapper.getDomainChecker(functionStr);
        
        for (double x = config.xMin; x <= config.xMax; x += step) {
            // 检查是否在定义域内
            if (!domainChecker.isInDomain(x)) {
                continue;
            }
            
            try {
                double derivative = NumericalDerivative.calculateDerivativeAdaptive(functionStr, x);
                
                // 检查结果是否有效
                if (!Double.isNaN(derivative) && !Double.isInfinite(derivative)) {
                    entries.add(new Entry((float) x, (float) derivative));
                }
            } catch (Exception e) {
                // 跳过无法计算的点
                continue;
            }
        }

        if (entries.isEmpty()) {
            return null;
        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(color);
        dataSet.setLineWidth(config.lineWidth);
        dataSet.setDrawCircles(config.drawCircles);
        dataSet.setDrawValues(config.drawValues);
        
        return dataSet;
    }

    /**
     * 简化的绘图方法（使用默认配置）
     */
    public static PlotResult plotFunctions(LineChart chart, String originalFunction, 
                                         String derivativeFunction) {
        return plotFunctions(chart, originalFunction, derivativeFunction, null);
    }

    /**
     * 设置图表样式
     */
    public static void setupChartStyle(LineChart chart) {
        if (chart == null) return;

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

    /**
     * 清空图表
     */
    public static void clearChart(LineChart chart) {
        if (chart != null) {
            chart.clear();
            chart.invalidate();
        }
    }

    /**
     * 获取推荐的绘图配置
     */
    public static PlotConfig getRecommendedConfig(String functionStr) {
        PlotConfig config = new PlotConfig();
        
        // 根据函数类型调整配置
        FunctionMapper.FunctionType type = FunctionMapper.FunctionTypeDetector.detectType(functionStr);
        
        switch (type) {
            case LOGARITHMIC:
                config.setRange(0.1, 10).setSamplePoints(500);
                break;
            case TRIGONOMETRIC:
                config.setRange(-Math.PI * 2, Math.PI * 2).setSamplePoints(1000);
                break;
            case EXPONENTIAL:
                config.setRange(-2, 2).setSamplePoints(500);
                break;
            case RATIONAL:
                config.setRange(-10, 10).setSamplePoints(1000);
                break;
            default:
                config.setRange(-10, 10).setSamplePoints(1000);
                break;
        }
        
        return config;
    }

    /**
     * 数值微分计算器（用于验证符号微分）
     */
    public static class NumericalDerivative {
        
        /**
         * 使用五点中心差分公式计算数值微分
         */
        public static double calculateDerivative(String functionStr, double x, double h) {
            try {
                // f'(x) ≈ (f(x-2h) - 8f(x-h) + 8f(x+h) - f(x+2h)) / (12h)
                double f_x_minus_2h = FunctionMapper.evaluateFunction(functionStr, x - 2*h);
                double f_x_minus_h = FunctionMapper.evaluateFunction(functionStr, x - h);
                double f_x_plus_h = FunctionMapper.evaluateFunction(functionStr, x + h);
                double f_x_plus_2h = FunctionMapper.evaluateFunction(functionStr, x + 2*h);

                // 检查所有值都有效
                if (!Double.isNaN(f_x_minus_2h) && !Double.isInfinite(f_x_minus_2h) &&
                    !Double.isNaN(f_x_minus_h) && !Double.isInfinite(f_x_minus_h) &&
                    !Double.isNaN(f_x_plus_h) && !Double.isInfinite(f_x_plus_h) &&
                    !Double.isNaN(f_x_plus_2h) && !Double.isInfinite(f_x_plus_2h)) {

                    return (f_x_minus_2h - 8*f_x_minus_h + 8*f_x_plus_h - f_x_plus_2h) / (12*h);
                }

                // 如果五点法失败，使用中心差分
                double yMinus = FunctionMapper.evaluateFunction(functionStr, x - h);
                double yPlus = FunctionMapper.evaluateFunction(functionStr, x + h);

                if (!Double.isNaN(yMinus) && !Double.isInfinite(yMinus) &&
                    !Double.isNaN(yPlus) && !Double.isInfinite(yPlus)) {
                    return (yPlus - yMinus) / (2 * h);
                }

            } catch (Exception e) {
                // 忽略错误
            }
            return Double.NaN;
        }

        /**
         * 自适应步长计算
         */
        public static double calculateDerivativeAdaptive(String functionStr, double x) {
            // 根据x的大小自适应步长
            double h = Math.min(0.0001, Math.abs(x) * 0.001);
            if (h == 0) h = 0.0001; // 避免h为0
            
            return calculateDerivative(functionStr, x, h);
        }
    }
}
