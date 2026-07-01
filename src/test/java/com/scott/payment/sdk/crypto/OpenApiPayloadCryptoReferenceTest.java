package com.scott.payment.sdk.crypto;

import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.common.OpenApiEncryptedRequest;
import com.scott.payment.sdk.model.common.OpenApiEncryptedResponse;
import com.scott.payment.sdk.model.common.OpenApiPayloadParts;
import com.scott.payment.sdk.model.common.PaymentMethod;
import com.scott.payment.sdk.model.payment.PaymentCreateRequest;
import com.scott.payment.sdk.testkit.OpenApiTestSupport;
import com.scott.payment.sdk.util.OrderNoGenerator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiPayloadCryptoReferenceTest
 * @date : 2026-07-01 14:12
 * @email : scott_x@163.com
 * @description : OpenAPI 报文加密参考用例，负责演示商户如何使用 SDK 方法完成请求加密、compact payload 拆分和响应解密。
 *                本测试只构造本地示例数据，不发起真实 HTTP 请求，不创建支付订单，不修改支付、代付、退款、客户、资金、密钥或配置状态。
 *                示例日志用于商户沙盒联调和文档引用，卡号、CVC 等敏感明文字段必须脱敏输出，完整密文字段仅建议在受控排查场景短期开启。
 * @status : create
 */
@Slf4j
class OpenApiPayloadCryptoReferenceTest {

    /**
     * 演示商户如何使用 SDK 方法生成 POST 请求体。
     *
     * case 目的：展示业务请求 DTO 如何序列化并加密成网关 POST 请求体中的 livemode + data。
     * 关键输入：2606177036 沙盒商户配置、平台请求公钥、支付创建示例参数。
     * 结果摘要：输出脱敏后的请求明文摘要和真实密文请求体；不调用网关接口，不创建支付订单，不修改资金、密钥或配置。
     */
    @Test
    void shouldEncryptPostRequestBodyWithSdkMethod() {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        OpenApiPayloadCrypto crypto = new OpenApiPayloadCrypto();
        PaymentCreateRequest plainRequest = paymentCreateRequest();
        String plainJson = JsonSupport.toJson(plainRequest);

        OpenApiPayloadParts requestParts = crypto.encryptToParts(plainJson,
                RsaKeyUtils.readPublicKey(config.getPlatformPublicKey()));
        OpenApiEncryptedRequest encryptedRequest = OpenApiEncryptedRequest.builder()
                .livemode(config.getLivemode())
                .data(requestParts.toCompactPayload())
                .build();

        log.info("商户参考用例-请求原始明文报文: {}", JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(plainRequest)));
        log.info("商户参考用例-请求密文参数: {}", JsonSupport.toLogJson(encryptedRequest));
        log.debug("商户参考用例-请求参数拆分: {}", JsonSupport.toLogJson(requestParts));

        String encryptedAesKey = requestParts.getEncryptedAesKey();
        String iv = requestParts.getIv();
        String cipherText = requestParts.getCipherText();
        String tag = requestParts.getTag();
        log.info("encryptedAesKey：{}" , encryptedAesKey);
        log.info("iv：{}" , iv);
        log.info("cipherText：{}" , cipherText);
        log.info("tag：{}" , tag);

