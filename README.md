# AwesomeLint
## 使用说明

AwesomeLint可以分为Lint代码静态检查以及Lint代码增量检查。其中Lint代码静态检查以aar的方式引入到项目中。Lint代码增量检查工具以插件的方式引入到项目中。

<strong>注意：目前aar以及插件都是在本地实现的，引用是按照本地引用方法即可。</strong>

### 代码静态检查功能的使用

Lint自定义规则是封装在aar中的，直接添加依赖即可

> 如果Lint规则没有生效，可以尝试重启Android Studio

### git增量检查的使用

在项目中引用插件 <em>apply plugin: 'lintplugin'</em>

LintPlugin的配置，与apply plugin 都可以发在顶级build.gradle文件中

```gradle
     lintConfig {
        //Lint检查文件的类型，默认是.java和.xml。可以自定义其他类型的文件
        lintCheckFileType = ".java,.xml" 
        //默认是false。为true的时候会扫描git commit时候所有的代码并且输出扫描
        lintReportAll = false 
     } 
```

> 每次git commit都会通过git hooks触发Lint检查。检查结果会以TXT格式输出到项目根目录下，如果有问题，则会触发 git reset命令回滚提交。

### 环境准备

使用<strong>git提交增量检查</strong>时需要配置ANDROID_HOME环境变量(需要以ANDROID_HOME命名并加入到path中，因为在Lint框架中执行Lint检查时需要获取Android环境变量)

```txt 
        Windows环境：在电脑->属性->环境变量中编辑即可
```

```sh
        Linux环境：编辑 ~/.bashrc即可
        vi ~./bashrc
        export ANDROID_HOME=$HOME/{Android SDK 路径}
        export PATH=$PATH:$ANDROID_HOME/tools
```

### 关于git hooks脚本

Windows系统与Linux系统对应不同的git hooks脚本，触发git增量检查功能需要将git hooks脚本 **修改名称（修改为post-commit）** 并且复制到项目根目录的.git/hooks（此目录是隐藏目录）目录下。

<strong>Windows系统下的git hooks脚本</strong>

头部路径需要修改：#!C:/Program\ Files/Git/bin/sh.exe   改为    #!{git安装路径}/bin/sh.exe

```shell
#!C:/Program\ Files/Git/bin/sh.exe
./gradlew lintCheck
exit 0
```



<strong>Linux系统下git hooks脚本</strong>

```shell
#!/bin/sh
./gradlew lintCheck
exit 0
```

