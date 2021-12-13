package qio.processor;

import qio.Qio;

import java.io.*;
import java.util.Enumeration;
import java.util.Properties;

import static qio.Qio.command;
import static qio.Qio.getResourceUri;

public class PropertiesProcessor {

    Qio qio;

    public PropertiesProcessor(Qio qio){
        this.qio = qio;
    }

    protected InputStream getPropertiesFile(String propertyFile) throws Exception{

        InputStream is = this.getClass().getResourceAsStream(Qio.RESOURCES + propertyFile);

        if(is == null) {
            String resourceUri = qio.getResourceUri();
            File file = new File(resourceUri + File.separator + propertyFile);
            if(!file.exists()) {
                throw new Exception(propertyFile + " properties file cannot be located...");
            }
            is = new FileInputStream(file);
        }
        return is;
    }

    public void run() throws IOException {

        if (qio.getPropertiesFiles() != null) {

            for (String propertyFile : qio.getPropertiesFiles()) {
                InputStream is = null;
                Properties prop = null;
                try {

                    is = getPropertiesFile(propertyFile);
                    prop = new Properties();
                    prop.load(is);

                    Enumeration properties = prop.propertyNames();
                    while (properties.hasMoreElements()) {
                        String key = (String) properties.nextElement();
                        String value = prop.getProperty(key);
                        qio.getPropertyStorage().getProperties().put(key, value);
                    }

                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }

        }

    }

}
