package com.example.androidinterview.module.derivative.calculator;

import com.example.androidinterview.module.derivative.classifier.TermClassifier;
import com.example.androidinterview.module.derivative.core.SymbolicDerivative;
import com.example.androidinterview.module.simplifier.ExpressionSimplifier;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 求导计算器
 * 实现M、N、P三种类型的求导法则：
 * - M类：按基本函数求导法则求导
 * - N类：相乘时：(u*v)' = u'*v + u*v'，相除时：(u/v)' = (u'*v - u*v')/u^2
 * - P类：复合函数求导，如pow(f(x),g(x))的求导
 */
public class DerivativeCalculator {
    
    // 基础函数导数规则
    private static final Map<String, String> DERIV_RULES = new HashMap<>();
    static {
        // 三角函数
        DERIV_RULES.put("sin", "cos({arg})");
        DERIV_RULES.put("cos", "-sin({arg})");
        DERIV_RULES.put("tan", "1/(cos({arg}))^2");
        DERIV_RULES.put("cot", "-1/(sin({arg}))^2");
        DERIV_RULES.put("sec", "sec({arg})*tan({arg})");
        DERIV_RULES.put("csc", "-csc({arg})*cot({arg})");

        // 反三角函数
        DERIV_RULES.put("arcsin", "1/sqrt(1-({arg})^2)");
        DERIV_RULES.put("arccos", "-1/sqrt(1-({arg})^2)");
        DERIV_RULES.put("arctan", "1/(1+({arg})^2)");

        // 对数/指数
        DERIV_RULES.put("ln", "1/({arg})");
        DERIV_RULES.put("log", "1/((ln(10))*({arg}))");
        DERIV_RULES.put("exp", "exp({arg})");
        
        // 幂函数
        DERIV_RULES.put("pow", "{a}*({arg})^({a}-1)");
    }
    
    /**
     * 根据项的类型计算导数
     * @param term 要求导的项
     * @param type 项的类型
     * @return 导数结果
     */
    public static String calculateDerivative(String term, TermClassifier.TermType type) {
        if (term == null || term.trim().isEmpty()) {
            return "0";
        }
        
        term = term.trim();
        
        switch (type) {
            case M:
                return calculateMDerivative(term);
            case N:
                return calculateNDerivative(term);
            case P:
                return calculatePDerivative(term);
            case UNKNOWN:
            default:
                return "0";
        }
    }
    
    /**
     * M类求导：基本函数求导法则
     */
    private static String calculateMDerivative(String term) {
        // 纯变量x
        if (term.equals("x")) return "1";
        if (term.equals("-x")) return "-1";
        
        // 常数
        if (term.matches("-?\\d+(\\.\\d+)?")) {
            return "0";
        }
        
        // 简单幂函数 x^a
        Matcher powerMatcher = Pattern.compile("(-?)(\\d*\\.?\\d*)?x\\^(\\-?\\d+(\\.\\d+)?)").matcher(term);
        if (powerMatcher.matches()) {
            return deriveSimplePower(term);
        }
        
        // 常数底数的指数函数 a^x
        Matcher constBaseExpMatcher = Pattern.compile("(\\d+(\\.\\d+)?)\\^x").matcher(term);
        if (constBaseExpMatcher.matches()) {
            String base = constBaseExpMatcher.group(1);
            return base + "^x*ln(" + base + ")";
        }
        
        // 基本函数 sin(x), cos(x) 等
        Matcher basicFuncMatcher = Pattern.compile("(sin|cos|tan|cot|sec|csc|arcsin|arccos|arctan|ln|log|exp)\\(([^)]+)\\)").matcher(term);
        if (basicFuncMatcher.matches()) {
            String func = basicFuncMatcher.group(1);
            String innerExpr = basicFuncMatcher.group(2);
            
            // 检查内部表达式是否简单
            if (isSimpleInnerExpression(innerExpr)) {
                String outerDeriv = DERIV_RULES.get(func).replace("{arg}", innerExpr);
                String innerDeriv = calculateMDerivative(innerExpr);
                return innerDeriv.equals("1") ? outerDeriv : "(" + outerDeriv + ")*(" + innerDeriv + ")";
            }
        }
        
        // pow(x,a) 形式
        Matcher powMatcher = Pattern.compile("pow\\((x),(\\-?\\d+(\\.\\d+)?)\\)").matcher(term);
        if (powMatcher.matches()) {
            String a = powMatcher.group(2);
            String outerDeriv = DERIV_RULES.get("pow").replace("{a}", a).replace("{arg}", "x");
            return outerDeriv;
        }
        
        return "0";
    }
    
