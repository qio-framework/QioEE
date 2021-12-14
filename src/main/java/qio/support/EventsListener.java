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
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static qio.Qio.getResourceUri;


@WebListener
public class EventsListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        try {

            ServletContext servletContext = sce.getServletContext();

            InputStream is = null;

            if(is == null) {
                is = this.getClass().getResourceAsStream("/src/main/resources/qio.props");
            }
            if(is == null) {
                try {
                    String uri = getResourceUri(servletContext) + File.separator + "qio.props";
                    is = new FileInputStream(uri);
                } catch (FileNotFoundException fnfe) {
                }
            }

            if (is == null) {
                throw new Exception("Qio : qio.props not found in src/main/resources/");
            }




            Properties props = new Properties();
            props.load(is);

            Object env = props.get("qio.env");

            Boolean isBasic = true;
            Boolean createDb = false;
            Boolean dropDb = false;
            if(env != null){
                String environment = env.toString().replaceAll("\\s+", "");
                List<String> properties = Arrays.asList(environment.split(","));
                for(String prop : properties){
                    if(prop.equals("create")){
                        isBasic = false;
                        createDb = true;
                    }
                    if(prop.equals("drop")){
                        isBasic = false;
                        dropDb = true;
                    }
                    if (prop.equals("plain") ||
                            prop.equals("basic") ||
                                prop.equals("empty") ||
                                    prop.equals("")){
                        isBasic = true;
                    }
                }
            }

            if(isBasic && (createDb || dropDb))
                throw new Exception("You need to either set qio.env=basic for basic systems that do not need " +
                        "a database connection, or qio.env=create to create a db using src/main/resource/create-db.sql, " +
                        "or qio.env=create,drop to both create and drop a database.");

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
                    .setBasic(isBasic)
                    .setCreateDb(createDb)
                    .setDropDb(dropDb)
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
        if(!qio.isBasic) {
            DbMediator mediator = (DbMediator) qio.getElement(Qio.DBMEDIATOR);
            try {
                mediator.dropDb();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
