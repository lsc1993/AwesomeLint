package com.ls.lintlib.detector;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;

import java.util.Arrays;
import java.util.List;

/**
 * 一个自定义Lint规则的简单示例
 *     Detector: 根据定义的lint规则进行扫描
 *     Issue: 描述一个自定义Lint规则
 *
 * @author liushuanggo@gmail.com
 * Time: 2019-4-13
 */
public class SimpleDetector extends Detector implements Detector.UastScanner {

    /**
     * 一个Lint规则
     * 这里是关于使用android.util.Log以及system.out的提示
     * 参数说明：
     * id: 唯一值,简短描述当前问题
     * briefDescription: 简短总结
     * explanation: 完整的描述问题以及提出修改建议
     * category: 问题类别
     * priority: 优先级 10最重要
     * severity: 严重级别
     * Implementation: 提供Issue和 Detector提供映射关系
     */
    public static final Issue ISSUE = Issue.create(
            "Log",
            "请使用项目中提供的Log工具",
            "避免在项目中直接使用android.log以及system.out",
            Category.PERFORMANCE,
            5,
            Severity.ERROR,
            new Implementation(SimpleDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    /**
     * 需要被检查的方法名
     *
     * @return 方法名
     */
    @Override
    public List<String> getApplicableMethodNames() {
        return Arrays.asList("v", "d", "i", "w", "e", "println", "print");
    }

    /*@Override
    public void visitMethod(@NotNull JavaContext context, @Nullable JavaElementVisitor visitor, @NotNull PsiMethodCallExpression call, @NotNull PsiMethod method) {
        if (context.getEvaluator().isMemberInClass(method, "android.util.Log")
                || context.getEvaluator().isMemberInClass(method, "java.io.PrintStream")) {
            //向 Android Studio 报告问题
            context.report(
                    ISSUE,
                    call.getContext(),
                    context.getLocation(call.getOriginalElement()),
                    "建议使用封装好的Log工具类打印日志");
        }
    }
*/
    @Override
    public void visitMethod(@NotNull JavaContext context, @NotNull UCallExpression node, @NotNull PsiMethod method) {
        if (context.getEvaluator().isMemberInClass(method, "android.util.Log") || context.getEvaluator().isMemberInClass(method, "java.io.PrintStream")) {
            context.report(ISSUE, node, context.getLocation(node), "应该使用项目中的Log工具!");
        }
    }
}
