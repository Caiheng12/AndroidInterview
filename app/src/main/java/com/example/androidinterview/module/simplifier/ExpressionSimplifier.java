package com.example.androidinterview.module.simplifier;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 表达式化简模块
 * 负责将求导结果进行化简，包括常数合并、指数化简、冗余清理等
 */
public class ExpressionSimplifier {

    // ===================== 项内化简：常数合并和指数化简 =====================
    public static String simplifyTerm(String term) {
        if (term == null || term.isEmpty()) return "0";

        // 纯数字直接保留
        if (term.matches("-?\\d+(\\.\\d+)?")) {
            return term.replaceAll("(\\d+)\\.0", "$1");
        }

        // 1. 基础冗余化简：去掉明显的错误形式
        term = term.replaceAll("\\(0\\)", "0")
                .replaceAll("\\(1\\)", "1")
                .replaceAll("(\\d+)\\.0\\b", "$1");
        
        // 2. 处理零乘法：0*anything = 0
        if (term.contains("0")) {
            term = term.replaceAll("0\\*[^+\\-]*", "0")
                    .replaceAll("[^+\\-]*\\*0", "0")
                    .replaceAll("^0$", "0");
            if (term.equals("0")) return "0";
        }
        
        // 3. 处理1的乘法：1*anything = anything
        term = term.replaceAll("\\*1(?=$|\\+|\\-|\\*|/|\\))", "")
                .replaceAll("1\\*(?=[a-zA-Z(])", "")
                .replaceAll("^1\\*", "");
        
        // 4. 处理-1的乘法：-1*anything = -anything
        term = term.replaceAll("-1\\*(?=[a-zA-Z(])", "-")
                .replaceAll("(?<=^|\\+|\\-|\\*|/|\\()1\\*", "");
        
        // 5. 处理x/x → 1
        term = term.replaceAll("x/x", "1");
        
        // 6. 处理x^1 → x
        term = term.replaceAll("x\\^1\\b", "x");
        
        // 7. 处理多余的*号
        term = term.replaceAll("\\*\\*", "*")
                .replaceAll("\\+\\*", "+")
                .replaceAll("-\\*", "-");
        
        // 8. 只在简单乘法时进行指数化简
        if (!term.contains("(") && !term.contains("/") && !term.contains("+") && !term.contains("-") && term.contains("*")) {
            term = simplifyExponentsInTerm(term);
        }
        
        // 9. 最终清理
        term = term.replaceAll("-1(?=\\w)", "-");
        
        return term.isEmpty() ? "0" : term;
    }
    
