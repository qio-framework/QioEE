package qio.processor;

import qio.Qio;
import qio.model.support.ObjectDetails;

import javax.servlet.ServletContext;
import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class InstanceProcessor {

    Map<String, ObjectDetails> classes;

    public InstanceProcessor(Builder builder){
        this.classes = builder.classes;
    }

    public Map<String, ObjectDetails> getClasses() {
        return classes;
    }

    public static class Builder{

        ClassLoader loader;
        Boolean runEmbedded;
        ServletContext servletContext;
        Map<String, ObjectDetails> classes;

        public Builder(){
            this.classes = new HashMap<>();
            this.loader = Thread.currentThread().getContextClassLoader();
        }

        public Builder asEmbedded(Boolean runEmbedded) {
            this.runEmbedded = runEmbedded;
            return this;
        }

        public Builder withContext(ServletContext servletContext){
            this.servletContext = servletContext;
            return this;
        }

        public String getPath(){
            if(runEmbedded) {
                return Paths.get("src", "main", "java")
                        .toAbsolutePath()
                        .toString();
            }else{
                return Paths.get("webapps", servletContext.getContextPath(), "WEB-INF", "classes")
                        .toAbsolutePath()
                        .toString();
            }
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
                            cls.getName().equals("qkio.support.Runner") ||
                            (cls.getName() == this.getClass().getName())) {
                        continue;
                    }

                    ObjectDetails objectDetails = new ObjectDetails();
                    objectDetails.setClazz(cls);
                    objectDetails.setName(Qio.Assistant.getName(cls.getName()));
//                    objectDetails.setPath(cls.getName());

                    Constructor[] constructors = cls.getDeclaredConstructors();
                    for(Constructor constructor : constructors){
                        constructor.setAccessible(true);
                        if(constructor.getParameterCount() == 0){
                            Object object = constructor.newInstance();
                            objectDetails.setObject(object);
                        }
                    }

                    classes.put(objectDetails.getName(), objectDetails);

                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }


        public InstanceProcessor build() throws Exception{
            System.out.println(Qio.Assistant.SIGNATURE + " injecting dependencies");
            createClasses(getPath());
            return new InstanceProcessor(this);
        }

    }

}
