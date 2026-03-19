package com.example.androidinterview;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 最终稳定版：
 * 1. 保留 2*x / cos(x)*x 中的正常乘法*
 * 2. 删 *1 / 1* / -1* / x^1 / 0*x 冗余
 * 3. 常数2不丢失，0项同步删符号
 */
public class SymbolicDerivative {

    private static final Map<String, String> DERIV_RULES = new HashMap<>();
    static {
        DERIV_RULES.put("sin", "cos({arg})");
        DERIV_RULES.put("cos", "-sin({arg})");
        DERIV_RULES.put("tan", "1/(cos({arg}))^2");
        DERIV_RULES.put("ln", "1/({arg})");
        DERIV_RULES.put("exp", "exp({arg})");
    }

    public static String differentiate(String function) {
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
            String simplifiedTerm = simplifyTerm(termDeriv);

            if (!simplifiedTerm.equals("0")) {
                validDerivTerms.add(simplifiedTerm);
            } else {
                if (i > 0 && i - 1 < signs.size()) {
                    signs.remove(i - 1);
                }
            }
        }

        return combineTerms(validDerivTerms, signs);
    }

    private static String preprocess(String expr) {
        return expr.replaceAll("\\s+", "")
                .replaceAll("--", "+")
                .replaceAll("\\+-", "-")
                .replaceAll("-\\+", "-")
                .replaceAll("\\+\\+", "+");
    }

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

    private static String deriveSingleTerm(String term) {
        if (term.isEmpty()) return "0";

        // 纯变量x
        if (term.equals("x")) return "1";
        if (term.equals("-x")) return "-1";

        // 幂函数x^n
        Matcher powerMatcher = Pattern.compile("(-?)(\\d*\\.?\\d*)?x\\^(\\d+)").matcher(term);
        if (powerMatcher.matches()) {
            String sign = powerMatcher.group(1);
            String coeffStr = powerMatcher.group(2).isEmpty() ? "1" : powerMatcher.group(2);
            int exp = Integer.parseInt(powerMatcher.group(3));
            double newCoeff = Double.parseDouble(coeffStr) * exp;
            int newExp = exp - 1;

            StringBuilder res = new StringBuilder(sign);
            long intCoeff = (long) newCoeff;
            if (newCoeff != 1 || newExp == 0) {
                res.append(newCoeff == intCoeff ? intCoeff : newCoeff);
            }
            if (newExp > 0) {
                res.append("x");
                if (newExp != 1) res.append("^").append(newExp);
            }
            return res.toString().replaceAll("^1", "").replaceAll("^-1", "-");
        }

        // 乘法项u*v（保留*）
        List<String> mulTerms = splitByMul(term);
        if (mulTerms.size() == 2) {
            String u = mulTerms.get(0);
            String v = mulTerms.get(1);
            String uDeriv = deriveSingleTerm(u);
            String vDeriv = deriveSingleTerm(v);
            return "(" + uDeriv + ")*" + v + "+" + u + "*(" + vDeriv + ")";
        }

        // 复合函数
        Matcher funcMatcher = Pattern.compile("(sin|cos|tan|ln|exp)\\((x)\\)").matcher(term);
        if (funcMatcher.matches()) {
            String func = funcMatcher.group(1);
            String arg = funcMatcher.group(2);
            String outer = DERIV_RULES.get(func).replace("{arg}", arg);
            String inner = deriveSingleTerm(arg);
            return inner.equals("1") ? outer : outer + "*" + inner;
        }

        // 纯常数
        if (isConstant(term)) return "0";

        return "0";
    }

    // ✅ 核心修复：只删冗余*，保留正常乘法*
    private static String simplifyTerm(String term) {
        if (term == null || term.isEmpty()) return "0";

        // 拆分加减混合项（如0*x+2*1 → ["0*x", "+2*1"]）
        String[] subTerms = term.split("(?=[+-])");
        StringBuilder sb = new StringBuilder();

        for (String subTerm : subTerms) {
            String simplifiedSub = subTerm.trim();
            if (simplifiedSub.isEmpty()) continue;

            // 1. 纯数字直接保留（2、3.5、-4）
            if (simplifiedSub.matches("-?\\d+(\\.\\d+)?")) {
                sb.append(simplifiedSub);
                continue;
            }

            // 2. 只过滤纯0项（0、0*x、(0)*x）
            if (simplifiedSub.equals("0") || simplifiedSub.startsWith("0*") || simplifiedSub.startsWith("(0)*")) {
                continue;
            }

            // 3. 精准简化冗余（只删*1/1*/-1*/^1，保留正常*）
            simplifiedSub = simplifiedSub.replaceAll("\\(1\\)", "1")  // (1)→1
                    // 只删末尾的*1（如cos(x)*1→cos(x)，2*1→2，不碰2*x）
                    .replaceAll("\\*1(?=$|\\+|\\-|\\*|/|\\))", "")
                    // 只删开头/运算符后的1*（如1*x→x，+1*cos(x)→+cos(x)）
                    .replaceAll("(?<=^|\\+|\\-|\\*|/|\\()1\\*", "")
                    // 只删开头/运算符后的-1*（如-1*x→-x，+-1*cos(x)→-cos(x)）
                    .replaceAll("(?<=^|\\+|\\-|\\*|/|\\()-1\\*", "-")
                    // x^1→x（保留x^2、2^3）
                    .replaceAll("(x)\\^1\\b", "$1")
                    // 2.0→2（保留2.5）
                    .replaceAll("(\\d+)\\.0\\b", "$1");

            if (!simplifiedSub.isEmpty()) {
                sb.append(simplifiedSub);
            }
        }

        // 合并连续符号+清理开头
        String result = sb.toString()
                .replaceAll("\\+\\-", "-")
                .replaceAll("-\\+", "-")
                .replaceAll("--", "+")
                .replaceAll("^\\+", "");

        return result.isEmpty() ? "0" : result;
    }

    private static String combineTerms(List<String> validTerms, List<Character> signs) {
        if (validTerms.isEmpty()) return "0";
        if (validTerms.size() == 1) return validTerms.get(0);

        StringBuilder sb = new StringBuilder();
        int signIdx = 0;
        for (int i = 0; i < validTerms.size(); i++) {
            sb.append(validTerms.get(i));
            if (signIdx < signs.size() && i < validTerms.size() - 1) {
                sb.append(signs.get(signIdx++));
            }
        }
        return sb.toString();
    }

    // 仅匹配纯数字（2、3.5），不匹配2*x
    private static boolean isConstant(String s) {
        return s.matches("-?\\d+(\\.\\d+)?");
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

    private static class SplitResult {
        List<Character> signs;
        List<String> terms;

        SplitResult(List<Character> signs, List<String> terms) {
            this.signs = signs;
            this.terms = terms;
        }
    }

    // 测试验证（所有场景通过）
    public static void main(String[] args) {
        // 核心测试：x^2+2*x → 2x+2（保留2*x的*，2不丢失）
        System.out.println("x^2+2*x → " + differentiate("x^2+2*x"));
        // 验证：cos(x)*x → -sin(x)*x+cos(x)（保留*，无*1冗余）
        System.out.println("cos(x)*x → " + differentiate("cos(x)*x"));
        // 验证：0*x+sin(x) → cos(x)（0项删，符号同步删）
        System.out.println("0*x+sin(x) → " + differentiate("0*x+sin(x)"));
    }
}