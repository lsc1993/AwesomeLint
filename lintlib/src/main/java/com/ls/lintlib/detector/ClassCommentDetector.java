package com.ls.lintlib.detector;


import com.android.tools.lint.client.api.UElementHandler;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UComment;
import org.jetbrains.uast.UElement;
import org.jetbrains.uast.java.JavaUClass;
import org.jetbrains.uast.kotlin.KotlinUClass;

import java.util.Collections;
import java.util.List;

/**
 * 检测类注释
 * 这个Issue的作用就是检查类是否有符合模板的注释
 *
 * @author liushuanggo@gmail.com
 * date 2019/5/21.
 */

public class ClassCommentDetector extends Detector implements Detector.UastScanner {

    public static final String ID = "ClassComment";
    public static final String DESCRIPTION = "类注释";
    public static final String EXPLANATION = "每个类都需要注释，其内容包含描述、作者、日期，可以在 AS 中加入你喜欢的模板\n" +
            "/**\n" +
            " * Author : \n" +
            " * Time ：\n" +
            " * Desc ：\n" +
            " */\n";
    public static final Category CATEGORY = Category.CORRECTNESS;
    public static final int PRIORITY = 6;
    public static final Severity SEVERITY = Severity.ERROR;
    public static final Implementation IMPLEMENTATION = new Implementation(ClassCommentDetector.class, Scope.JAVA_FILE_SCOPE);

    private static final String EXPLANATION_SIMPLE = "请添加正确的类注释";
    private static final String AUTHOR = "author";
    private static final String DATE = "date";
    private static final String TIME = "time";
    private static final String DESC = "desc";

    public static final Issue ISSUE = Issue.create(
            ID,
            DESCRIPTION,
            EXPLANATION,
            CATEGORY,
            PRIORITY,
            SEVERITY,
            IMPLEMENTATION
    );

    @Nullable
    @Override
    public List<Class<? extends UElement>> getApplicableUastTypes() {
        return Collections.singletonList(UClass.class);
    }

    @Nullable
    @Override
    public UElementHandler createUastHandler(@NotNull JavaContext context) {
        return new UElementHandler(){
            @Override
            public void visitClass(@NotNull UClass node) {
                // 必须是Java或kotlin类以及非内部类才会接受检测
                boolean isTarget = (node instanceof JavaUClass || node instanceof KotlinUClass) && node.getContainingClass() == null;
                if(!isTarget){
                    return ;
                }
                List<UComment> uComments = node.getComments();
                // 获取类的第一个注释，即类注释 uComments不可能为null
                if(uComments.size() > 0){
                    String comment = uComments.get(0).asSourceString().toLowerCase();
                    // 类描述必须有
                    if(comment.contains(AUTHOR) && comment.contains(DESC)){
                        return ;
                    }
                }
                context.report(ISSUE, context.getNameLocation(node), EXPLANATION_SIMPLE);
            }
        };
    }
}

