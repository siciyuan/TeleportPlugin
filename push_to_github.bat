@echo off
cd /d "C:\Users\scroam\Desktop\teleport-plugin-refactored"

set PATH=D:\Program Files\Git\bin;%PATH%

echo 初始化 Git 仓库...
git init

echo 添加远程仓库...
git remote add origin https://github.com/siciyuan/mc-sever.git

echo 设置用户名和邮箱...
git config user.name "siciyuan"
git config user.email "your@email.com"

echo 添加文件...
git add .

echo 提交代码...
git commit -m "Initial commit: TeleportPlugin v2.2.0"

echo 推送到 GitHub...
git push -u origin master

echo 完成！
pause