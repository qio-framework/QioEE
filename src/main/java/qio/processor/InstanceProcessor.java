package qio.processor;

import qio.Qio;
import qio.annotate.Events;
import qio.model.support.ObjectDetails;

import javax.servlet.ServletContext;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class InstanceProcessor {

    Qio qio;
    Map<String, ObjectDetails> classes;

    public InstanceProcessor(Builder builder){
        this.qio = builder.qio;
        this.classes = builder.classes;
    }

    public Map<String, ObjectDetails> getClasses() {
        return classes;
    }

    public static class Builder{

        Qio qio;
        ClassLoader loader;
        Map<String, ObjectDetails> classes;

        public Builder(){
            this.classes = new HashMap<>();
            this.loader = Thread.currentThread().getContextClassLoader();
        }
        public Builder withQio(Qio qio){
            this.qio = qio;
            return this;
        }

        protected void createClasses(String classesPath){
            File pathFile = new File(classesPath);
            File[] files = pathFile.listFiles();
            for (File file : files) {

                if (file.isDirectory()) {
                    createClasses(file.getPath());
                    continue;
                }

                try {

                    if(!file.toString().endsWith(".java") &&
                            !file.toString().endsWith(".class")){
                        continue;
                    }

                    Class cls = null;
                    String classPath = "";

                    if(file.toString().endsWith(".java")){
                        String separator = System.getProperty("file.separator");
                        String regex = "java\\" + separator;
                        String[] pathParts = file.getPath().split(regex);
                        classPath = pathParts[1]
                                .replace("\\", ".")
                                .replace("/",".")
                                .replace(".java", "");
                        cls = loader.loadClass(classPath);
                    }else{
                        String separator = System.getProperty("file.separator");
                        String regex = "classes\\" + separator;
                        String[] pathParts = file.getPath().split(regex);
                        classPath = pathParts[1]
                                .replace("\\", ".")
                                .replace("/",".")
                                .replace(".class", "");
                        cls = loader.loadClass(classPath);
                    }

                    if (cls.isAnnotation() ||
                            cls.isInterface() ||
                            (cls.getName() == this.getClass().getName())) {
                        continue;
                    }


                    ObjectDetails objectDetails = new ObjectDetails();
                    objectDetails.setClazz(cls);
                    objectDetails.setName(Qio.getName(cls.getName()));
                    Object object = getObject(cls);
                    objectDetails.setObject(object);

                    if(cls.isAnnotationPresent(Events.class)){
                        qio.setQioEvents(object);
                    }

                    classes.put(objectDetails.getName(), objectDetails);

                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }

        protected Object getObject(Class cls) throws
                IllegalAccessException, InstantiationException, InvocationTargetException {
            Constructor[] constructors = cls.getDeclaredConstructors();
            for(Constructor constructor : constructors){
                constructor.setAccessible(true);
                if(constructor.getParameterCount() == 0){
                    return constructor.newInstance();
                }
            }
            return null;
        }

        public InstanceProcessor build() throws Exception{
            System.out.println(Qio.SIGNATURE + " initializing dependencies");
            createClasses(qio.getClassesUri());
            return new InstanceProcessor(this);
        }

    }

}
