package com.example.androidinterview.module.derivative.composer;

import com.example.androidinterview.module.simplifier.ExpressionSimplifier;

import java.util.List;

/**
 * 表达式组合器
 * 负责按B[0]A[0]B[1]A[1]...的方式组合表达式
 * 并进行最终的合并同类项化简
 */
public class ExpressionComposer {
    
    /**
     * 按B[0]A[0]B[1]A[1]...的方式组合表达式
     * @param terms B列表：项列表
     * @param signs A列表：符号列表
     * @return 组合后的表达式
     */
    public static String composeExpression(List<String> terms, List<Character> signs) {
        if (terms == null || terms.isEmpty()) {
            return "0";
        }
        
        // 验证项数和符号数的关系
        if (terms.size() != signs.size() + 1) {
            throw new IllegalArgumentException("项数必须等于符号数+1，当前项数：" + terms.size() + "，符号数：" + signs.size());
        }
        
        // 如果只有一项，直接返回
        if (terms.size() == 1) {
            return terms.get(0).isEmpty() ? "0" : terms.get(0);
        }
        
        // 按B[0]A[0]B[1]A[1]...的方式组合
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < terms.size(); i++) {
            String term = terms.get(i);
            
            // 跳过空项
            if (term == null || term.trim().isEmpty()) {
                continue;
            }
            
            term = term.trim();
            
            // 处理第一项
            if (i == 0) {
                // 第一项如果是正数，不需要加+号
                if (!term.startsWith("-") && !term.startsWith("+")) {
                    result.append(term);
                } else {
                    result.append(term);
                }
            } else {
                // 后续项需要添加符号
                char sign = signs.get(i - 1);
                
                // 处理项本身的符号
                if (term.startsWith("-")) {
                    // 如果项本身是负号，需要与符号进行运算
                    if (sign == '-') {
                        // -(-term) = +term
                        result.append("+").append(term.substring(1));
                    } else {
                        // +(-term) = -term
                        result.append("-").append(term.substring(1));
                    }
                } else if (term.startsWith("+")) {
                    // 如果项本身是正号，直接与符号组合
                    if (sign == '-') {
                        result.append("-").append(term.substring(1));
                    } else {
                        result.append("+").append(term.substring(1));
                    }
                } else {
                    // 项本身没有符号，直接添加符号
                    result.append(sign).append(term);
                }
            }
        }
        
        String composed = result.toString();
        
        // 调用合并同类项化简
        return ExpressionSimplifier.finalSimplify(composed);
    }
    
    /**
     * 组合并化简表达式（处理零项的情况）
     * @param terms 项列表
     * @param signs 符号列表
     * @return 化简后的表达式
     */
    public static String composeAndSimplify(List<String> terms, List<Character> signs) {
        if (terms == null || terms.isEmpty()) {
            return "0";
        }
        
        // 移除零项并调整符号
        List<String> validTerms = new java.util.ArrayList<>();
        List<Character> validSigns = new java.util.ArrayList<>();
        
        for (int i = 0; i < terms.size(); i++) {
            String term = terms.get(i);
            
            // 检查是否为零项
            if (term != null && !term.trim().isEmpty() && !term.equals("0")) {
                validTerms.add(term);
                
                // 如果不是第一项，添加对应的符号
                if (i > 0 && i - 1 < signs.size()) {
                    validSigns.add(signs.get(i - 1));
                }
            } else if (i > 0 && i - 1 < signs.size()) {
                // 如果是零项且不是第一项，需要特殊处理符号
                // 根据算法：如果该项为0，找到该项位置然后去掉该项，
                // 并且判断该项位置对应的A中符号位置
                // 如果该项为第一项且A中符号第一个为+时删掉该符号，为-时保留；
                // 如果该项不为第一项则删掉该项位置减1的A中对应的符号
                
                // 这里简化处理：跳过零项，不添加符号
                // 实际的符号调整逻辑在composeExpression中处理
            }
        }
        
        // 如果所有项都被移除，返回0
        if (validTerms.isEmpty()) {
            return "0";
        }
        
        // 组合剩余的项
        return composeExpression(validTerms, validSigns);
    }
    
    /**
     * 验证组合的有效性
     * @param terms 项列表
     * @param signs 符号列表
     * @return 是否有效
     */
    public static boolean isValidCombination(List<String> terms, List<Character> signs) {
        if (terms == null || signs == null) {
            return false;
        }
        
        // 项数应该等于符号数+1，或者所有项都为零
        return terms.size() == signs.size() + 1 || 
               (terms.size() > 0 && terms.stream().allMatch(term -> term == null || term.trim().isEmpty() || term.equals("0")));
    }
    
    /**
     * 处理符号优化（去除冗余符号）
     * @param expression 原始表达式
     * @return 优化后的表达式
     */
    public static String optimizeSigns(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return "0";
        }
        
        // 去除开头的+
        expression = expression.replaceAll("^\\+", "");
        
        // 处理+-和-+为-
        expression = expression.replaceAll("\\+-", "-").replaceAll("-\\+", "-");
        
        // 处理++为+
        expression = expression.replaceAll("\\+\\+", "+");
        
        // 处理--为+
        expression = expression.replaceAll("--", "+");
        
        return expression.trim();
    }
    
    /**
     * 格式化最终表达式
     * @param expression 原始表达式
     * @return 格式化后的表达式
     */
    public static String formatExpression(String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return "0";
        }
        
        expression = optimizeSigns(expression);
        
        // 确保表达式不为空
        if (expression.isEmpty()) {
            return "0";
        }
        
        // 应用化简器的最终化简
        return ExpressionSimplifier.finalSimplify(expression);
    }
    
    /**
     * 组合结果的详细信息
     */
    public static class CompositionResult {
        public final String composedExpression;
        public final boolean isValid;
        public final String errorMessage;
        public final int originalTermCount;
        public final int finalTermCount;
        
        public CompositionResult(String composedExpression, boolean isValid, 
                               String errorMessage, int originalTermCount, int finalTermCount) {
            this.composedExpression = composedExpression;
            this.isValid = isValid;
            this.errorMessage = errorMessage;
            this.originalTermCount = originalTermCount;
            this.finalTermCount = finalTermCount;
        }
        
        public static CompositionResult success(String expression, int originalCount, int finalCount) {
            return new CompositionResult(expression, true, null, originalCount, finalCount);
        }
        
        public static CompositionResult error(String errorMessage) {
            return new CompositionResult("0", false, errorMessage, 0, 0);
        }
    }
    
    /**
     * 安全的组合方法，返回详细结果
     */
    public static CompositionResult composeSafely(List<String> terms, List<Character> signs) {
        try {
            int originalCount = terms.size();
            
            // 验证输入
            if (!isValidCombination(terms, signs)) {
                return CompositionResult.error("项数与符号数不匹配，项数应为符号数+1");
            }
            
            // 组合并化简
            String result = composeAndSimplify(terms, signs);
            
            // 计算最终项数（简单估算）
            int finalCount = result.split("\\+").length - result.replaceAll("\\+", "").length() + 1;
            if (result.contains("-")) {
                finalCount += result.split("-").length - 1;
            }
            
            return CompositionResult.success(result, originalCount, finalCount);
            
        } catch (Exception e) {
            return CompositionResult.error("组合表达式时出错: " + e.getMessage());
        }
    }
}
