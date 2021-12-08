package qio.web;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import qio.Qio;
import qio.annotate.JsonOutput;
import qio.model.web.*;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RequestModulator {

    Qio qio;

    public RequestModulator(Qio qio){
        this.qio = qio;
    }

    public boolean handle(String verb, HttpServletRequest req, HttpServletResponse resp) throws Exception {

        ServletContext servletContext = req.getServletContext();
        ResponseData redirectData = (ResponseData) servletContext.getAttribute(Qio.QIO_REDIRECT);
        setRedirectAttributes(redirectData, req, servletContext);

        String uri = getUri(req);

        if(StaticResource.isResource(uri, qio.getResources())){
            StaticResource staticResource = new StaticResource(uri, servletContext, resp);
            staticResource.serve();
            return true;
        }

        EndpointMapping endpointMapping = getHttpMapping(verb, uri);

        if(endpointMapping == null){
            badge(resp);
            resp.getWriter().println(uri + " is 404. " + verb);
            resp.getWriter().flush();
            return false;
        }

        ResponseData responseData = new ResponseData();
        if (!endpointMapping.getVerb().equals(verb)) {
            responseData.put("verb", verb);
            badge(resp);
            resp.getWriter().println(verb + " not allowed for " + uri);
            resp.getWriter().flush();
            return false;
        }

        Object[] parameters = getEndpointParameters(uri, endpointMapping, req, resp, responseData);
        Method method = endpointMapping.getMethod();
        method.setAccessible(true);

        Object object = endpointMapping.getClassDetails().getObject();

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

        }catch(ClassCastException ccex){
            qio.sign();
            System.out.println("");
            System.out.println("Attempted to cast an object at the data layer with an incorrect Class type.");
            System.out.println("");
            ccex.printStackTrace();
        }catch (Exception ex){
            System.out.println("");
            System.out.println(Qio.PROCESS + "   " +  endpointMapping.getVerb() + " :: " + endpointMapping.getPath());
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
        if(uri.equals(""))uri = "/";
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

    protected List<String> getRegexParts(EndpointMapping mapping){
        return Arrays.asList(mapping.getRegexedPath().split("/"));
    }

    protected String getRedirect(String response){
        String[] redirectParts = response.split("]");
        return redirectParts[1];
    }

    private Object[] getEndpointParameters(String uri,
                                   EndpointMapping endpointMapping,
                                   HttpServletRequest req,
                                   HttpServletResponse resp,
                                   ResponseData data){

        List<EndpointPosition> endpointValues = getEndpointValues(uri, endpointMapping);
        List<Object> params = new ArrayList<>();
        List<String> typeNames = endpointMapping.getTypeNames();
        int idx = 0;
        for(int z = 0; z <  typeNames.size(); z++){
            String type = typeNames.get(z);
            if(type.equals("javax.servlet.http.HttpServletRequest")){
                params.add(req);
            }
            if(type.equals("javax.servlet.http.HttpServletResponse")){
                params.add(resp);
            }
            if(type.equals("qio.model.web.ResponseData")){
                params.add(data);
            }
            if(type.equals("java.lang.Integer")){
                params.add(Integer.valueOf(endpointValues.get(idx).getValue()));
                idx++;
            }
            if(type.equals("java.lang.Long")){
                params.add(Long.valueOf(endpointValues.get(idx).getValue()));
                idx++;
            }
            if(type.equals("java.math.BigDecimal")){
                params.add(new BigDecimal(endpointValues.get(idx).getValue()));
                idx++;
            }
            if(type.equals("java.lang.String")){
                params.add(endpointValues.get(idx).getValue());
                idx++;
            }
        }

        return params.toArray();
    }

    protected List<EndpointPosition> getEndpointValues(String uri, EndpointMapping mapping){
        List<String> pathParts = getPathParts(uri);
        List<String> regexParts = getRegexParts(mapping);

        List<EndpointPosition> httpValues = new ArrayList<>();
        for(int n = 0; n < regexParts.size(); n++){
            String regex = regexParts.get(n);
            if(regex.contains("A-Za-z0-9")){
                httpValues.add(new EndpointPosition(n, pathParts.get(n)));
            }
        }
        return httpValues;
    }

    protected EndpointMapping getHttpMapping(String verb, String uri){
        EndpointMapping endpointMapping = null;
        for (Map.Entry<String, EndpointMapping> mappingEntry : qio.getEndpointMappings().getMappings().entrySet()) {
            EndpointMapping mapping = mappingEntry.getValue();
            Matcher matcher = Pattern.compile(mapping.getRegexedPath())
                    .matcher(uri);
            if(matcher.matches() &&
                    mapping.getVerb().equals(verb) &&
                        variablesMatchUp(uri, mapping)){
                endpointMapping = mapping;
                break;
            }
        }
        return endpointMapping;
    }

    protected boolean variablesMatchUp(String uri, EndpointMapping endpointMapping){
        List<String> parts = Arrays.asList(uri.split("/"));

        for(int z = 0; z < endpointMapping.getTypeDetails().size(); z++){
            try{
                TypeFeature typeDetail = endpointMapping.getTypeDetails().get(z);
                int position = endpointMapping.getVariablePositions().get(z);
                String pathPart = parts.get(position);
                String type = typeDetail.getType();

                if(type.equals("java.lang.Integer")){
                    Integer.parseInt(pathPart);
                }else if(type.equals("java.lang.Long")){
                    Long.parseLong(pathPart);
                }
            }catch (Exception ex){
                return false;
            }
        }
        return true;
    }

    protected static void badge(HttpServletResponse resp) throws Exception{
        resp.getWriter().println("");
        resp.getWriter().println("\n//|  Qio  \\\\\\\\\\\\\n");
        resp.getWriter().println("");
    }

    public String removeLast(String s) {
        return (s == null || s.length() == 0)
                ? null
                : (s.substring(0, s.length() - 1));
    }
}
