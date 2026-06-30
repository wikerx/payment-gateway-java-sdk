# Changelog

## 0.1.0-SNAPSHOT

- 初始化 `payment-gateway-java-sdk` Maven 项目。
- 支持 Java 8、JDK `HttpURLConnection` 默认传输层和可替换 `HttpTransport`。
- 支持商户配置加载、PEM/文本密钥读取、HS256 JWT、RSA-OAEP-256 + A256GCM 请求/响应加解密。
- 实现代收创建、支付查询、代付创建/查询/取消、退款创建/查询、余额查询、客户创建/查询方法。
- 响应外壳调整为 `code/msg/data/livemode`，`code=0` 表示成功。
- 增加敏感字段 `toString()` 排除和日志脱敏测试。