        assertThat(encryptedRequest.getLivemode()).isEqualTo(config.getLivemode());
        assertThat(encryptedRequest.getData()).isEqualTo(requestParts.toCompactPayload());
        assertThat(encryptedRequest.getData().split("\\.", -1)).hasSize(5);
    }

    /**
     * 演示商户如何使用 SDK 方法拆分请求或响应中的 data。
     *
     * case 目的：展示如何从 compact payload 中读取 protectedHeader、header、encryptedAesKey、iv、cipherText 和 tag。
     * 关键输入：本地生成的 OpenAPI 密文请求体。
     * 结果摘要：输出密文结构拆分字段；不解密业务明文，不发起外部渠道调用，不修改交易、资金、密钥或配置状态。
     */
    @Test
    void shouldSplitCompactPayloadWithSdkMethod() {
        OpenApiEncryptedRequest encryptedRequest = encryptedPaymentCreateRequest();
        OpenApiPayloadCrypto crypto = new OpenApiPayloadCrypto();

        OpenApiPayloadParts splitParts = crypto.splitCompactPayload(encryptedRequest.getData());

        log.debug("商户参考用例-请求参数拆分: {}", JsonSupport.toLogJson(splitParts));

        assertThat(splitParts.getHeader()).contains("\"typ\":\"PAYMENT-PAYLOAD\"")
                .contains("\"alg\":\"RSA-OAEP-256\"")
                .contains("\"enc\":\"A256GCM\"");
        assertThat(splitParts.getEncryptedAesKey()).isNotBlank();
        assertThat(splitParts.getIv()).isNotBlank();
        assertThat(splitParts.getCipherText()).isNotBlank();
        assertThat(splitParts.getTag()).isNotBlank();
        assertThat(splitParts.toCompactPayload()).isEqualTo(encryptedRequest.getData());
    }

    /**
     * 演示商户如何使用 SDK 方法解密平台返回的响应 data。
     *
     * case 目的：展示商户侧如何用商户响应私钥解密网关响应 data。
     * 关键输入：本地模拟的 OpenApiEncryptedResponse、商户响应私钥、响应密文 data。
     * 结果摘要：输出响应密文外壳、响应密文拆分字段和响应明文摘要；不调用网关接口，不修改支付、代付、退款、资金、密钥或配置状态。
     */
    @Test
    void shouldDecryptEncryptedResponseWithSdkMethod() throws Exception {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        OpenApiPayloadCrypto crypto = new OpenApiPayloadCrypto();
        String responsePlainJson = JsonSupport.toJson(Collections.singletonMap("tradeNo", "pay_123"));
        String responseData = crypto.encrypt(responsePlainJson, merchantResponsePublicKey(config));
        OpenApiEncryptedResponse encryptedResponse = OpenApiEncryptedResponse.builder()
                .code(0)
                .msg("")
                .livemode(config.getLivemode())
                .data(responseData)
                .build();

        OpenApiPayloadParts responseParts = crypto.splitCompactPayload(encryptedResponse.getData());
        String decryptedJson = crypto.decrypt(encryptedResponse.getData(), RsaKeyUtils.readPrivateKey(config.getMerchantResponsePrivateKey()));

        log.info("商户参考用例-响应原始密文参数: {}", JsonSupport.toLogJson(encryptedResponse));
        log.info("商户参考用例-响应参数拆分: {}", JsonSupport.toLogJson(responseParts));
        log.info("商户参考用例-响应原始明文参数: {}", JsonSupport.toLogJson(JsonSupport.fromJson(decryptedJson, Map.class)));

        assertThat(responseParts.getEncryptedAesKey()).isNotBlank();
        assertThat(responseParts.getIv()).isNotBlank();
        assertThat(responseParts.getCipherText()).isNotBlank();
        assertThat(responseParts.getTag()).isNotBlank();
        assertThat(decryptedJson).contains("\"tradeNo\":\"pay_123\"");
    }

    private OpenApiEncryptedRequest encryptedPaymentCreateRequest() {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        OpenApiPayloadCrypto crypto = new OpenApiPayloadCrypto();
        String plainJson = JsonSupport.toJson(paymentCreateRequest());
        OpenApiPayloadParts requestParts = crypto.encryptToParts(plainJson,
                RsaKeyUtils.readPublicKey(config.getPlatformPublicKey()));
        return OpenApiEncryptedRequest.builder()
                .livemode(config.getLivemode())
                .data(requestParts.toCompactPayload())
                .build();
    }

    private PublicKey merchantResponsePublicKey(OpenApiClientConfig config) throws Exception {
        PrivateKey privateKey = RsaKeyUtils.readPrivateKey(config.getMerchantResponsePrivateKey());
        RSAPrivateCrtKey rsaPrivateKey = (RSAPrivateCrtKey) privateKey;
        RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(rsaPrivateKey.getModulus(), rsaPrivateKey.getPublicExponent());
        return KeyFactory.getInstance("RSA").generatePublic(publicKeySpec);
    }

    private PaymentCreateRequest paymentCreateRequest() {
        Map<String, Object> card = new LinkedHashMap<String, Object>();
        card.put("number", "4242424242424242");
        card.put("expMonth", "06");
        card.put("expYear", "2026");
        card.put("cvc", "123");

        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setOrderNo(OrderNoGenerator.generate("PAY"));
        request.setCurrency("USD");
        request.setAmount(OpenApiTestSupport.amount("12.34"));
        request.setClientIp("47.125.221.223");
        request.setWebsite("https://manage.forgottenthrone.com/");
        request.setPaymentMethod(PaymentMethod.CARD);
        request.setPaymentMethodData(card);
        return request;
    }
}
