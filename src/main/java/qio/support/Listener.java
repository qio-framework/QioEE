package qio.support;

import qio.Qio;
import qio.model.Element;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class Listener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        Element qioElement = (Element)servletContext.getAttribute(Qio.QIO);
        Qio qio = (Qio) qioElement.getBean();
        DbMediator mediator = (DbMediator) qio.getBean(Qio.DBMEDIATOR);
        try {
            mediator.createDb();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext servletContext = sce.getServletContext();
        Element qioElement = (Element)servletContext.getAttribute(Qio.QIO);
        Qio qio = (Qio) qioElement.getBean();
        DbMediator mediator = (DbMediator) qio.getBean(Qio.DBMEDIATOR);
        try {
            mediator.dropDb();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
