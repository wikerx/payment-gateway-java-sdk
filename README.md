# Payment Gateway Java SDK

商户服务端 Java SDK，用于对接 Payment Gateway OpenAPI。SDK 会按当前 `@VerificationAndProcessing` 注解加密协议生成 `Authorization: Bearer <jwt>`，按 `livemode` 选择沙盒或生产环境，对 POST 请求发送 `livemode + data` 加密外壳，并自动解密响应 `data`。

> 本 SDK 只能用于商户服务端。禁止放在浏览器、移动端 App、桌面客户端或任何会暴露 JWT 密钥、RSA 私钥、卡号、CVC 的环境。

## 要求

- Java 8
- Maven
- 默认 HTTP 传输使用 JDK 8 `HttpURLConnection`
- 不依赖 Spring Boot，不引入 OkHttp、Apache HttpClient、Gson 或 fastjson 1.x

## 安装

```xml
<dependency>
    <groupId>com.scott.payment</groupId>
    <artifactId>payment-gateway-java-sdk</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Java import 包名为 `com.scott.payment.sdk`，Maven 发布坐标为 `com.scott.payment:payment-gateway-java-sdk`。

常用 import 示例：

```java
import com.scott.payment.sdk.OpenApiClient;
import com.scott.payment.sdk.OpenApiResult;
import com.scott.payment.sdk.model.balance.BalanceResponse;
import com.scott.payment.sdk.model.common.CardPaymentMethodData;
import com.scott.payment.sdk.model.common.CustomerInfo;
import com.scott.payment.sdk.model.customer.CustomerCreateRequest;
import com.scott.payment.sdk.model.customer.CustomerResponse;
import com.scott.payment.sdk.model.payment.CardPaymentRequest;
import com.scott.payment.sdk.model.payment.CheckoutPaymentRequest;
import com.scott.payment.sdk.model.payment.PaymentResponse;
import com.scott.payment.sdk.model.payout.PayoutCancelRequest;
import com.scott.payment.sdk.model.payout.PayoutCancelResponse;
import com.scott.payment.sdk.model.payout.PayoutCreateRequest;
import com.scott.payment.sdk.model.payout.PayoutResponse;
import com.scott.payment.sdk.model.refund.RefundCreateRequest;
import com.scott.payment.sdk.model.refund.RefundResponse;
```

## 配置

将配置文件放到商户服务端 classpath，例如 `merchant-config.properties`。

```properties
payment.gateway.base-url=http://localhost:58060
payment.gateway.merchant-no=2606177036
payment.gateway.livemode=false
payment.gateway.api-private-key=<merchant-api-private-key>
payment.gateway.debug-raw-log-enabled=true
payment.gateway.platform-request-public-key-path=classpath:keys/2606177036_PLATFORM_REQUEST_PUBLIC_KEY.pem
payment.gateway.merchant-response-private-key-path=classpath:keys/2606177036_MERCHANT_RESPONSE_PRIVATE_KEY.pem
```

`livemode=false` 只请求沙盒 / 测试数据源，`livemode=true` 只请求生产数据源。SDK 不根据 API 私钥是否包含 `_test_` 推断环境。

`payment.gateway.debug-raw-log-enabled=true` 会打印完整请求地址、请求头、请求明文、请求密文、响应密文和响应明文，便于沙盒联调核验。开启后完整密文 `data`、客户资料、余额和交易状态等数据会进入应用日志；`Authorization` JWT、卡号、CVC、邮箱、手机号、证件号和密钥类字段仍会脱敏，生产环境建议关闭。

密钥也可以直接使用文本配置，文件配置优先于文本配置：

```properties
payment.gateway.platform-request-public-key=<x509-public-key-base64-or-pem>
payment.gateway.merchant-response-private-key=<pkcs8-private-key-base64-or-pem>
```

商户可以在 PEM 文件模式和文本密钥模式之间切换：

- 使用 PEM 文件：配置 `payment.gateway.platform-request-public-key-path` 和 `payment.gateway.merchant-response-private-key-path`；
- 使用文本密钥：注释掉上述 path 配置，改为配置 `payment.gateway.platform-request-public-key` 和 `payment.gateway.merchant-response-private-key`。

## 创建客户端

```java
OpenApiClient client = OpenApiClient.create();
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

