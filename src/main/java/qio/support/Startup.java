package qio.support;

import qio.Qio;
import qio.model.Element;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

@WebListener
public class Startup implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        try {

            ServletContext servletContext = sce.getServletContext();
            FileInputStream is = new FileInputStream(
                    Qio.getResourceUri(servletContext) + File.separator + "qio.props"
            );
            Properties props = new Properties();
            props.load(is);

            Boolean devMode = Boolean.parseBoolean(props.get("qio.dev").toString());
            String[] resources = props.get("qio.assets").toString().split(",");
            String[] properties = props.get("qio.properties").toString().split(",");

            new Qio.Injector()
                    .devMode(devMode)
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
        DbMediator mediator = (DbMediator) qio.getElement(Qio.DBMEDIATOR);
        try {
            mediator.dropDb();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
