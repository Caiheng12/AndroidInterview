package com.example.androidinterview.module.derivative.core;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 符号求导器 - 完全重写版本
 * 解决递归调用、类型判断、表达式解析三大核心问题
 */
public class SymbolicDerivative {
    // 基础函数求导规则（标准化）
    private static final Map<String, String> DERIV_RULES;
    static {
        DERIV_RULES = new HashMap<>();
        DERIV_RULES.put("sin", "cos({arg})");
        DERIV_RULES.put("cos", "-sin({arg})");
        DERIV_RULES.put("tan", "1/(cos({arg})*cos({arg}))");
        DERIV_RULES.put("ln", "1/({arg})");
        DERIV_RULES.put("exp", "exp({arg})");
        DERIV_RULES.put("sqrt", "0.5/pow({arg},0.5)");
    }

    // ================== 核心工具函数：加括号（解决乘除优先级问题） ==================
    private static String addParenthesesIfNeeded(String expr) {
        if (expr == null || expr.isEmpty()) return "0";
        // 已带最外层括号 → 直接返回
        if (expr.startsWith("(") && expr.endsWith(")")) return expr;
        // 纯常数/纯变量 → 直接返回
        if (isConstant(expr) || expr.equals("x") || expr.equals("-x")) return expr;
        // 检查最外层是否有加减号（有则加括号）
        int parenCount = 0;
        boolean hasOuterAddSub = false;
        for (char c : expr.toCharArray()) {
            if (c == '(') parenCount++;
            if (c == ')') parenCount--;
            if ((c == '+' || c == '-') && parenCount == 0) {
                hasOuterAddSub = true;
                break;
            }
        }
        return hasOuterAddSub ? "(" + expr + ")" : expr;
    }

    // ================== 核心工具函数：常数判断（支持分数/小数/整数） ==================
    private static boolean isConstant(String expr) {
        if (expr == null || expr.contains("x")) return false;
        // 匹配：整数(123)、小数(123.45)、分数(1/2)、负常数(-1/2/-123.45)
        return expr.matches("-?\\d+(\\.\\d+)?") || expr.matches("-?\\d+/\\d+");
    }

    // ================== 核心工具函数：解析常数（分数转小数） ==================
    private static double parseConstant(String s) {
        s = s.trim();
        if (s.contains("/")) {
            String[] parts = s.split("/");
            return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
        }
        return Double.parseDouble(s);
    }

