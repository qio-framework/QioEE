package qio.processor;

import qio.annotate.Variable;
import qio.annotate.verbs.Delete;
import qio.annotate.verbs.Get;
import qio.annotate.verbs.Post;
import qio.annotate.verbs.Put;
import qio.storage.ElementStorage;
import qio.model.support.ObjectDetails;
import qio.model.web.HttpMapping;
import qio.model.web.HttpMappings;
import qio.model.web.TypeFeature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class EndpointProcessor {

    public static final String GET    = "Get";
    public static final String POST   = "Post";
    public static final String PUT    = "Put";
    public static final String DELETE = "Delete";

    ElementProcessor elementProcessor;
    ElementStorage elementStorage;
    Map<String, ObjectDetails> processed;
    HttpMappings httpMappings;

    public EndpointProcessor(ElementStorage elementStorage,
                             ElementProcessor elementProcessor){
        this.elementStorage = elementStorage;
        this.elementProcessor = elementProcessor;
        this.processed = new HashMap<>();
        this.httpMappings = new HttpMappings();
    }

    public EndpointProcessor run() throws Exception{
        while(!allAnnotationsProcessed()){
            processWebAnnotations();
        }
        return this;
    }

    private boolean allAnnotationsProcessed(){
        return this.processed.size() == this.elementProcessor.getHttpClasses().size();
    }

    private void processWebAnnotations() throws Exception{
        for(Map.Entry<String, ObjectDetails> entry : this.elementProcessor.getHttpClasses().entrySet()){
            Class clazz = entry.getValue().getClazz();
            Method[] methods = clazz.getDeclaredMethods();
            for(Method method: methods){
                if(method.isAnnotationPresent(Get.class)){
                    setGetMapping(method, entry.getValue());
                    processed.put(entry.getKey(), entry.getValue());
                }
                if(method.isAnnotationPresent(Post.class)){
                    setPostMapping(method, entry.getValue());
                    processed.put(entry.getKey(), entry.getValue());
                }
                if(method.isAnnotationPresent(Put.class)){
                    setPutMapping(method, entry.getValue());
                    processed.put(entry.getKey(), entry.getValue());
                }
                if(method.isAnnotationPresent(Delete.class)){
                    setDeleteMapping(method, entry.getValue());
                    processed.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    protected void setGetMapping(Method method, ObjectDetails objectDetails) throws Exception{
        Get get = method.getAnnotation(Get.class);
        String path = get.value();
        HttpMapping mapping = new HttpMapping();
        mapping.setVerb(GET);
        setBaseDetailsAdd(path, mapping, method, objectDetails);
    }

    protected void setPostMapping(Method method, ObjectDetails objectDetails) throws Exception{
        Post post = method.getAnnotation(Post.class);
        String path = post.value();
        HttpMapping mapping = new HttpMapping();
        mapping.setVerb(POST);
        setBaseDetailsAdd(path, mapping, method, objectDetails);
    }

    protected void setPutMapping(Method method, ObjectDetails objectDetails) throws Exception{
        Put put = method.getAnnotation(Put.class);
        String path = put.value();
        HttpMapping mapping = new HttpMapping();
        mapping.setVerb(PUT);
        setBaseDetailsAdd(path, mapping, method, objectDetails);
    }

    protected void setDeleteMapping(Method method, ObjectDetails objectDetails) throws Exception{
        Delete delete = method.getAnnotation(Delete.class);
        String path = delete.value();
        HttpMapping mapping = new HttpMapping();
        mapping.setVerb(DELETE);
        setBaseDetailsAdd(path, mapping, method, objectDetails);
    }

    protected void setBaseDetailsAdd(String path, HttpMapping mapping, Method method, ObjectDetails objectDetails) throws Exception{

        mapping.setTypeNames(new ArrayList<>());


        List<TypeFeature> typeDetails = new ArrayList<>();
        Annotation[][] paramAnnotations = method.getParameterAnnotations();
        Class[] paramTypes = method.getParameterTypes();
        for (int n = 0; n < paramAnnotations.length; n++) {
            for (Annotation a: paramAnnotations[n]) {
                if (a instanceof Variable) {
                    TypeFeature details = new TypeFeature();
                    details.setName(paramTypes[n].getTypeName());
                    details.setType(paramTypes[n].getTypeName());
                    typeDetails.add(details);
                }
            }
        }


//https://regex101.com/r/sYeDyN/1
//\/(post){1}\/[A-Za-z0-9]*\/(paul){1}$
//\/(get){1}\/[A-Za-z0-9]\/[A-Za-z0-9]\/[A-Za-z0-9]\/$

        StringBuilder regexPath = new StringBuilder();
        regexPath.append("\\/(");
        int count = 0;
        String[] parts = path.split("/");
        for(String part: parts){
            count++;
            if(!part.equals("")) {
                if (part.matches("(\\{\\{[a-zA-Z]*\\}\\})")) {
                    regexPath.append("(.*[A-Za-z0-9])");
                    mapping.getVariablePositions().add(count - 1);
                } else {
                    regexPath.append("(" + part.toLowerCase() + "){1}");
                }
                if (count < parts.length) {
                    regexPath.append("\\/");
                }
            }
        }
        regexPath.append(")$");

        mapping.setRegexedPath(regexPath.toString());
        mapping.setTypeDetails(typeDetails);
        mapping.setPath(path);
        mapping.setMethod(method);
        mapping.setClassDetails(objectDetails);

        String key = mapping.getVerb().concat("-").concat(path);
        if(httpMappings.contains(key)){
            throw new Exception("Request path + " + path + " exists multiple times.");
        }

        httpMappings.add(key, mapping);
    }


    public HttpMappings getMappings() {
        return httpMappings;
    }
}