    /**
     * N类求导：乘除法求导法则
     */
    private static String calculateNDerivative(String term) {
        // 乘法：u*v
        String[] mulParts = splitByMultiplication(term);
        if (mulParts.length == 2) {
            return deriveMultiplication(mulParts[0], mulParts[1]);
        }
        
        // 除法：u/v
        String[] divParts = splitByDivision(term);
        if (divParts.length == 2) {
            return deriveDivision(divParts[0], divParts[1]);
        }
        
        return "0";
    }
    
    /**
     * P类求导：复合函数求导法则
     */
    private static String calculatePDerivative(String term) {
        // 复合幂函数 pow(f(x), g(x))
        Matcher powCompositeMatcher = Pattern.compile("pow\\(([^,]+),([^)]+)\\)").matcher(term);
        if (powCompositeMatcher.matches()) {
            return deriveCompositePower(term);
        }
        
        // 复合基本函数 f(g(x))
        Matcher compositeFuncMatcher = Pattern.compile("(sin|cos|tan|cot|sec|csc|arcsin|arccos|arctan|ln|log|exp)\\(([^)]+)\\)").matcher(term);
        if (compositeFuncMatcher.matches()) {
            String func = compositeFuncMatcher.group(1);
            String innerExpr = compositeFuncMatcher.group(2);
            
            String outerDeriv = DERIV_RULES.get(func).replace("{arg}", "(" + innerExpr + ")");
            String innerDeriv = SymbolicDerivative.differentiate(innerExpr);
            
            // 需要加括号的情况
            boolean outerNeedsParen = outerDeriv.contains("+") || outerDeriv.contains("-");
            boolean innerNeedsParen = innerDeriv.contains("+") || innerDeriv.contains("-");
            
            String result = (outerNeedsParen ? "(" + outerDeriv + ")" : outerDeriv) + "*" + 
                          (innerNeedsParen ? "(" + innerDeriv + ")" : innerDeriv);
            
            return ExpressionSimplifier.finalSimplify(result);
        }
        
        return "0";
    }
    
    /**
     * 求导简单幂函数
     */
    private static String deriveSimplePower(String term) {
        Matcher powerMatcher = Pattern.compile("(-?)(\\d*\\.?\\d*)?x\\^(\\-?\\d+(\\.\\d+)?)").matcher(term);
        if (powerMatcher.matches()) {
            String sign = powerMatcher.group(1);
            String coeffStr = powerMatcher.group(2).isEmpty() ? "1" : powerMatcher.group(2);
            double exp = Double.parseDouble(powerMatcher.group(3));
            double newCoeff = Double.parseDouble(coeffStr) * exp;
            double newExp = exp - 1;

            StringBuilder res = new StringBuilder(sign);
            
            if (newCoeff != 1 || newExp == 0) {
                if (newCoeff == (long) newCoeff) {
                    res.append((long) newCoeff);
                } else {
                    res.append(String.format("%.1f", newCoeff));
                }
            }
            
            if (newExp != 0) {
                if (res.length() > 0 && !res.toString().endsWith("*")) {
                    res.append("*");
                }
                res.append("x");
                if (newExp != 1) {
                    res.append("^").append(newExp == (long) newExp ? (long) newExp : newExp);
                }
            }
            return res.toString().replaceAll("^1", "").replaceAll("^-1", "-");
        }
        return "0";
    }
    
    /**
     * 乘法求导：(u*v)' = u'*v + u*v'
     */
    private static String deriveMultiplication(String u, String v) {
        String uDeriv = SymbolicDerivative.differentiate(u);
        String vDeriv = SymbolicDerivative.differentiate(v);
        
        boolean uNeedsParen = u.contains("+") || u.contains("-");
        boolean vNeedsParen = v.contains("+") || v.contains("-");
        boolean uDerivNeedsParen = uDeriv.contains("+") || uDeriv.contains("-");
        boolean vDerivNeedsParen = vDeriv.contains("+") || vDeriv.contains("-");
        
        String term1 = (uDerivNeedsParen ? "(" + uDeriv + ")" : uDeriv) + "*" + 
                     (vNeedsParen ? "(" + v + ")" : v);
        String term2 = (uNeedsParen ? "(" + u + ")" : u) + "*" + 
                     (vDerivNeedsParen ? "(" + vDeriv + ")" : vDeriv);
        
        return ExpressionSimplifier.finalSimplify(term1 + "+" + term2);
    }
    
