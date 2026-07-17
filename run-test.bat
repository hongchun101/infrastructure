@echo off
set JAVA_HOME=C:\lang\jdk25
set PATH=C:\lang\jdk25\bin;%PATH%
echo === ENV === 
java -version
echo === GRADLE === 
"C:\Users\101\.gradle\wrapper\dists\gradle-9.3.0-bin\79n14ral3mx1ozqr3csh2u872\gradle-9.3.0\bin\gradle.bat" %*
