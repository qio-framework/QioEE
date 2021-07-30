package qio.processor;

import qio.Qio;
import qio.annotate.Dependency;
import qio.annotate.Property;
import qio.storage.ElementStorage;
import qio.storage.PropertyStorage;
import qio.model.Element;
import qio.model.support.ObjectDetails;
import qio.model.web.MethodFeature;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

public class ConfigurationProcessor {

    ElementStorage elementStorage;
    ElementProcessor elementProcessor;
    PropertyStorage propertyStorage;

    List<Class> unprocessedConfigs;

    Map<String, MethodFeature> methods;
    List<MethodFeature> iterableMethods;
    Set<MethodFeature> processedMethods;

    Map<String, Integer> issues;

    public ConfigurationProcessor(ElementStorage elementStorage,
                                  ElementProcessor elementProcessor,
                                  PropertyStorage propertyStorage) throws Exception{
        this.unprocessedConfigs = elementProcessor.getConfigs();
        this.elementProcessor = elementProcessor;
        this.propertyStorage = propertyStorage;
        this.elementStorage = elementStorage;
        this.methods = new HashMap<>();
        this.processedMethods = new HashSet();
        this.iterableMethods = new ArrayList<>();
        this.issues = new HashMap<>();
    }

    public ConfigurationProcessor run() throws Exception{
        setMapDependencyMethods();
        setIterableMethods(methods);
        while(!allDependenciesProcessed()){
            process(0);
        }
        return this;
    }

    protected void process(int idx) throws Exception{
        Integer classCount = elementProcessor.getClasses().size();

        if(idx > iterableMethods.size()) idx = 0;

        for(Integer z = idx; z < iterableMethods.size(); z++){
            MethodFeature methodFeature = iterableMethods.get(z);
            Method method = methodFeature.getMethod();
            String methodName = Qio.Assistant.getName(method.getName());
            Object object = methodFeature.getObject();

            try {

                Object dependency = method.invoke(object);
                String clsName = Qio.Assistant.getName(dependency.getClass().getName());

                if(elementProcessor.getClasses().get(clsName) != null){
                    elementProcessor.getClasses().get(clsName).setObject(dependency);
                }else {
                    ObjectDetails objectDetails = new ObjectDetails();
                    objectDetails.setClazz(dependency.getClass());
                    objectDetails.setName(clsName);
//                    objectDetails.setPath(dependency.getClass().getName());
                    objectDetails.setObject(dependency);
                    elementProcessor.getClasses().put(clsName, objectDetails);
                }

                createAddElement(method, dependency);
                processedMethods.add(methodFeature);

            }catch(Exception ex){

                process(z + 1);

                if(issues.get(methodName) != null){
                    int count = issues.get(methodName);
                    count++;
                    issues.replace(methodName, count);
                }else{
                    issues.put(methodName, 1);
                }

                if(issues.get(methodName) != null &&
                        issues.get(methodName) >= classCount){

                    StringBuilder builder = new StringBuilder();
                    for(Map.Entry<String, Integer> entry: issues.entrySet()){
                        builder.append("       " + entry.getKey() + " :: " + entry.getValue() + " attempts \n");
                    }
                    throw new Exception("The following dependencies have not been resolved : \n\n\n" + builder.toString() + "\n\n" + ex.toString());
                }
            }
        }
    }

    private boolean setIterableMethods(Map<String, MethodFeature> methods) {
        for(Map.Entry<String, MethodFeature> entry : methods.entrySet()){
            iterableMethods.add(entry.getValue());
        }
        return true;
    }

    protected Boolean allDependenciesProcessed(){
        return processedMethods.size() == iterableMethods.size();
    }

    protected void createAddElement(Method method, Object object){
        Element element = new Element();
        element.setElement(object);
        String classKey = Qio.Assistant.getName(method.getName());
        this.elementStorage.getElements().put(classKey, element);
    }

    protected void setMapDependencyMethods() throws Exception {
        for(Class config : unprocessedConfigs){

            Object object = null;
            Constructor[] constructors = config.getConstructors();
            for(Constructor constructor : constructors){
                if(constructor.getParameterCount() == 0) {
                    object = constructor.newInstance();
                }
            }

            List<Method> declaredMethods = Arrays.asList(config.getDeclaredMethods());
            for(Method method: declaredMethods){

                if(method.isAnnotationPresent(Dependency.class)) {
                    String methodKey = method.getName().toLowerCase();
                    if (methods.containsKey(methodKey)) {
                        throw new Exception("More than one dependency with the same name defined : " + method.getName());
                    }

                    if(elementStorage.getElements().containsKey(methodKey)){
                        System.out.println("\n\n");
                        System.out.println("Warning: you elements being injected twice, once by configuration, the other via @Inject.");
                        System.out.println("Take a look at " + config.getName() + " and @Inject for " + method.getName());
                        System.out.println("\n\n");
                        Element existingElement = elementStorage.getElements().get(methodKey);
                        existingElement.setElement(object);
                        elementStorage.getElements().replace(methodKey, existingElement);
                    }

                    MethodFeature methodFeature = new MethodFeature();
                    methodFeature.setName(method.getName());
                    methodFeature.setMethod(method);
                    methodFeature.setObject(object);
                    methods.put(methodKey, methodFeature);
                }
            }

            List<Field> declaredFields = Arrays.asList(config.getDeclaredFields());
            for(Field field: declaredFields){
                if(field.isAnnotationPresent(Property.class)){
                    Property property = field.getAnnotation(Property.class);
                    String key = property.value();
                    if(!propertyStorage.getProperties().containsKey(key)){
                        throw new Exception(key + " property is missing");
                    }
                    String value = propertyStorage.getProperties().get(key);
                    field.setAccessible(true);
                    field.set(object, value);
                }
            }

        }
    }

}
