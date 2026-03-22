package com.example.androidinterview.module.derivative.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * 表达式解析器
 * 负责将输入表达式解析为两个可变列表：
 * - 列表A：存放加减符号
 * - 列表B：存放加减符号连接的每一项
 */
public class ExpressionParser {
    
    /**
     * 解析表达式为符号列表和项列表
     * @param expression 输入的表达式
     * @return ParseResult 包含符号列表和项列表
     */
    public static ParseResult parseExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return new ParseResult(new ArrayList<>(), new ArrayList<>());
        }
        
        // 预处理表达式
        String preprocessed = preprocessExpression(expression);
        
        // 按加减号拆分
        return splitByAddSub(preprocessed);
    }
    
    /**
     * 预处理表达式
     */
    private static String preprocessExpression(String expr) {
        return expr.replaceAll("\\s+", "")
                .replaceAll("--", "+")
                .replaceAll("\\+-", "-")
                .replaceAll("-\\+", "-")
                .replaceAll("\\+\\+", "+");
    }
    
    /**
     * 按加减号拆分表达式
     */
    private static ParseResult splitByAddSub(String expr) {
        List<Character> signs = new ArrayList<>();
        List<String> terms = new ArrayList<>();
        
        // 去掉开头的+
        expr = expr.replaceAll("^\\+", "");
        
        int startIdx = 0;
        String firstSign = "";
        
        // 处理开头的负号
        if (expr.startsWith("-")) {
            firstSign = "-";
            startIdx = 1;
        }
        
        int parenCount = 0;
        int lastSplit = startIdx;
        
        for (int i = startIdx; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') {
                parenCount++;
            } else if (c == ')') {
                parenCount--;
            } else if ((c == '+' || c == '-') && parenCount == 0) {
                // 添加当前项
                terms.add(firstSign + expr.substring(lastSplit, i));
                firstSign = "";
                // 添加符号
                signs.add(c);
                lastSplit = i + 1;
            }
        }
        
        // 添加最后一项
        terms.add(firstSign + expr.substring(lastSplit));
        
        return new ParseResult(signs, terms);
    }
    
    /**
     * 解析结果类
     */
    public static class ParseResult {
        public final List<Character> signs;    // 列表A：加减符号
        public final List<String> terms;       // 列表B：每一项
        
        public ParseResult(List<Character> signs, List<String> terms) {
            this.signs = new ArrayList<>(signs);
            this.terms = new ArrayList<>(terms);
        }
        
        /**
         * 验证解析结果是否有效
         * 规则：项数应该等于符号数+1
         */
        public boolean isValid() {
            return terms.size() == signs.size() + 1;
        }
        
        /**
         * 获取项数
         */
        public int getTermCount() {
            return terms.size();
        }
        
        /**
         * 获取符号数
         */
        public int getSignCount() {
            return signs.size();
        }
    }
}