    /**
     * 除法求导：(u/v)' = (u'*v - u*v')/v^2
     */
    private static String deriveDivision(String u, String v) {
        String uDeriv = SymbolicDerivative.differentiate(u);
        String vDeriv = SymbolicDerivative.differentiate(v);
        
        boolean uNeedsParen = u.contains("+") || u.contains("-");
        boolean vNeedsParen = v.contains("+") || v.contains("-");
        boolean uDerivNeedsParen = uDeriv.contains("+") || uDeriv.contains("-");
        boolean vDerivNeedsParen = vDeriv.contains("+") || vDeriv.contains("-");
        
        String numerator = (uDerivNeedsParen ? "(" + uDeriv + ")" : uDeriv) + "*" + 
                          (vNeedsParen ? "(" + v + ")" : v) + "-" +
                          (uNeedsParen ? "(" + u + ")" : u) + "*" + 
                          (vDerivNeedsParen ? "(" + vDeriv + ")" : vDeriv);
        
        String denominator = (vNeedsParen ? "(" + v + ")" : v) + "^2";
        
        return ExpressionSimplifier.finalSimplify("(" + numerator + ")/" + denominator);
    }
    
    /**
     * 复合幂函数求导：按照三种情况处理
     * 情况1：只有第一项含变量，导数为：a*pow(f(x),a-1)*(f(x)的导数)
     * 情况2：只有第二项含变量，导数为：ln(a)*pow(a,f(x))*(f(x)的导数)
     * 情况3：pow(f(x),g(x))两项都含变量，导数为：pow(f(x),g(x))*{g'(x)*ln(f(x))+g(x)*f'(x)/f(x)}
     */
    private static String deriveCompositePower(String term) {
        // 解析 pow(f(x), g(x))
        String[] parts = parsePowArguments(term);
        if (parts.length != 2) {
            return "0";
        }
        
        String base = parts[0];
        String exponent = parts[1];
        
        // 判断base和exponent是否包含变量
        boolean baseHasVariable = containsVariable(base);
        boolean exponentHasVariable = containsVariable(exponent);
        
        // 情况1：只有第一项含变量，第二项是常数
        if (baseHasVariable && !exponentHasVariable) {
            return derivePowCase1(base, exponent);
        }
        
        // 情况2：只有第二项含变量，第一项是常数
        if (!baseHasVariable && exponentHasVariable) {
            return derivePowCase2(base, exponent);
        }
        
        // 情况3：pow(f(x),g(x))两项都含变量
        if (baseHasVariable && exponentHasVariable) {
            return derivePowCase3(base, exponent);
        }
        
        // 都不含变量（常数），导数为0
        return "0";
    }
    
    // 情况1：只有第一项含变量，第二项是常数
    private static String derivePowCase1(String base, String exponent) {
        // 验证exponent是否为常数（支持小数）
        if (!isConstant(exponent)) {
            return "0"; // 如果不是常数，按情况3处理
        }
        
        // 对于简单底数，直接使用calculateDerivative
        String baseDeriv;
        if (isSimpleTerm(base)) {
            baseDeriv = calculateDerivative(base, TermClassifier.classifyTerm(base));
        } else {
            baseDeriv = SymbolicDerivative.differentiate(base);
        }
        
        if (baseDeriv.equals("0")) {
            return "0";
        }
        
        // a*pow(f(x),a-1)*(f(x)的导数)
        StringBuilder result = new StringBuilder();
        
        // 系数a
        if (!exponent.equals("1")) {
            result.append(exponent).append("*");
        }
        
        // pow(f(x),a-1)
        String newExponent = exponent.equals("1") ? "0" : "(" + exponent + "-1)";
        result.append("pow(").append(base).append(",").append(newExponent).append(")");
        
        // *(f(x)的导数)
        result.append("*");
        if (needsParentheses(baseDeriv)) {
            result.append("(").append(baseDeriv).append(")");
        } else {
            result.append(baseDeriv);
        }
        
        return ExpressionSimplifier.finalSimplify(result.toString());
    }
    
