package com.scott.payment.sdk.crypto;

import com.scott.payment.sdk.OpenApiClientConfig;
import com.scott.payment.sdk.json.JsonSupport;
import com.scott.payment.sdk.logging.OpenApiLogSanitizer;
import com.scott.payment.sdk.model.common.OpenApiEncryptedRequest;
import com.scott.payment.sdk.model.common.OpenApiPayloadParts;
import com.scott.payment.sdk.model.payment.PaymentCreateRequest;
import com.scott.payment.sdk.testkit.OpenApiTestSupport;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author : scott
 * @version : v1.0.0
 * @classname : OpenApiPayloadPartsReferenceTest
 * @date : 2026-07-01 19:42
 * @email : scott_x@163.com
 * @description : OpenAPI compact payload 拆分字段参考用例，负责演示商户如何从 SDK 加密结果中读取 protectedHeader、header、encryptedAesKey、iv、cipherText 和 tag。
 *                本测试只构造本地示例数据，不发起真实 HTTP 请求，不创建支付订单，不修改支付、代付、退款、客户、资金、密钥或配置状态。
 *                示例日志用于商户沙盒联调和 Apifox 文档引用，卡号、CVC 等敏感明文字段必须脱敏输出，完整密文字段仅建议在受控排查场景短期开启。
 * @status : create
 */
@Slf4j
class OpenApiPayloadPartsReferenceTest {

    /**
     * 演示商户如何加密业务请求并读取 compact payload 五段参数。
     *
     * case 目的：展示 `data` 如何拆分为 protectedHeader、encryptedAesKey、iv、cipherText 和 tag，便于商户编写文档或排查加密参数。
     * 关键输入：2606177036 沙盒商户配置、平台请求公钥、支付创建示例参数。
     * 结果摘要：输出请求原始明文报文、请求密文参数和请求参数拆分；不调用网关接口，不修改资金、密钥或配置。
     */
    @Test
    void shouldEncryptRequestAndReadPayloadParts() {
        OpenApiClientConfig config = OpenApiTestSupport.clientConfig();
        OpenApiPayloadCrypto crypto = new OpenApiPayloadCrypto();
        PaymentCreateRequest plainRequest = paymentCreateRequest();
        String plainJson = JsonSupport.toJson(plainRequest);

        OpenApiPayloadParts requestParts = crypto.encryptToParts(
                plainJson,
                RsaKeyUtils.readPublicKey(config.getPlatformPublicKey()));
        OpenApiEncryptedRequest encryptedRequest = OpenApiEncryptedRequest.builder()
                .livemode(config.getLivemode())
                .data(requestParts.toCompactPayload())
                .build();
        OpenApiPayloadParts splitParts = crypto.splitCompactPayload(encryptedRequest.getData());

        log.info("商户参考用例-请求原始明文报文: {}", JsonSupport.toLogJson(OpenApiLogSanitizer.sanitizeObject(plainRequest)));
        log.info("商户参考用例-请求密文参数: {}", JsonSupport.toLogJson(encryptedRequest));
        log.info("商户参考用例-请求参数拆分: {}", JsonSupport.toLogJson(splitParts));

        assertThat(splitParts.getHeader()).contains("\"typ\":\"PAYMENT-PAYLOAD\"")
                .contains("\"alg\":\"RSA-OAEP-256\"")
                .contains("\"enc\":\"A256GCM\"");
        assertThat(splitParts.getEncryptedAesKey()).isNotBlank();
        assertThat(splitParts.getIv()).isNotBlank();
        assertThat(splitParts.getCipherText()).isNotBlank();
        assertThat(splitParts.getTag()).isNotBlank();
        assertThat(splitParts.toCompactPayload()).isEqualTo(encryptedRequest.getData());
    }

    private PaymentCreateRequest paymentCreateRequest() {
        Map<String, Object> card = new LinkedHashMap<String, Object>();
        card.put("number", "4242424242424242");
        card.put("expMonth", "06");
        card.put("expYear", "2026");
        card.put("cvc", "123");

        PaymentCreateRequest request = new PaymentCreateRequest();
        request.setOrderNo("ORDER-PAYLOAD-PARTS");
        request.setCurrency("USD");
        request.setAmount(OpenApiTestSupport.amount("12.34"));
        request.setClientIp("47.125.221.223");
        request.setWebsite("https://manage.forgottenthrone.com/");
        request.setPaymentMethod("CARD");
        request.setPaymentMethodData(card);
        return request;
    }
}
