package io.cockroachdb.pooltool.web;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;

@Controller
public class CustomErrorController implements ErrorController {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @RequestMapping("/error")
    public ModelAndView handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exception = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
        Object exceptionType = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);
        Object servletName = request.getAttribute(RequestDispatcher.ERROR_SERVLET_NAME);
        Object requestUri = request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);

        HttpStatus httpStatus;
        if (status != null) {
            httpStatus = HttpStatus.valueOf(Integer.parseInt(status.toString()));
        } else {
            httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        Map<String, Object> model = new HashMap<>();
        model.put("message", message);
        model.put("status", httpStatus);
        model.put("uri", requestUri);
        model.put("servletName", servletName);
        model.put("exception", exception);
        model.put("exceptionType", exceptionType);
        model.put("timestamp", Instant.now().toString());

        return new ModelAndView("error", model);
    }
}
