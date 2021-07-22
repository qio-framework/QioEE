package qio;

import qio.processor.EndpointProcessor;
import qio.web.RequestModulator;
import qio.model.web.HttpMappings;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HttpMediator extends HttpServlet {

    RequestModulator requestModulator;

    @Override
    public void init(ServletConfig config) {
        ServletContext context = config .getServletContext();
        HttpMappings httpMappings = (HttpMappings) context.getAttribute(Qio.HTTP_MAPPINGS);
        String[] resources = (String[]) context.getAttribute(Qio.HTTP_RESOURCES);
        requestModulator = new RequestModulator(resources, httpMappings);
    }

    protected void handle(String verb, HttpServletRequest req, HttpServletResponse resp) {
        try {
            requestModulator.handle(verb, req, resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        handle(EndpointProcessor.GET, req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        handle(EndpointProcessor.POST, req, resp);
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        handle(EndpointProcessor.PUT, req, resp);
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        handle(EndpointProcessor.DELETE, req, resp);
    }

}
