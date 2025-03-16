# 🤖 Algenie - 轻量级AI虚拟助手

<p align="center">
  <img src="https://img.shields.io/badge/版本-1.0.0-blue.svg" alt="版本" />
  <img src="https://img.shields.io/badge/语言-Java-orange.svg" alt="语言" />
  <img src="https://img.shields.io/badge/Spring-Boot-brightgreen.svg" alt="Spring Boot" />
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

- 🧩 Algenie基于Java和TypeScript开发
- ☕ Java 17 (Spring)用于构建后端
- 🚀 Spring Boot 简化开发和人机交互管理
- 🧠 Spring AI 负责与各种语言模型的集成
- 🖼️ awt 负责UI界面

</details>

## 🚀 安装与使用

### 基础要求

- ☕ Java 17或更高版本
- 📦 安装最新的Maven
- 🌐 现代浏览器(对WebUI界面)

### 下载与运行

1️⃣ **从GitHub克隆下最新的代码**

```bash
git clone https://github.com/kong-fa/AIgenie.git
cd algenie
```

2️⃣ **使用Maven安装**

```bash
mvn clean package
```

3️⃣ **运行生成的JAR**

```bash
java -jar target/algenie-1.0.0.jar
```

## ⚙️ 配置说明

Algenie通过`application.yaml`文件配置，主要配置项如下：

```yaml
spring:
  ai:
    openai:
      api-key: your-api-key-here
      base-url: https://api.your-ai-provider.com/v1
      model: gpt-4
    azure:
      openai:
        api-key: your-azure-api-key
        endpoint: your-endpoint-url
        deployment-id: 0
        api-version: 2023-05-15

algenie:
  chat-history-limit: 10
  use-custom-client: true
```

## 🔑 关键功能说明

<table>
  <tr>
    <th>设置项</th>
    <th>描述</th>
  </tr>
  <tr>
    <td><code>api-key</code></td>
    <td>OpenAI或其他AI服务提供商的API密钥</td>
  </tr>
  <tr>
    <td><code>base-url</code></td>
    <td>用于指定自定义API端点的URL(如国内代理服务)</td>
  </tr>
  <tr>
    <td><code>model</code></td>
    <td>可指定使用的模型名称</td>
  </tr>
  <tr>
    <td><code>user-custom-client</code></td>
    <td>是否使用定制的HTTP客户端</td>
  </tr>
</table>

## 🎯 使用场景

### 基本操作

- 🖥️ 通过GUI，在浏览器中输入和查看AI对话结果
- 🔌 通过API接口，集成到现有应用程序中

### 高级用法：定制和扩展

- 📝 创建自定义提示词以满足特定需求
- ⚙️ 调整参数以获得最佳性能和效果

## 📁 结构文档

代码目录结构如下:

```
com.algenie
  ├── config     // 配置相关
  ├── model      // 数据模型
  │   ├── chat   // 对话相关模型
  │   └── util   // 工具类
  ├── api        // 接口定义
  ├── controller // 控制器
  ├── components // 组件
  └── service    // 服务实现
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
