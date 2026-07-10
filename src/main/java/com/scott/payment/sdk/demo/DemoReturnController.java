package com.scott.payment.sdk.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles the frontend return URL used by checkout and local payment demos.
 */
@Controller
public class DemoReturnController {

    /**
     * Displays all query parameters returned by the gateway after the payer leaves the payment page.
     *
     * This endpoint is only a demo landing page. Merchants should still confirm the final payment result
     * through payin webhook notifications or the payment retrieve API before updating funds or orders.
     *
     * @param servletRequest raw HTTP request containing gateway return parameters
     * @param model Thymeleaf model
     * @return return result page
     */
    @GetMapping("/demo/return")
    public String payinReturn(HttpServletRequest servletRequest, Model model) {
        Map<String, String> params = firstValueParams(servletRequest);
        model.addAttribute("params", params);
        model.addAttribute("returnUrl", DemoLocalUrls.PAYIN_RETURN_URL);
        model.addAttribute("tradeNo", value(params, "tradeNo"));
        model.addAttribute("orderNo", value(params, "orderNo"));
        model.addAttribute("status", value(params, "status"));
        model.addAttribute("code", value(params, "code"));
        model.addAttribute("message", value(params, "message"));
        return "demo/return";
    }

    private Map<String, String> firstValueParams(HttpServletRequest servletRequest) {
        Map<String, String> params = new LinkedHashMap<String, String>();
        for (Map.Entry<String, String[]> entry : servletRequest.getParameterMap().entrySet()) {
            String[] values = entry.getValue();
            params.put(entry.getKey(), values == null || values.length == 0 ? "" : values[0]);
        }
        return params;
    }

    private String value(Map<String, String> params, String name) {
        String value = params.get(name);
        return value == null ? "" : value;
    }
}
