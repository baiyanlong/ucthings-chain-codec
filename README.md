# ucthings-chain-codec (链式编解码)
根据迅哥与自身想法的结合搞一个可配置的，支持多个语言的脚本的链式编解码工程


## 项目背景

```text
1.本工程主要是将现有繁琐编解码进行改进，通过可配置，链式调用，多种语言支持等优势替换现有编解码 即ucthings-transport
```

## 技术栈选型

* 待定
    

## 环境和安装

* 待定

## 部署和运维

* 待定
   
## 项目架构

* 待定

```text
├── lib  依赖jar
            ├── logs 日志输出
            ├── protocol-application.jar 单体协议jar
            ├── resources 配置文件目录
            │   ├── application.yml 启动配置文件
            │   ├── banner.txt banner
            │   └── log4j2-spring.xml 日志输出配置文件
            └── startup.sh 启动脚本
```

## 整体架构

```text
参见doc目录
1. 成都城投协议层.drawio
2. 设备状态处理逻辑.png

## 目录结构

```text
├── deploy                      部署相关
├── doc                         文档相关
├── pom.xml
├── protocol-protocol-application 主程序启动
├── protocol-common             通用处理
├── protocol-http               http服务，用于和智联交互
                                    智联文档：
                                    1. 洛书上位机-设备层单灯协议(1).docx
├── protocol-tcp                tcp服务，用于单灯和集线器交互
                                    协议文档：
                                     1. 【内部】路灯控制系统(CAT1方案)集线器通讯协议_V0.1.3.docx
                                     2. 路灯控制系统(CAT1方案)通讯协议_V1.16_20210122.docx
                                     3. 路灯控制系统(CAT1方案)OTA功能流程与命令设计_V1.4_20201212.docx
                                     4. Cat1 路灯Demo指令整理.md
                                     5. Cat1 集线器 Demo 指令整理.md
└── transport-common            协议通用（tcp，mqtt）
```

### 项目成员

排名不分先后

* 白艳龙


# 开发基础服务信息

