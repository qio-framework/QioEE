package qio.support;

import qio.Qio;
import qio.jdbc.BasicDataSource;
import org.h2.tools.RunScript;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.sql.Connection;

public class DbMediator {

    ServletContext context;

    public DbMediator(ServletContext context){
        this.context = context;
    }

    public Boolean createDb() throws Exception {
        String command = "java -jar " + System.getProperty("user.home") +
                File.separator + ".m2" +
                File.separator + "repository" +
                File.separator + "com" +
                File.separator + "h2database" +
                File.separator + "h2" +
                File.separator + "1.4.200" +
                File.separator +
                "h2-1.4.200.jar &";
        Runtime.getRuntime().exec(command);
        return true;
    }

    public Boolean dropDb() throws Exception {
        System.out.println("dropping db...");

        String artifactPath = Qio.Assistant.getPath();
        if(Qio.devMode) {
            File dropFile = new File(artifactPath +
                    File.separator + "qio" +
                    File.separator + "drop-db.sql");
            if(!dropFile.exists()){
                dropFile.createNewFile();
                FileWriter writer = new FileWriter(dropFile);
                writer.append("DROP ALL OBJECTS;");
                writer.flush();
                writer.close();
            }

            BasicDataSource dataSource = (BasicDataSource) Qio.z.get("datasource").getBean();
            Connection conn = dataSource.getConnection();
            RunScript.execute(conn, new FileReader(dropFile));
            conn.commit();
            conn.close();
        }
        return true;
    }

}


