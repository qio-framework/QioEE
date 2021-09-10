package qio.processor;

import qio.annotate.Inject;
import qio.annotate.Property;
import qio.model.support.ObjectDetails;
import qio.Qio;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnnotationProcessor {

    Qio qio;
    Map<String, ObjectDetails> processed;
    List<ObjectDetails> annotations;

    public AnnotationProcessor(Qio qio){
        this.qio = qio;
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
                    if(qio.getElementStorage().getElements().containsKey(fieldKey)){
                        Object element = qio.getElementStorage().getElements().get(fieldKey).getElement();
                        field.setAccessible(true);
                        field.set(object, element);
                        processedFieldsCount++;
                    }else{
                        processAnnotations(z + 1);
                    }
                }
                if(field.isAnnotationPresent(Property.class)){
                    Property annotation = field.getAnnotation(Property.class);
                    String key = annotation.value();

                    if(qio.getPropertyStorage().getProperties().containsKey(key)){
                        field.setAccessible(true);
                        String value = qio.getPropertyStorage().getProperties().get(key);
                        attachValue(field, object, value);
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
                String key = Qio.getName(objectDetails.getName());
                processed.put(key, objectDetails);
            }
        }
    }

    protected void attachValue(Field field, Object object, String stringValue) throws Exception{
        Type type = field.getType();
        if(type.getTypeName().equals("boolean") || type.getTypeName().equals("java.lang.Boolean")){
            Boolean value = Boolean.valueOf(stringValue);
            field.set(object, value);
        }
        if(type.getTypeName().equals("int") || type.getTypeName().equals("java.lang.Integer")){
            Integer value = Integer.valueOf(stringValue);
            field.set(object, value);
        }
        if(type.getTypeName().equals("float") || type.getTypeName().equals("java.lang.Float")){
            Float value = Float.valueOf(stringValue);
            field.set(object, value);
        }
        if(type.getTypeName().equals("double") || type.getTypeName().equals("java.lang.Double")){
            Double value = Double.valueOf(stringValue);
            field.set(object, value);
        }
        if(type.getTypeName().equals("java.math.BigDecimal")){
            BigDecimal value = new BigDecimal(stringValue);
            field.set(object, value);
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
        for(Map.Entry<String, ObjectDetails> entry: qio.getElementProcessor().getAnnotatedClasses().entrySet()){
            ObjectDetails objectDetails = entry.getValue();
            if(!annotations.contains(objectDetails))annotations.add(objectDetails);
        }
    }

    protected Boolean allAnnotationsProcessed(){
        return this.processed.size() == qio.getElementProcessor().getAnnotatedClasses().size();
    }
}
