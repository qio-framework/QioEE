package qio.processor;

import qio.storage.PropertyStorage;
import qio.Qio;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Properties;

public class PropertiesProcessor {

    PropertyStorage propertyStorage;

    public PropertiesProcessor(Builder builder){
        this.propertyStorage = builder.propertyStorage;
    }

    public PropertyStorage getPropertiesData(){
        return this.propertyStorage;
    }

    public static class Builder{

        Boolean runEmbedded;
        ServletContext context;
        String[] propertiesFiles;
        PropertyStorage propertyStorage;

        public Builder(){
            this.propertiesFiles = new String[]{};
            this.propertyStorage = new PropertyStorage();
        }

        public Builder asEmbedded(Boolean runEmbedded){
            this.runEmbedded = runEmbedded;
            return this;
        }

        public Builder withContext(ServletContext context){
            this.context = context;
            return this;
        }

        public Builder withFiles(String[] propertiesFiles){
            this.propertiesFiles = propertiesFiles;
            return this;
        }

        protected File getPropertiesFile(String propertyFile) throws Exception{
            String classPath = Paths.get("webapps", context.getContextPath(), "WEB-INF", "classes")
                    .toAbsolutePath()
                    .toString();

            if(runEmbedded != null && runEmbedded){
                classPath = Paths.get("src", "main", "resources")
                        .toAbsolutePath()
                        .toString();
            }

            File file = new File(classPath.concat(File.separator).concat(propertyFile));
            if(!file.exists()) {
                throw new Exception(propertyFile.concat(" properties file cannot be located..."));
            }
            return file;
        }

        protected void traversePropertiesData() throws Exception{

            System.out.println(Qio.Assistant.SIGNATURE + " resolving properties");
            if(propertiesFiles != null) {

                for (String propertyFile : propertiesFiles) {
                    FileInputStream fis = null;
                    Properties prop = null;
                    try {
                        File file = getPropertiesFile(propertyFile);
                        fis = new FileInputStream(file);
                        prop = new Properties();
                        prop.load(fis);

                        Enumeration properties = prop.propertyNames();
                        while (properties.hasMoreElements()) {
                            String key = (String) properties.nextElement();
                            String value = prop.getProperty(key);
                            propertyStorage.getProperties().put(key, value);
                        }
                    } catch (FileNotFoundException fnfe) {
                        fnfe.printStackTrace();
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    } finally {
                        if (fis != null) {
                            fis.close();
                        }
                    }
                }
            }

        }

        public PropertiesProcessor process() throws Exception{
            traversePropertiesData();
            return new PropertiesProcessor(this);
        }

    }

}