    // 情况2：只有第二项含变量，第一项是常数
    private static String derivePowCase2(String base, String exponent) {
        // 验证base是否为常数（支持小数）
        if (!isConstant(base)) {
            return "0"; // 如果不是常数，按情况3处理
        }
        
        // 对于简单指数，直接使用calculateDerivative
        String exponentDeriv;
        if (isSimpleTerm(exponent)) {
            exponentDeriv = calculateDerivative(exponent, TermClassifier.classifyTerm(exponent));
        } else {
            exponentDeriv = SymbolicDerivative.differentiate(exponent);
        }
        
        if (exponentDeriv.equals("0")) {
            return "0";
        }
        
        // ln(a)*pow(a,f(x))*(f(x)的导数)
        StringBuilder result = new StringBuilder();
        
        // ln(a)
        result.append("ln(").append(base).append(")*");
        
        // pow(a,f(x))
        result.append("pow(").append(base).append(",").append(exponent).append(")");
        
        // *(f(x)的导数)
        result.append("*");
        if (needsParentheses(exponentDeriv)) {
            result.append("(").append(exponentDeriv).append(")");
        } else {
            result.append(exponentDeriv);
        }
        
        return ExpressionSimplifier.finalSimplify(result.toString());
    }
    
    // 情况3：pow(f(x),g(x))两项都含变量
    private static String derivePowCase3(String base, String exponent) {
        String baseDeriv = SymbolicDerivative.differentiate(base);
        String exponentDeriv = SymbolicDerivative.differentiate(exponent);
        
        // 如果两个导数都是0，结果为0
        if (baseDeriv.equals("0") && exponentDeriv.equals("0")) {
            return "0";
        }
        
        // pow(f(x),g(x))*{g'(x)*ln(f(x))+g(x)*f'(x)/f(x)}
        StringBuilder result = new StringBuilder();
        result.append("pow(").append(base).append(",").append(exponent).append(")");
        result.append("*{");
        
        boolean hasFirstTerm = !exponentDeriv.equals("0");
        boolean hasSecondTerm = !baseDeriv.equals("0");
        
        // 第一项：(g(x)的导数)*ln(f(x))
        if (hasFirstTerm) {
            if (needsParentheses(exponentDeriv)) {
                result.append("(").append(exponentDeriv).append(")");
            } else {
                result.append(exponentDeriv);
            }
            result.append("*ln(");
            if (needsParentheses(base)) {
                result.append("(").append(base).append(")");
            } else {
                result.append(base);
            }
            result.append(")");
        }
        
        // 第二项：g(x)*(f(x)的导数)/f(x)
        if (hasSecondTerm) {
            if (hasFirstTerm) {
                result.append("+");
            }
            
            if (needsParentheses(exponent)) {
                result.append("(").append(exponent).append(")");
            } else {
                result.append(exponent);
            }
            result.append("*");
            if (needsParentheses(baseDeriv)) {
                result.append("(").append(baseDeriv).append(")");
            } else {
                result.append(baseDeriv);
            }
            result.append("/");
            if (needsParentheses(base)) {
                result.append("(").append(base).append(")");
            } else {
                result.append(base);
            }
        }
        
        result.append("}");
        
        return ExpressionSimplifier.finalSimplify(result.toString());
    }
    
    /**
     * 判断是否需要加括号
     */
    private static boolean needsParentheses(String expr) {
        if (expr == null || expr.trim().isEmpty()) {
            return false;
        }
        
        expr = expr.trim();
        
        // 如果包含加减号，需要加括号
        if (expr.contains("+") || expr.contains("-")) {
            return true;
        }
        
        // 如果是单个数字或简单变量，不需要加括号
        if (expr.matches("-?\\d+(\\.\\d+)?") || expr.equals("x") || expr.equals("1") || expr.equals("0")) {
            return false;
        }
        
        // 其他情况（如函数调用）通常需要加括号以避免歧义
        return expr.contains("(") && expr.contains(")");
    }
    
