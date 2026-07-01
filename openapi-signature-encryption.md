# 签名算法与报文加密、解密算法

本文档面向商户服务端接入 OpenAPI。示例代码优先使用 SDK 已封装方法，商户可直接参考 SDK 测试用例复制代码。

对应 SDK 参考用例：

- `src/test/java/com/scott/payment/sdk/jwt/OpenApiSignatureReferenceTest.java`
- `src/test/java/com/scott/payment/sdk/crypto/OpenApiPayloadCryptoReferenceTest.java`
- `src/test/java/com/scott/payment/sdk/jwt/MerchantJwtSignerTest.java`

> 注意：SDK 必须运行在商户服务端，不允许放在浏览器、移动端 App 或任何会暴露 API 私钥、商户响应私钥、卡号、CVC 的环境。

---

## 1. 商户配置

SDK 默认读取 classpath 下的 `merchant-config.properties`。当前 SDK 已内置 2606177036 沙盒商户示例配置。

```properties
payment.gateway.base-url=http://localhost:58060
payment.gateway.merchant-no=2606177036
payment.gateway.livemode=false
payment.gateway.api-private-key=pi_test_IiLeEu803nK1p8nt8KY9ENPmWrnLKuwKV4MyrGoYtjr78O6317yWhl4CnELIf1tFse53fhErDCthW7ecoi5XlFOoAd0yxdf1fvo
payment.gateway.debug-raw-log-enabled=true
payment.gateway.platform-request-public-key-path=classpath:keys/2606177036_PLATFORM_REQUEST_PUBLIC_KEY.pem
payment.gateway.merchant-response-private-key-path=classpath:keys/2606177036_MERCHANT_RESPONSE_PRIVATE_KEY.pem
```

加载配置示例：

```java
OpenApiClientConfig config = MerchantConfigLoader.load();
```

配置字段说明：

| 字段 | 说明 |
| --- | --- |
| `payment.gateway.base-url` | 网关地址 |
| `payment.gateway.merchant-no` | 商户号，会写入 JWT `merchantId` |
| `payment.gateway.livemode` | 环境标识，`false` 测试环境，`true` 生产环境 |
| `payment.gateway.api-private-key` | API 私钥，用于 HS256 JWT 签名 |
| `payment.gateway.platform-request-public-key-path` | 平台请求公钥，用于商户加密请求 `data` |
| `payment.gateway.merchant-response-private-key-path` | 商户响应私钥，用于商户解密平台响应 `data` |

---

## 2. JWT 签名算法

平台接口使用 `Authorization: Bearer {jwt}` 进行身份认证。JWT 使用商户 API 私钥按 HS256 签名。

JWT Header 固定为：

```json
{
  "typ": "JWT",
  "alg": "HS256"
}
```

JWT Payload 示例：

```json
{
  "iss": "merchant",
  "aud": ["gateway"],
  "merchantId": "2606177036",
  "livemode": false,
  "jti": "payment-550e8400-e29b-41d4-a716-446655440000",
  "iat": 1782874330,
  "exp": 1782874510
}
```

字段说明：

| 字段 | 是否必填 | 类型 | 说明 |
| --- | --- | --- | --- |
| `iss` | Y | String | 固定为 `merchant` |
| `aud` | Y | String / Array | 必须包含 `gateway` |
| `merchantId` | Y | String | 商户号 |
| `livemode` | Y | Boolean | `false` 测试环境，`true` 生产环境 |
| `jti` | Y | String | 每次请求唯一，用于防重放 |
| `iat` | Y | Long | Unix 秒级签发时间 |
| `exp` | Y | Long | Unix 秒级过期时间 |

### 2.1 使用 SDK 生成 JWT

对应 SDK 用例：`OpenApiSignatureReferenceTest.shouldGenerateAuthorizationHeaderWithMerchantConfig`

```java
OpenApiClientConfig config = MerchantConfigLoader.load();
String jwtId = "payment-" + UUID.randomUUID().toString();
Date issuedAt = Date.from(Instant.now());

String token = new MerchantJwtSigner().sign(
        config.getMerchantId(),
        config.getMerchantJwtSecret(),
        config.getLivemode(),
        jwtId,
        issuedAt,
        config.getJwtTtlSeconds());

String authorization = OpenApiConstants.AUTHORIZATION_PREFIX + token;
```

### 2.2 解析 JWT Claims 用于本地核验

> 该步骤只用于本地核验。生产日志不得打印完整 JWT 或 API 私钥。

```java
Claims claims = Jwts.parserBuilder()
        .setSigningKey(config.getMerchantJwtSecret().getBytes(StandardCharsets.UTF_8))
        .build()
        .parseClaimsJws(token)
        .getBody();
```

建议日志：

