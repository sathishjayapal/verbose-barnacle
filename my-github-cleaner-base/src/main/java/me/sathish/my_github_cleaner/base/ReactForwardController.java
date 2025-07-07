package me.sathish.my_github_cleaner.base;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Serve Reacts index.html for all requests that are not relevant for the backend.
 */
@Controller
public class ReactForwardController {

    @GetMapping("{path:^(?!api|public|css|js|images)[^\\.]*}/**")
    public String handleForward() {
        return "forward:/";
    }
}
