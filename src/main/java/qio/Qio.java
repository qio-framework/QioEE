package qio;

import qio.storage.ElementStorage;
import qio.processor.ElementProcessor;
import qio.storage.PropertyStorage;
import qio.jdbc.BasicDataSource;
import qio.model.Element;
import qio.model.support.ObjectDetails;
import qio.processor.InstanceProcessor;
import qio.processor.PropertiesProcessor;
import qio.support.Initializer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class Qio {

    public static final String QIO            = "qio";
    public static final String DBMEDIATOR     = "dbmediator";
    public static final String DATASOURCE     = "datasource";
    public static final String HTTP_RESOURCES = "qio-resources";
    public static final String HTTP_MAPPINGS  = "qio-mappings";
    public static final String HTTP_REDIRECT  = "[redirect]";
    public static final String QIO_REDIRECT   = "qio-redirect";

    ElementStorage elementStorage;

    BasicDataSource basicDataSource;

    public static Map<String, Element> z;

    public static Boolean devMode;
    public static Boolean runEmbedded;
    public static Boolean dataEnabled;

    public static ServletContext servletContext;

    String[] resources;

    ElementProcessor elementProcessor;
    PropertyStorage propertyStorage;

    public Qio(ElementStorage elementStorage){
        this.elementStorage = elementStorage;
    }

    public Qio(Injector injector) throws Exception{

        Qio.devMode = injector.devMode;
        Qio.runEmbedded = injector.runEmbedded;
        Qio.dataEnabled = injector.dataEnabled;
        Qio.servletContext = injector.servletContext;

        this.resources = injector.resources;
        this.elementStorage = injector.elementStorage;
        this.elementProcessor = injector.elementProcessor;
        this.propertyStorage = injector.propertyStorage;

        new Initializer.Builder()
                .withQio(this)
                .withResources(resources)
                .withElementStorage(elementStorage)
                .withElementProcessor(elementProcessor)
                .withPropertyStorage(propertyStorage)
                .initialize()
                .build();
    }

    public Object getBean(String name){
        String beanName = name.toLowerCase();
        if(elementStorage.getBeans().containsKey(beanName)){
            return elementStorage.getBeans().get(beanName).getBean();
        }
        return null;
    }

    public Map<String, Element> getBeans(){
        return this.elementStorage.getBeans();
    }

    public Object get(String preSql, Object[] params, Class<?> cls){
        Object result = null;
        try {
            String sql = hydrateSql(preSql, params);
            Connection connection = basicDataSource.getConnection();
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

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public Integer getInteger(String preSql, Object[] params){
        Integer result = null;
        try {
            String sql = hydrateSql(preSql, params);
            Connection connection = basicDataSource.getConnection();
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

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public Long getLong(String preSql, Object[] params){
        Long result = null;
        try {
            String sql = hydrateSql(preSql, params);
            Connection connection = basicDataSource.getConnection();
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
        }catch(Exception ex){
            ex.printStackTrace();
        }
        return result;
    }

    public boolean save(String preSql, Object[] params){
        try {
            String sql = hydrateSql(preSql, params);
            Connection connection = basicDataSource.getConnection();
            Statement stmt = connection.createStatement();
            Boolean saved = stmt.execute(sql);
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
            Connection connection = basicDataSource.getConnection();
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
            Qio.Injector.badge();
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
            Connection connection = basicDataSource.getConnection();
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
            Connection connection = basicDataSource.getConnection();
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
                if (type.getTypeName().equals("int")) {
                    field.set(object, rs.getInt(name));
                } else if (type.getTypeName().equals("double")) {
                    field.set(object, rs.getDouble(name));
                } else if (type.getTypeName().equals("float")) {
                    field.set(object, rs.getFloat(name));
                } else if (type.getTypeName().equals("long")) {
                    field.set(object, rs.getLong(name));
                } else if (type.getTypeName().equals("boolean")) {
                    field.set(object, rs.getBoolean(name));
                } else if (type.getTypeName().equals("java.lang.Integer")) {
                    field.set(object, rs.getInt(name));
                } else if (type.getTypeName().equals("java.lang.Double")) {
                    field.set(object, rs.getDouble(name));
                }  else if (type.getTypeName().equals("java.lang.Float")) {
                    field.set(object, rs.getFloat(name));
                } else if (type.getTypeName().equals("java.lang.Long")) {
                    field.set(object, rs.getLong(name));
                } else if (type.getTypeName().equals("java.math.BigDecimal")) {
                    field.set(object, rs.getBigDecimal(name));
                } else if (type.getTypeName().equals("java.lang.Boolean")) {
                    field.set(object, rs.getBoolean(name));
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

                    if (type.getTypeName().equals("int") ||
                            type.getTypeName().equals("java.lang.Integer")) {
                        field.set(object, Integer.parseInt(value));
                    }
                    else if (type.getTypeName().equals("double") ||
                            type.getTypeName().equals("java.lang.Double")) {
                        field.set(object, Double.parseDouble(value));
                    }
                    else if (type.getTypeName().equals("float") ||
                            type.getTypeName().equals("java.lang.Float")) {
                        field.set(object, Float.parseFloat(value));
                    }
                    else if (type.getTypeName().equals("long") ||
                            type.getTypeName().equals("java.lang.Long")) {
                        field.set(object, Long.parseLong(value));
                    }
                    else if (type.getTypeName().equals("boolean") ||
                            type.getTypeName().equals("java.lang.Boolean")) {
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

    public static boolean set(Map<String, Element> beans){
        Qio.z = beans;
        return true;
    }

    public void setDataSource(BasicDataSource basicDataSource) {
        this.basicDataSource = basicDataSource;
    }

    public static class Injector{

        Boolean devMode;
        Boolean runEmbedded;
        Boolean dataEnabled;
        String[] resources;
        String[] propertyFiles;
        String dependencyFile;
        ElementStorage elementStorage;
        ServletContext servletContext;

        ElementProcessor elementProcessor;
        PropertyStorage propertyStorage;

        public Injector(){
            this.devMode = false;
            this.runEmbedded = false;
            this.dataEnabled = false;
            this.elementStorage = new ElementStorage();
            this.propertyStorage = new PropertyStorage();
        }

        public Qio.Injector asEmbedded(Boolean runEmbedded){
            this.runEmbedded = runEmbedded;
            return this;
        }

        public Qio.Injector dependencyFile(String dependencyFile){
            this.dependencyFile = dependencyFile;
            return this;
        }

        public Qio.Injector withPropertyFiles(String[] propertyFiles){
            this.propertyFiles = propertyFiles;
            return this;
        }

        public Qio.Injector devMode(boolean devMode){
            this.devMode = devMode;
            return this;
        }

        public Qio.Injector withDataEnabled(boolean dataEnabled){
            this.dataEnabled = dataEnabled;
            return this;
        }

        public Qio.Injector withContext(ServletContext servletContext){
            this.servletContext = servletContext;
            return this;
        }

        public Qio.Injector withWebResources(String[] resources){
            this.resources = resources;
            return this;
        }

        public static void badge(){


            System.out.println(Assistant.BLACK);
            System.out.println("               \n\n\n");
            System.out.println("                 ----- ");
            System.out.println("              ( " + Assistant.BLUE + "  Qio " + Assistant.BLACK + "  )");
            System.out.println("                 ----- ");
            System.out.println("           \n\n\n");
            System.out.println(Assistant.BLACK);

//            System.out.println("               \n\n\n");
//            System.out.println("===========================================");
//            System.out.println("===========================================");
//            System.out.println("               \n");
//            System.out.println(Assistant.BLUE + "       8888 ");
//            System.out.println(Assistant.BLUE + "    8888   8888");
//            System.out.println(Assistant.BLUE + "  8888      8888 " + Assistant.BLACK + "888    888");
//            System.out.println(Assistant.BLUE + "  8888      8888 " + Assistant.BLACK + "888  888 888");
//            System.out.println(Assistant.BLUE + "  8888      8888 " + Assistant.BLACK + "    888    888");
//            System.out.println(Assistant.BLUE + "   8888   8888   " + Assistant.BLACK + "888 888    888");
//            System.out.println(Assistant.BLUE + "     88888888    " + Assistant.BLACK + "888  888  888");
//            System.out.println(Assistant.BLUE + "           888   " + Assistant.BLACK + "888    8888");
//            System.out.println("               \n");
//            System.out.println("===========================================");
//            System.out.println("===========================================");
//            System.out.println("               \n\n\n");
        }

        public Qio inject() throws Exception{

            System.out.println("\n\n");
            System.out.println("                 ----- ");
            System.out.println("              ( " + Qio.Assistant.BLUE + "  Qio " + Qio.Assistant.BLACK + "  )");
            System.out.println("                 ----- ");

            System.out.println("\n" + Qio.Assistant.SIGNATURE + " beginning setup");

            PropertiesProcessor propertiesProcessor = new PropertiesProcessor.Builder()
                    .asEmbedded(runEmbedded)
                    .withFiles(propertyFiles)
                    .withContext(servletContext)
                    .process();

            this.propertyStorage = propertiesProcessor.getPropertiesData();

            InstanceProcessor processor = new InstanceProcessor.Builder()
                    .asEmbedded(runEmbedded)
                    .withContext(servletContext)
                    .build();

            Map<String, ObjectDetails> classes = processor.getClasses();

            this.elementProcessor = new ElementProcessor.Builder()
                    .withClasses(classes)
                    .withBeanData(elementStorage)
                    .prepare()
                    .build();
            return new Qio(this);
        }
    }

    public static class Assistant {

        public static final String BLACK = "\033[0;30m";
        public static final String BLUE = "\033[1;34m";
        public static final String SIGNATURE = "       +  ";

        public static String removeLast(String s) {
            return (s == null) ? null : s.replaceAll(".$", "");
        }

        public static String getMain() {
            for (final Map.Entry<String, String> entry : System.getenv().entrySet())
                if (entry.getKey().startsWith("JAVA_MAIN_CLASS")) // like JAVA_MAIN_CLASS_13328
                    return entry.getValue();
            throw new IllegalStateException("Cannot determine main class.");
        }

        public static String getPath(){
            if(Qio.runEmbedded) {
                return Paths.get("src", "main", "resources")
                        .toAbsolutePath()
                        .toString();
            }else{
                return Paths.get("webapps", servletContext.getContextPath(), "WEB-INF", "classes")
                        .toAbsolutePath()
                        .toString();
            }
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

}