```java
log.info("签名算法示例-JWT Claims摘要: {}", JsonSupport.toLogJson(logFields(
        "issuer", claims.getIssuer(),
        "audience", claims.get("aud", List.class),
        "merchantId", claims.get("merchantId", String.class),
        "livemode", claims.get("livemode", Boolean.class),
        "jti", claims.getId(),
        "iat", claims.getIssuedAt().getTime() / 1000L,
        "exp", claims.getExpiration().getTime() / 1000L)));
```

---

## 3. 请求 Header 生成

### 3.1 POST 请求 Header

POST 请求有 JSON 请求体，需要 `Content-Type`。

```java
Map<String, String> headers = new LinkedHashMap<String, String>();
headers.put(OpenApiConstants.HEADER_AUTHORIZATION, authorization);
headers.put(OpenApiConstants.HEADER_ACCEPT, OpenApiConstants.ACCEPT);
headers.put(OpenApiConstants.HEADER_USER_AGENT, OpenApiConstants.USER_AGENT);
headers.put(OpenApiConstants.HEADER_REQUEST_ID, UUID.randomUUID().toString());
headers.put(OpenApiConstants.HEADER_CONTENT_TYPE, OpenApiConstants.CONTENT_TYPE);
```

日志输出必须脱敏 `Authorization`：

```java
log.info("签名算法示例-POST请求头: {}",
        JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeHeaders(headers)));
```

### 3.2 GET 请求 Header

GET 请求没有请求体，不需要 `Content-Type`，但仍必须携带 Bearer JWT。

对应 SDK 用例：`OpenApiSignatureReferenceTest.shouldBuildGetRequestHeadersWithoutBodyForApifox`

```java
Map<String, String> headers = new LinkedHashMap<String, String>();
headers.put(OpenApiConstants.HEADER_AUTHORIZATION, authorization);
headers.put(OpenApiConstants.HEADER_ACCEPT, OpenApiConstants.ACCEPT);
headers.put(OpenApiConstants.HEADER_USER_AGENT, OpenApiConstants.USER_AGENT);
headers.put(OpenApiConstants.HEADER_REQUEST_ID, UUID.randomUUID().toString());
```

---

## 4. 报文加密算法

平台接口业务报文使用混合加密：

```text
RSA-OAEP-256 + A256GCM
```

compact payload 格式：

```text
protectedHeader.encryptedAesKey.iv.cipherText.tag
```

五段说明：

| 序号 | 字段 | 说明 |
| --- | --- | --- |
| 1 | `protectedHeader` | Base64URL 编码后的加密头 |
| 2 | `encryptedAesKey` | RSA-OAEP-SHA256 加密后的 AES-256 会话密钥 |
| 3 | `iv` | AES-GCM 12 字节随机 IV |
| 4 | `cipherText` | AES-GCM 加密后的业务密文 |
| 5 | `tag` | AES-GCM 16 字节认证标签 |

protected header 原文固定为：

```json
{
  "typ": "PAYMENT-PAYLOAD",
  "alg": "RSA-OAEP-256",
  "enc": "A256GCM"
}
```

> SDK 内部使用显式 `OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT)`，避免不同 JCE Provider 下 MGF1 摘要不一致。

---

## 5. POST 请求体加密示例

对应 SDK 用例：`OpenApiSignatureReferenceTest.shouldBuildPostRequestHeadersAndEncryptedBodyForApifox`

### 5.1 准备业务请求参数

```java
import com.scott.payment.sdk.util.OrderNoGenerator;
import com.scott.payment.sdk.model.common.PaymentMethod;

Map<String, Object> card = new LinkedHashMap<String, Object>();
card.put("number", "4242424242424242");
card.put("expMonth", "06");
card.put("expYear", "2026");
card.put("cvc", "123");

PaymentCreateRequest request = new PaymentCreateRequest();
request.setOrderNo(OrderNoGenerator.generate("PAY"));
request.setCurrency("USD");
request.setAmount(new BigDecimal("12.34"));
request.setClientIp("47.125.221.223");
request.setWebsite("https://manage.forgottenthrone.com/");
request.setPaymentMethod(PaymentMethod.CARD);
request.setPaymentMethodData(card);
```

### 5.2 序列化明文 JSON

```java
String plainJson = JsonSupport.toJson(request);
```

### 5.3 使用平台请求公钥加密

```java
OpenApiClientConfig config = MerchantConfigLoader.load();
OpenApiPayloadParts payloadParts = new OpenApiPayloadCrypto().encryptToParts(
        plainJson,
        RsaKeyUtils.readPublicKey(config.getPlatformPublicKey()));
```

### 5.4 生成真实 POST 请求体

```java
OpenApiEncryptedRequest encryptedRequest = OpenApiEncryptedRequest.builder()
        .livemode(config.getLivemode())
        .data(payloadParts.toCompactPayload())
        .build();
```