    // ===================== 指数化简 =====================
    private static String simplifyExponentsInTerm(String term) {
        // 1. 处理乘法：x^a * x^b -> x^(a+b)
        Pattern multPattern = Pattern.compile("x\\^(-?\\d+(?:\\.\\d+)?)(?:\\*x\\^(-?\\d+(?:\\.\\d+)?))+");
        Matcher multMatcher = multPattern.matcher(term);
        
        while (multMatcher.find()) {
            String match = multMatcher.group();
            String[] powers = match.split("\\*x\\^");
            
            if (powers.length >= 2) {
                try {
                    double sum = 0;
                    for (int i = 0; i < powers.length; i++) {
                        String powerStr = i == 0 ? powers[i].replace("x^", "") : powers[i];
                        sum += Double.parseDouble(powerStr);
                    }
                    
                    String replacement = "x^" + (sum == (long) sum ? String.valueOf((long) sum) : String.format("%.1f", sum));
                    term = term.replace(match, replacement);
                } catch (NumberFormatException e) {
                    // 解析失败，跳过
                }
            }
        }
        
        // 2. 处理除法：x^a / x^b -> x^(a-b)
        Pattern divPattern = Pattern.compile("x\\^(-?\\d+(?:\\.\\d+)?)\\/x\\^(-?\\d+(?:\\.\\d+)?)");
        Matcher divMatcher = divPattern.matcher(term);
        
        while (divMatcher.find()) {
            String match = divMatcher.group();
            String[] parts = match.split("/x\\^");
            
            if (parts.length == 2) {
                try {
                    double power1 = Double.parseDouble(parts[0].replace("x^", ""));
                    double power2 = Double.parseDouble(parts[1]);
                    double result = power1 - power2;
                    
                    String replacement = "x^" + (result == (long) result ? String.valueOf((long) result) : String.format("%.1f", result));
                    term = term.replace(match, replacement);
                } catch (NumberFormatException e) {
                    // 解析失败，跳过
                }
            }
        }
        
        // 3. 处理幂的幂：x^a^b -> x^(a*b)
        Pattern powerPowerPattern = Pattern.compile("x\\^(-?\\d+(?:\\.\\d+)?)\\^(-?\\d+(?:\\.\\d+)?)");
        Matcher powerPowerMatcher = powerPowerPattern.matcher(term);
        
        while (powerPowerMatcher.find()) {
            String match = powerPowerMatcher.group();
            String[] powers = match.substring(2).split("\\^"); // 去掉开头的"x^"
            
            if (powers.length == 2) {
                try {
                    double power1 = Double.parseDouble(powers[0]);
                    double power2 = Double.parseDouble(powers[1]);
                    double result = power1 * power2;
                    
                    String replacement = "x^" + (result == (long) result ? String.valueOf((long) result) : String.format("%.1f", result));
                    term = term.replace(match, replacement);
                } catch (NumberFormatException e) {
                    // 解析失败，跳过
                }
            }
        }
        
        // 4. 处理带括号的幂的幂：(x^a)^b -> x^(a*b)
        Pattern parenPowerPattern = Pattern.compile("\\(x\\^(-?\\d+(?:\\.\\d+)?)\\)\\^(-?\\d+(?:\\.\\d+)?)");
        Matcher parenPowerMatcher = parenPowerPattern.matcher(term);
        
        while (parenPowerMatcher.find()) {
            String match = parenPowerMatcher.group();
            String innerPower = parenPowerMatcher.group(1);
            String outerPower = parenPowerMatcher.group(2);
            
            try {
                double power1 = Double.parseDouble(innerPower);
                double power2 = Double.parseDouble(outerPower);
                double result = power1 * power2;
                
                String replacement = "x^" + (result == (long) result ? String.valueOf((long) result) : String.format("%.1f", result));
                term = term.replace(match, replacement);
            } catch (NumberFormatException e) {
                // 解析失败，跳过
            }
        }
        
        return term;
    }

    // ===================== 最终化简 =====================
    public static String finalSimplify(String expr) {
        if (expr == null || expr.isEmpty()) return "0";
        
        // 应用所有化简规则到最终结果
        expr = expr.replaceAll("\\(1\\)", "1")
                .replaceAll("\\*1(?=$|\\+|\\-|\\*|/|\\))", "")
                .replaceAll("(?<=^|\\+|\\-|\\*|/|\\()1\\*", "")
                .replaceAll("(?<=^|\\+|\\-|\\*|/|\\()-1\\*", "-")
                .replaceAll("(x)\\^1\\b", "$1")
                .replaceAll("(\\d+)\\.0\\b", "$1")
                .replaceAll("-1(?=\\w)", "-")
                .replaceAll("\\(0\\)", "0")
                .replaceAll("\\*0(?=$|\\+|\\-|\\*|/|\\))", "")
                .replaceAll("(?<=^|\\+|\\-|\\*|/|\\()0\\*", "")
                .replaceAll("(?<=^|\\+|\\-|\\*|/|\\()0\\+", "")
                .replaceAll("(?<=^|\\+|\\-|\\*|/|\\()0\\-", "-")
                // 新增：x/x → 1
                .replaceAll("x/x", "1")
                // 新增：(x-x) → 0
                .replaceAll("\\(x-x\\)", "0")
                // 新增：(f(x)-f(x)) → 0 (通用形式)
                .replaceAll("\\(([^+\\-*/()]+)-\\1\\)", "0")
                // 处理多余的*号
                .replaceAll("\\*\\*", "*")
                .replaceAll("\\+\\*", "+")
                .replaceAll("-\\*", "-");
        
        // 处理特殊情况：0/(anything) → 0
        if (expr.startsWith("0/")) {
            return "0";
        }
        
        // 对最终结果进行指数化简
        expr = simplifyExponentsInTerm(expr);
        
        return expr;
    }