OpenApiResult<PaymentResponse> result = client.createCheckoutPayment(request);
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

OpenApiResult<PaymentResponse> result = client.createCardPayment(request);
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

OpenApiResult<PayoutResponse> result = client.createPayout(request);
```

## 退款

```java
RefundCreateRequest request = new RefundCreateRequest();
request.setTradeNo("pay_123");
request.setCurrency("USD");
request.setAmount(new BigDecimal("14.99"));
request.setRefundAmount(new BigDecimal("14.99"));
request.setRefundReason("Customer request");

OpenApiResult<RefundResponse> result = client.createRefund(request);
```

## 查询与余额

```java
OpenApiResult<PaymentResponse> payment = client.retrievePayment("pay_123");
OpenApiResult<PayoutResponse> payout = client.retrievePayout("po_123");
OpenApiResult<RefundResponse> refund = client.retrieveRefund("re_123");
OpenApiResult<List<BalanceResponse>> balances = client.retrieveBalances("USD");
```

## 客户

```java
CustomerCreateRequest request = new CustomerCreateRequest();
request.setFirstname("Ada");
request.setLastname("Lovelace");
request.setEmail("ada@example.com");
request.setCountry("US");

OpenApiResult<CustomerResponse> result = client.createCustomer(request);
```

## 当前协议说明

- 已集成接口统一按后端 `@VerificationAndProcessing` 走 Bearer JWT。JWT 使用商户 API 私钥做 HS256 签名，并包含 `merchantId`、`livemode`、`jti`、`iat`、`exp`。
- POST 请求体格式为 `{"livemode":false,"data":"compact密文"}`；GET 请求无 body，但 JWT 中仍必须携带 `livemode`。
- 响应外层包含 `livemode`。SDK 会校验响应 `livemode` 与本地配置一致，不一致时抛出 `OpenApiResponseException`。
- SDK 当前封装了代收、退款、代付、余额、客户接口，并按接口提供独立 case，例如 `FundAccountsBalanceInquiryTest`。这些对外 API 均按当前 `@VerificationAndProcessing` 注解加密协议调用。
- compact payload header 固定：`typ=PAYMENT-PAYLOAD`、`alg=RSA-OAEP-256`、`enc=A256GCM`，不输出 `kid`。

## 异常

SDK 根异常为 `OpenApiException`。

| 异常 | 场景 |
|---|---|
| `OpenApiConfigException` | 配置缺失、密钥为空、URL 非法 |
| `OpenApiCryptoException` | RSA、AES-GCM、密钥解析、密文格式错误 |
| `OpenApiHttpException` | 网络失败或 HTTP 非 2xx |
| `OpenApiResponseException` | 响应 JSON 非法、响应 data 解密或反序列化失败 |
| `OpenApiValidationException` | 请求参数基础校验失败 |

HTTP 2xx 且业务失败时，SDK 返回 `OpenApiResult<T>`，商户应根据 `code`、`msg` 和 `isSuccess()` 处理。

## 日志

SDK 只依赖 `slf4j-api`。默认日志使用 `请求头: {}`、`API调用开始: {}`、`API调用结束: {}` 等标准 key-value + JSON 格式。默认日志只输出摘要和脱敏 Header，完整 JWT、密钥、卡号、CVC、邮箱、手机号和证件号不会进入普通日志。

如需核验实际传输数据，可在 `merchant-config.properties` 中设置：

```properties
payment.gateway.debug-raw-log-enabled=true
```

开启后会额外打印：

- `请求地址: {}`：SDK 实际请求的平台 URL；
- `请求头: {}`：SDK 实际请求 Header，`Authorization` 会脱敏；
- `请求原始明文报文: {}`：加密前业务请求 DTO，按字段输出对象结构并脱敏敏感字段；
- `请求参数拆分: {}`：请求 `data` 的 compact payload 拆分字段，包含 `protectedHeader`、`header`、`encryptedAesKey`、`iv`、`cipherText`、`tag`；
- `请求密文参数: {}`：SDK 真实发送给平台的请求 body，即 `OpenApiEncryptedRequest`；
- `响应原始密文参数: {}`：平台返回的原始响应状态、响应 Header 和 `OpenApiEncryptedResponse`；
- `响应原始明文参数: {}`：SDK 解密并转换后的业务响应 DTO，按字段输出对象结构并脱敏敏感字段；
- `响应参数拆分: {}`：响应 `data` 的 compact payload 拆分字段，包含 `protectedHeader`、`header`、`encryptedAesKey`、`iv`、`cipherText`、`tag`。

报文日志会过滤 null 字段，避免商户联调时看到大量无效参数。`requestId` 是 SDK 链路追踪字段，只出现在 `X-Request-Id` Header、`API调用开始` 和 `API调用结束` 日志中，不会放入请求或响应报文日志。

该开关只建议用于沙盒联调或本地排查。开启后会打印请求明文、响应明文和完整密文 data；Authorization、卡号、CVC、邮箱、手机号、证件号和密钥类字段仍会脱敏，不建议在生产环境或包含真实持卡人数据的环境开启。

### 手动拆分密文参数

如果商户需要在文档或本地 case 中单独查看 `encryptedAesKey`、`iv`、`cipherText`、`tag`，可以直接使用 SDK 的拆分方法：

```java
OpenApiPayloadCrypto crypto = new OpenApiPayloadCrypto();
OpenApiPayloadParts parts = crypto.splitCompactPayload(encryptedRequest.getData());

