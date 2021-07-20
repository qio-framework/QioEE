package qio.support;

import qio.Qio;
import qio.processor.ElementProcessor;
import qio.storage.ElementStorage;
import qio.storage.PropertyStorage;
import qio.jdbc.BasicDataSource;
import qio.model.Element;
import qio.processor.AnnotationProcessor;
import qio.processor.ConfigurationProcessor;
import qio.processor.EndpointProcessor;
import qio.model.web.HttpMappings;

public class Initializer {

    Qio qio;

    String[] resources;

    EndpointProcessor endpointProcessor;

    ElementStorage elementStorage;
    ElementProcessor elementProcessor;
    PropertyStorage propertyStorage;

    ConfigurationProcessor configurationProcessor;
    AnnotationProcessor annotationProcessor;

    public Initializer(Qio qio,
                       String[] resources,
                       ElementStorage elementStorage,
                       ElementProcessor elementProcessor,
                       PropertyStorage propertyStorage){
        this.qio = qio;
        this.elementStorage = elementStorage;
        this.elementProcessor = elementProcessor;
        this.propertyStorage = propertyStorage;
        this.resources = resources != null ? resources : new String[]{};
    }

    public Initializer init() throws Exception{

        Element qbean = new Element();
        qbean.setBean(qio);
        elementStorage.getBeans().put(Qio.QIO, qbean);
        Qio.set(elementStorage.getBeans());

        if(Qio.devMode)initializeDevelopmentDb();

        if(elementProcessor.getConfigs() != null &&
                elementProcessor.getConfigs().size() > 0){
            this.configurationProcessor = new ConfigurationProcessor(elementStorage, elementProcessor, propertyStorage);
            configurationProcessor.run();
        }

        this.annotationProcessor = new AnnotationProcessor(elementStorage, elementProcessor, propertyStorage);
        annotationProcessor.run();

        System.out.println(Qio.Assistant.SIGNATURE + " processing endpoints");
        this.endpointProcessor = new EndpointProcessor(elementStorage, elementProcessor);
        endpointProcessor.run();

        if(Qio.dataEnabled != null &&
                Qio.dataEnabled) {
            System.out.println(Qio.Assistant.SIGNATURE + " validating datasource");
            setDataSource();
        }

        setHttpMappings();

        System.out.println(Qio.Assistant.SIGNATURE + " project ready \u2713");
        System.out.println(Qio.Assistant.SIGNATURE + " Go to \033[1;33mhttp://localhost:8080" + Qio.servletContext.getContextPath() + "\033[0m port may differ\n\n\n\n\n");

        return this;
    }

    protected void setDataSource() throws Exception{
        Element element = elementStorage.getBeans().get(Qio.DATASOURCE);
        if(element == null){
            Qio.Injector.badge();
            throw new Exception("No data source configured... \nmake sure the method name in your config for your data source is named 'dataSource'\n\n\n\n\n");
        }
        BasicDataSource basicDataSource = (BasicDataSource) element.getBean();
        setDataSource(basicDataSource);
    }

    public void setDataSource(BasicDataSource basicDataSource) {
        qio.setDataSource(basicDataSource);
    }

    protected void setHttpMappings(){
        endpointProcessor = getHttpProcessor();
        HttpMappings httpMappings = endpointProcessor.getMappings();

        Qio.servletContext.setAttribute(Qio.HTTP_MAPPINGS, httpMappings);
        Qio.servletContext.setAttribute(Qio.HTTP_RESOURCES, resources);
        Qio.servletContext.setAttribute(Qio.QIO, Qio.z.get(Qio.QIO));
    }

    protected void initializeDevelopmentDb() throws Exception{
        DbMediator mediator = new DbMediator(Qio.servletContext);
        Element element = new Element();
        element.setBean(mediator);
        elementStorage.getBeans().put(Qio.DBMEDIATOR, element);
        createDb();
    }

    public static void createDb() throws Exception {
        DbMediator dbSupport = (DbMediator) Qio.z.get(Qio.DBMEDIATOR).getBean();
        dbSupport.createDb();
    }

    public EndpointProcessor getHttpProcessor() {
        return endpointProcessor;
    }


}
