package qio.processor;

import qio.annotate.Inject;
import qio.annotate.Property;
import qio.storage.ElementStorage;
import qio.storage.PropertyStorage;
import qio.model.support.ObjectDetails;
import qio.Qio;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationProcessor {

    ElementStorage elementStorage;
    ElementProcessor elementProcessor;
    PropertyStorage propertyStorage;
    Map<String, ObjectDetails> processed;
    List<ObjectDetails> annotations;

    public AnnotationProcessor(ElementStorage elementStorage,
                               ElementProcessor elementProcessor,
                               PropertyStorage propertyStorage){
        this.elementStorage = elementStorage;
        this.elementProcessor = elementProcessor;
        this.propertyStorage = propertyStorage;
        this.processed = new HashMap<>();
        this.annotations = new ArrayList<>();
        map();
    }

    public void run() throws Exception{
        while(!allAnnotationsProcessed()){
            processAnnotations(0);
            break;
        }
    }

    private void processAnnotations(int idx) throws Exception {

        if(idx > annotations.size())idx = 0;

        for(Integer z = idx; z < annotations.size(); z++){
            ObjectDetails objectDetails = annotations.get(z);
            Integer fieldsCount = getAnnotatedFieldsCount(objectDetails.getClazz());
            Integer processedFieldsCount = 0;

            Object object = objectDetails.getObject();
            Field[] fields = objectDetails.getClazz().getDeclaredFields();

            for(Field field: fields) {
                if(field.isAnnotationPresent(Inject.class)) {
                    String fieldKey = field.getName().toLowerCase();
                    if(elementStorage.getElements().containsKey(fieldKey)){
                        Object element = elementStorage.getElements().get(fieldKey).getElement();
                        field.setAccessible(true);
                        field.set(object, element);
                        processedFieldsCount++;
                    }else{
                        processAnnotations(z + 1);
                        //    throw new Exception(field.getName() + " is missing on " + object.getClass().getName());
                    }
                }
                if(field.isAnnotationPresent(Property.class)){
                    Property annotation = field.getAnnotation(Property.class);
                    String key = annotation.value();

                    if(propertyStorage.getProperties().containsKey(key)){
                        String value = propertyStorage.getProperties().get(key);
                        field.setAccessible(true);
                        field.set(object, value);

                        processedFieldsCount++;
                    }else{
                        processAnnotations(z + 1);
                        throw new Exception(field.getName() + " is missing on " + object.getClass().getName());
                    }
                }
            }

            if(fieldsCount !=
                    processedFieldsCount){
                processAnnotations( z + 1);
            }else{
                String key = Qio.Assistant.getName(objectDetails.getName());
                processed.put(key, objectDetails);
            }
        }
    }

    protected Integer getAnnotatedFieldsCount(Class clazz) throws Exception{
        Integer count = 0;
        Field[] fields = clazz.getDeclaredFields();
        for(Field field: fields){
            if(field.isAnnotationPresent(Inject.class)){
                count++;
            }
            if(field.isAnnotationPresent(Property.class)){
                count++;
            }
        }
        return count;
    }

    private void map(){
        for(Map.Entry<String, ObjectDetails> entry: this.elementProcessor.getAnnotatedClasses().entrySet()){
            ObjectDetails objectDetails = entry.getValue();
            if(!annotations.contains(objectDetails))annotations.add(objectDetails);
        }
    }


    protected Boolean allAnnotationsProcessed(){
        return this.processed.size() == this.elementProcessor.getAnnotatedClasses().size();
    }
}