String encryptedAesKey = parts.getEncryptedAesKey();
String iv = parts.getIv();
String cipherText = parts.getCipherText();
String tag = parts.getTag();
```

如果只是想演示“明文如何加密并拆分”，可以使用：

```java
OpenApiPayloadParts parts = crypto.encryptToParts(plainJson, platformRequestPublicKey);
String realRequestData = parts.toCompactPayload();
```

参考用例：`OpenApiPayloadPartsReferenceTest`，专门演示 `protectedHeader`、`encryptedAesKey`、`iv`、`cipherText`、`tag` 拆分；`OpenApiPayloadCryptoReferenceTest` 包含请求加密、密文参数拆分和响应解密三个综合 case。

### 签名算法与 Apifox 文档

商户签名、请求头、POST 加密请求体、GET 请求头、响应解密的可复制参考代码见：

- `OpenApiSignatureReferenceTest`：JWT 签名、Authorization、POST/GET 请求头和 POST 加密请求体示例；
- `OpenApiPayloadPartsReferenceTest`：compact payload 五段字段拆分示例；
- `OpenApiPayloadCryptoReferenceTest`：请求加密、compact payload 拆分和响应解密示例；
- `openapi-signature-encryption.md`：可导入 Apifox 的 Markdown 文档。

## FAQ

**SDK 使用哪种鉴权协议？**  
当前 SDK 对外 API 统一使用 `Authorization: Bearer {jwt}`，POST 请求统一发送 `livemode + data` 加密外壳，GET 请求没有请求体但响应 `data` 仍会解密为业务 DTO。

**为什么 GET 查询没有请求体 data？**  
GET 查询通过 Bearer JWT 携带 `merchantId + livemode` 完成鉴权和数据源路由，响应 `data` 仍按商户响应公钥加密。

**是否可以在前端使用？**  
不可以。SDK 会持有 JWT 密钥、平台公钥和商户响应私钥，必须只运行在商户服务端。
