package com.ls.lintplugin

import com.android.annotations.NonNull
import com.android.annotations.Nullable
import com.android.tools.lint.LintCliClient
import com.android.tools.lint.LintStats
import com.android.tools.lint.Main
import com.android.tools.lint.Reporter
import com.android.tools.lint.Warning
import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.Location
import com.android.tools.lint.detector.api.Position
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.TextFormat
import com.android.utils.SdkUtils
import com.google.common.base.Splitter

/**
 * 重写Reporter,以TXT格式输出Lint扫描结果
 * 通过HXLintReporter来将Lint扫描的结果精确到每一行的修改
 *
 * @author liushuanggo@gmail.com
 */
class LintTxtReporter extends Reporter {

    private Writer writer

    private List<Integer> startLines
    private List<Integer> endLines

    public int issueNumber = 0

    protected LintTxtReporter(@NonNull LintCliClient client, File output, Writer writer, List<Integer> start, List<Integer> end) {
        super(client, output)
        this.writer = writer
        this.startLines = start
        this.endLines = end
    }

    @Override
    void write(LintStats stats, List<Warning> issues) throws IOException {
        issueNumber = 0
        StringBuilder output = new StringBuilder(issues.size() * 200)
        output.append(outputBanner())
        output.append("\n")
        output.append("Lint检查日期: " + new Date().toString())
        output.append("\n\n")
        if (issues.isEmpty()) {
            if (isDisplayEmpty()) {
                output.append("没有扫描结果")
            }
        } else {
            Issue lastIssue = null
            boolean isBetweenNewLines
            int lineNo
            for (Warning warning : issues) {
                isBetweenNewLines = false

                //输出的行号与文件中对应的行号相差1,所以这里进行加1操作
                lineNo = warning.line + 1

                /*
                 * 1.找出扫描结果的行号是否在修改代码之间
                 */
                for (int i = 0; i < startLines.size(); i++) {
                    if (lineNo >= startLines.get(i) && lineNo < endLines.get(i)) {
                        //println("w line " + lineNo + " " + startLines.get(i) + " " + endLines.get(i))
                        isBetweenNewLines = true
                        break
                    }
                }

                /*
                 * 2.如果Lint扫描到的Issue不在修改的范围之内,结束这次循环
                 */
                if (!isBetweenNewLines) {
                    continue
                }

                /*
                 * 3.Lint扫描的Issue在修改的范围内,将扫描结果写入文件
                 */
                if (warning.issue != lastIssue) {
                    explainIssue(output, lastIssue)
                    lastIssue = warning.issue
                }

                //记录Issue的数量
                issueNumber++

                String p = warning.path
                if (p != null) {
                    output.append("(").append(issueNumber).append(")")
                    output.append("文件名: ")
                    appendPath(output, p)
                    output.append('\n')
                    output.append("问题行号: ")

                    if (warning.line >= 0) {
                        output.append(Integer.toString(lineNo))
                        output.append('\n')
                    }
                }

                Severity severity = warning.severity
                if (severity == Severity.FATAL) {
                    severity = Severity.ERROR
                }

                output.append(severity.getDescription())
                output.append(": ")
                output.append(TextFormat.RAW.convertTo(warning.message, TextFormat.TEXT))

                if (warning.issue != null) {
                    output.append(" [")
                    output.append(warning.issue.getId())
                    output.append(']')
                }
                output.append('\n')

                if (warning.errorLine != null && !warning.errorLine.isEmpty()) {
                    output.append("问题代码: ")
                    output.append(warning.errorLine)
                }
                //output.append('\n')

                if (warning.location != null && warning.location.getSecondary() != null) {
                    Location location = warning.location.getSecondary()
                    boolean omitted = false
                    while (location != null) {
                        if (location.getMessage() != null && !location.getMessage().isEmpty()) {
                            output.append("    ")
                            String path = client.getDisplayPath(warning.project, location.getFile())
                            appendPath(output, path)

                            Position start = location.getStart()
                            if (start != null) {
                                int line = start.getLine()
                                if (line >= 0) {
                                    output.append(':')
                                    output.append(Integer.toString(line + 1))
                                }
                            }

                            if (location.getMessage() != null && !location.getMessage().isEmpty()) {
                                output.append(": ")
                                output.append(TextFormat.RAW.convertTo(location.message, TextFormat.TEXT))
                            }

                            output.append('\n')
                        } else {
                            omitted = true
                        }

                        location = location.getSecondary()
                    }

                    if (omitted) {
                        location = warning.location.getSecondary()
                        StringBuilder sb = new StringBuilder(100)
                        sb.append("Also affects: ")
                        int begin = sb.length()
                        while (location != null) {
                            if (location.getMessage() == null
                                    || location.getMessage().isEmpty()) {
                                if (sb.length() > begin) {
                                    sb.append(", ")
                                }

                                String path = client.getDisplayPath(warning.project, location.getFile())
                                appendPath(sb, path)

                                Position start = location.getStart()
                                if (start != null) {
                                    int line = start.getLine()
                                    if (line >= 0) {
                                        sb.append(':')
                                        sb.append(Integer.toString(line + 1))
                                    }
                                }
                            }

                            location = location.getSecondary()
                        }
                        String wrapped = Main.wrap(sb.toString(), Main.MAX_LINE_WIDTH, "     ")
                        output.append(wrapped)
                    }
                }

                if (warning.isVariantSpecific()) {
                    List<String> names
                    if (warning.includesMoreThanExcludes()) {
                        output.append("Applies to variants: ")
                        names = warning.getIncludedVariantNames()
                    } else {
                        output.append("Does not apply to variants: ")
                        names = warning.getExcludedVariantNames()
                    }
                    output.append(Joiner.on(", ").join(names))
                    output.append('\n')
                }
            }

            if (issueNumber == 0) {
                output.append("没有扫描结果")
                output.append('\n')
            }
            explainIssue(output, lastIssue)
            output.append('\n\n')
            output.append("====================================================================================")
                    .append("\n")
                    .append("+++++++++++++++++++ " + "共发现" + issueNumber + "个Issue,请根据Issue说明的提示信息修改."+ "++++++++++++++++++++++++")
                    .append("\n")
                    .append("====================================================================================")
                    .append("\n")
            writer.write(output.toString())
            writer.write('\n')
            writer.flush()
        }
    }

