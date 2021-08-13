package qio.support;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import qio.Qio;
import qio.annotate.Property;
import qio.model.Element;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static qio.Qio.getResourceUri;


@WebListener
public class Listener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        try {

            ServletContext servletContext = sce.getServletContext();
            FileInputStream is = new FileInputStream(
                    getResourceUri(servletContext) + File.separator + "qio.props"
            );
            Properties props = new Properties();
            props.load(is);

            Object devModeProp = props.get("qio.dev");
            Boolean devMode = false;
            if(devModeProp != null){
                devMode = Boolean.parseBoolean(devModeProp.toString().replaceAll("\\s+", ""));
            }

            Object resourcesProp = props.get("qio.assets");
            Object propertiesProp = props.get("qio.properties");

            List<String> resourcesPre = new ArrayList<>();
            if(resourcesProp != null){
                String resourceStr = resourcesProp.toString();
                if(!resourceStr.equals("")){
                    resourcesPre = Arrays.asList(resourceStr.split(","));
                }
            }
            List<String> propertiesPre = new ArrayList<>();
            if(propertiesProp != null){
                String propString = propertiesProp.toString();
                if(!propString.equals("")){
                    propertiesPre = Arrays.asList(propString.split(","));
                }
            }

            List<String> resources = new ArrayList<>();
            if(!resourcesPre.isEmpty()){
                for(String resource: resourcesPre){
                    resource = resource.replaceAll("\\s+", "");
                    resources.add(resource);
                }
            }

            List<String> properties = new ArrayList<>();
            if(!propertiesPre.isEmpty()){
                for(String property : propertiesPre){
                    property = property.replaceAll("\\s+","");
                    if(property.equals("this")){
                        property = "qio.props";
                    }
                    properties.add(property);
                }
            }

            new Qio.Injector()
                    .setDevEnv(devMode)
                    .withContext(servletContext)
                    .withWebResources(resources)
                    .withPropertyFiles(properties)
                    .inject();

        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        Element qioElement = (Element)servletContext.getAttribute(Qio.QIO);
        Qio qio = (Qio) qioElement.getElement();
        if(qio.inDevMode()) {
            DbMediator mediator = (DbMediator) qio.getElement(Qio.DBMEDIATOR);
            try {
                mediator.dropDb();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
