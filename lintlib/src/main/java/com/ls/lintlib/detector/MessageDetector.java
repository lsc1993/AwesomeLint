package com.ls.lintlib.detector;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiMethod;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UCallExpression;

import java.util.Arrays;
import java.util.List;

/**
 * 检查代码中直接使用 new Message() 的情况
 * 建议使用 handler.obtainMessage() 或者 Message.obtain() 方法"
 *
 * @author liushuanggo@gmail.com
 * date 2019/5/21
 */
public class MessageDetector extends Detector implements Detector.UastScanner {

    public static final Issue ISSUE = Issue.create(
            "Message_Obtain_Tip",
            "建议使用handler.obtainMessage()或者Message.obtain()方法",
            "为了减少内存开销,不应该直接使用{new Message()},应该使用 {handler.obtainMessage()方法} 或者 {Message.obtain()方法},这样的话从整个Message池中返回一个新的Message实例,从而能够避免重复Message创建对象,减少内存开销.",
            Category.PERFORMANCE,
            5,
            Severity.WARNING,
            new Implementation(MessageDetector.class, Scope.JAVA_FILE_SCOPE)
    );


    @Nullable
    @Override
    public List<String> getApplicableConstructorTypes() {
        return Arrays.asList("android.os.Message");
    }

    @Override
    public void visitConstructor(@NotNull JavaContext context, @NotNull UCallExpression node, @NotNull PsiMethod constructor) {
        context.report(ISSUE, node, context.getLocation(node), "避免直接使用new Message()");
    }
}