    /**
     * 解析pow函数的参数，正确处理包含函数的参数
     */
    private static String[] parsePowArguments(String term) {
        // 使用更智能的解析方法，处理括号嵌套
        if (!term.startsWith("pow(") || !term.endsWith(")")) {
            return new String[0];
        }
        
        // 去掉外层的pow()
        String inner = term.substring(4, term.length() - 1);
        
        // 找到逗号分隔符，考虑括号嵌套
        int parenCount = 0;
        int commaIndex = -1;
        
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '(') {
                parenCount++;
            } else if (c == ')') {
                parenCount--;
            } else if (c == ',' && parenCount == 0) {
                commaIndex = i;
                break;
            }
        }
        
        if (commaIndex == -1) {
            return new String[0];
        }
        
        String base = inner.substring(0, commaIndex).trim();
        String exponent = inner.substring(commaIndex + 1).trim();
        
        return new String[]{base, exponent};
    }
    
    /**
     * 按乘号拆分表达式
     */
    private static String[] splitByMultiplication(String term) {
        int parenCount = 0;
        for (int i = 0; i < term.length(); i++) {
            char c = term.charAt(i);
            if (c == '(') parenCount++;
            else if (c == ')') parenCount--;
            else if (c == '*' && parenCount == 0) {
                return new String[]{term.substring(0, i).trim(), term.substring(i + 1).trim()};
            }
        }
        return new String[0];
    }
    
    /**
     * 按除号拆分表达式
     */
    private static String[] splitByDivision(String term) {
        int parenCount = 0;
        for (int i = 0; i < term.length(); i++) {
            char c = term.charAt(i);
            if (c == '(') parenCount++;
            else if (c == ')') parenCount--;
            else if (c == '/' && parenCount == 0) {
                return new String[]{term.substring(0, i).trim(), term.substring(i + 1).trim()};
            }
        }
        return new String[0];
    }
    
    /**
     * 判断内部表达式是否简单
     */
    private static boolean isSimpleInnerExpression(String expr) {
        expr = expr.trim();
        return expr.equals("x") || expr.matches("-?\\d+(\\.\\d+)?");
    }
    
    /**
     * 判断表达式是否包含变量
     */
    private static boolean containsVariable(String expr) {
        if (expr == null || expr.trim().isEmpty()) {
            return false;
        }
        
        expr = expr.trim();
        
        // 检查是否包含变量x
        if (expr.contains("x")) {
            return true;
        }
        
        // 检查是否包含其他函数（这些函数内部通常包含变量）
        return expr.contains("sin(") || expr.contains("cos(") || 
               expr.contains("tan(") || expr.contains("ln(") || 
               expr.contains("log(") || expr.contains("exp(") || 
               expr.contains("pow(") || expr.contains("sqrt(");
    }
    
    /**
     * 判断表达式是否为常数
     */
    private static boolean isConstant(String expr) {
        if (expr == null || expr.trim().isEmpty()) {
            return false;
        }
        
        expr = expr.trim();
        
        // 含x → 一定不是常数
        if (expr.contains("x")) {
            return false;
        }
        
        // 匹配：整数、小数
        return expr.matches("-?\\d+(\\.\\d+)?");
    }
    
    /**
     * 判断表达式是否为简单项（可以由calculateDerivative处理）
     */
    private static boolean isSimpleTerm(String expr) {
        if (expr == null || expr.trim().isEmpty()) {
            return false;
        }
        
        expr = expr.trim();
        
        // 简单变量、常数
        if (expr.equals("x") || expr.equals("-x") || 
            expr.matches("-?\\d+(\\.\\d+)?")) {
            return true;
        }
        
        // 简单幂函数
        if (expr.matches(".*x\\^.*")) {
            return true;
        }
        
        // 简单基本函数
        if (expr.matches("sin\\(x\\)|cos\\(x)|tan\\(x)|ln\\(x)|log\\(x)|exp\\(x)")) {
            return true;
        }
        
        // 简单加减表达式（如x-1, x^2+2*x等）
        if (isSimpleAdditiveExpression(expr)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 判断是否为简单加减表达式
     */
    private static boolean isSimpleAdditiveExpression(String expr) {
        // 不包含复杂函数
        if (expr.contains("sin(") || expr.contains("cos(") || 
            expr.contains("tan(") || expr.contains("ln(") || 
            expr.contains("pow(")) {
            return false;
        }
        
        // 只包含x、数字、+、-、*、/、^
        return expr.matches("[x0-9+\\-*/^.()]+");
    }
}
