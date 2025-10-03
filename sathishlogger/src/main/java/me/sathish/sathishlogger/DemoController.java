package me.sathish.sathishlogger;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class DemoController {

    private final DemoService demoService;

    public DemoController(DemoService demoService) {
        this.demoService = demoService;
    }

    @GetMapping("/hello")
    public String hello(@RequestParam(defaultValue = "world") String name) {
        return demoService.greet(name);
    }

    @GetMapping("/compute/{n}")
    public long compute(@PathVariable int n) {
        return demoService.fib(n);
    }
}
