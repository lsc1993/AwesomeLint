package com.ls.lintplugin

import com.android.tools.lint.XmlReporter
import com.ls.lintlib.LintIssueRegistry
import org.gradle.api.Plugin
import org.gradle.api.Project

class LintPlugin implements Plugin<Project> {

    def fileNameFix = [".java", ".xml"] as String[]

    @Override
    void apply(Project project) {
        project.extensions.create("lintConfig", LintConfig.class)
        project.task("lintCheck") << {
            println("=========== Lint check start ==============")

            String[] filenamePostfix
            if (project.lintConfig != null) {
                String fileType = project.lintConfig.lintCheckFileType
                if (fileType != null) {
                    filenamePostfix = fileType.split(",")
                }
            }
            if (filenamePostfix == null || filenamePostfix.length <= 0) {
                filenamePostfix = fileNameFix
            }

            List<String> list = getCommitChange(project)
            List<File> files = new ArrayList<>()
            File file
            List<Integer> startIndex = new ArrayList<>()
            List<Integer> endIndex = new ArrayList<>()

            for (String s : list) {
                println("file path: " + s)
                if (isMatchFile(filenamePostfix, s)) {
                    file = new File(s)
                    files.add(file)
                    getFileChangeStatus(s, project, startIndex, endIndex)
                }
            }

            println("need checked files size:" + files.size())
            println(System.getenv("ANDROID_HOME"))

            def cl = new LintToolClient()
            def flag = cl.flags // LintCliFlags 用于设置Lint检查的一些标志
            flag.setExitCode = true
            /*
             * HtmlReport
             * 输出HTML格式的报告
             * 输出路径:/{$rootDir}/lint-all-result.html
             */
            //Reporter reporter = new HtmlReporter(cl, new File("lint-result.html"), flag)
            //flag.reporters.add(reporter)

            //是否输出全部的扫描结果
            if (project.lintConfig != null && project.lintConfig.lintReportAll) {
                File outputResult = new File("lint-check-result-all.xml")
                def xmlReporter = new XmlReporter(cl, outputResult)
                flag.reporters.add(xmlReporter)
            }

            /*
             * 输出TXT格式的报告
             */
            File lintResult = new File("lint-check-result.txt")
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(lintResult), "UTF-8"))
            def txtReporter = new LintTxtReporter(cl, lintResult, writer, startIndex, endIndex)
            flag.reporters.add(txtReporter)

            /*
             * 执行run方法开始lint检查
             *
             * LintIssueRegistry()-> 自定义Lint检查规则
             * files->需要检查的文件文件
             * result 检查结果 设置flag.setExitCode = true时, 有错误的时候返回1 反之返回0
             */
            cl.run(new LintIssueRegistry(), files)

            println("issue number: " + txtReporter.issueNumber)

            //根据报告中存在的问题进行判断是否需要回退
            if (txtReporter.issueNumber > 0) {
                //回退commit
                "git reset HEAD~1".execute(null, project.getRootDir())
            }

            println("============ Lint check end ===============")
        }

        /*
         * gradle task: 将git hooks 脚本复制到.git/hooks文件夹下
         * 根据不同的系统类型复制不同的git hooks脚本(现支持Windows、Linux两种)
         */
        project.task("installGitHooks").doLast {
            println("OS Type:" + System.getProperty("os.name"))
            File postCommit
            String OSType = System.getProperty("os.name")
            if (OSType.contains("Windows")) {
                postCommit = new File(project.rootDir, "post-commit-windows")
            } else {
                postCommit = new File(project.rootDir, "post-commit")
            }

            project.copy {
                from (postCommit) {
                    rename {
                        String filename ->
                            "post-commit"
                    }
                }
                into new File(project.rootDir, ".git/hooks/")
            }
        }
    }

    /**
     * 通过Git命令获取需要检查的文件
     *
     * @param project gradle.Project
     * @return 文件名
     */
    static List<String> getCommitChange(Project project) {
        ArrayList<String> filterList = new ArrayList<>()
        try {
            //此命令获取本次提交的文件 在git commit之后执行
            String command = "git diff --name-only --diff-filter=ACMRTUXB HEAD~1 HEAD~0"
            String changeInfo = command.execute(null, project.getRootDir()).text.trim()
            if (changeInfo == null || changeInfo.empty) {
                return filterList
            }

            String[] lines = changeInfo.split("\\n")
            return lines.toList()
        } catch (Exception e) {
            e.printStackTrace()
            return filterList
        }
    }

    /**
     * 通过git diff获取已提交文件的修改,包括文件的添加行的行号、删除行的行号、修改行的行号
     *
     * @param filePath 文件路径
     * @param project Project对象
     * @param startIndex 修改开始的下表数组
     * @param endIndex 修改结束的下表数组
     */
    void getFileChangeStatus(String filePath, Project project, List<Integer> startIndex, List<Integer> endIndex) {
        try {
            String command = "git diff --unified=0 --ignore-blank-lines --ignore-all-space HEAD~1 HEAD " + filePath
            String changeInfo = command.execute(null, project.getRootDir()).text.trim()
            String[] changeLogs = changeInfo.split("@@")
            String[] indexArray

            for (int i = 1; i < changeLogs.size(); i += 2) {
                indexArray = changeLogs[i].trim().split(" ")
                try {
                    int start, end
                    String[] startArray = null
                    if (indexArray.length > 1) {
                        startArray = indexArray[1].split(",")
                    }

                    if (startArray != null && startArray.length > 1) {
                        start = Integer.parseInt(startArray[0])
                        end = Integer.parseInt(startArray[0]) + Integer.parseInt(startArray[1])
                    } else {
                        start = Integer.parseInt(startArray[0])
                        end = start + 1
                    }
                    startIndex.add(start)
                    endIndex.add(end)
                } catch (NumberFormatException e) {
                    e.printStackTrace()
                    startIndex.add(0)
                    endIndex.add(0)
                }

            }
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    /**
     * 检查特定后缀的文件
     * 比如: .java .xml等
     *
     * @param fileName 文件名
     * @return 匹配 返回true 否则 返回 false
     */
    boolean isMatchFile(String[] fileNamePostfix, String fileName) {
        for (String fix : fileNamePostfix) {
            if (fileName.endsWith(fix)) {
                return true
            }
        }
        return false
    }
}

class LintConfig {
    def lintCheckFileType = ""
    def lintReportAll = false
}