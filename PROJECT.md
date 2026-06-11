# 天机学堂 (TJXT)

## 项目简介

基于 Spring AI 的在线教育平台，集成 AI 智能问答与课程推荐功能。

## 技术栈

| 分类 | 技术 |
|------|------|
| 基础框架 | Spring Boot 3.3.5 + Spring Cloud Alibaba 2023.0.3.2 |
| AI 能力 | Spring AI 1.0.0 + 通义千问 DashScope + RAG 向量检索 + 多智能体 |
| 注册配置 | Nacos |
| 网关 | Spring Cloud Gateway |
| 数据库 | MySQL 8.0 + ShardingSphere |
| 缓存 | Redis + Redisson |
| 消息队列 | RabbitMQ |
| 搜索引擎 | Elasticsearch 8.13.4 |
| 对象存储 | 阿里云 OSS + 腾讯云 COS |
| 视频点播 | 腾讯云 VOD |
| 定时任务 | XXL-Job |
| 分布式事务 | Seata |
| ORM | MyBatis-Plus |
| 工具库 | Hutool + Lombok |

## 模块说明

| 模块 | 功能 |
|------|------|
| tj-aigc | AI 对话中心，基于 Spring AI + 通义千问的智能问答、RAG 知识库、多智能体编排、聊天记忆管理 |
| tj-course | 课程管理，分类、目录、内容、教师管理、草稿与发布流程 |
| tj-user | 用户中心，学生、教师、运营人员多角色管理 |
| tj-learning | 学习服务，课程进度、签到、积分、笔记、互动问答（含 AI 自动回复）、排行榜 |
| tj-exam | 考试与题库，题目管理、试题分类 |
| tj-search | 搜索服务，基于 ES 的课程搜索、兴趣标签、个性化推荐 |
| tj-trade | 交易中心，购物车、下单、退款 |
| tj-pay | 支付服务，支付 SDK 封装 |
| tj-promotion | 营销中心，优惠券管理、兑换码算法、优惠叠加策略 |
| tj-media | 媒体服务，文件上传、视频点播（阿里云/腾讯云多平台）|
| tj-message | 消息服务，短信通知、站内信 |
| tj-remark | 点赞系统，Redis 异步落库 |
| tj-data | 数据看板，ECharts 可视化报表 |
| tj-common | 公共组件，通用工具类、异常处理、AOP |
| tj-api | 内部 Feign 接口定义 |
| tj-auth | 认证授权中心 |
| tj-gateway | 微服务网关，鉴权、路由、Swagger 聚合 |

## AI 功能详解

### 1. 增强型智能助手（RAG + 流式对话）

集成 Spring AI 与阿里通义千问（DashScope）大语言模型。

- 将课程知识向量化存入 Elasticsearch 向量库，提问时检索 TopK 文档作为上下文，降低大模型幻觉
- 基于 Project Reactor（Flux）实现 SSE 流式输出，前端逐字展示
- 聊天历史存入 Redis，支持多轮对话上下文
- 支持用户主动终止输出，中断时自动保存已生成内容
- 提供纯文本聊天接口供内部服务调用

### 2. 多智能体编排系统（路由工作流）

采用路由智能体模式实现意图识别与任务分发：

- 路由智能体分析用户意图，分发至下游子智能体
- 课程推荐智能体结合 RAG 知识库推荐课程
- 课程咨询智能体查询课程详情回答用户
- 课程购买智能体调用预下单工具生成订单
- 知识讲解智能体基于向量库回答课程知识问题

智能体系统提示词通过 Nacos 配置中心动态加载，支持热更新。

### 3. AI 自动回复

用户在互动问答模块提问题时，系统异步调用 AI 接口自动生成回答，提升师生互动效率。

## 高并发架构设计

- 签到模块使用 Redis BitMap 存储，节省 80% 空间
- 学习进度采用 Redis 缓存 + DelayQueue 异步落库
- 积分排行榜基于 ZSet 实时排行，XXL-Job 分片持久化至分库分表
- 优惠券自研脱库校验兑换码算法 + 并发安全领券 + 最优叠加推荐
- 点赞评论通过 RabbitMQ 异步解耦

## 部署架构

- Docker 容器化部署
- OpenJDK 17 (Eclipse Temurin)
- Nacos 配置中心 + 服务发现
- 多环境配置 (dev / test / local)
