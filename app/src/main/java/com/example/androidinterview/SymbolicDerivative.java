package com.example.androidinterview;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SymbolicDerivative {

    private static final Map<String, String> DERIVATIVE_RULES = new HashMap<>();
    private static final Pattern FUNCTION_PATTERN = Pattern.compile("(sin|cos|tan|exp|log|sqrt|abs)\\(([^)]+)\\)");
    private static final Pattern POWER_PATTERN = Pattern.compile("([+-]?\\d*\\.?\\d*)\\*?x\\^([+-]?\\d*\\.?\\d+)");
    private static final Pattern LINEAR_PATTERN = Pattern.compile("([+-]?\\d*\\.?\\d*)\\*?x");
    private static final Pattern CONSTANT_PATTERN = Pattern.compile("[+-]?\\d+\\.?\\d*");

    static {
        // 基本导数规则 - 使用exp4j兼容的格式
        DERIVATIVE_RULES.put("sin", "cos");
        DERIVATIVE_RULES.put("cos", "-sin");
        DERIVATIVE_RULES.put("tan", "sec^2");
        DERIVATIVE_RULES.put("exp", "exp");
        DERIVATIVE_RULES.put("log", "pow(x,-1)"); // 使用pow函数格式
        DERIVATIVE_RULES.put("sqrt", "1/(2*sqrt)");
        DERIVATIVE_RULES.put("abs", "sign");
    }

    public static String differentiate(String function) {
        function = function.replaceAll("\\s+", ""); // 移除空格
        function = function.replace("-x", "-1*x"); // 标准化负号
        function = function.replace("+x", "+1*x"); // 标准化正号
        if (function.startsWith("x")) function = "1*" + function;
        
        return differentiateRecursive(function);
    }

    private static String differentiateRecursive(String function) {
        // 处理常数
        if (isConstant(function)) {
            return "0";
        }

        // 处理变量x
        if (function.equals("x")) {
            return "1";
        }

        // 处理基本函数
        Matcher functionMatcher = FUNCTION_PATTERN.matcher(function);
        if (functionMatcher.matches()) {
            String funcName = functionMatcher.group(1);
            String innerFunc = functionMatcher.group(2);
            
            if (DERIVATIVE_RULES.containsKey(funcName)) {
                String derivative = DERIVATIVE_RULES.get(funcName);
                String innerDerivative = differentiateRecursive(innerFunc);
                
                if (innerDerivative.equals("0")) {
                    return "0";
                } else if (innerDerivative.equals("1")) {
                    return derivative + "(" + innerFunc + ")";
                } else {
                    return derivative + "(" + innerFunc + ")*(" + innerDerivative + ")";
                }
            }
        }

        // 处理幂函数 ax^n
        Matcher powerMatcher = POWER_PATTERN.matcher(function);
        if (powerMatcher.matches()) {
            String coeff = powerMatcher.group(1);
            String exponent = powerMatcher.group(2);
            
            if (coeff.isEmpty() || coeff.equals("+")) coeff = "1";
            if (coeff.equals("-")) coeff = "-1";
            
            double exp = Double.parseDouble(exponent);
            if (exp == 0) {
                return "0";
            }
            
            double newCoeff = Double.parseDouble(coeff) * exp;
            double newExp = exp - 1;
            
            String result = "";
            if (newCoeff != 1) {
                result += (newCoeff == -1 ? "-" : newCoeff) + "*";
            }
            
            if (newExp == 0) {
                result = result.substring(0, result.length() - 1); // 移除*
                return result.isEmpty() ? "1" : result;
            } else if (newExp == 1) {
                result += "x";
            } else {
                result += "x^" + newExp;
            }
            
            return result;
        }

        // 处理线性项 ax
        Matcher linearMatcher = LINEAR_PATTERN.matcher(function);
        if (linearMatcher.matches()) {
            String coeff = linearMatcher.group(1);
            if (coeff.isEmpty() || coeff.equals("+")) return "1";
            if (coeff.equals("-")) return "-1";
            return coeff;
        }

        // 处理加法和减法
        if (function.contains("+") || function.contains("-")) {
            String[] terms = splitTerms(function);
            StringBuilder result = new StringBuilder();
            
            for (int i = 0; i < terms.length; i++) {
                String term = terms[i];
                if (!term.isEmpty()) {
                    String derivative = differentiateRecursive(term);
                    if (!derivative.equals("0")) {
                        if (result.length() > 0) {
                            if (term.startsWith("-")) {
                                result.append(derivative);
                            } else {
                                result.append("+").append(derivative);
                            }
                        } else {
                            result.append(derivative);
                        }
                    }
                }
            }
            
            return result.length() > 0 ? result.toString() : "0";
        }

        // 默认情况
        return "0";
    }

    private static boolean isConstant(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String[] splitTerms(String expression) {
        // 更准确的加减法分割
        java.util.List<String> terms = new java.util.ArrayList<>();
        StringBuilder currentTerm = new StringBuilder();
        
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            
            if (c == '+' || c == '-') {
                // 检查是否是符号而不是运算符
                if (i == 0 || isOperator(expression.charAt(i - 1))) {
                    currentTerm.append(c);
                } else {
                    if (currentTerm.length() > 0) {
                        terms.add(currentTerm.toString());
                        currentTerm = new StringBuilder();
                    }
                    currentTerm.append(c);
                }
            } else {
                currentTerm.append(c);
            }
        }
        
        if (currentTerm.length() > 0) {
            terms.add(currentTerm.toString());
        }
        
        return terms.toArray(new String[0]);
    }

    private static boolean isOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/' || c == '(';
    }

    public static String simplifyExpression(String expression) {
        if (expression == null || expression.isEmpty()) return "0";
        
        // 移除不必要的括号
        expression = expression.replaceAll("\\(([^+\\-*/()]+)\\)", "$1");
        
        // 简化系数
        expression = expression.replaceAll("\\+1\\*", "+");
        expression = expression.replaceAll("-1\\*", "-");
        expression = expression.replaceAll("^1\\*", "");
        
        // 简化 +0 和 -0
        expression = expression.replaceAll("\\+0", "");
        expression = expression.replaceAll("-0", "");
        
        return expression.isEmpty() ? "0" : expression;
    }
}