请求体格式：

```json
{
  "livemode": false,
  "data": "{compactPayload}"
}
```

### 5.5 拆分密文参数

```java
OpenApiPayloadParts splitParts = new OpenApiPayloadCrypto()
        .splitCompactPayload(encryptedRequest.getData());

String encryptedAesKey = splitParts.getEncryptedAesKey();
String iv = splitParts.getIv();
String cipherText = splitParts.getCipherText();
String tag = splitParts.getTag();
```

标准日志：

```java
log.info("签名算法示例-POST请求原始明文报文: {}",
        JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(request)));
log.info("签名算法示例-POST请求密文参数: {}",
        JsonSupport.toLogJson(encryptedRequest));
log.debug("签名算法示例-POST请求参数拆分: {}",
        JsonSupport.toLogJson(payloadParts));
```

---

## 6. 响应解密示例

对应 SDK 用例：`OpenApiPayloadCryptoReferenceTest.shouldDecryptEncryptedResponseWithSdkMethod`

平台响应外层：

```json
{
  "code": 0,
  "msg": "",
  "livemode": false,
  "data": "{compactPayload}"
}
```

使用商户响应私钥解密：

```java
OpenApiClientConfig config = MerchantConfigLoader.load();
OpenApiPayloadCrypto crypto = new OpenApiPayloadCrypto();

OpenApiPayloadParts responseParts = crypto.splitCompactPayload(encryptedResponse.getData());
String decryptedJson = crypto.decrypt(
        encryptedResponse.getData(),
        RsaKeyUtils.readPrivateKey(config.getMerchantResponsePrivateKey()));
```

标准日志：

```java
log.info("商户参考用例-响应原始密文参数: {}",
        JsonSupport.toLogJson(encryptedResponse));
log.info("商户参考用例-响应参数拆分: {}",
        JsonSupport.toLogJson(responseParts));
log.info("商户参考用例-响应原始明文参数: {}",
        JsonSupport.toLogJson(JsonSupport.fromJson(decryptedJson, Map.class)));
```

---

## 7. 最终请求示例

### 7.1 POST

```http
POST /pay-api/trade/payment HTTP/1.1
Host: localhost:58060
Authorization: Bearer {jwt}
Content-Type: application/json; charset=UTF-8
Accept: application/json
User-Agent: payment-gateway-java-sdk/0.1.0-SNAPSHOT java/1.8
X-Request-Id: 83196038-3d30-4b23-93fc-5997fa769455
```

请求体：

```json
{
  "livemode": false,
  "data": "{compactPayload}"
}
```

### 7.2 GET

```http
GET /pay-api/trade/payment/pay_123 HTTP/1.1
Host: localhost:58060
Authorization: Bearer {jwt}
Accept: application/json
User-Agent: payment-gateway-java-sdk/0.1.0-SNAPSHOT java/1.8
X-Request-Id: 83196038-3d30-4b23-93fc-5997fa769455
```

GET 请求没有请求体，不需要 `Content-Type`。JWT Payload 中仍必须包含 `merchantId` 和 `livemode`。

---

## 8. 本地运行参考用例

```bash
mvn -q -Dtest=OpenApiSignatureReferenceTest,OpenApiPayloadCryptoReferenceTest,MerchantJwtSignerTest test
```

运行后可查看日志：

- `签名算法示例-Authorization请求头`
- `签名算法示例-JWT Claims摘要`
- `签名算法示例-POST请求地址`
- `签名算法示例-POST请求头`
- `签名算法示例-POST请求原始明文报文`
- `签名算法示例-POST请求密文参数`
- `签名算法示例-POST请求参数拆分`
- `签名算法示例-GET请求地址`
- `签名算法示例-GET请求头`
- `商户参考用例-响应原始密文参数`
- `商户参考用例-响应参数拆分`
- `商户参考用例-响应原始明文参数`

---

## 9. 注意事项

1. API 私钥只用于 HS256 JWT 签名，不参与业务报文加密。
2. 平台请求公钥只用于商户加密请求 `data`。
3. 商户响应私钥只用于商户解密平台响应 `data`。
4. JWT Header 和 Payload 只是 Base64URL 编码，不是加密，不要放入卡号、证件号、手机号、邮箱、密钥等敏感信息。
5. `Bearer` 与 JWT 之间有且仅有一个英文空格。
6. `jti` 每次请求必须唯一。
7. `exp - iat` 不应超过 180 秒。
8. POST 请求体中的 `livemode` 必须与 JWT Payload 中的 `livemode` 一致。
9. GET 请求无 body，但仍需 Bearer JWT。
10. 生产日志不得打印完整 JWT、API 私钥、商户响应私钥、卡号或 CVC。
