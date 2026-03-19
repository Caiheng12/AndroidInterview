package com.example.androidinterview;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 最终版求导工具类：支持复合函数+隐函数+极致化简（无冗余0/1/符号）
 */
public class SymbolicDerivative {

    // 基础函数导数规则
    private static final Map<String, String> DERIVATIVE_RULES = new HashMap<>();
    // 运算符优先级
    private static final Map<Character, Integer> OP_PRIORITY = new HashMap<>();

    static {
        // 1. 函数导数规则
        DERIVATIVE_RULES.put("sin", "cos({func})");
        DERIVATIVE_RULES.put("cos", "-sin({func})");
        DERIVATIVE_RULES.put("tan", "1/(cos({func}))^2");
        DERIVATIVE_RULES.put("exp", "exp({func})");
        DERIVATIVE_RULES.put("log", "1/({func})");
        DERIVATIVE_RULES.put("ln", "1/({func})");
        DERIVATIVE_RULES.put("sqrt", "1/(2*sqrt({func}))");

        // 2. 运算符优先级
        OP_PRIORITY.put('(', 0);
        OP_PRIORITY.put('+', 1);
        OP_PRIORITY.put('-', 1);
        OP_PRIORITY.put('*', 2);
        OP_PRIORITY.put('/', 2);
        OP_PRIORITY.put('^', 3);
    }

    // ===================== 对外暴露的唯一入口 =====================
    public static String differentiate(String function) {
        if (function == null || function.trim().isEmpty()) {
            return "0";
        }
        // 标准化 → 求导 → 极致化简（三步一体，确保结果干净）
        String normalized = normalize(function);
        String rawDerivative = derive(normalized, "x");
        return simplify(rawDerivative);
    }

