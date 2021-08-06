package qio.support;

import qio.Qio;
import qio.model.support.ObjectDetails;
import qio.processor.*;
import qio.storage.ElementStorage;
import qio.storage.PropertyStorage;
import qio.jdbc.BasicDataSource;
import qio.model.Element;
import qio.model.web.EndpointMappings;

import javax.servlet.ServletContext;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import static qio.Qio.SIGNATURE;

public class Initializer {

    public Initializer(){}

    public static class Builder {

        Qio qio;
        Object qioEvents;
        String[] resources;
        ElementStorage elementStorage;
        ElementProcessor elementProcessor;
        PropertyStorage propertyStorage;

        String[] propertyFiles;

        Map<String, ObjectDetails> classes;
        EndpointProcessor endpointProcessor;
        AnnotationProcessor annotationProcessor;
        ConfigurationProcessor configurationProcessor;

        public Builder(){
            this.classes = new HashMap<>();
        }

        public Builder withQio(Qio qio){
            this.qio = qio;
            return this;
        }
        public Builder withResources(String[] resources){
            this.resources = resources;
            return this;
        }
        public Builder withElementStorage(ElementStorage elementStorage){
            this.elementStorage = elementStorage;
            return this;
        }
        public Builder withElementProcessor(ElementProcessor elementProcessor){
            this.elementProcessor = elementProcessor;
            return this;
        }
        public Builder withPropertyStorage(PropertyStorage propertyStorage){
            this.propertyStorage = propertyStorage;
            return this;
        }

        private void sayHello(){
            System.out.println("\n\n");
            System.out.println("                 ----- ");
            System.out.println("              ( " + Qio.BLUE + "  Qio " + Qio.BLACK + "  )");
            System.out.println("                 ----- ");

            System.out.println("\n" + Qio.SIGNATURE + " beginning setup");
        }

        private void runPropertiesProcessor() throws Exception {
            PropertiesProcessor propertiesProcessor = new PropertiesProcessor.Builder()
                    .withFiles(qio.getPropertiesFiles())
                    .withQio(qio)
                    .process();
            this.propertyStorage = propertiesProcessor.getPropertiesData();
        }

        private void runInstanceProcessor() throws Exception {
            InstanceProcessor instanceProcessor = new InstanceProcessor.Builder()
                    .withQio(qio)
                    .build();
            this.classes = instanceProcessor.getClasses();
        }

        private void runElementsProcessor() throws Exception {
            this.elementProcessor = new ElementProcessor.Builder()
                    .withClasses(classes)
                    .withElementData(elementStorage)
                    .prepare()
                    .build();
        }

        private void setQioElement(){
            Element qioElement = new Element();
            qioElement.setElement(qio);
            elementStorage.getElements().put(Qio.QIO, qioElement);
            Qio.set(elementStorage.getElements());
            qio.getServletContext().setAttribute(Qio.QIO, qioElement);
        }

        private void setHttpResources(){
            qio.getServletContext().setAttribute(Qio.HTTP_RESOURCES, resources);
        }
        
        private void checkInitDevDb() throws Exception{
            if (qio.inDevMode()){
                DbMediator mediator = new DbMediator(qio);
                Element element = new Element();
                element.setElement(mediator);
                elementStorage.getElements().put(Qio.DBMEDIATOR, element);
                mediator.createDb();
            }
        }

        private void runConfigProcessor() throws Exception {
            if(elementProcessor.getConfigs() != null &&
                    elementProcessor.getConfigs().size() > 0){
                configurationProcessor = new ConfigurationProcessor(elementStorage, elementProcessor, propertyStorage);
                configurationProcessor.run();
            }
        }

        private void runAnnotationProcessor() throws Exception {
            annotationProcessor = new AnnotationProcessor(elementStorage, elementProcessor, propertyStorage);
            annotationProcessor.run();
        }

        private void runEndpointProcessor() throws Exception {
            System.out.println(SIGNATURE + " processing endpoints");
            endpointProcessor = new EndpointProcessor(elementStorage, elementProcessor);
            endpointProcessor.run();
        }

        private void validateDatasource() throws Exception {
            System.out.println(SIGNATURE + " validating datasource");
            Element element = elementStorage.getElements().get(Qio.DATASOURCE);
            if(element != null){
                BasicDataSource basicDataSource = (BasicDataSource) element.getElement();
                qio.setDataSource(basicDataSource);
            }else{
                System.out.println("    - data source validated as 'none'");
            }
        }

        protected void setHttpMappings(){
            EndpointMappings endpointMappings = endpointProcessor.getMappings();
            qio.getServletContext().setAttribute(Qio.ENDPOINT_MAPPINGS, endpointMappings);
        }

        private void sayReady(){
            System.out.println(SIGNATURE + " project ready \u2713");
            System.out.println(SIGNATURE + " Go to \033[1;33mhttp://localhost:8080" + qio.getServletContext().getContextPath() + "\033[0m port may differ\n\n\n\n\n");
        }

        private void validateResources(){
            if(resources == null) resources = new String[]{};
        }

        private void setQioAttributes(){
            setQioElement();
            setHttpResources();
            validateResources();
        }

        private void setQioDbAttributes() throws Exception {
            validateDatasource();
            checkInitDevDb();
        }

        private void dispatchEvent() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
            if(qioEvents != null) {
                Method setupComplete = qioEvents.getClass().getDeclaredMethod("setupComplete", Qio.class);
                if(setupComplete != null) {
                    setupComplete.setAccessible(true);
                    setupComplete.invoke(qioEvents, qio);
                }
            }
        }

        public Builder initialize() throws Exception {

            sayHello();
            runPropertiesProcessor();
            runInstanceProcessor();
            runElementsProcessor();

            setQioAttributes();
            runConfigProcessor();
            runAnnotationProcessor();
            runEndpointProcessor();
            setQioDbAttributes();
            setHttpMappings();

            sayReady();
            dispatchEvent();

            return this;
        }

        public Initializer build() {
            return new Initializer();
        }
    }

}
