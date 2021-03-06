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
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Qio {

    public static final String QIO            = "qio";
    public static final String DBMEDIATOR     = "dbmediator";
    public static final String DATASOURCE     = "datasource";
    public static final String HTTP_RESOURCES = "qio-resources";
    public static final String HTTP_REDIRECT  = "[redirect]";
    public static final String QIO_REDIRECT   = "qio-redirect";
    public static final String RESOURCES      = "/src/main/resources/";
    public static final String BLACK          = "\033[0;30m";
    public static final String BLUE           = "\033[1;34m";
    public static final String PROCESS        = "        [+]  ";

    public void sign(){
        command("\n\n\n\n\n\n\n\n        //| " + Qio.BLUE + " Q" +
                Qio.BLACK + "io    \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\ \n");
    }

    Boolean fatJar;
    Object events;
    ElementStorage elementStorage;
    DataSource dataSource;

    public static Map<String, Element> z;

    public Boolean createDb;
    public Boolean dropDb;
    public Boolean isBasic;
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

    public Qio(Injector injector) throws Exception {
        this.dbScript = "create-db.sql";
        this.isBasic = injector.isBasic;
        this.createDb = injector.createDb;
        this.dropDb = injector.dropDb;
        this.servletContext = injector.servletContext;
        this.resources = injector.resources;
        this.propertiesFiles = injector.propertyFiles;
        this.elementStorage = new ElementStorage();
        this.propertyStorage = new PropertyStorage();
        this.objects = new HashMap<>();
        this.fatJar = getFatJar();
        new Initializer.Builder()
                .withQio(this)
                .build();
    }

    public Boolean isJar(){
        return this.fatJar;
    }

    private Boolean getFatJar(){
        String uri = null;
        try {
            uri = getClassesUri();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uri.contains("jar:file:") ? true : false;
    }

    public Object getElement(String name){
        String key = name.toLowerCase();
        if(elementStorage.getElements().containsKey(key)){
            return elementStorage.getElements().get(key).getElement();
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

    public Object set(HttpServletRequest req, Class cls){
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
                        field.set(object, Integer.valueOf(value));
                    }
                    else if (type.getTypeName().equals("double") || type.getTypeName().equals("java.lang.Double")) {
                        field.set(object, Double.valueOf(value));
                    }
                    else if (type.getTypeName().equals("float") || type.getTypeName().equals("java.lang.Float")) {
                        field.set(object, Float.valueOf(value));
                    }
                    else if (type.getTypeName().equals("long") || type.getTypeName().equals("java.lang.Long")) {
                        field.set(object, Long.valueOf(value));
                    }
                    else if (type.getTypeName().equals("boolean") || type.getTypeName().equals("java.lang.Boolean")) {
                        field.set(object, Boolean.valueOf(value));
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

    public static Object get(HttpServletRequest req, Class cls){
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
                        field.set(object, Integer.valueOf(value));
                    }
                    else if (type.getTypeName().equals("double") || type.getTypeName().equals("java.lang.Double")) {
                        field.set(object, Double.valueOf(value));
                    }
                    else if (type.getTypeName().equals("float") || type.getTypeName().equals("java.lang.Float")) {
                        field.set(object, Float.valueOf(value));
                    }
                    else if (type.getTypeName().equals("long") || type.getTypeName().equals("java.lang.Long")) {
                        field.set(object, Long.valueOf(value));
                    }
                    else if (type.getTypeName().equals("boolean") || type.getTypeName().equals("java.lang.Boolean")) {
                        field.set(object, Boolean.valueOf(value));
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
                        field.set(object, Integer.valueOf(value));
                    }
                    else if (type.getTypeName().equals("double") || type.getTypeName().equals("java.lang.Double")) {
                        field.set(object, Double.valueOf(value));
                    }
                    else if (type.getTypeName().equals("float") || type.getTypeName().equals("java.lang.Float")) {
                        field.set(object, Float.valueOf(value));
                    }
                    else if (type.getTypeName().equals("long") || type.getTypeName().equals("java.lang.Long")) {
                        field.set(object, Long.valueOf(value));
                    }
                    else if (type.getTypeName().equals("boolean") || type.getTypeName().equals("java.lang.Boolean")) {
                        field.set(object, Boolean.valueOf(value));
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

        Boolean isBasic;
        Boolean createDb;
        Boolean dropDb;
        List<String> resources;
        List<String> propertyFiles;
        ServletContext servletContext;

        public Injector(){}
        public Qio.Injector setBasic(boolean isBasic){
            this.isBasic = isBasic;
            return this;
        }
        public Qio.Injector setCreateDb(boolean createDb){
            this.createDb = createDb;
            return this;
        }
        public Qio.Injector setDropDb(boolean dropDb){
            this.dropDb = dropDb;
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

        public Qio inject() throws Exception {
            return new Qio(this);
        }

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

        if(!isBasic &&
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

        final String RESOURCES_URI = "/src/main/resources/";
        URL indexUri = Qio.class.getResource(RESOURCES_URI);
        if (indexUri == null) {
            throw new FileNotFoundException("Qio : unable to find resource " + RESOURCES_URI);
        }

        return indexUri.toURI().toString();

    }

    public String getClassesUri() throws Exception {
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

        classesUri = this.getClass().getResource("").toURI().toString();
        if(classesUri == null){
            throw new Exception("Qio : unable to locate class uri");
        }
        return classesUri;
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

    public String getMain(){
        try {
            JarFile jarFile = getJarFile();
            JarEntry jarEntry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
            InputStream in = jarFile.getInputStream(jarEntry);
            Scanner scanner = new Scanner(in);

            String line = "";
            do{
                line = scanner.nextLine();
                if(line.contains("Main-Class")){
                    line = line.replace("Main-Class", "");
                    break;
                }
            }
            while(scanner.hasNext());

            line = line.replace(":", "").trim();
            return line;

        } catch (IOException ioex) {
            ioex.printStackTrace();
        }


        throw new IllegalStateException("Apologies, it seems you are trying to run this as a jar but have not main defined.");
    }

    public Enumeration getJarEntries(){
        JarFile jarFile = getJarFile();
        return jarFile.entries();
    }

    public JarFile getJarFile(){
        try {
            URL jarUri = Qio.class.getClassLoader().getResource("qio/");
            String jarPath = jarUri.getPath().substring(5, jarUri.getPath().indexOf("!"));

            return new JarFile(jarPath);
        }catch(IOException ex){
            ex.printStackTrace();
        }
        return null;
    }


    public static Enumeration getEntries(){
        try {
            URL jarUriTres = Qio.class.getClassLoader().getResource("qio/");
            String jarPath = jarUriTres.getPath().substring(5, jarUriTres.getPath().indexOf("!"));

            return new JarFile(jarPath).entries();
        }catch(IOException ex){
            ex.printStackTrace();
        }
        return null;
    }

    public String getProjectName() {
        if(isJar()) {
            JarFile jarFile = getJarFile();
            String path = jarFile.getName();
            String[] bits = path.split("/");
            if(bits.length == 0){
                bits = path.split("\\");
            }
            String namePre = bits[bits.length - 1];
            return namePre.replace(".jar", "");
        }else{
            ServletContext context = getServletContext();
            return context.getServletContextName();
        }
    }

    public StringBuilder convert(InputStream in){
        StringBuilder builder = new StringBuilder();
        Scanner scanner = new Scanner(in);
        do{
            builder.append(scanner.nextLine());
        }while(scanner.hasNext());
        try {
            in.close();
        }catch (IOException ioex) {
            ioex.printStackTrace();
        }
        return builder;
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