    // ===================== 核心求导逻辑 =====================
    private static String derive(String expr, String var) {
        expr = expr.trim();

        // 1. 常数 → 0
        if (isConstant(expr)) return "0";
        // 2. 变量自身 → 1，其他变量 → 0
        if (isVariable(expr)) return expr.equals(var) ? "1" : "0";
        // 3. 括号包裹 → 递归内层
        if (expr.startsWith("(") && expr.endsWith(")") && isBalanced(expr)) {
            return derive(expr.substring(1, expr.length() - 1), var);
        }

        // 4. 复合函数（链式法则）
        Matcher funcMatcher = Pattern.compile("(sin|cos|tan|exp|log|ln|sqrt)\\((.+)\\)").matcher(expr);
        if (funcMatcher.matches()) {
            String outer = funcMatcher.group(1);
            String inner = funcMatcher.group(2);
            String outerDeriv = DERIVATIVE_RULES.get(outer).replace("{func}", inner);
            String innerDeriv = derive(inner, var);
            if (innerDeriv.equals("0")) return "0";
            if (innerDeriv.equals("1")) return outerDeriv;
            return "(" + outerDeriv + ")*(" + innerDeriv + ")";
        }

        // 5. 加减法（最低优先级，先拆分）
        List<String> addSub = splitTopLevel(expr, '+', '-');
        if (addSub.size() > 1) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < addSub.size(); i++) {
                String term = addSub.get(i).trim();
                if (term.isEmpty() || term.equals("+") || term.equals("-")) continue;

                String sign = (i > 0 && addSub.get(i-1).equals("-")) ? "-" : "+";
                String termDeriv = derive(term, var);
                if (termDeriv.equals("0")) continue;

                if (sb.length() == 0) {
                    sb.append(sign.equals("-") ? "-" : "").append(termDeriv);
                } else {
                    sb.append(sign).append(termDeriv);
                }
            }
            return sb.length() == 0 ? "0" : sb.toString();
        }

        // 6. 乘除法
        List<String> mulDiv = splitTopLevel(expr, '*', '/');
        if (mulDiv.size() > 1) {
            String u = mulDiv.get(0).trim();
            String v = String.join("", mulDiv.subList(1, mulDiv.size())).trim();
            String uDeriv = derive(u, var);
            String vDeriv = derive(v, var);

            if (expr.contains("*")) {
                return String.format("(%s)*%s+%s*(%s)", uDeriv, v, u, vDeriv);
            } else {
                return String.format("((%s)*%s-%s*(%s))/(%s^2)", uDeriv, v, u, vDeriv, v);
            }
        }

        // 7. 幂函数 (u^n)' = n*u^(n-1)*u'
        Matcher powerMatcher = Pattern.compile("(.+)\\^([+-]?\\d+(\\.\\d+)?)").matcher(expr);
        if (powerMatcher.matches()) {
            String base = powerMatcher.group(1);
            double exp = Double.parseDouble(powerMatcher.group(2));
            String baseDeriv = derive(base, var);
            if (baseDeriv.equals("0")) return "0";

            double newExp = exp - 1;
            String expPart = newExp == 1 ? "" : "^" + newExp;
            String basePart = base + expPart;

            if (exp == 1) return baseDeriv;
            if (baseDeriv.equals("1")) return exp + "*" + basePart;
            return exp + "*" + basePart + "*(" + baseDeriv + ")";
        }

        return "0";
    }

    // ===================== 极致化简（核心优化） =====================
    private static String simplify(String expr) {
        if (expr == null || expr.isEmpty()) return "0";
        
        String s = expr;
        
        // 超级简化 - 一次性处理所有情况
        s = superSimplify(s);
        
        // 最终清理
        s = s.replaceAll("^\\+", "");
        if (s.isEmpty() || s.equals("+") || s.equals("-")) return "0";
        return s;
    }
    
    private static String superSimplify(String s) {
        // 1. 移除所有 *1
        s = s.replaceAll("\\*1", "");
        s = s.replaceAll("1\\*", "");
        
        // 2. 移除所有 ^1
        s = s.replaceAll("\\^1", "");
        
        // 3. 处理 ^0 情况
        s = s.replaceAll("([a-zA-Z()]+)\\^0", "1");
        s = s.replaceAll("(\\d+)\\*([a-zA-Z()]+)\\^0", "$1");
        
        // 4. 处理系数*1
        s = s.replaceAll("(\\d+)\\*1", "$1");
        
        // 5. 符号清理
        s = s.replaceAll("\\+\\+", "+");
        s = s.replaceAll("\\-\\-", "+");
        s = s.replaceAll("\\+\\-", "-");
        s = s.replaceAll("\\-\\+", "-");
        s = s.replaceAll("^\\+", "");
        
        return s;
    }
    
    private static String simplifyRound(String s) {
        // 1. 处理括号内的0和1
        s = s.replaceAll("\\(0\\)", "0");
        s = s.replaceAll("\\(1\\)", "1");
        
        // 2. 移除0乘项：0*任何数 = 0
        s = s.replaceAll("0\\*[^+\\-*/()]+", "0");
        s = s.replaceAll("[^+\\-*/()]+\\*0", "0");
        s = s.replaceAll("\\(0\\)\\*[^+\\-*/()]+", "0");
        s = s.replaceAll("[^+\\-*/()]+\\*\\(0\\)", "0");
        
        // 3. 移除1乘项：1*任何数 = 任何数
        s = s.replaceAll("1\\*([^+\\-*/()]+)", "$1");
        s = s.replaceAll("([^+\\-*/()]+)\\*1", "$1");
        s = s.replaceAll("\\(1\\)\\*", "");
        s = s.replaceAll("\\*\\(1\\)", "");
        
        // 3.1. 强力移除所有*1
        s = s.replaceAll("\\*1", "");
        s = s.replaceAll("1\\*", "");
        
        // 3.2. 处理括号内的1
        s = s.replaceAll("\\(1\\)", "1");
        
        // 4. 处理-1乘项：-1*x = -x
        s = s.replaceAll("-1\\*([^+\\-*/()]+)", "-$1");
        s = s.replaceAll("\\(-1\\)\\*([^+\\-*/()]+)", "-$1");
        
        // 5. 幂次简化：x^1 = x, x^0 = 1
        s = s.replaceAll("([a-zA-Z]+|\\([^)]+\\))\\^1\\b", "$1");
        s = s.replaceAll("([a-zA-Z]+|\\([^)]+\\))\\^0", "1");
        
        // 6. 处理系数：如 2*x^1 = 2*x, 3*x^0 = 3
        s = s.replaceAll("(\\d+)\\*([a-zA-Z]+)\\^1", "$1*$2");
        s = s.replaceAll("(\\d+)\\*([a-zA-Z]+)\\^0", "$1");
        
        // 6.1. 处理变量幂次后的*1：如 x^2*1 = x^2, x^n*1 = x^n
        s = s.replaceAll("([a-zA-Z]+|\\([^)]+\\))\\^\\d+\\*1", "$1");
        s = s.replaceAll("([a-zA-Z]+|\\([^)]+\\))\\^[+-]?\\d+(\\.\\d+)?\\*1", "$1");
        
        // 6.2. 处理*1在任意位置：如 sin(x)*1 = sin(x)
        s = s.replaceAll("([a-zA-Z()]+)\\*1", "$1");
        
        // 6.3. 处理1*在开头：如 1*x = x, 1*sin(x) = sin(x)
        s = s.replaceAll("^1\\*", "");
        s = s.replaceAll("\\^1\\*1", "^1");
        s = s.replaceAll("\\^0\\*1", "^0");
        
        // 7. 移除加减0
        s = s.replaceAll("\\+0\\b", "");
        s = s.replaceAll("-0\\b", "");
        s = s.replaceAll("^0\\b", "");
        
        // 8. 符号合并
        s = s.replaceAll("\\+\\+", "+");
        s = s.replaceAll("\\-\\-", "+");
        s = s.replaceAll("\\+\\-", "-");
        s = s.replaceAll("\\-\\+", "-");
        
        // 9. 移除冗余括号（单一项）
        s = s.replaceAll("\\(([^+\\-*/()]+)\\)", "$1");
        
        // 10. 处理空乘号：如 (sin(x))*x = sin(x)*x
        s = s.replaceAll("\\)\\*\\(", ")*(");
        
        // 11. 处理负号位置：如 -sin(x)*x = -(sin(x)*x)
        if (s.matches("-[^+\\-*/].*\\*.*")) {
            int starIndex = s.indexOf('*');
            if (starIndex > 1) {
                s = "-(" + s.substring(1, starIndex) + ")" + s.substring(starIndex);
            }
        }
        
        return s;
    }

    // ===================== 测试用例 =====================
    public static void main(String[] args) {
        // 测试化简效果
        System.out.println("=== 化简测试 ===");
        testSimplify("(-1*sin(x)*1)*x^1 + (cos(x)*1)*x^0", "-sin(x)*x+cos(x)");
        testSimplify("2*x^1", "2*x");
        testSimplify("3*x^0", "3");
        testSimplify("1*sin(x)", "sin(x)");
        testSimplify("0*cos(x)", "0");
        testSimplify("-1*x", "-x");
        
        // 测试求导+化简
        System.out.println("\n=== 求导+化简测试 ===");
        testDerivative("cos(x)*x", "-sin(x)*x+cos(x)");
        testDerivative("x^2+2*x", "2*x+2");
        testDerivative("sin(x^2)", "2*x*cos(x^2)");
    }
    
    private static void testSimplify(String input, String expected) {
        String result = simplify(input);
        System.out.println("输入: " + input);
        System.out.println("化简: " + result);
        System.out.println("期望: " + expected);
        System.out.println("正确: " + result.equals(expected));
        System.out.println();
    }
    
    private static void testDerivative(String func, String expected) {
        String result = differentiate(func);
        System.out.println("函数: " + func);
        System.out.println("导数: " + result);
        System.out.println("期望: " + expected);
        System.out.println("正确: " + result.equals(expected));
        System.out.println();
    }
    
    // ===================== 辅助工具方法 =====================
    private static String normalize(String expr) {
        return expr.replaceAll("\\s+", "")
                .replaceAll("(?<!\\d)x", "1*x")
                .replaceAll("(?<!\\d)-x", "-1*x");
    }

    private static List<String> splitTopLevel(String expr, char... ops) {
        List<String> terms = new ArrayList<>();
        int paren = 0, last = 0;
        Set<Character> opSet = new HashSet<>();
        for (char op : ops) opSet.add(op);

        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') paren++;
            else if (c == ')') paren--;
            else if (opSet.contains(c) && paren == 0) {
                terms.add(expr.substring(last, i).trim());
                terms.add(String.valueOf(c));
                last = i + 1;
            }
        }
        terms.add(expr.substring(last).trim());
        terms.removeIf(String::isEmpty);
        return terms;
    }

    private static boolean isConstant(String expr) {
        try {
            Double.parseDouble(expr);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isVariable(String expr) {
        return expr.matches("[a-zA-Z]+");
    }

    private static boolean isBalanced(String expr) {
        int count = 0;
        for (char c : expr.toCharArray()) {
            if (c == '(') count++;
            if (c == ')') count--;
            if (count < 0) return false;
        }
        return count == 0;
    }
}