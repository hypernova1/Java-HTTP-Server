package org.sam.server.http.context;

import org.sam.server.context.BeanContainer;
import org.sam.server.http.web.request.HttpRequest;
import org.sam.server.http.web.request.Request;
import org.sam.server.http.web.response.HttpResponse;
import org.sam.server.http.web.response.Response;

import java.io.IOException;
import java.net.Socket;

public class HttpHandler implements Runnable {
    private final Socket connect;
    private final BeanContainer beanContainer;

    public HttpHandler(Socket connect, BeanContainer beanContainer) {
        this.connect = connect;
        this.beanContainer = beanContainer;
    }

    @Override
    public void run() {
        try {
            Request request = HttpRequest.from(connect.getInputStream());
            Response response = HttpResponse.of(connect.getOutputStream(), request.getUrl(), request.getMethod());

            if (StaticResourceHandler.isStaticResourceRequest(request, response)) {
                return;
            }

            HttpLauncher.execute(request, response, beanContainer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                connect.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
