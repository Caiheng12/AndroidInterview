# M/N/P类分类标准

## 🎯 分类概述
符号求导算法将所有数学表达式分为三大类：M类（基本项）、P类（复合项）、N类（乘除项），每类采用不同的求导策略。

## 📋 分类优先级
**M类 → P类 → N类**
- 优先判断M类（基本项）
- 其次判断P类（复合项）
- 最后归为N类（乘除项）

---

## 🔵 M类（基本项）- Basic Terms

### 📝 定义
可以直接求导的基础数学表达式，无需递归调用。

### 🎯 包含类型

#### 1. 变量
```
x → 1
```

#### 2. 常数
```
2 → 0
3.14 → 0
-5 → 0
```

#### 3. 基本函数（参数为x）
```
sin(x) → cos(x)
cos(x) → -sin(x)
tan(x) → sec²(x)
ln(x) → 1/x
exp(x) → exp(x)
sqrt(x) → 1/(2*sqrt(x))
```

#### 4. 简单幂函数
```
x^n → n*x^(n-1)
x^2 → 2x
x^3 → 3x²
x^0.5 → 0.5*x^(-0.5)
```

#### 5. 常数乘变量
```
2*x → 2
3.5*x → 3.5
-2*x → -2
```

### 🔍 判断标准
```java
boolean isMClass(String term) {
    // 1. 变量x
    if (term.equals("x")) return true;
    
    // 2. 常数
    if (isConstant(term)) return true;
    
    // 3. 基本函数
    if (matchesBasicFunction(term)) return true;
    
    // 4. 简单幂函数
    if (matchesSimplePower(term)) return true;
    
    // 5. 常数乘变量
    if (matchesConstantTimesX(term)) return true;
    
    return false;
}
```

---

## 🟢 P类（复合项）- Composite Terms

### 📝 定义
需要应用链式法则的复合函数或复杂幂函数。

### 🎯 包含类型

#### 1. 复合函数（参数不是x）
```
sin(x+1) → cos(x+1)*(x+1)'
cos(x^2) → -sin(x^2)*(x^2)'
ln(x^2+1) → 1/(x^2+1)*(x^2+1)'
exp(sin(x)) → exp(sin(x))*cos(x)
```

#### 2. pow函数（三种情况）

**情况1：底数是简单项，指数是复杂项**
```
pow(x, sin(x)+1) → pow(x, sin(x)+1)*(cos(x)*ln(x)+(sin(x)+1)/x)
pow(2, x^2) → pow(2, x^2)*2x*ln(2)
```

**情况2：底数是复杂项，指数是简单项**
```
pow(x^2+1, 3) → 3*pow(x^2+1, 2)*(x^2+1)'
pow(sin(x), 2) → 2*pow(sin(x), 1)*cos(x)
```

**情况3：底数和指数都是复杂项**
```
pow(x+sin(x), x^2+1) → pow(x+sin(x), x^2+1)*((x^2+1)*(x+sin(x))'/(x+sin(x)) + ln(x+sin(x))*(x^2+1)')
```

### 🔍 判断标准
```java
boolean isPClass(String term) {
    // 1. 复合函数
    if (matchesCompositeFunction(term)) return true;
    
    // 2. pow函数
    if (term.startsWith("pow(") && term.endsWith(")")) return true;
    
    return false;
}
```

---

## 🟡 N类（乘除项）- Multiplication/Division Terms

### 📝 定义
涉及乘法或除法运算的表达式，需要应用乘法法则或除法法则。

### 🎯 包含类型

#### 1. 乘法表达式
```
u*v → u'*v + u*v'
x*sin(x) → 1*sin(x) + x*cos(x)
(x+1)*(x^2) → (x+1)'*x^2 + (x+1)*(x^2)'
```

#### 2. 除法表达式
```
u/v → (u'*v - u*v')/v²
sin(x)/x → (cos(x)*x - sin(x)*1)/x²
(x^2+1)/(x+1) → (2x*(x+1) - (x^2+1)*1)/(x+1)²
```

### 🔍 判断标准
```java
boolean isNClass(String term) {
    // 包含顶层乘法或除法运算符
    return containsTopLevelMultiplication(term) || 
           containsTopLevelDivision(term);
}
```

---

## 📊 分类决策树

```mermaid
graph TD
    A[输入表达式] --> B{是否为M类?}
    B -->|是| C[M类求导]
    B -->|否| D{是否为P类?}
    D -->|是| E[P类求导]
    D -->|否| F[N类求导]
    
    B -->|检查| G[变量x?]
    B -->|检查| H[常数?]
    B -->|检查| I[基本函数sin(x)等?]
    B -->|检查| J[简单幂函数x^n?]
    B -->|检查| K[常数乘变量2*x等?]
    
    D -->|检查| L[复合函数sin(g(x))?]
    D -->|检查| M[pow函数?]
    
    F -->|检查| N[包含*或/运算符?]
```

---

## 🎯 边界情况处理

### ⚠️ 易混淆情况

#### 1. sin(x) vs sin(x+1)
```
sin(x) → M类（基本函数）
sin(x+1) → P类（复合函数）
```

#### 2. x^2 vs pow(x, sin(x))
```
x^2 → M类（简单幂函数）
pow(x, sin(x)) → P类（复杂幂函数）
```

#### 3. 2*x vs (x+1)*x
```
2*x → M类（常数乘变量）
(x+1)*x → N类（乘法运算）
```

#### 4. x/x vs x/(sin(x))
```
x/x → N类（除法运算，但可化简为1）
x/(sin(x)) → N类（复杂除法运算）
```

---

## 📋 分类表

| 表达式 | 类型 | 求导方法 | 示例 |
|--------|------|----------|------|
| x | M类 | 直接求导 | x → 1 |
| 5 | M类 | 直接求导 | 5 → 0 |
| sin(x) | M类 | 查表求导 | sin(x) → cos(x) |
| x^2 | M类 | 幂函数求导 | x^2 → 2x |
| sin(x+1) | P类 | 链式法则 | sin(x+1) → cos(x+1) |
| pow(x, 2) | P类 | pow求导 | pow(x, 2) → 2x |
| x*sin(x) | N类 | 乘法法则 | x*sin(x) → sin(x)+x*cos(x) |
| sin(x)/x | N类 | 除法法则 | sin(x)/x → (cos(x)*x-sin(x))/x² |

---

## 🔧 实现要点

### 1. 负号处理
分类前需要正确处理表达式开头的负号：
```
-sin(x) → 处理负号后分类sin(x)
-(x+1) → 处理负号后分类(x+1)
```

### 2. 括号处理
分类时需要考虑括号的影响：
```
(x+1)*x → N类（乘法运算）
(x+1) → M类（括号内是基本项）
```

### 3. 优先级判断
严格按照M→P→N的优先级进行分类，避免误判。

---

*最后更新: 2026-03-22*
*版本: v2.0 - 完整分类标准*
