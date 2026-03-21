package com.example.androidinterview.module.mapper;

import net.objecthunter.exp4j.Expression;
import net.objecthunter.exp4j.ExpressionBuilder;

import java.util.List;

/**
 * 函数映射模块
 * 负责将符号表达式转换为可计算的数值表达式，并进行数值计算
 */
public class FunctionMapper {

    /**
     * 将符号表达式转换为可计算的数值表达式
     */
    public static Expression mapToExpression(String functionStr) {
        try {
            // 转换pow函数为exp4j支持的格式
            String convertedExpr = convertPowToExp4jFormat(functionStr);
            ExpressionBuilder builder = new ExpressionBuilder(convertedExpr)
                    .variable("x");
            
            // 添加pow函数支持，用于非整数指数
            builder.function(new net.objecthunter.exp4j.function.Function("pow", 2) {
                @Override
                public double apply(double... args) {
                    return Math.pow(args[0], args[1]);
                }
            });
            
            return builder.build();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 将pow函数转换为exp4j支持的格式
     */
    private static String convertPowToExp4jFormat(String expr) {
        // 保持pow格式，因为已经添加了pow函数支持
        return expr;
    }

    /**
     * 计算函数在给定x值下的数值
     */
    public static double evaluateFunction(String functionStr, double x) {
        try {
            Expression expression = mapToExpression(functionStr);
            if (expression != null) {
                expression.setVariable("x", x);
                return expression.evaluate();
            }
        } catch (Exception e) {
            // 忽略计算错误
        }
        return Double.NaN;
    }

    /**
     * 批量计算函数值
     */
    public static double[] evaluateFunctionBatch(String functionStr, double[] xValues) {
        double[] results = new double[xValues.length];
        Expression expression = mapToExpression(functionStr);
        
        if (expression == null) {
            // 填充NaN
            for (int i = 0; i < results.length; i++) {
                results[i] = Double.NaN;
            }
            return results;
        }

        for (int i = 0; i < xValues.length; i++) {
            try {
                expression.setVariable("x", xValues[i]);
                results[i] = expression.evaluate();
            } catch (Exception e) {
                results[i] = Double.NaN;
            }
        }
        
        return results;
    }

    /**
     * 检查表达式是否在给定x值处有定义
     */
    public static boolean isDefinedAt(String functionStr, double x) {
        try {
            double result = evaluateFunction(functionStr, x);
            return !Double.isNaN(result) && !Double.isInfinite(result);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取函数的定义域检查器
     */
    public static DomainChecker getDomainChecker(String functionStr) {
        return new DomainChecker(functionStr);
    }

    /**
     * 定义域检查器类
     */
    public static class DomainChecker {
        private final String functionStr;

        public DomainChecker(String functionStr) {
            this.functionStr = functionStr;
        }

        /**
         * 检查x是否在函数定义域内
         */
        public boolean isInDomain(double x) {
            // 1. 对数函数：ln(x)/log(x) → x>0
            if (functionStr.contains("ln(") || functionStr.contains("log(")) {
                if (x <= 0) return false;
            }

            // 2. 平方根：sqrt(...) → 内部≥0
            if (functionStr.contains("sqrt(")) {
                // 简化检查：如果x是sqrt的直接参数
                if (functionStr.contains("sqrt(x)") && x < 0) {
                    return false;
                }
            }

            // 3. 反三角函数：arcsin(x)/arccos(x) → x∈[-1,1]
            if (functionStr.contains("arcsin(") || functionStr.contains("arccos(")) {
                if (x < -1 || x > 1) return false;
            }

            // 4. 分母不为0（简化检查）
            if (functionStr.contains("/x")) {
                if (x == 0) return false;
            }

            // 5. 幂函数：x^负数 → x≠0
            if (functionStr.matches(".*x\\^\\-\\d+.*")) {
                if (x == 0) return false;
            }

            // 6. 指数函数：a^x → 底数a>0且a≠1
            if (functionStr.matches(".*\\d+\\.?\\d*\\^x.*")) {
                // 这里可以添加更复杂的底数解析
            }

            return true;
        }

        /**
         * 获取推荐的定义域范围
         */
        public double[] getRecommendedDomain() {
            // 根据函数类型返回推荐的x范围
            if (functionStr.contains("ln(") || functionStr.contains("log(")) {
                return new double[]{0.1, 10}; // 对数函数避免x<=0
            } else if (functionStr.contains("arcsin(") || functionStr.contains("arccos(")) {
                return new double[]{-1, 1}; // 反三角函数定义域
            } else {
                return new double[]{-10, 10}; // 默认范围
            }
        }
    }

    /**
     * 函数类型检测器
     */
    public static class FunctionTypeDetector {
        
        /**
         * 检测函数类型
         */
        public static FunctionType detectType(String functionStr) {
            if (functionStr.contains("sin(") || functionStr.contains("cos(") || 
                functionStr.contains("tan(")) {
                return FunctionType.TRIGONOMETRIC;
            } else if (functionStr.contains("ln(") || functionStr.contains("log(")) {
                return FunctionType.LOGARITHMIC;
            } else if (functionStr.contains("exp(") || functionStr.matches(".*\\d+\\.?\\d*\\^x.*")) {
                return FunctionType.EXPONENTIAL;
            } else if (functionStr.contains("x") && functionStr.contains("^")) {
                return FunctionType.POLYNOMIAL;
            } else if (functionStr.contains("/")) {
                return FunctionType.RATIONAL;
            } else {
                return FunctionType.UNKNOWN;
            }
        }
    }

    /**
     * 函数类型枚举
     */
    public enum FunctionType {
        TRIGONOMETRIC,    // 三角函数
        LOGARITHMIC,      // 对数函数
        EXPONENTIAL,      // 指数函数
        POLYNOMIAL,       // 多项式函数
        RATIONAL,         // 有理函数
        UNKNOWN           // 未知类型
    }
}
