package qio.support;

import qio.Qio;
import qio.jdbc.BasicDataSource;
import org.h2.tools.RunScript;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;

public class DbMediator {

    Qio qio;

    public DbMediator(Qio qio){
        this.qio = qio;
    }

    public Boolean createDb() throws Exception {

        String artifactPath = qio.getResourceUri();

        File createFile = new File(artifactPath + File.separator + "create-db.sql");

        BasicDataSource dataSource = (BasicDataSource) Qio.z.get(Qio.DATASOURCE).getElement();
        Connection conn = dataSource.getConnection();
        RunScript.execute(conn, new FileReader(createFile));
        conn.commit();
        conn.close();

        return true;
    }

    public Boolean dropDb() {
        System.out.println("\n\n");
        System.out.println("");
        System.out.println("               " + Qio.BLUE + "  Qio " + Qio.BLACK);
        System.out.println("                ----- ");
        System.out.println("           cleaning dev env...");
        System.out.println("\n\n\n\n");

        if(qio.inDevMode()) {
            BasicDataSource dataSource = (BasicDataSource) Qio.z.get(Qio.DATASOURCE).getElement();
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


