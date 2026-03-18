package com.college.erp.exception;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ModelAndView handleAllExceptions(Exception ex) {
        System.err.println("CRITICAL GLOBAL EXCEPTION CAUGHT: " + ex.getMessage());
        ex.printStackTrace();

        ModelAndView mav = new ModelAndView();
        mav.addObject("exception", ex);
        mav.addObject("url", "Global Error");
        mav.setViewName("error"); // Assumes there's an error.html template, but the System.err prints are what I
                                  // need
        return mav;
    }
}