    // ================== 核心工具函数：拆分pow参数（支持嵌套括号） ==================
    private static String[] splitPowArgs(String inner) {
        int parenCount = 0;
        int splitIndex = -1;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '(') parenCount++;
            if (c == ')') parenCount--;
            if (c == ',' && parenCount == 0) { // 只分割最外层逗号
                splitIndex = i;
                break;
            }
        }
        if (splitIndex == -1) return new String[]{"", ""};
        return new String[]{
                inner.substring(0, splitIndex).trim(),
                inner.substring(splitIndex + 1).trim()
        };
    }

    // ================== 核心1：M类（基本函数）求导 ==================
    private static String deriveM(String term) {
        if (term.equals("x")) return "1";
        if (isConstant(term)) return "0";
        
        // 简单幂函数 x^a
        Matcher powerMatcher = Pattern.compile("^(-?\\d*(\\.\\d*)?)?x\\^(-?\\d+(\\.\\d+)?)$").matcher(term);
        if (powerMatcher.matches()) {
            String coeff = powerMatcher.group(1);
            String exp = powerMatcher.group(3);
            
            if (coeff == null || coeff.isEmpty() || coeff.equals("-")) {
                coeff = coeff.equals("-") ? "-1" : "1";
            }
            
            double expValue = Double.parseDouble(exp);
            double newExp = expValue - 1;
            double coeffValue = expValue * Double.parseDouble(coeff);
            String newCoeff = coeffValue == 1.0 ? "" : (coeffValue == -1.0 ? "-" : String.valueOf((int)coeffValue));
            
            if (newExp == 1.0) {
                return newCoeff + "x";
            } else if (newExp == 0.0) {
                return newCoeff;
            } else {
                String newExpStr = newExp == (int)newExp ? String.valueOf((int)newExp) : String.valueOf(newExp);
                return newCoeff + "x^" + newExpStr;
            }
        }
        
        // 处理常数乘法如 2*x, 3*x 等
        Matcher constMulMatcher = Pattern.compile("^(-?\\d+(\\.\\d+)?)\\*x$").matcher(term);
        if (constMulMatcher.matches()) {
            return constMulMatcher.group(1);
        }
        
        // 处理 x*常数 如 x*2, x*3 等
        Matcher mulConstMatcher = Pattern.compile("^x\\*(-?\\d+(\\.\\d+)?)$").matcher(term);
        if (mulConstMatcher.matches()) {
            return mulConstMatcher.group(1);
        }
        
        // 基本函数（M类）：sin(x), cos(x), ln(x), exp(x) - 注意：只有x作为参数
        Matcher basicFuncMatcher = Pattern.compile("(sin|cos|tan|ln|exp|sqrt)\\((x)\\)").matcher(term);
        if (basicFuncMatcher.matches()) {
            String func = basicFuncMatcher.group(1);
            return DERIV_RULES.get(func).replace("{arg}", "x");
        }
        
        return "0";
    }

    // ================== 核心2：N类（乘除）求导 ==================
    private static String deriveN(String term) {
        String normalized = term.replaceAll("\\s+", "");
        // 拆分乘除项
        boolean isMultiply = normalized.contains("*");
        boolean isDivide = normalized.contains("/");
        if (!isMultiply && !isDivide) return term;

        String u = "", v = "";
        if (isMultiply) {
            String[] parts = normalized.split("\\*", 2); // 只拆第一个*（避免嵌套）
            u = parts[0].trim();
            v = parts[1].trim();
        } else {
            String[] parts = normalized.split("/", 2);
            u = parts[0].trim();
            v = parts[1].trim();
        }

        // 处理括号：如果u或v被括号包围，去掉括号
        u = removeOuterBrackets(u);
        v = removeOuterBrackets(v);

        // 递归求导u'和v'
        String uDeriv = differentiate(u);
        String vDeriv = differentiate(v);

        // 加括号（解决含加减项的优先级问题）
        u = addParenthesesIfNeeded(u);
        v = addParenthesesIfNeeded(v);
        uDeriv = addParenthesesIfNeeded(uDeriv);
        vDeriv = addParenthesesIfNeeded(vDeriv);

        // 组合结果，并处理常数求导为0的情况
        String term1, term2;
        if (uDeriv.equals("0")) {
            term1 = "0";
        } else {
            // 优化：如果u求导为1，省略1*
            if (uDeriv.equals("1")) {
                term1 = v;
            } else {
                term1 = uDeriv + "*" + v;
            }
        }
        
        if (vDeriv.equals("0")) {
            term2 = "0";
        } else {
            // 优化：如果v求导为1，省略1*
            if (vDeriv.equals("1")) {
                term2 = u;
            } else {
                term2 = u + "*" + vDeriv;
            }
        }
        
        if (isMultiply) {
            if (term1.equals("0") && term2.equals("0")) {
                return "0";
            } else if (term1.equals("0")) {
                return term2;
            } else if (term2.equals("0")) {
                return term1;
            } else {
                return term1 + "+" + term2;
            }
        } else {
            return "(" + term1 + "-" + term2 + ")/(" + v + "^2)";
        }
    }
    
    // 移除外层括号
    private static String removeOuterBrackets(String expr) {
        if (expr.startsWith("(") && expr.endsWith(")")) {
            return expr.substring(1, expr.length() - 1).trim();
        }
        return expr;
    }

    // ================== 核心3：P类（复合函数）求导（含pow特例） ==================
    private static String deriveP(String term) {
        String normalized = term.replaceAll("\\s+", "");
        // 场景1：复合函数（P类）- 只有参数不是单纯x的才是复合函数
        Matcher funcMatcher = Pattern.compile("(sin|cos|tan|ln|exp|sqrt)\\((.+)\\)").matcher(normalized);
        if (funcMatcher.matches()) {
            String func = funcMatcher.group(1);
            String inner = funcMatcher.group(2).trim();
            
            // 只有当内部表达式不是单纯x时，才是复合函数
            if (!inner.equals("x")) {
                // 复合函数：sin(x+1) → cos(x+1)*(x+1)'
                String innerDeriv = differentiate(inner); // 递归求导内层
                String outerDeriv = DERIV_RULES.get(func).replace("{arg}", addParenthesesIfNeeded(inner));
                
                // 优化：如果内函数求导为1，省略*1
                if (innerDeriv.equals("1")) {
                    return addParenthesesIfNeeded(outerDeriv);
                } else {
                    return addParenthesesIfNeeded(outerDeriv) + "*" + addParenthesesIfNeeded(innerDeriv);
                }
            }
        }

        // 场景2：pow函数（三类情况）
        if (normalized.startsWith("pow(") && normalized.endsWith(")")) {
            String inner = normalized.substring(4, normalized.length() - 1).trim();
            String[] parts = splitPowArgs(inner);
            if (parts.length != 2 || parts[0].isEmpty() || parts[1].isEmpty()) return "0";

            String base = parts[0].trim();
            String exp = parts[1].trim();
            String baseDeriv = differentiate(base);
            String expDeriv = differentiate(exp);

            // 加括号
            base = addParenthesesIfNeeded(base);
            exp = addParenthesesIfNeeded(exp);
            baseDeriv = addParenthesesIfNeeded(baseDeriv);
            expDeriv = addParenthesesIfNeeded(expDeriv);

            // 情况1：指数为常数（非整数也支持）
            if (isConstant(exp) && !isConstant(base)) {
                double a = parseConstant(exp);
                return a + "*pow(" + base + "," + (a - 1) + ")*" + baseDeriv;
            }
            // 情况2：底数为常数（非整数也支持）
            else if (isConstant(base) && !isConstant(exp)) {
                double a = parseConstant(base);
                return "ln(" + a + ")*pow(" + a + "," + exp + ")*" + expDeriv;
            }
            // 情况3：两项都含变量（支持复杂表达式）
            else if (!isConstant(base) && !isConstant(exp)) {
                String innerPart = expDeriv + "*ln(" + base + ")+" + exp + "*" + baseDeriv + "/" + base;
                return "pow(" + base + "," + exp + ")*(" + innerPart + ")";
            }
            // 都为常数
            else {
                return "0";
            }
        }
        return term;
    }

    // ================== 核心4：加减项拆分（解决符号问题） ==================
    private static Map<String, List<?>> splitAddSub(String func) {
        List<Character> signs = new ArrayList<>();
        List<String> terms = new ArrayList<>();
        String normalized = func.replaceAll("\\s+", "");
        
        // 简化处理：直接按顶层加减号拆分
        int parenCount = 0;
        int start = 0;
        
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            if (c == '(') {
                parenCount++;
            } else if (c == ')') {
                parenCount--;
            } else if ((c == '+' || c == '-') && parenCount == 0) {
                if (i > start) {
                    String term = normalized.substring(start, i).trim();
                    if (!term.isEmpty()) {
                        terms.add(term);
                        signs.add(i > 0 ? normalized.charAt(i) : '+');
                    }
                }
                start = i + 1;
            }
        }
        
        // 添加最后一项
        if (start < normalized.length()) {
            String term = normalized.substring(start).trim();
            if (!term.isEmpty()) {
                terms.add(term);
                if (terms.size() > 1) {
                    signs.add('+');
                }
            }
        }

        Map<String, List<?>> result = new HashMap<>();
        result.put("signs", signs);
        result.put("terms", terms);
        return result;
    }

    // ================== 对外入口：求导主函数 ==================
    public static String differentiate(String function) {
        if (function == null || function.isEmpty()) return "0";
        
        // 处理负号情况 - 更精确的处理
        boolean hasNegative = false;
        String cleanFunction = function.trim();
        
        // 只有在开头是负号且不是减法运算时才处理
        if (cleanFunction.startsWith("-")) {
            // 检查是否是负号而不是减法
            if (cleanFunction.length() == 1 || cleanFunction.charAt(1) != '(') {
                hasNegative = true;
                cleanFunction = cleanFunction.substring(1);
            }
        }
        
        // 拆分加减项
        Map<String, List<?>> splitResult = splitAddSub(cleanFunction);
        List<Character> signs = (List<Character>) splitResult.get("signs");
        List<String> terms = (List<String>) splitResult.get("terms");

        // 逐项求导
        List<String> derivTerms = new ArrayList<>();
        for (String term : terms) {
            String deriv;
            // 新的分类优先级：M类（基本项）→ P类（复合项）→ N类（乘除项）
            if (isMClass(term)) {
                deriv = deriveM(term);
            } else if (isPClass(term)) {
                deriv = deriveP(term);
            } else if (isNClass(term)) {
                deriv = deriveN(term);
            } else {
                deriv = "0";
            }
            derivTerms.add(deriv);
        }

        // 组合结果
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < derivTerms.size(); i++) {
            if (i > 0 && i - 1 < signs.size()) {
                sb.append(signs.get(i - 1));
            }
            sb.append(derivTerms.get(i));
        }
        
        String result = sb.toString().replaceAll("^\\+", "").replaceAll("\\*\\*", "\\*");
        
        // 添加负号
        if (hasNegative) {
            result = "-" + result;
        }
        
        return simplify(result);
    }
    
    // ================== 化简函数 ==================
    private static String simplify(String expr) {
        if (expr == null || expr.isEmpty()) return expr;
        
        // 1. 负负得正、负正得负等符号化简
        expr = simplifySigns(expr);
        
        // 2. 数学化简：x/x=1
        expr = simplifyMath(expr);
        
        // 3. 移除 *1 的情况，但保留常数与变量之间的*
        expr = simplifyMultiplication(expr);
        
        // 4. 0删除原则：0后面跟着.小数点时不可删除
        expr = simplifyZero(expr);
        
        // 5. 移除开头的+
        expr = expr.replaceAll("^\\+", "");
        
        // 移除空括号
        expr = expr.replaceAll("\\(\\)", "");
        
        return expr.trim();
    }
    
    // 符号化简：负负得正、负正得负等
    private static String simplifySigns(String expr) {
        // 负负得正
        expr = expr.replaceAll("--", "+");
        // 负正得负
        expr = expr.replaceAll("-\\+", "-");
        expr = expr.replaceAll("\\+\\-", "-");
        return expr;
    }
    
    // 数学化简：x/x=1等
    private static String simplifyMath(String expr) {
        // 处理复杂除法化简：(表达式)/(相同表达式) = 1
        expr = simplifyComplexDivision(expr);
        
        // x/x = 1
        expr = expr.replaceAll("x/x", "1");
        // (x)/(x) = 1
        expr = expr.replaceAll("\\(x\\)/\\(x\\)", "1");
        
        return expr;
    }
    
    // 处理复杂除法化简：(表达式)/(相同表达式) = 1
    private static String simplifyComplexDivision(String expr) {
        // 查找形如 (A)/(A) 的模式
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\(([^()]+)\\)/\\(([^()]+)\\)");
        java.util.regex.Matcher matcher = pattern.matcher(expr);
        
        while (matcher.find()) {
            String numerator = matcher.group(1);
            String denominator = matcher.group(2);
            
            if (numerator.equals(denominator)) {
                expr = expr.replace(matcher.group(), "1");
                matcher = pattern.matcher(expr); // 重新匹配
            }
        }
        
        // 处理简单情况：A/A = 1（当A不是单个字符时）
        java.util.regex.Pattern simplePattern = java.util.regex.Pattern.compile("([^+\\-*/()^]+)/\\1");
        java.util.regex.Matcher simpleMatcher = simplePattern.matcher(expr);
        
        while (simpleMatcher.find()) {
            String term = simpleMatcher.group(1);
            if (term.length() > 1) {
                expr = expr.replace(simpleMatcher.group(), "1");
                simpleMatcher = simplePattern.matcher(expr); // 重新匹配
            }
        }
        
        return expr;
    }
    
    // 乘法化简：处理*1的情况，但保留常数与变量之间的*
    private static String simplifyMultiplication(String expr) {
        // 重要：不删除任何常数与变量之间的*号
        // 2*x、3*sin(x)、5*cos(x)、2.5*x等都应该保留*
        
        // 只移除变量与1之间的*：x*1 → x
        expr = expr.replaceAll("x\\*1(?![0-9a-zA-Z()])", "x");
        
        // 只移除1*变量的情况：1*x → x
        expr = expr.replaceAll("1\\*x", "x");
        
        // 移除1*函数的情况：1*sin(x) → sin(x)
        expr = expr.replaceAll("1\\*sin", "sin");
        expr = expr.replaceAll("1\\*cos", "cos");
        expr = expr.replaceAll("1\\*tan", "tan");
        expr = expr.replaceAll("1\\*ln", "ln");
        expr = expr.replaceAll("1\\*exp", "exp");
        expr = expr.replaceAll("1\\*sqrt", "sqrt");
        
        // 移除1*括号的情况：1*(x+1) → (x+1)
        expr = expr.replaceAll("1\\*\\(", "(");
        
        // 移除其他1*的情况（但不包括常数开头的）
        // 使用负向断言确保前面不是数字
        expr = expr.replaceAll("(?<!\\d)\\*1(?![0-9a-zA-Z()])", "");
        expr = expr.replaceAll("(?<!\\d)1\\*(?!\\d)", "");
        
        return expr;
    }
    
    // 0删除原则：0后面跟着.小数点时不可删除
    private static String simplifyZero(String expr) {
        // 0后面跟着.小数点时不可删除
        // 只删除独立的+0和-0
        expr = expr.replaceAll("\\+0(?!\\.)", "");
        expr = expr.replaceAll("-0(?!\\.)", "");
        
        // 处理开头的情况
        if (expr.startsWith("0") && expr.length() > 1 && expr.charAt(1) != '.') {
            expr = expr.substring(1);
        }
        
        return expr;
    }

    // ================== 分类判断函数 ==================
    private static boolean isMClass(String term) {
        // 处理负号情况
        String cleanTerm = term;
        if (cleanTerm.startsWith("-")) {
            cleanTerm = cleanTerm.substring(1);
        }
        if (cleanTerm.startsWith("(") && cleanTerm.endsWith(")")) {
            cleanTerm = cleanTerm.substring(1, cleanTerm.length() - 1);
        }
        
        if (cleanTerm.equals("x") || isConstant(cleanTerm)) return true;
        
        // 简单幂函数：x^2, 2*x^2
        if (Pattern.compile("^(-?\\d*(\\.\\d*)?)?x\\^(-?\\d+(\\.\\d+)?)$").matcher(cleanTerm).matches()) return true;
        
        // 基本函数：sin(x), cos(x), ln(x), exp(x) - 只有x作为参数
        if (Pattern.compile("(sin|cos|tan|ln|exp|sqrt)\\((x)\\)").matcher(cleanTerm).matches()) return true;
        
        // 简单乘法：2*x, 3*x^2
        if (Pattern.compile("^(-?\\d+(\\.\\d+)?)\\*x$").matcher(cleanTerm).matches()) return true;
        if (Pattern.compile("^x\\*(-?\\d+(\\.\\d+)?)$").matcher(cleanTerm).matches()) return true;
        
        return false;
    }
    
    private static boolean isPClass(String term) {
        // 处理负号情况
        String cleanTerm = term;
        if (cleanTerm.startsWith("-")) {
            cleanTerm = cleanTerm.substring(1);
        }
        if (cleanTerm.startsWith("(") && cleanTerm.endsWith(")")) {
            cleanTerm = cleanTerm.substring(1, cleanTerm.length() - 1);
        }
        
        String normalized = cleanTerm.replaceAll("\\s+", "");
        
        // 复合函数：sin(x+1), cos(x^2), ln(x^2+1) - 参数不是单纯x
        Matcher funcMatcher = Pattern.compile("(sin|cos|tan|ln|exp|sqrt)\\((.+)\\)").matcher(normalized);
        if (funcMatcher.matches()) {
            String inner = funcMatcher.group(2).trim();
            if (!inner.equals("x")) return true;
        }
        
        // pow函数：pow(f(x), g(x))
        if (normalized.startsWith("pow(") && normalized.endsWith(")")) return true;
        
        return false;
    }
    
    private static boolean isNClass(String term) {
        // 处理负号情况
        String cleanTerm = term;
        if (cleanTerm.startsWith("-")) {
            cleanTerm = cleanTerm.substring(1);
        }
        if (cleanTerm.startsWith("(") && cleanTerm.endsWith(")")) {
            cleanTerm = cleanTerm.substring(1, cleanTerm.length() - 1);
        }
        
        String normalized = cleanTerm.replaceAll("\\s+", "");
        return normalized.contains("*") || normalized.contains("/");
    }

    // ================== 向后兼容的方法 ==================
    public static String differentiate(String function, double x) {
        return differentiate(function);
    }
}
