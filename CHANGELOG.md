# Changelog

## 0.1.0-SNAPSHOT

- 初始化 `payment-gateway-java-sdk` Maven 项目。
- 支持 Java 8、JDK `HttpURLConnection` 默认传输层和可替换 `HttpTransport`。
- 支持商户配置加载、PEM/文本密钥读取、HS256 JWT、RSA-OAEP-256 + A256GCM 请求/响应加解密。
- 实现代收创建、支付查询、代付创建/查询/取消、退款创建/查询、余额查询、客户创建/查询方法。
- 响应外壳调整为 `code/msg/data/livemode`，`code=0` 表示成功。
- 增加敏感字段 `toString()` 排除和日志脱敏测试。
- 增加 Thymeleaf 页面联调控制台，入口为 `/payment-sdk/demo/apis`，支持客户、代收、退款、代付和余额查询 API 的沙盒参数生成、页面提交调用和响应展示。
- 完善代收 Demo 页面：支持 `customerId/customer` 二选一、收银台支付方式下拉选择，以及直连支付方式切换时自动联动 `paymentMethodData` 示例。
- 完善代付 Demo 页面：发起代付支持币种和支付方式下拉选择，并复用支付方式参数示例联动。