    private void appendPath(@NonNull StringBuilder sb, @NonNull String path) {
        sb.append(path)
    }

    /**
     * 对出现的Issue进行说明
     *
     * @param output 输出
     * @param issue Lint Issue
     */
    private void explainIssue(@NonNull StringBuilder output, @Nullable Issue issue) {
        if (issue == null || issue == IssueRegistry.LINT_ERROR || issue == IssueRegistry.BASELINE) {
            return
        }

        output.append("\n请根据以下提示修改.")
        output.append("\n===================================Issue说明========================================\n")

        String explanation = issue.getExplanation(TextFormat.TEXT)
        if (explanation.trim().isEmpty()) {
            return
        }

        String indent = "   "
        String formatted = SdkUtils.wrap(explanation, Main.MAX_LINE_WIDTH - indent.length(), null)
        output.append('\n')
        output.append(indent)
        output.append("关于 Lint Issue \"").append(issue.getId()).append("\"的说明\n")
        for (String line : Splitter.on('\n').split(formatted)) {
            if (!line.isEmpty()) {
                output.append(indent)
                output.append(line)
            }
            output.append('\n')
        }
        output.append("==================================== end ==========================================\n\n\n")
        List<String> moreInfo = issue.getMoreInfo()
        if (!moreInfo.isEmpty()) {
            for (String url : moreInfo) {
                if (formatted.contains(url)) {
                    continue
                }
                output.append(indent)
                output.append(url)
                output.append('\n')
            }
            output.append('\n')
        }
    }

    private String outputBanner() {
        StringBuilder builder = new StringBuilder()
        builder.append("====================================================================================")
                .append("\n")
                .append("+++++++++++++++++++++++++++++++++++ Lint扫描结果 ++++++++++++++++++++++++++++++++++++")
                .append("\n")
                .append("====================================================================================")
                .append("\n")
        return builder.toString()
    }
}