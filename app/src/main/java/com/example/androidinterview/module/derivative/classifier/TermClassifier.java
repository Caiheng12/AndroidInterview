package com.example.androidinterview.module.derivative.classifier;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 项分类器
 * 负责将B中每一项判断类型进行分类：
 * - M类：基本函数（sin, cos, tan, ln, log, exp等直接作用于变量的函数）
 * - N类：基本函数相除和相除（包含乘法或除法运算的表达式）
 * - P类：复合函数（如pow(f(x), g(x))等复合形式）
 */
public class TermClassifier {
    
    // 基本函数模式
    private static final Pattern BASIC_FUNCTION_PATTERN = Pattern.compile(
        "^(sin|cos|tan|cot|sec|csc|arcsin|arccos|arctan|ln|log|exp)\\(([^)]+)\\)$"
    );
    
    // 幂函数模式
    private static final Pattern POWER_FUNCTION_PATTERN = Pattern.compile(
        "^pow\\(([^,]+),([^)]+)\\)$"
    );
    
    // 简单幂模式（如x^2, 2^x）
    private static final Pattern SIMPLE_POWER_PATTERN = Pattern.compile(
        "^(-?\\d*(\\.\\d*)?)?x\\^(-?\\d+(\\.\\d+)?)$|^(-?\\d+(\\.\\d+)?)\\^x$"
    );
    
    // 乘法模式 - 简化检查
    private static final Pattern MULTIPLICATION_PATTERN = Pattern.compile(
        ".*\\*.*"
    );
    
    // 除法模式 - 简化检查
    private static final Pattern DIVISION_PATTERN = Pattern.compile(
        ".*/.*"
    );
    
    // 变量模式
    private static final Pattern VARIABLE_PATTERN = Pattern.compile(
        "^-?x$|^x$"
    );
    
    // 常数模式
    private static final Pattern CONSTANT_PATTERN = Pattern.compile(
        "^-?\\d+(\\.\\d+)?$"
    );
    
    /**
     * 分类项的类型
     * @param term 要分类的项
     * @return TermType 项的类型
     */
    public static TermType classifyTerm(String term) {
        if (term == null || term.trim().isEmpty()) {
            return TermType.UNKNOWN;
        }
        
        term = term.trim();
        
        // 1. 检查是否为常数
        if (CONSTANT_PATTERN.matcher(term).matches()) {
            return TermType.M; // 常数视为基本函数
        }
        
        // 2. 检查是否为纯变量
        if (VARIABLE_PATTERN.matcher(term).matches()) {
            return TermType.M; // 纯变量视为基本函数
        }
        
        // 3. 优先检查pow函数 - 所有pow()都应该是P类
        Matcher powMatcher = POWER_FUNCTION_PATTERN.matcher(term);
        if (powMatcher.matches()) {
            return TermType.P; // pow函数统一归为P类
        }
        
        // 4. 优先检查乘除法（包含函数的乘除法）
        if (containsTopLevelMultiplication(term)) {
            return TermType.N; // 乘法属于N类
        }
        
        if (containsTopLevelDivision(term)) {
            return TermType.N; // 除法属于N类
        }
        
        // 5. 检查简单幂形式（不包括pow函数）
        if (SIMPLE_POWER_PATTERN.matcher(term).matches()) {
            return TermType.M; // 简单幂函数
        }
        
        // 6. 检查是否为基本函数
        Matcher basicFuncMatcher = BASIC_FUNCTION_PATTERN.matcher(term);
        if (basicFuncMatcher.matches()) {
            String innerExpr = basicFuncMatcher.group(2).trim();
            // 如果内部表达式只是变量或常数，则为基本函数
            if (isSimpleInnerExpression(innerExpr)) {
                return TermType.M;
            } else {
                // 如果内部表达式复杂，则为复合函数
                return TermType.P;
            }
        }
        
        // 7. 检查复杂表达式（包含括号内的运算）
        if (term.contains("(") && term.contains(")")) {
            return TermType.P; // 复合函数
        }
        
        // 8. 默认为未知类型
        return TermType.UNKNOWN;
    }
    
    /**
     * 判断内部表达式是否简单（只包含变量或常数）
     */
    private static boolean isSimpleInnerExpression(String expr) {
        expr = expr.trim();
        return expr.equals("x") || CONSTANT_PATTERN.matcher(expr).matches();
    }
    
    /**
     * 判断表达式是否复杂（包含运算符或函数调用）
     */
    private static boolean isComplexExpression(String expr) {
        if (expr == null || expr.trim().isEmpty()) {
            return false;
        }
        
        expr = expr.trim();
        
        // 包含运算符
        if (expr.contains("+") || expr.contains("-") || expr.contains("*") || expr.contains("/")) {
            return true;
        }
        
        // 包含函数调用
        if (expr.contains("sin(") || expr.contains("cos(") || expr.contains("tan(") ||
            expr.contains("ln(") || expr.contains("log(") || expr.contains("exp(") ||
            expr.contains("pow(") || expr.contains("sqrt(")) {
            return true;
        }
        
        // 包含括号
        if (expr.contains("(") && expr.contains(")")) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 项类型枚举
     */
    public enum TermType {
        /** 基本函数 - 包括常数、纯变量、简单函数调用 */
        M("基本函数"),
        
        /** 基本函数相除和相除 - 包含乘法或除法运算 */
        N("乘除运算"),
        
        /** 复合函数 - 复杂的函数组合 */
        P("复合函数"),
        
        /** 未知类型 */
        UNKNOWN("未知类型");
        
        private final String description;
        
        TermType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        @Override
        public String toString() {
            return description;
        }
    }
    
    /**
     * 获取项类型的详细说明
     */
    public static String getTypeDescription(TermType type) {
        switch (type) {
            case M:
                return "基本函数：包括常数、纯变量、sin(x)、cos(x)、ln(x)等简单函数";
            case N:
                return "乘除运算：包含乘法(*)或除法(/)的表达式，如u*v、u/v等";
            case P:
                return "复合函数：复杂的函数组合，如pow(f(x),g(x))、f(g(x))等";
            case UNKNOWN:
                return "未知类型：无法识别的表达式形式";
            default:
                return "未定义类型";
        }
    }
    
    /**
     * 检查是否包含顶层乘法运算符
     */
    private static boolean containsTopLevelMultiplication(String term) {
        int parenCount = 0;
        for (int i = 0; i < term.length(); i++) {
            char c = term.charAt(i);
            if (c == '(') {
                parenCount++;
            } else if (c == ')') {
                parenCount--;
            } else if (c == '*' && parenCount == 0) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查是否包含顶层除法运算符
     */
    private static boolean containsTopLevelDivision(String term) {
        int parenCount = 0;
        for (int i = 0; i < term.length(); i++) {
            char c = term.charAt(i);
            if (c == '(') {
                parenCount++;
            } else if (c == ')') {
                parenCount--;
            } else if (c == '/' && parenCount == 0) {
                return true;
            }
        }
        return false;
    }
}
