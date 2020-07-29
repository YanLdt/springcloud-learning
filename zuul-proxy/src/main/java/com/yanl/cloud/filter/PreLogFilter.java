package com.yanl.cloud.filter;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import java.util.logging.Logger;

@Component
@Slf4j
public class PreLogFilter extends ZuulFilter {
    /**
     * 过滤器类型 有pre, routing, post, error四种
     * @return
     */
    @Override
    public String filterType() {
        return "pre";
    }

    /**
     * 过滤器执行顺序，数值越小，优先级越高
     * @return
     */
    @Override
    public int filterOrder() {
        return 1;
    }

    /**
     * 是否进行过滤
     * @return
     */
    @Override
    public boolean shouldFilter() {
        return true;
    }

    /**
     * 自定义的过滤去逻辑
     * @return
     * @throws ZuulException
     */
    @Override
    public Object run() throws ZuulException {
        RequestContext requestContext = RequestContext.getCurrentContext();
        HttpServletRequest request = requestContext.getRequest();
        String host = request.getRemoteHost();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        log.info("Remote Host: {}, method: {}, uri: {}", host, method, uri);
        return null;
    }
}
