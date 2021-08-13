package qio.processor;

import qio.Qio;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

import static qio.Qio.command;

public class PropertiesProcessor {

    Qio qio;

    public PropertiesProcessor(Qio qio){
        this.qio = qio;
    }

    protected File getPropertiesFile(String propertyFile) throws Exception{
        String resourceUri = qio.getResourceUri();
        File file = new File(resourceUri + File.separator + propertyFile);
        if(!file.exists()) {
            throw new Exception(propertyFile.concat(" properties file cannot be located..."));
        }
        return file;
    }

    public void run() throws IOException {
        command(Qio.PROCESS + " resolving properties");

        if (qio.getPropertiesFiles() != null) {

            for (String propertyFile : qio.getPropertiesFiles()) {
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
                        qio.getPropertyStorage().getProperties().put(key, value);
                    }

                } catch (FileNotFoundException fnfe) {
                    fnfe.printStackTrace();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                } catch (Exception ex) {
                    ex.printStackTrace();
                } finally {
                    if (fis != null) {
                        fis.close();
                    }
                }
            }

        }

    }

}
