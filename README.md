# Payment Gateway Java SDK

商户服务端 Java SDK，用于对接 Payment Gateway OpenAPI。SDK 会按后端接口实际协议生成 `Authorization`，对新协议代收创建请求发送 Bearer JWT 和加密 data，对历史接口发送 Payment API 私钥鉴权，解析响应外壳，并在响应 `data` 为 compact 密文时自动解密。

> 本 SDK 只能用于商户服务端。禁止放在浏览器、移动端 App、桌面客户端或任何会暴露 JWT 密钥、RSA 私钥、卡号、CVC 的环境。

## 要求

- Java 8
- Maven
- 默认 HTTP 传输使用 JDK 8 `HttpURLConnection`
- 不依赖 Spring Boot，不引入 OkHttp、Apache HttpClient、Gson 或 fastjson 1.x

## 安装

```xml
<dependency>
    <groupId>com.wikerx.payment</groupId>
    <artifactId>payment-gateway-java-sdk</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

## 配置

将配置文件放到商户服务端 classpath，例如 `merchant-config.properties`。

```properties
merchant.id=<merchant-id>
merchant.api.private-key=<merchant-api-private-key>
merchant.platform.public-key-file=classpath:keys/platform-request-public-key.pem
merchant.response.private-key-file=classpath:keys/merchant-response-private-key.pem
merchant.openapi.base-url=https://payment-gateway.example.com
```

也支持文本密钥，文件配置优先于文本配置：

```properties
merchant.platform.public-key=<x509-public-key-base64-or-pem>
merchant.response.private-key=<pkcs8-private-key-base64-or-pem>
```

## 创建客户端

```java
PaymentGatewayClient client = PaymentGatewayClient.create();
```

多商户或测试场景可指定 classpath 配置名：

```java
PaymentGatewayClient client = PaymentGatewayClient.create("merchant-config-us.properties");
```

## 收银台支付

```java
CheckoutPaymentRequest request = new CheckoutPaymentRequest();
request.setOrderNo("ORDER-1001");
request.setCurrency("USD");
request.setAmount(new BigDecimal("14.99"));
request.setReturnUrl("https://merchant.example.com/return");
request.setNotifyUrl("https://merchant.example.com/notify");
request.setPaymentMethod("CHECKOUT");

PaymentGatewayResult<PaymentResponse> result = client.createCheckoutPayment(request);
```

金额请使用 `new BigDecimal("14.99")`，不要使用 `new BigDecimal(14.99)`。

## 信用卡直连

```java
CardPaymentRequest request = new CardPaymentRequest();
request.setOrderNo("ORDER-CARD-1001");
request.setCurrency("USD");
request.setAmount(new BigDecimal("20.00"));
request.setWebsite("https://merchant.example.com");

CardPaymentMethodData card = new CardPaymentMethodData();
card.setNumber("4111111111111111");
card.setExpMonth("12");
card.setExpYear("2030");
card.setCvc("123");
request.setPaymentMethodData(card);

PaymentGatewayResult<PaymentResponse> result = client.createCardPayment(request);
```

卡号和 CVC 已从模型 `toString()` 中排除；商户侧仍必须避免自行打印请求对象。

## 代付

```java
PayoutCreateRequest request = new PayoutCreateRequest();
request.setOrderNo("PO-1001");
request.setCurrency("USD");
request.setAmount(new BigDecimal("9.99"));
request.setPaymentMethod("PAY_PAL");
request.setPaymentMethodData(Collections.singletonMap("email", "receiver@example.com"));

PaymentGatewayResult<PayoutResponse> result = client.createPayout(request);
```

## 退款

```java
RefundCreateRequest request = new RefundCreateRequest();
request.setTradeNo("pay_123");
request.setCurrency("USD");
request.setAmount(new BigDecimal("14.99"));
request.setRefundAmount(new BigDecimal("14.99"));
request.setRefundReason("Customer request");

PaymentGatewayResult<RefundResponse> result = client.createRefund(request);
```

## 查询与余额

```java
PaymentGatewayResult<PaymentResponse> payment = client.retrievePayment("pay_123");
PaymentGatewayResult<PayoutResponse> payout = client.retrievePayout("po_123");
PaymentGatewayResult<RefundResponse> refund = client.retrieveRefund("re_123");
PaymentGatewayResult<List<BalanceResponse>> balances = client.retrieveBalances("USD");
```

## 客户

```java
CustomerCreateRequest request = new CustomerCreateRequest();
request.setFirstname("Ada");
request.setLastname("Lovelace");
request.setEmail("ada@example.com");
request.setCountry("US");

PaymentGatewayResult<CustomerResponse> result = client.createCustomer(request);
```

## 当前协议说明

- 代收创建 `/pay-api/trade/payment`：按后端 `@VerificationAndProcessing` 走 Bearer JWT + `{"data":"compact密文"}`，JWT 使用商户 API 私钥做 HS256 签名。
- 代付、退款、余额、客户和 GET 查询：当前后端未统一标注新加密注解，SDK 按实际代码发送 `Authorization: Payment Base64(apiPrivateKey)` + 普通 JSON/GET，响应 `data` 如为密文字符串仍会自动解密。
- compact payload header 固定：`typ=PAYMENT-PAYLOAD`、`alg=RSA-OAEP-256`、`enc=A256GCM`，不输出 `kid`。

## 异常

SDK 根异常为 `PaymentGatewayException`。

| 异常 | 场景 |
|---|---|
| `PaymentGatewayConfigException` | 配置缺失、密钥为空、URL 非法 |
| `PaymentGatewayCryptoException` | RSA、AES-GCM、密钥解析、密文格式错误 |
| `PaymentGatewayHttpException` | 网络失败或 HTTP 非 2xx |
| `PaymentGatewayResponseException` | 响应 JSON 非法、响应 data 解密或反序列化失败 |
| `PaymentGatewayValidationException` | 请求参数基础校验失败 |

HTTP 2xx 且业务失败时，SDK 返回 `PaymentGatewayResult<T>`，商户应根据 `code`、`msg` 和 `isSuccess()` 处理。

## 日志

SDK 只依赖 `slf4j-api`。日志仅记录 event、method、path、脱敏商户号、requestId、statusCode、elapsedMillis、success、exceptionType，不记录完整 JWT、密钥、卡号、CVC、账户号、请求明文、响应明文或完整密文 data。

## FAQ

**为什么公开文档仍写旧签名？**  
后端当前是新旧协议并存：带 `@VerificationAndProcessing` 的接口使用 Bearer JWT + 加密 data；未带注解的接口继续使用 `Payment Base64(apiPrivateKey)`。

**为什么只有代收创建默认加密请求体？**  
后端当前只有代收创建方法标注了 `@VerificationAndProcessing`。其余第一版接口按现有后端普通请求形态发送，待后端统一注解后 SDK 可切换为加密 POST。

**是否可以在前端使用？**  
不可以。SDK 会持有 JWT 密钥、平台公钥和商户响应私钥，必须只运行在商户服务端。
