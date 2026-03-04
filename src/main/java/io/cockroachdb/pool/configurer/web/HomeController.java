package io.cockroachdb.pool.configurer.web;

import java.sql.SQLException;
import java.util.concurrent.Callable;

import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@WebController
@RequestMapping("/")
public class HomeController {
    @GetMapping("/")
    public Callable<String> homePage(Model model) {
        return () -> "home";
    }

    @GetMapping("/error-test")
    public String errorTest() {
        throw new IllegalStateException("Disturbance!");
    }

    @GetMapping("/error-test2")
    public String errorTest2() throws Exception {
        throw new SQLException("Disturbance!", "12345");
    }

    @GetMapping("/error-test3")
    public String errorTest3() {
        throw new IncorrectResultSizeDataAccessException(100, 1);
    }
}
