package qio;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import qio.model.Element;
import qio.model.support.ObjectDetails;
import qio.model.web.EndpointMappings;
import qio.processor.ElementProcessor;
import qio.processor.EndpointProcessor;
import qio.storage.ElementStorage;
import qio.storage.PropertyStorage;
import qio.support.Initializer;

import javax.sql.DataSource;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Qio {

    public static final String QIO            = "qio";
    public static final String DBMEDIATOR     = "dbmediator";
    public static final String DATASOURCE     = "datasource";
    public static final String HTTP_RESOURCES = "qio-resources";
    public static final String HTTP_REDIRECT  = "[redirect]";
    public static final String QIO_REDIRECT   = "qio-redirect";
    public static final String BLACK          = "\033[0;30m";
    public static final String BLUE           = "\033[1;34m";
    public static final String PROCESS        = "        [+]  ";

    public void sign(){
        command("\n\n\n\n\n\n\n\n        //| " + Qio.BLUE + " Q" +
                Qio.BLACK + "io    \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\ \n");
    }

    Object events;
    ElementStorage elementStorage;
    DataSource dataSource;

    public static Map<String, Element> z;

    public Boolean devMode;
    public ServletContext servletContext;
    public String dbScript;

    List<String> resources;
    List<String> propertiesFiles;
    PropertyStorage propertyStorage;
    EndpointProcessor endpointProcessor;
    ElementProcessor elementProcessor;
    Map<String, ObjectDetails> objects;
    EndpointMappings endpointMappings;

    public Qio(ElementStorage elementStorage){
        this.elementStorage = elementStorage;
    }

    public Qio(Injector injector) throws Exception{
        this.dbScript = "create-db.sql";
        this.devMode = injector.devMode;
        this.servletContext = injector.servletContext;
        this.resources = injector.resources;
        this.propertiesFiles = injector.propertyFiles;
        this.elementStorage = new ElementStorage();
        this.propertyStorage = new PropertyStorage();
        this.objects = new HashMap<>();

        new Initializer.Builder()
                .withQio(this)
                .build();
    }

    public Object getElement(String name){
        String elementName = name.toLowerCase();
        if(elementStorage.getElements().containsKey(elementName)){
            return elementStorage.getElements().get(elementName).getElement();
        }
        return null;
    }

    public Map<String, Element> getElements(){
        return this.elementStorage.getElements();
    }

    public Object get(String preSql, Object[] params, Class<?> cls){
        Object result = null;
        String sql = "";
        try {
            sql = hydrateSql(preSql, params);
            Connection connection = dataSource.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if(rs.next()){
                result = extractData(rs, cls);
            }
            if(result == null){
                throw new Exception(cls + " not found using '" + sql + "'");
            }

            connection.commit();
            connection.close();

        } catch (SQLException ex) {
            sign();
            System.out.println("bad sql grammar : " + sql);
            System.out.println("\n\n\n");
            ex.printStackTrace();
        } catch (Exception ex) {}

        return result;
    }

    public Integer getInteger(String preSql, Object[] params){
        Integer result = null;
        String sql = "";
        try {
            sql = hydrateSql(preSql, params);
            Connection connection = dataSource.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if(rs.next()){
                result = Integer.parseInt(rs.getObject(1).toString());
            }

            if(result == null){
                throw new Exception("no results using '" + sql + "'");
            }

            connection.commit();
            connection.close();

        } catch (SQLException ex) {
            sign();
            System.out.println("bad sql grammar : " + sql);
            System.out.println("\n\n\n");
            ex.printStackTrace();
        } catch (Exception ex) {}

        return result;
    }

    public Long getLong(String preSql, Object[] params){
        Long result = null;
        String sql = "";
        try {
            sql = hydrateSql(preSql, params);
            Connection connection = dataSource.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            if(rs.next()){
                result = Long.parseLong(rs.getObject(1).toString());
            }

            if(result == null){
                throw new Exception("no results using '" + sql + "'");
            }

            connection.commit();
            connection.close();
        } catch (SQLException ex) {
            sign();
            System.out.println("bad sql grammar : " + sql);
            System.out.println("\n\n\n");
            ex.printStackTrace();
        } catch (Exception ex) {}

        return result;
    }

    public boolean save(String preSql, Object[] params){
        try {
            String sql = hydrateSql(preSql, params);
            Connection connection = dataSource.getConnection();
            Statement stmt = connection.createStatement();
            stmt.execute(sql);
            connection.commit();
            connection.close();
        }catch(Exception ex){
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public List<Object> getList(String preSql, Object[] params, Class cls){
        List<Object> results = new ArrayList<>();
        try {
            String sql = hydrateSql(preSql, params);
            Connection connection = dataSource.getConnection();
            Statement stmt = connection.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            results = new ArrayList<>();
            while(rs.next()){
                Object obj = extractData(rs, cls);
                results.add(obj);
            }
            connection.commit();
            connection.close();
        }catch(ClassCastException ccex){
            sign();
            System.out.println("");
            System.out.println("Wrong Class type, attempted to cast the return data as a " + cls);
            System.out.println("");
            ccex.printStackTrace();
        }catch (Exception ex){ ex.printStackTrace(); }
        return results;
    }

    public boolean update(String preSql, Object[] params){
        try {
            String sql = hydrateSql(preSql, params);
            Connection connection = dataSource.getConnection();
            Statement stmt = connection.createStatement();
            Boolean rs = stmt.execute(sql);
            connection.commit();
            connection.close();
        }catch(Exception ex){
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean delete(String preSql, Object[] params){
        try {
            String sql = hydrateSql(preSql, params);
            Connection connection = dataSource.getConnection();
            Statement stmt = connection.createStatement();
            stmt.execute(sql);
            connection.commit();
            connection.close();
        }catch(Exception ex){
            return false;
        }
        return true;
    }


    protected String hydrateSql(String sql, Object[] params){
        for(Object object : params){
            if(object != null) {
                String parameter = object.toString();
                if (object.getClass().getTypeName().equals("java.lang.String")) {
                    parameter = parameter.replace("'", "''")
                            .replace("$", "\\$")
                            .replace("#", "\\#")
                            .replace("@", "\\@");
                }
                sql = sql.replaceFirst("\\[\\+\\]", parameter);
            }else{
                sql = sql.replaceFirst("\\[\\+\\]", "null");
            }
        }
        return sql;
    }

    protected Object extractData(ResultSet rs, Class cls) throws Exception{
        Object object = new Object();
        Constructor[] constructors = cls.getConstructors();
        for(Constructor constructor: constructors){
            if(constructor.getParameterCount() == 0){
                object = constructor.newInstance();
            }
        }

        Field[] fields = object.getClass().getDeclaredFields();
        for(Field field: fields){
            field.setAccessible(true);
            String originalName = field.getName();
            String regex = "([a-z])([A-Z]+)";
            String replacement = "$1_$2";
            String name = originalName.replaceAll(regex, replacement).toLowerCase();
            Type type = field.getType();
            if (hasColumn(rs, name)) {
                if (type.getTypeName().equals("int") || type.getTypeName().equals("java.lang.Integer")) {
                    field.set(object, rs.getInt(name));
                } else if (type.getTypeName().equals("double") || type.getTypeName().equals("java.lang.Double")) {
                    field.set(object, rs.getDouble(name));
                } else if (type.getTypeName().equals("float") || type.getTypeName().equals("java.lang.Float")) {
                    field.set(object, rs.getFloat(name));
                } else if (type.getTypeName().equals("long") || type.getTypeName().equals("java.lang.Long")) {
                    field.set(object, rs.getLong(name));
                } else if (type.getTypeName().equals("boolean") || type.getTypeName().equals("java.lang.Boolean")) {
                    field.set(object, rs.getBoolean(name));
                } else if (type.getTypeName().equals("java.math.BigDecimal")) {
                    field.set(object, rs.getBigDecimal(name));
                } else if (type.getTypeName().equals("java.lang.String")) {
                    field.set(object, rs.getString(name));
                }
            }
        }
        return object;
    }

    public static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
        ResultSetMetaData rsmd = rs.getMetaData();
        for (int x = 1; x <= rsmd.getColumnCount(); x++) {
            if (columnName.equals(rsmd.getColumnName(x).toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public static Object hydrate(HttpServletRequest req, Class cls){
        Object object =  null;
        try {
            object = cls.getConstructor().newInstance();
            Field[] fields = cls.getDeclaredFields();
            for(Field field : fields){

                String value = req.getParameter(field.getName());
                if(value != null &&
                        !value.equals("")){

                    field.setAccessible(true);

                    Type type = field.getType();

                    if (type.getTypeName().equals("int") || type.getTypeName().equals("java.lang.Integer")) {
                        field.set(object, Integer.parseInt(value));
                    }
                    else if (type.getTypeName().equals("double") || type.getTypeName().equals("java.lang.Double")) {
                        field.set(object, Double.parseDouble(value));
                    }
                    else if (type.getTypeName().equals("float") || type.getTypeName().equals("java.lang.Float")) {
                        field.set(object, Float.parseFloat(value));
                    }
                    else if (type.getTypeName().equals("long") || type.getTypeName().equals("java.lang.Long")) {
                        field.set(object, Long.parseLong(value));
                    }
                    else if (type.getTypeName().equals("boolean") || type.getTypeName().equals("java.lang.Boolean")) {
                        field.set(object, Boolean.getBoolean(value));
                    }
                    else if (type.getTypeName().equals("java.math.BigDecimal")) {
                        field.set(object, new BigDecimal(value));
                    }
                    else if (type.getTypeName().equals("java.lang.String")) {
                        field.set(object, value);
                    }
                }
            }
        }catch(Exception ex){
            ex.printStackTrace();
        }

        return object;
    }

    public static boolean set(Map<String, Element> elements){
        Qio.z = elements;
        return true;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public static class Injector{

        Boolean devMode;
        List<String> resources;
        List<String> propertyFiles;
        ServletContext servletContext;

        public Injector(){}
        public Qio.Injector setDevEnv(boolean devMode){
            this.devMode = devMode;
            return this;
        }
        public Qio.Injector withPropertyFiles(List<String> propertyFiles){
            this.propertyFiles = propertyFiles;
            return this;
        }
        public Qio.Injector withContext(ServletContext servletContext){
            this.servletContext = servletContext;
            return this;
        }
        public Qio.Injector withWebResources(List<String> resources){
            this.resources = resources;
            return this;
        }

        public Qio inject() throws Exception{
            return new Qio(this);
        }
    }

    public boolean inDevMode(){
        return this.devMode;
    }
    public ServletContext getServletContext(){
        return this.servletContext;
    }
    public String getDbScript(){
        return this.dbScript;
    }
    public String getDbUrl() throws Exception {
        if(propertyStorage.getProperties().containsKey("db.url")){
            return propertyStorage.getProperties().get("db.url");
        }

        if(inDevMode() &&
                !propertyStorage.getProperties().containsKey("db.url")){
            throw new Exception("\n\n           Reminder, in order to be in dev mode \n" +
                    "           you need to configure a datasource.\n\n\n");
        }

        throw new Exception("           \n\ndb.url is missing from qio.props file\n\n\n");

    }
    public Object getEvents(){
        return this.events;
    }
    public void setEvents(Object events){
        this.events = events;
    }
    public List<String> getResources(){
        return this.resources;
    }
    public void setResources(List<String> resources){
        this.resources = resources;
    }
    public List<String> getPropertiesFiles(){
        return this.propertiesFiles;
    }
    public void setPropertiesFiles(List<String> propertiesFiles){
        this.propertiesFiles = propertiesFiles;
    }
    public ElementStorage getElementStorage(){
        return this.elementStorage;
    }
    public PropertyStorage getPropertyStorage(){
        return this.propertyStorage;
    }
    public EndpointProcessor getEndpointProcessor(){
        return this.endpointProcessor;
    }
    public void setEndpointProcessor(EndpointProcessor endpointProcessor){
        this.endpointProcessor = endpointProcessor;
    }
    public ElementProcessor getElementProcessor(){
        return this.elementProcessor;
    }
    public void setElementProcessor(ElementProcessor elementProcessor){
        this.elementProcessor = elementProcessor;
    }
    public EndpointMappings getEndpointMappings() {
        return endpointMappings;
    }
    public void setEndpointMappings(EndpointMappings endpointMappings) {
        this.endpointMappings = endpointMappings;
    }
    public Map<String, ObjectDetails> getObjects() {
        return this.objects;
    }
    public void setObjects(Map<String, ObjectDetails> objects) {
        this.objects = objects;
    }

    public static void command(String command){
        try {
            System.out.println(new String(command.getBytes(), "UTF-8"));
        }catch(UnsupportedEncodingException ueex){
            ueex.printStackTrace();
        }
    }

    public String getResourceUri() throws Exception{
        return getResourceUri(servletContext);
    }

    public static String getResourceUri(ServletContext servletContext) throws Exception{
        String resourceUri = Paths.get("src", "main", "resources")
                .toAbsolutePath()
                .toString();
        File resourceDir = new File(resourceUri);
        if(resourceDir.exists()){
            return resourceUri;
        }
        String classesUri = Paths.get("webapps", servletContext.getContextPath(), "WEB-INF", "classes")
                .toAbsolutePath()
                .toString();
        File classesDir = new File(classesUri);
        if(classesDir.exists()) {
            return classesUri;
        }
        throw new Exception("Qio : unable to locate resource path");
    }

    public String getClassesUri() throws Exception{
        String classesUri = Paths.get("webapps", getServletContext().getContextPath(), "WEB-INF", "classes")
                .toAbsolutePath()
                .toString();
        File classesDir = new File(classesUri);
        if(classesDir.exists()){
            return classesUri;
        }

        classesUri = Paths.get("src", "main", "java")
                .toAbsolutePath()
                .toString();
        classesDir = new File(classesUri);
        if(classesDir.exists()){
            return classesUri;
        }
        throw new Exception("Qio : unable to locate class uri");
    }

    public static String getTypeName(String typeName) {
        int index = typeName.lastIndexOf(".");
        if(index > 0){
            typeName = typeName.substring(index + 1);
        }
        return typeName;
    }

    public static String getName(String nameWithExt){
        int index = nameWithExt.lastIndexOf(".");
        String qualifiedName = nameWithExt;
        if(index > 0){
            qualifiedName = qualifiedName.substring(index + 1);
        }
        return qualifiedName.toLowerCase();
    }

    public static <T> Collector<T, ?, T> toSingleton() {
        return Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if (list.size() != 1) {
                        throw new IllegalStateException();
                    }
                    return list.get(0);
                }
        );
    }

}
