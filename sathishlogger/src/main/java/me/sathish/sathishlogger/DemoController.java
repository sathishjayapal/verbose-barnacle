package me.sathish.sathishlogger;

import org.slf4j.Logger;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")

public class DemoController {

    private final DemoService demoService;
static final Logger log = org.slf4j.LoggerFactory.getLogger(DemoController.class);
    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "world") String name) {
        log.error("Hello World");
        return demoService.greet(name);
    }

    @GetMapping("/compute/{n}")
    public long compute(@PathVariable int n) {
        return demoService.fib(n);
    }
}
