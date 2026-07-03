package com.scott.payment.sdk.demo;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Thymeleaf demo console for merchant OpenAPI integration.
 */
@Controller
public class DemoApiController {

    private final DemoApiService demoApiService;
    private final DemoApiFormFactory formFactory;

    public DemoApiController(DemoApiService demoApiService) {
        this.demoApiService = demoApiService;
        this.formFactory = new DemoApiFormFactory(demoApiService.getConfig());
    }

    @GetMapping("/demo")
    public String demoRoot() {
        return "redirect:/demo/apis";
    }

    @GetMapping("/demo/apis")
    public String index(Model model) {
        model.addAttribute("groups", DemoApiCatalog.groups());
        model.addAttribute("merchantNo", demoApiService.getConfig().getMerchantId());
        model.addAttribute("baseUrl", demoApiService.getConfig().getBaseUrl());
        model.addAttribute("livemode", demoApiService.getConfig().getLivemode());
        return "demo/index";
    }

    @GetMapping("/demo/apis/{apiCode}")
    public String api(@PathVariable String apiCode, Model model) {
        DemoApiDefinition definition = DemoApiCatalog.getRequired(apiCode);
        populateApiModel(model, definition, formFactory.create(definition), null);
        return "demo/api";
    }

    @PostMapping("/demo/apis/{apiCode}")
    public String invoke(@PathVariable String apiCode,
                         @ModelAttribute("form") DemoApiForm form,
                         Model model) {
        DemoApiDefinition definition = DemoApiCatalog.getRequired(apiCode);
        DemoApiInvocation invocation = demoApiService.invoke(definition, form);
        populateApiModel(model, definition, form, invocation);
        return "demo/api";
    }

    private void populateApiModel(Model model,
                                  DemoApiDefinition definition,
                                  DemoApiForm form,
                                  DemoApiInvocation invocation) {
        model.addAttribute("definition", definition);
        model.addAttribute("form", form);
        model.addAttribute("invocation", invocation);
        model.addAttribute("groups", DemoApiCatalog.groups());
        model.addAttribute("merchantNo", demoApiService.getConfig().getMerchantId());
        model.addAttribute("baseUrl", demoApiService.getConfig().getBaseUrl());
        model.addAttribute("livemode", demoApiService.getConfig().getLivemode());
    }
}
