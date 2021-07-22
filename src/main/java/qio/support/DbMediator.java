package qio.support;

import qio.Qio;
import qio.jdbc.BasicDataSource;
import org.h2.tools.RunScript;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;

public class DbMediator {

    ServletContext context;

    public DbMediator(ServletContext context){
        this.context = context;
    }

    public Boolean createDb() throws Exception {

        String artifactPath = Qio.Assistant.getPath();
        if(Qio.devMode) {
            File createFile = new File(artifactPath +
                    File.separator + "qio" +
                    File.separator + "create-db.sql");

            BasicDataSource dataSource = (BasicDataSource) Qio.z.get(Qio.DATASOURCE).getBean();
            Connection conn = dataSource.getConnection();
            RunScript.execute(conn, new FileReader(createFile));
            conn.commit();
            conn.close();
        }

        return true;
    }

    public Boolean dropDb() throws Exception {
        System.out.println("cleaning db...");

        if(Qio.devMode) {
            BasicDataSource dataSource = (BasicDataSource) Qio.z.get(Qio.DATASOURCE).getBean();
            String[] dbParts = dataSource.getDbUrl().split("jdbc:h2:");

            String dbPath = dbParts[1];
            if (dbPath.startsWith("~" + File.separator)) {
                dbPath = System.getProperty("user.home") + dbPath.substring(1);
            }

            File dbMvFile = new File(dbPath + ".mv.db");
            if(dbMvFile.exists()){
                dbMvFile.delete();
            }
            File dbTraceFile = new File(dbPath + ".trace.db");
            if(dbTraceFile.exists()){
                dbTraceFile.delete();
            }
        }
        return true;
    }

}


