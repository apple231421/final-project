package THE_JEONG.Hospital.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@RequestMapping("/policy")
public class PolicyController {

    @GetMapping("/privacy-policy")
    public String privacyPolicyPage() {
        return "/policy/privacy-policy";
    }

    @GetMapping("/terms-of-service")
    public String termsOfServicePage() {
        return "/policy/terms-of-service";
    }

    @GetMapping("/related-sites")
    public String relatedSitesPage() {
        return "/policy/related-sites";
    }
}