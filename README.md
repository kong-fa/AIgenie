# 🤖 Algenie - 轻量级AI虚拟助手

<p align="center">
  <img src="https://img.shields.io/badge/版本-1.0.0-blue.svg" alt="版本" />
  <img src="https://img.shields.io/badge/Java-21-orange.svg" alt="Java" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.x-brightgreen.svg" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Spring%20AI-1.0%20GA-success.svg" alt="Spring AI" />
</p>

<p align="center">
  <i>为你的应用程序添加智能交互功能的轻量级框架</i>
</p>

---

## 📖 目录

- [功能特点](#-功能特点)
- [技术架构](#-技术架构)
- [安装与使用](#-安装与使用)
- [配置说明](#-配置说明)
- [关键功能说明](#-关键功能说明)
- [使用场景](#-使用场景)
- [结构文档](#-结构文档)
- [扩展指南](#-扩展指南)
- [常见问题](#-常见问题)
- [致谢](#-致谢)

---

## ✨ 功能特点

Algenie是一个轻量级的通用AI虚拟助手平台，具有高度可定制和扩展的特性。它可以为你的应用程序添加智能交互功能，同时保持了灵活性和易用性。

| 特点 | 描述 |
|------|------|
| 🔌 **通用性和可扩展性** | 为你的应用添加AI能力，易于集成和扩展 |
| 🔄 **支持多种模型** | 高度兼容开源以及各种API平台的语言模型 |
| ⚡ **即时响应能力** | 优化的架构确保快速响应 |
| 📊 **多模态支持（待开发）** | 可以处理文本、图像等多种类型的输入 |
| 🎨 **开箱即用的UI** | 为用户提供即用型界面 |
| 🆓 **开源免费** | 开源项目，可以自由使用和贡献 |

## 🏗️ 技术架构

<details open>
<summary><b>核心技术栈</b></summary>

- 🧩 Algenie 基于 Java 开发
- ☕ Java 21 LTS 用于构建后端
- 🚀 Spring Boot 3.5.x 简化开发和人机交互管理
- 🧠 Spring AI 1.0 GA 负责与各种语言模型的集成
- 🖼️ Swing/AWT 负责桌面 UI 界面

</details>

## 🚀 安装与使用

### 基础要求

- ☕ Java 21 或更高版本
- 📦 安装最新的 Maven 3.6+
- 🖥️ 桌面图形环境（用于 Swing GUI）

### 下载与运行

1️⃣ **从GitHub克隆下最新的代码**

```bash
git clone https://github.com/kong-fa/AIgenie.git
cd AIgenie
```

2️⃣ **使用Maven安装**

```bash
mvn clean package
```

3️⃣ **运行生成的JAR**

```bash
java -jar target/aIgenie-1.0-SNAPSHOT.jar
```

## ⚙️ 配置说明

AIgenie 通过 `src/main/resources/application.yaml` 文件配置，结构示意如下：

```yaml
spring:
  ai:
    openai:
      api-key: sk-xxxxxxxx
      base-url: https://api.siliconflow.cn/v1
      chat:
        options:
          model: Pro/deepseek-ai/DeepSeek-V3
          temperature: 0.7
          max-tokens: 2000

aigenie:
  system-prompt: 你是一个有用的AI助手，名为'AIgenie'。请简洁明了地回答用户的问题。
  chat-history-limit: 10
  use-custom-client: true   # true=使用内置 RestTemplate 流式客户端；false=使用 Spring AI ChatClient
  theme: 浅色
  docking-enabled: true
```

## 🔑 关键配置项说明

<table>
  <tr>
    <th>配置项</th>
    <th>描述</th>
  </tr>
  <tr>
    <td><code>spring.ai.openai.api-key</code></td>
    <td>OpenAI 或其他 OpenAI 兼容 API 服务商的密钥</td>
  </tr>
  <tr>
    <td><code>spring.ai.openai.base-url</code></td>
    <td>API Endpoint，默认指向硅基流动；可改为 OpenAI 官方或其他代理</td>
  </tr>
  <tr>
    <td><code>spring.ai.openai.chat.options.model</code></td>
    <td>使用的模型名称</td>
  </tr>
  <tr>
    <td><code>spring.ai.openai.chat.options.temperature</code></td>
    <td>采样温度（自定义客户端会从该字段读取）</td>
  </tr>
  <tr>
    <td><code>spring.ai.openai.chat.options.max-tokens</code></td>
    <td>单次回复最大 token 数</td>
  </tr>
  <tr>
    <td><code>aigenie.use-custom-client</code></td>
    <td>是否使用内置 RestTemplate 流式客户端，默认 <code>true</code>。<br>设为 <code>false</code> 时改用 Spring AI 1.0 的 ChatClient</td>
  </tr>
  <tr>
    <td><code>aigenie.chat-history-limit</code></td>
    <td>保留多少轮对话历史作为上下文</td>
  </tr>
  <tr>
    <td><code>aigenie.docking-enabled</code></td>
    <td>是否启用窗口边缘吸附停靠</td>
  </tr>
</table>

## 🎯 使用场景

### 基本操作

- 🖥️ 启动 JAR 后会弹出一个无边框桌面悬浮窗，在窗口底部输入框中输入并按回车与 AI 对话
- 📌 标题栏支持拖动、置顶、最小化、关闭、设置；窗口可吸附到屏幕边缘
- 🔌 通过 `AIService` 接口可在自有应用中集成相同的 AI 调用能力

### 高级用法：定制和扩展

- 📝 创建自定义提示词以满足特定需求
- ⚙️ 调整参数以获得最佳性能和效果

## 📁 结构文档

代码目录结构如下:

```
com.aIgenie
  ├── AIApplication.java              // Spring Boot 启动入口（非 web 模式 + 启动 GUI）
  ├── config/                         // Spring 配置（AIService Bean 装配）
  ├── controller/                     // 聊天控制器，桥接 UI 与 AI 服务
  ├── model/                          // 数据模型（ChatMessage 等）
  ├── service/                        // AI 服务接口
  │   └── impl/                       // RestTemplate 流式实现 + Spring AI ChatClient 实现
  ├── view/                           // Swing 顶层窗口
  │   ├── components/                 // 标题栏、输入栏、消息渲染等组件
  │   └── dialogs/                    // 设置对话框
  └── util/                           // 工具类（如停靠行为）
```

## 🔧 扩展指南

Algenie的架构设计支持多种扩展方式:

<details open>
<summary><b>集成AI模型提供商</b></summary>

- ✅ 已实现OpenAI、Azure OpenAI等集成
- 🔄 通过实现相应接口添加新的提供商支持

</details>

<details open>
<summary><b>自定义用户界面</b></summary>

- 🎨 基于`/components`目录中的组件构建自定义界面

</details>

## ❓ 常见问题

<details>
<summary><b>API连接超时或失败</b></summary>

- 🔍 检查网络连接和API密钥是否正确

</details>

<details>
<summary><b>调用限制问题</b></summary>

- 🔄 增加错误重试机制，或考虑升级API套餐

</details>

<details>
<summary><b>模型响应不符合预期</b></summary>

- ⚙️ 调整提示词或系统角色设置

</details>

<details>
<summary><b>请求超时限制</b></summary>

- ⏱️ 修改配置中的超时参数

</details>

## 🙏 致谢

如果Algenie对您的工作有所帮助，请考虑在GitHub上给予星标支持 ⭐

---

<p align="center">
  © Copyright Algenie Team<br>
  <a href="https://github.com/kong-fa/AIgenie.git">https://github.com/kong-fa/AIgenie.git</a>
</p>
