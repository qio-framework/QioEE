package qio.web;

import qio.Qio;
import qio.annotate.JsonOutput;
import qio.model.web.HttpMapping;
import qio.model.web.HttpMappings;
import qio.model.web.ResponseData;
import qio.model.web.TypeFeature;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestModulator {

    String[] resources;
    HttpMappings httpMappings;

    public RequestModulator(String[] resources, HttpMappings httpMappings){
        this.resources = resources;
        this.httpMappings = httpMappings;
    }

    public boolean handle(String verb, HttpServletRequest req, HttpServletResponse resp) throws Exception {

        ServletContext servletContext = req.getServletContext();
        ResponseData redirectData = (ResponseData) servletContext.getAttribute(Qio.QIO_REDIRECT);
        setRedirectAttributes(redirectData, req, servletContext);

        String uri = getUri(req);

        if(StaticResource.isResource(uri, resources)){
            StaticResource staticResource = new StaticResource(uri, servletContext, resp);
            staticResource.serve();
            return true;
        }

        HttpMapping httpMapping = getHttpMapping(verb, uri);

        if(httpMapping == null){
            badge(resp);
            resp.getWriter().println(uri + " is 404. " + verb);
            resp.getWriter().flush();
            return false;
        }

        ResponseData responseData = new ResponseData();
        if (!httpMapping.getVerb().equals(verb)) {
            responseData.put("verb", verb);
            badge(resp);
            resp.getWriter().println(verb + " not allowed for " + uri);
            resp.getWriter().flush();
            return false;
        }

        Object[] parameters = getParameters(uri, httpMapping, req, resp, responseData);

        Method method = httpMapping.getMethod();
        method.setAccessible(true);

        Object object = httpMapping.getClassDetails().getObject();

        try {

            String response = (String) method.invoke(object, parameters);

            if(method.isAnnotationPresent(JsonOutput.class)){
                resp.setContentType("application/json");
                resp.setCharacterEncoding("UTF-8");
                resp.getWriter().print(response);
                resp.getWriter().flush();
            }else if(response.startsWith(Qio.HTTP_REDIRECT)){
                String path = getRedirect(response);
                ServletContext context = req.getServletContext();
                context.setAttribute("qio-redirect", responseData);
                resp.sendRedirect(context.getContextPath() + path);
            }else{
                setRequestAttributes(req, responseData);
                req.getRequestDispatcher(response).forward(req, resp);
            }

            return true;

        }catch(ClassCastException ccex){
            Qio.Injector.badge();
            System.out.println("");
            System.out.println("Attempted to cast an object at the data layer with an incorrect Class type.");
            System.out.println("");
            ccex.printStackTrace();
        }catch (Exception ex){
            System.out.println("");
            System.out.println(Qio.Assistant.SIGNATURE + "   " +  httpMapping.getVerb() + " :: " + httpMapping.getPath());
            System.out.println("");
            ex.printStackTrace();
        }
        return true;
    }

    private void setRequestAttributes(HttpServletRequest req, ResponseData responseData) {
        for (Map.Entry<String, Object> objEntry : responseData.getData().entrySet()) {
            req.setAttribute(objEntry.getKey(), objEntry.getValue());
        }
    }

    private String getUri(HttpServletRequest req) {
        String uri = req.getRequestURI().replaceFirst(req.getContextPath(), "").toLowerCase();
        if(!uri.equals("/") &&
                uri.endsWith("/")){
            uri = removeLast(uri);
        }
        return uri;
    }

    private void setRedirectAttributes(ResponseData redirectData, HttpServletRequest req, ServletContext servletContext) {
        if(redirectData != null) {
            for (Map.Entry<String, Object> entry : redirectData.getData().entrySet()) {
                req.setAttribute(entry.getKey(), entry.getValue());
            }
            servletContext.removeAttribute(Qio.QIO_REDIRECT);
        }
    }

    protected List<String> getPathParts(String uri){
        return Arrays.asList(uri.split("/"));
    }

    protected List<String> getRegexParts(HttpMapping mapping){
        return Arrays.asList(mapping.getRegexedPath().split("/"));
    }

    protected String getRedirect(String response){
        String[] redirectParts = response.split("]");
        return redirectParts[1];
    }


    protected Object[] getParameters(String uri,
                                   HttpMapping httpMapping,
                                   HttpServletRequest req,
                                   HttpServletResponse resp,
                                   ResponseData data){

        List<String> values = getHttpValues(uri, httpMapping);
        List<Object> parameters = new ArrayList<>();
        parameters.add(req);
        parameters.add(resp);
        parameters.add(data);

        for(int z = 0; z < httpMapping.getTypeDetails().size(); z++){
            TypeFeature details = httpMapping.getTypeDetails().get(z);
            Object preObj = values.get(z);
            Object obj = preObj;
            if (details.getType().equals("int")) {
                obj = Integer.parseInt(preObj.toString());
            } else if (details.getType().equals("double")) {
                obj = Double.parseDouble(preObj.toString());
            } else if (details.getType().equals("long")) {
                obj = Long.parseLong(preObj.toString());
            } else if (details.getType().equals("java.lang.Integer")) {
                obj = Integer.parseInt(preObj.toString());
            } else if (details.getType().equals("java.lang.Long")) {
                obj = Long.parseLong(preObj.toString());
            } else if (details.getType().equals("java.math.BigDecimal")) {
                obj = new BigDecimal(preObj.toString());
            }
            parameters.add(obj);
        }
        return parameters.toArray();
    }

    protected List<String> getHttpValues(String uri, HttpMapping mapping){
        List<String> pathParts = getPathParts(uri);
        List<String> regexParts = getRegexParts(mapping);

        List<String> good = new ArrayList<>();
        for(int n = 0; n < regexParts.size(); n++){
            String regex = regexParts.get(n);
            if(regex.contains("A-Za-z0-9")){
                good.add(pathParts.get(n));
            }
        }
        return good;
    }

    protected HttpMapping getHttpMapping(String verb, String uri){
        HttpMapping httpMapping = null;
        for (Map.Entry<String, HttpMapping> mappingEntry : httpMappings.getMappings().entrySet()) {
            HttpMapping mapping = mappingEntry.getValue();
            //System.out.println(uri + "         ::::::::     " + mapping.getRegexedPath());
            Matcher matcher = Pattern.compile(mapping.getRegexedPath())
                    .matcher(uri);
            if(matcher.matches() &&
                    mapping.getVerb().equals(verb) &&
                    variablesMatchUp(uri, mapping)){
                httpMapping = mapping;
                break;
            }
        }
        return httpMapping;
    }

    protected boolean variablesMatchUp(String uri, HttpMapping httpMapping){
        List<String> parts = Arrays.asList(uri.split("/"));

        for(int z = 0; z < httpMapping.getTypeDetails().size(); z++){
            try{
                TypeFeature typeDetail = httpMapping.getTypeDetails().get(z);
                int position = httpMapping.getVariablePositions().get(z);
                String pathPart = parts.get(position);
                String type = typeDetail.getType();

                if(type.equals("java.lang.Integer")){
                    Object obj = Integer.parseInt(pathPart);
                }else if(type.equals("java.lang.Long")){
                    Object obj = Long.parseLong(pathPart);
                }
            }catch (Exception ex){
                return false;
            }
        }
        return true;
    }

    protected static void badge(HttpServletResponse resp) throws Exception{
        resp.getWriter().println("");
        resp.getWriter().println("       ------  ");
        resp.getWriter().println("     (   Qio   )");
        resp.getWriter().println("       ------  ");
        resp.getWriter().println("");
    }

    public String removeLast(String s) {
        return (s == null || s.length() == 0)
                ? null
                : (s.substring(0, s.length() - 1));
    }
}
