package qio.processor;

import qio.annotate.*;
import qio.model.Element;
import qio.model.support.ObjectDetails;
import qio.storage.ElementStorage;
import qio.storage.PropertyStorage;

import java.lang.reflect.Field;
import java.util.*;

public class ElementProcessor {

    Integer jdbcCount;
    Integer serviceCount;
    Integer elementCount;
    List<Class> configs;
    Map<String, ObjectDetails> classes;
    Map<String, ObjectDetails> httpClasses;
    Map<String, ObjectDetails> annotatedClasses;

    public ElementProcessor(Builder builder){
        this.jdbcCount = builder.jdbcCount;
        this.serviceCount = builder.serviceCount;
        this.elementCount = builder.elementCount;
        this.configs = builder.configs;
        this.classes = builder.classes;
        this.httpClasses = builder.httpClasses;
        this.annotatedClasses = builder.annotatedClasses;
    }

    public Map<String, ObjectDetails> getClasses() {
        return this.classes;
    }

    public Map<String, ObjectDetails> getHttpClasses() {
        return httpClasses;
    }

    public Map<String, ObjectDetails> getAnnotatedClasses() {
        return annotatedClasses;
    }

    public List<Class> getConfigs() { return configs; }

    public static class Builder{
        List<Class> configs;
        ElementStorage elementStorage;
        Integer jdbcCount;
        Integer serviceCount;
        Integer elementCount;
        PropertyStorage propertyStorage;
        Map<String, ObjectDetails> classes;
        Map<String, ObjectDetails> annotatedClasses;
        Map<String, ObjectDetails> httpClasses;

        public Builder(){
            this.jdbcCount = 0;
            this.serviceCount = 0;
            this.elementCount = 0;
            this.annotatedClasses = new HashMap<>();
            this.httpClasses = new HashMap<>();
            this.configs = new ArrayList<>();
            this.propertyStorage = new PropertyStorage();
        }

        protected void setConfigsData(){
            for(Map.Entry<String, ObjectDetails> entry : classes.entrySet()){
                Class clazz = entry.getValue().getClazz();
                if(clazz.isAnnotationPresent(Config.class)){
                    configs.add(clazz);
                }
            }
        }

        protected void setElementData() throws Exception {
            for(Map.Entry<String, ObjectDetails> entry : classes.entrySet()){
                Class clazz = entry.getValue().getClazz();

                if(clazz.isAnnotationPresent(qio.annotate.Element.class)){
                    buildAddElement(entry);
                    elementCount++;
                }
                if(clazz.isAnnotationPresent(DataStore.class)){
                    buildAddElement(entry);
                    jdbcCount++;
                }
                if(clazz.isAnnotationPresent(Service.class)){
                    buildAddElement(entry);
                    serviceCount++;
                }
                if(clazz.isAnnotationPresent(HttpHandler.class)){
                    httpClasses.put(entry.getKey(), entry.getValue());
                }

                Field[] fields = clazz.getDeclaredFields();
                for (Field field : fields) {
                    if (field.isAnnotationPresent(Inject.class)) {
                        annotatedClasses.put(entry.getKey(), entry.getValue());
                    }
                    if (field.isAnnotationPresent(Property.class)) {
                        annotatedClasses.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }

        protected void buildAddElement(Map.Entry<String, ObjectDetails> entry){
            Element element = new Element();
            String key = entry.getKey();
            Object object = entry.getValue().getObject();
            element.setElement(object);
            this.elementStorage.getElements().put(key, element);
        }

        public Builder withClasses(Map<String, ObjectDetails> classes){
            this.classes = classes;
            return this;
        }

        public Builder withElementData(ElementStorage elementStorage){
            this.elementStorage = elementStorage;
            return this;
        }

        public Builder prepare() throws Exception {
            setConfigsData();
            setElementData();
            return this;
        }

        public ElementProcessor build(){
            return new ElementProcessor(this);
        }
    }
}
