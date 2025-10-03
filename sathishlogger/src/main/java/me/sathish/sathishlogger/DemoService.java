package me.sathish.sathishlogger;

import org.springframework.stereotype.Service;

@Service
public class DemoService {

    public String greet(String name) {
        return "Hello, " + name + "!";
    }

    public long fib(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0");
        if (n <= 1) return n;
        // Naive to demonstrate method timing in logs
        return fib(n - 1) + fib(n - 2);
    }
}
