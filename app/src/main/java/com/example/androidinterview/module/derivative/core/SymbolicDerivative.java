package com.example.androidinterview.module.derivative.core;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.androidinterview.module.simplifier.ExpressionSimplifier;

/**
 * 符号求导核心模块
 * 负责数学表达式的符号微分计算
 */
public class SymbolicDerivative {

    // 所有基础函数导数规则（按高数教材标准）
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
        DERIV_RULES.put("ln", "1/({arg})");                // 自然对数
        DERIV_RULES.put("log", "1/((ln(10))*({arg}))");    // 10为底对数
        DERIV_RULES.put("exp", "exp({arg})");              // e^x
        // 新增：任意底数指数函数（a^x 或 pow(a,x)）
        DERIV_RULES.put("pow_const_x", "pow({a},{x})*ln({a})"); // a^x → a^x * ln(a)
        DERIV_RULES.put("exp_const_base", "{base}^x*ln({base})"); // 简写 a^x 专用

        // 幂函数（pow(x,a) 等价于 x^a）
        DERIV_RULES.put("pow", "{a}*({arg})^({a}-1)");
    }

    // ===================== 对外入口（含定义域校验） =====================
    public static String differentiate(String function, double x) {
        // 第一步：校验x是否在原函数定义域内，不在则返回"无意义"
        if (!isInDomain(function, x)) {
            return "导数在 x=" + x + " 处无意义（原函数定义域外）";
        }

        // 第二步：正常求导
        return differentiateInternal(function);
    }

    // 向后兼容的方法（无定义域校验）
    public static String differentiate(String function) {
        return differentiateInternal(function);
    }

    private static String differentiateInternal(String function) {
        if (function == null || function.trim().isEmpty()) {
            return "0";
        }

        String preprocessed = preprocess(function);
        SplitResult splitResult = splitByAddSub(preprocessed);
        List<Character> signs = new ArrayList<>(splitResult.signs);
        List<String> terms = splitResult.terms;

        List<String> validDerivTerms = new ArrayList<>();
        for (int i = 0; i < terms.size(); i++) {
            String term = terms.get(i).trim();
            String termDeriv = deriveSingleTerm(term);
            String simplifiedTerm = ExpressionSimplifier.simplifyTerm(termDeriv);

            if (!simplifiedTerm.equals("0")) {
                validDerivTerms.add(simplifiedTerm);
            } else {
                if (i > 0 && i - 1 < signs.size()) {
                    signs.remove(i - 1);
                }
            }
        }

        String derivExpr = ExpressionSimplifier.combineLikeTerms(validDerivTerms, signs);
        // 第三步：如果导数表达式为空，返回0
        return derivExpr.isEmpty() ? "0" : derivExpr;
    }

    // ===================== 定义域校验核心逻辑 =====================
    private static boolean isInDomain(String function, double x) {
        function = function.toLowerCase().replaceAll("\\s+", "");

        // 1. 对数函数：ln(x)/log(x) → x>0
        if (function.contains("ln(") || function.contains("log(")) {
            if (x <= 0) return false;
        }

        // 2. 平方根：sqrt(...) → 内部≥0
        Matcher sqrtMatcher = Pattern.compile("sqrt\\(([^)]+)\\)").matcher(function);
        while (sqrtMatcher.find()) {
            String inner = sqrtMatcher.group(1).replace("x", String.valueOf(x));
            try {
                double innerVal = evaluateExpr(inner);
                if (innerVal < 0) return false;
            } catch (Exception e) {
                return false;
            }
        }

        // 3. 反三角函数：
        // arcsin(x)/arccos(x) → x∈[-1,1]
        if (function.contains("arcsin(") || function.contains("arccos(")) {
            if (x < -1 || x > 1) return false;
        }

        // 4. 分母不为0
        Matcher divMatcher = Pattern.compile("/([^+\\-*/()]+)").matcher(function);
        while (divMatcher.find()) {
            String denom = divMatcher.group(1).replace("x", String.valueOf(x));
            try {
                double denomVal = evaluateExpr(denom);
                if (denomVal == 0) return false;
            } catch (Exception e) {
                return false;
            }
        }

        // 5. 幂函数：x^负数 → x≠0
        Matcher negPowMatcher = Pattern.compile("x\\^(-\\d+(\\.\\d+)?)").matcher(function);
        if (negPowMatcher.find() && x == 0) {
            return false;
        }

        // ✅ 新增：任意底数指数函数 a^x → 底数a>0且a≠1
        Matcher constBaseExpMatcher = Pattern.compile("(\\d+(\\.\\d+)?)\\^x").matcher(function);
        if (constBaseExpMatcher.matches()) {
            double base = Double.parseDouble(constBaseExpMatcher.group(1));
            if (base <= 0 || base == 1) {
                return false;
            }
        }
        // 校验 pow(a,x) 格式的底数
        Matcher powMatcher = Pattern.compile("pow\\((\\d+(\\.\\d+)?),x\\)").matcher(function);
        if (powMatcher.matches()) {
            double a = Double.parseDouble(powMatcher.group(1));
            if (a <= 0 || a == 1) {
                return false;
            }
        }

        // 其他情况默认在定义域内
        return true;
    }

    // ===================== 辅助：简单表达式求值（用于定义域校验） =====================
    private static double evaluateExpr(String expr) {
        // 简化实现：支持基础运算和常见函数
        try {
            // 替换数学函数
            expr = expr.replace("sqrt", "Math.sqrt")
                    .replace("ln", "Math.log")
                    .replace("log", "Math.log10")
                    .replace("exp", "Math.exp")
                    .replace("sin", "Math.sin")
                    .replace("cos", "Math.cos")
                    .replace("tan", "Math.tan");

            // 适配 pow(x,a) → Math.pow(x,a)
            expr = expr.replaceAll("pow\\(([^,]+),([^)]+)\\)", "Math.pow($1,$2)");
            
            // 简单的数值解析（支持基础运算）
            if (expr.matches("-?\\d+(\\.\\d+)?")) {
                return Double.parseDouble(expr);
            }
            
            // 对于复杂表达式，返回NaN（表示无法解析）
            return Double.NaN;
        } catch (Exception e) {
            return Double.NaN;
        }
    }

    // ===================== 预处理 =====================
    private static String preprocess(String expr) {
        return expr.replaceAll("\\s+", "")
                .replaceAll("--", "+")
                .replaceAll("\\+-", "-")
                .replaceAll("-\\+", "-")
                .replaceAll("\\+\\+", "+");
    }

    // ===================== 按加减拆分 =====================
    private static SplitResult splitByAddSub(String expr) {
        List<Character> signs = new ArrayList<>();
        List<String> terms = new ArrayList<>();
        expr = expr.replaceAll("^\\+", "");

        int startIdx = 0;
        String firstSign = "";
        if (expr.startsWith("-")) {
            firstSign = "-";
            startIdx = 1;
        }

        int parenCount = 0;
        int lastSplit = startIdx;
        for (int i = startIdx; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') parenCount++;
            else if (c == ')') parenCount--;
            else if ((c == '+' || c == '-') && parenCount == 0) {
                terms.add(firstSign + expr.substring(lastSplit, i));
                firstSign = "";
                signs.add(c);
                lastSplit = i + 1;
            }
        }
        terms.add(firstSign + expr.substring(lastSplit));
        return new SplitResult(signs, terms);
    }

    // ===================== 单项求导（支持任意次幂+全函数） =====================
    private static String deriveSingleTerm(String term) {
        if (term.isEmpty()) return "0";

        // 纯变量x
        if (term.equals("x")) return "1";
        if (term.equals("-x")) return "-1";

        // ✅ 支持任意次幂（整数/小数/负数，如 x^0.5、x^-2）
        Matcher powerMatcher = Pattern.compile("(-?)(\\d*\\.?\\d*)?x\\^(\\-?\\d+(\\.\\d+)?)").matcher(term);
        if (powerMatcher.matches()) {
            String sign = powerMatcher.group(1);
            String coeffStr = powerMatcher.group(2).isEmpty() ? "1" : powerMatcher.group(2);
            double exp = Double.parseDouble(powerMatcher.group(3)); // 改为double支持小数/负数
            double newCoeff = Double.parseDouble(coeffStr) * exp;
            double newExp = exp - 1;

            StringBuilder res = new StringBuilder(sign);
            // 系数处理：小数保留1位，整数转long
            if (newCoeff != 1 || newExp == 0) {
                if (newCoeff == (long) newCoeff) {
                    res.append((long) newCoeff);
                } else {
                    res.append(String.format("%.1f", newCoeff)); // 保留1位小数，避免冗余
                }
            }
            // 幂次处理：支持负数/小数次幂
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

        // 乘法项
        List<String> mulTerms = splitByMul(term);
        if (mulTerms.size() == 2) {
            String u = mulTerms.get(0);
            String v = mulTerms.get(1);
            String uDeriv = deriveSingleTerm(u);
            String vDeriv = deriveSingleTerm(v);
            return "(" + uDeriv + ")*" + v + "+" + u + "*(" + vDeriv + ")";
        }

        // 除法项
        List<String> divTerms = splitByDiv(term);
        if (divTerms.size() == 2) {
            String u = divTerms.get(0);
            String v = divTerms.get(1);
            String uDeriv = deriveSingleTerm(u);
            String vDeriv = deriveSingleTerm(v);
            return "((" + uDeriv + ")*" + v + "-" + u + "*(" + vDeriv + "))/(" + v + "^2)";
        }

        // ✅ 新增：匹配任意底数指数函数 a^x（如 2^x、e^x、10^x）
        Matcher constBaseExpMatcher = Pattern.compile("(\\d+(\\.\\d+)?)\\^x").matcher(term);
        if (constBaseExpMatcher.matches() && constBaseExpMatcher.group(1) != null) {
            String base = constBaseExpMatcher.group(1);
            // 求导规则：a^x → a^x * ln(a)
            return base + "^x*ln(" + base + ")";
        }

        // ✅ 新增：匹配 pow(a,x) 格式（如 pow(2,x)、pow(e,x)）
        Matcher powConstXMatcher = Pattern.compile("pow\\((\\d+(\\.\\d+)?),(x)\\)").matcher(term);
        if (powConstXMatcher.matches()) {
            String a = powConstXMatcher.group(1);
            String x = powConstXMatcher.group(3);
            // 求导规则：pow(a,x) → pow(a,x)*ln(a)
            return "pow(" + a + "," + x + ")*ln(" + a + ")";
        }

        // ✅ 支持所有基础复合函数（含pow）
        Matcher funcMatcher = Pattern.compile("(sin|cos|tan|cot|sec|csc|arcsin|arccos|arctan|ln|log|exp|pow)\\((.+)\\)").matcher(term);
        if (funcMatcher.matches()) {
            String func = funcMatcher.group(1);
            String innerExpr = funcMatcher.group(2);
            
            // 特殊处理pow函数的复合形式：pow(f(x), g(x))
            if (func.equals("pow")) {
                return derivePowComposite(innerExpr);
            }
            
            String outerDeriv = DERIV_RULES.get(func).replace("{arg}", innerExpr);
            String innerDeriv = deriveSingleTerm(innerExpr);
            return innerDeriv.equals("1") ? outerDeriv : "(" + outerDeriv + ")*(" + innerDeriv + ")";
        }

        // pow(x,a) 特殊处理（如 pow(x,2) → 2*x）
        Matcher powMatcher = Pattern.compile("pow\\((x),(\\-?\\d+(\\.\\d+)?)\\)").matcher(term);
        if (powMatcher.matches()) {
            String a = powMatcher.group(2);
            String outerDeriv = DERIV_RULES.get("pow").replace("{a}", a).replace("{arg}", "x");
            return outerDeriv;
        }

        // 最后判断常数
        if (isConstant(term)) return "0";

        return "0";
    }

    // ===================== pow复合函数求导 =====================
    private static String derivePowComposite(String innerExpr) {
        // 使用新的splitPowArgs方法解析 pow(f(x), g(x)) 形式
        String[] args = splitPowArgs(innerExpr);
        if (args.length != 2 || args[0].isEmpty() || args[1].isEmpty()) {
            return "0"; // 无法解析，返回0
        }
        
        String base = args[0];
        String exponent = args[1];
        
        // 判断base和exponent是否包含变量
        boolean baseHasVariable = containsVariable(base);
        boolean exponentHasVariable = containsVariable(exponent);
        
        // 情况1：只有第一项含变量，第二项是常数
        if (baseHasVariable && !exponentHasVariable) {
            // 导数：a*pow(f(x),a-1)*(f(x)的导数)，其中a为常数
            return derivePowCase1(base, exponent);
        }
        
        // 情况2：只有第二项含变量，第一项是常数
        if (!baseHasVariable && exponentHasVariable) {
            // 导数：ln(a)*pow(a,f(x))*(f(x)的导数)，其中a为常数
            return derivePowCase2(base, exponent);
        }
        
        // 情况3：pow(f(x),g(x))两项都含变量
        if (baseHasVariable && exponentHasVariable) {
            // 导数：pow(f(x),g(x))*{(g(x)的导数)*ln(f(x))+g(x)*(f(x)的导数)/f(x)}
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
        
        String baseDeriv = differentiateInternal(base); // 使用完整的求导方法支持多项式
        if (baseDeriv.equals("0")) {
            return "0";
        }
        
        // a*pow(f(x),a-1)*(f(x)的导数)
        StringBuilder result = new StringBuilder();
        
        // 系数a
        if (!exponent.equals("1")) {
            result.append(exponent).append("*");
        }
        
        // pow(f(x),a-1) - 统一使用pow格式
        String newExponent = exponent.equals("1") ? "0" : "(" + exponent + "-1)";
        result.append("pow(").append(base).append(",").append(newExponent).append(")");
        
        // *(f(x)的导数)
        if (!baseDeriv.equals("1")) {
            result.append("*(").append(baseDeriv).append(")");
        }
        
        // 应用化简器进行最终化简（包括合并同类项）
        return ExpressionSimplifier.finalSimplify(result.toString());
    }
    
    // 情况2：只有第二项含变量，第一项是常数
    private static String derivePowCase2(String base, String exponent) {
        // 验证base是否为常数（支持小数）
        if (!isConstant(base)) {
            return "0"; // 如果不是常数，按情况3处理
        }
        
        String exponentDeriv = differentiateInternal(exponent); // 使用完整的求导方法支持多项式
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
        if (!exponentDeriv.equals("1")) {
            result.append("*(").append(exponentDeriv).append(")");
        }
        
        // 应用化简器进行最终化简（包括合并同类项）
        return ExpressionSimplifier.finalSimplify(result.toString());
    }
    
    // 情况3：pow(f(x),g(x))两项都含变量
    private static String derivePowCase3(String base, String exponent) {
        String baseDeriv = differentiateInternal(base); // 使用完整求导方法
        String exponentDeriv = differentiateInternal(exponent); // 使用完整求导方法
        
        // 如果两个导数都是0，结果为0
        if (baseDeriv.equals("0") && exponentDeriv.equals("0")) {
            return "0";
        }
        
        // pow(f(x),g(x))*{(g(x)的导数)*ln(f(x))+g(x)*(f(x)的导数)/f(x)}
        StringBuilder result = new StringBuilder();
        result.append("pow(").append(base).append(",").append(exponent).append(")");
        result.append("*(");
        
        boolean hasFirstTerm = !exponentDeriv.equals("0");
        boolean hasSecondTerm = !baseDeriv.equals("0");
        
        // 第一项：(g(x)的导数)*ln(f(x))
        if (hasFirstTerm) {
            if (!exponentDeriv.equals("1")) {
                result.append("(").append(exponentDeriv).append(")");
            }
            result.append("*ln(").append(base).append(")");
        }
        
        // 第二项：g(x)*(f(x)的导数)/f(x)
        if (hasSecondTerm) {
            if (hasFirstTerm) {
                result.append("+");
            }
            result.append(exponent).append("*(").append(baseDeriv).append(")/").append(base);
        }
        
        result.append(")");
        
        // 应用化简器进行最终化简（包括合并同类项）
        return ExpressionSimplifier.finalSimplify(result.toString());
    }
    
    // 辅助方法：判断表达式是否包含变量
    private static boolean containsVariable(String expr) {
        // 简单检查是否包含变量x
        if (expr.contains("x")) {
            // 排除纯数字中的x（如sin中的x是变量）
            return true;
        }
        
        // 检查是否包含其他函数（这些函数内部通常包含变量）
        return expr.contains("sin(") || expr.contains("cos(") || 
               expr.contains("tan(") || expr.contains("ln(") || 
               expr.contains("log(") || expr.contains("exp(") || 
               expr.contains("pow(") || expr.contains("sqrt(");
    }
    
    // 辅助方法：找到括号外的逗号位置
    private static int findCommaOutsideParentheses(String expr) {
        int parenCount = 0;
        for (int i = 0; i < expr.length(); i++) {
            char c = expr.charAt(i);
            if (c == '(') parenCount++;
            else if (c == ')') parenCount--;
            else if (c == ',' && parenCount == 0) {
                return i;
            }
        }
        return -1;
    }

    // 安全拆分 pow(a,b) 的参数，处理括号和加减表达式
    private static String[] splitPowArgs(String inner) {
        int parenCount = 0;
        int splitIndex = -1;
        for (int i = 0; i < inner.length(); i++) {
            char c = inner.charAt(i);
            if (c == '(') parenCount++;
            if (c == ')') parenCount--;
            // 只在最外层逗号处拆分，忽略括号内的逗号
            if (c == ',' && parenCount == 0) {
                splitIndex = i;
                break;
            }
        }
        if (splitIndex == -1) return new String[]{"", ""};
        return new String[]{
            inner.substring(0, splitIndex).trim(),  // 底数（支持加减，如 x-1）
            inner.substring(splitIndex + 1).trim()  // 指数（支持 1/2, 0.5）
        };
    }

    // ===================== 辅助方法 =====================
    // 判断是否为纯常数（不含x，且是数字/分数）
    private static boolean isConstant(String expr) {
        if (expr == null) return false;
        String trimmed = expr.trim();
        // 含x → 一定不是常数
        if (trimmed.contains("x")) return false;
        // 匹配：整数、小数、分数（如 2, 3.14, 1/2, -0.5）
        return trimmed.matches("-?\\d+(\\.\\d+)?") || trimmed.matches("-?\\d+/\\d+");
    }

    // 解析常数为double（支持分数/小数）
    private static double parseConstant(String s) {
        if (s == null || s.isEmpty()) return 0;
        String trimmed = s.trim();
        if (trimmed.contains("/")) {
            String[] parts = trimmed.split("/");
            double num = Double.parseDouble(parts[0]);
            double den = Double.parseDouble(parts[1]);
            return num / den;
        }
        return Double.parseDouble(trimmed);
    }

    private static List<String> splitByMul(String term) {
        List<String> res = new ArrayList<>();
        int parenCount = 0;
        int lastSplit = 0;
        for (int i = 0; i < term.length(); i++) {
            char c = term.charAt(i);
            if (c == '(') parenCount++;
            else if (c == ')') parenCount--;
            else if (c == '*' && parenCount == 0) {
                res.add(term.substring(lastSplit, i));
                lastSplit = i + 1;
            }
        }
        res.add(term.substring(lastSplit));
        return res;
    }

    private static List<String> splitByDiv(String term) {
        List<String> res = new ArrayList<>();
        int parenCount = 0;
        int lastSplit = 0;
        for (int i = 0; i < term.length(); i++) {
            char c = term.charAt(i);
            if (c == '(') parenCount++;
            else if (c == ')') parenCount--;
            else if (c == '/' && parenCount == 0) {
                res.add(term.substring(lastSplit, i));
                lastSplit = i + 1;
            }
        }
        res.add(term.substring(lastSplit));
        return res;
    }

    private static class SplitResult {
        List<Character> signs;
        List<String> terms;

        SplitResult(List<Character> signs, List<String> terms) {
            this.signs = signs;
            this.terms = terms;
        }
    }

    // 组合项的方法
    private static String combineTerms(List<String> terms, List<Character> signs) {
        if (terms.isEmpty()) return "0";
        if (terms.size() == 1) {
            String term = terms.get(0);
            return signs.get(0) == '-' ? "-" + term : term;
        }
        
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < terms.size(); i++) {
            if (i > 0) {
                result.append(signs.get(i));
            } else {
                if (signs.get(i) == '-') {
                    result.append("-");
                }
            }
            result.append(terms.get(i));
        }
        
        return result.toString();
    }

}
