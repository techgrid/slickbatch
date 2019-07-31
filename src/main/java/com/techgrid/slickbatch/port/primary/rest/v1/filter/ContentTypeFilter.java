package com.techgrid.slickbatch.port.primary.rest.v1.filter;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;


import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
@Order(2)
public class ContentTypeFilter implements Filter {

        class Request extends HttpServletRequestWrapper {

            Request(HttpServletRequest request) {
                super(request);
            }

            @Override
            public String getContentType() {
                return APPLICATION_JSON_VALUE;
            }
        }

        @Override
        public void doFilter(
                ServletRequest request,
                ServletResponse response,
                FilterChain chain) throws IOException, ServletException {
            chain.doFilter(new Request((HttpServletRequest) request), response);
        }
}