    // ===================== 合并同类项 =====================
    public static String combineLikeTerms(List<String> validTerms, List<Character> signs) {
        if (validTerms.isEmpty()) return "0";
        if (validTerms.size() == 1) {
            String term = validTerms.get(0);
            // 对单项也进行最终化简
            return finalSimplify(term);
        }
        
        // 1. 提取每一项的系数和变量部分
        List<TermInfo> termInfos = new ArrayList<>();
        
        for (int i = 0; i < validTerms.size(); i++) {
            String term = validTerms.get(i);
            char sign = (i == 0 && !term.startsWith("-") && !term.startsWith("+")) ? '+' : 
                         (i > 0 ? signs.get(i - 1) : '+');
            
            if (term.startsWith("+") || term.startsWith("-")) {
                sign = term.charAt(0);
                term = term.substring(1);
            }
            
            TermInfo termInfo = extractTermInfo(term, sign);
            if (termInfo != null && !termInfo.isZero()) {
                termInfos.add(termInfo);
            }
        }
        
        // 2. 合并同类项
        Map<String, TermInfo> combinedTerms = new HashMap<>();
        
        for (TermInfo termInfo : termInfos) {
            String varPart = termInfo.variablePart;
            
            if (combinedTerms.containsKey(varPart)) {
                TermInfo existing = combinedTerms.get(varPart);
                existing.coefficient += termInfo.coefficient;
            } else {
                combinedTerms.put(varPart, termInfo);
            }
        }
        
        // 3. 构建最终表达式
        StringBuilder result = new StringBuilder();
        boolean first = true;
        
        for (TermInfo termInfo : combinedTerms.values()) {
            if (termInfo.isZero()) continue;
            
            if (!first) {
                result.append(termInfo.coefficient >= 0 ? "+" : "");
            } else {
                first = false;
            }
            
            if (termInfo.coefficient != 1 || termInfo.variablePart.isEmpty()) {
                String coeffStr = termInfo.coefficient == (long) termInfo.coefficient ? 
                                  String.valueOf((long) termInfo.coefficient) : 
                                  String.format("%.1f", termInfo.coefficient);
                result.append(coeffStr);
            }
            
            if (termInfo.variablePart.isEmpty()) {
                // 常数项，不添加变量部分
            } else {
                // 有变量部分，需要添加
                if (termInfo.coefficient != 1 && termInfo.coefficient != -1) {
                    result.append("*");
                }
                result.append(termInfo.variablePart);
            }
        }
        
        String finalResult = result.length() == 0 ? "0" : result.toString();
        return finalSimplify(finalResult);
    }
    
    // ===================== 提取项信息 =====================
    private static TermInfo extractTermInfo(String term, char sign) {
        if (term == null || term.trim().isEmpty()) return null;
        
        term = term.trim();
        double coefficient = 1.0;
        String variablePart = "";
        
        // 1. 提取系数
        Pattern coeffPattern = Pattern.compile("^(-?\\d+(?:\\.\\d+)?)(?:\\*|$)");
        Matcher coeffMatcher = coeffPattern.matcher(term);
        
        if (coeffMatcher.find()) {
            String coeffStr = coeffMatcher.group(1);
            try {
                coefficient = Double.parseDouble(coeffStr);
            } catch (NumberFormatException e) {
                coefficient = 1.0;
            }
            
            String remaining = term.substring(coeffMatcher.end()).trim();
            if (remaining.isEmpty()) {
                variablePart = ""; // 空字符串表示常数项
            } else {
                variablePart = remaining;
            }
        } else {
            // 没有显式系数，检查是否有负号
            if (term.startsWith("-")) {
                coefficient = -1.0;
                variablePart = term.substring(1);
            } else {
                variablePart = term;
            }
        }
        
        // 2. 应用符号
        if (sign == '-') {
            coefficient = -coefficient;
        }
        
        return new TermInfo(coefficient, variablePart);
    }

    // ===================== 项信息类 =====================
    public static class TermInfo {
        public double coefficient;
        public String variablePart;
        
        public TermInfo(double coefficient, String variablePart) {
            this.coefficient = coefficient;
            this.variablePart = variablePart;
        }
        
        public boolean isZero() {
            return Math.abs(coefficient) < 1e-10;
        }
    }
}
