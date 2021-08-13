package qio.processor;

import qio.Qio;
import qio.annotate.Events;
import qio.model.support.ObjectDetails;
;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static qio.Qio.command;

public class InstanceProcessor {

    Qio qio;
    ClassLoader cl;
    Map<String, ObjectDetails> objects;

    public InstanceProcessor(Qio qio){
        this.qio = qio;
        this.objects = new HashMap<>();
        this.cl = Thread.currentThread().getContextClassLoader();
    }

    public InstanceProcessor run() throws Exception {
        command(Qio.PROCESS + " initializing dependencies");
        createClasses(qio.getClassesUri());
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

                Class cls;
                String classPath;

                if(file.toString().endsWith(".java")){
                    String separator = System.getProperty("file.separator");
                    String regex = "java\\" + separator;
                    String[] pathParts = file.getPath().split(regex);
                    classPath = pathParts[1]
                            .replace("\\", ".")
                            .replace("/",".")
                            .replace(".java", "");
                    cls = cl.loadClass(classPath);
                }else{
                    String separator = System.getProperty("file.separator");
                    String regex = "classes\\" + separator;
                    String[] pathParts = file.getPath().split(regex);
                    classPath = pathParts[1]
                            .replace("\\", ".")
                            .replace("/",".")
                            .replace(".class", "");
                    cls = cl.loadClass(classPath);
                }

                if (cls.isAnnotation() ||
                        cls.isInterface() ||
                        (cls.getName() == this.getClass().getName())) {
                    continue;
                }

                if(cls.isAnnotationPresent(Events.class)){
                    qio.setEvents(getObject(cls));
                }

                ObjectDetails objectDetails = new ObjectDetails();
                objectDetails.setClazz(cls);
                objectDetails.setName(Qio.getName(cls.getName()));
                Object object = getObject(cls);
                objectDetails.setObject(object);

                qio.getObjects().put(objectDetails.getName(), objectDetails);

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

}
