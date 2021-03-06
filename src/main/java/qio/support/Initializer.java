package qio.support;

import qio.Qio;
import qio.model.Element;
import qio.model.web.EndpointMappings;
import qio.processor.*;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;

import static qio.Qio.*;

public class Initializer {

    public Initializer(){}

    public static class Builder {

        Qio qio;

        public Builder withQio(Qio qio){
            this.qio = qio;
            return this;
        }
        private void setQioAttributes(){
            Element qioElement = new Element();
            qioElement.setElement(qio);
            qio.getElementStorage().getElements().put(Qio.QIO, qioElement);
            Qio.set(qio.getElementStorage().getElements());
            qio.getServletContext().setAttribute(Qio.QIO, qioElement);

            qio.getServletContext().setAttribute(Qio.HTTP_RESOURCES, qio.getResources());
            if(qio.getResources() == null) qio.setResources(new ArrayList<>());
            if(qio.getPropertiesFiles() == null) qio.setPropertiesFiles(new ArrayList<>());
        }
        
        private void initDb() throws Exception{
            if (qio.createDb){
                DbMediator mediator = new DbMediator(qio);
                Element element = new Element();
                element.setElement(mediator);
                qio.getElementStorage().getElements().put(DBMEDIATOR, element);
                mediator.createDb();
            }
        }

        private void validateDatasource() throws Exception {
            Element element = qio.getElementStorage().getElements().get(Qio.DATASOURCE);
            if(element != null){
                DataSource dataSource = (DataSource) element.getElement();
                qio.setDataSource(dataSource);
            }
        }

        private void setQioDbAttributes() throws Exception {
            validateDatasource();
            initDb();
        }

        private void dispatchEvent() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            if(qio.getEvents() != null) {
                Method setupComplete = qio.getEvents().getClass().getDeclaredMethod("setupComplete", Qio.class);
                if(setupComplete != null) {
                    setupComplete.setAccessible(true);
                    setupComplete.invoke(qio.getEvents(), qio);
                }
            }
        }

        private void runElementsProcessor() throws Exception {
            ElementProcessor elementsProcessor = new ElementProcessor(qio).run();
            qio.setElementProcessor(elementsProcessor);
        }

        private void runConfigProcessor() throws Exception {
            if(qio.getElementProcessor().getConfigs() != null &&
                    qio.getElementProcessor().getConfigs().size() > 0){
                new ConfigurationProcessor(qio).run();
            }
        }

        private void runAnnotationProcessor() throws Exception {
            new AnnotationProcessor(qio).run();
        }

        private void runEndpointProcessor() throws Exception {
            EndpointProcessor endpointProcessor = new EndpointProcessor(qio).run();
            EndpointMappings endpointMappings = endpointProcessor.getMappings();
            qio.setEndpointMappings(endpointMappings);
        }

        private void runPropertiesProcessor() throws Exception {
            if(!qio.getPropertiesFiles().isEmpty()) {
                new PropertiesProcessor(qio).run();
            }
        }

        private void runInstanceProcessor() throws Exception {
            new InstanceProcessor(qio).run();
        }

        private void runProcessors() throws Exception {
            runPropertiesProcessor();
            runInstanceProcessor();
            runElementsProcessor();
            runConfigProcessor();
            runAnnotationProcessor();
            runEndpointProcessor();
        }

        private void sayReady(){
            String name = qio.getProjectName();
            command("[READY] " + name +"! : o . o . o . o . o . o . o . o . o . o . o . o  ");
        }

        public Initializer build() throws Exception{
            setQioAttributes();
            runProcessors();
            setQioDbAttributes();
            sayReady();
            dispatchEvent();
            return new Initializer();
        }
    }

}
