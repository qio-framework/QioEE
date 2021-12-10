package qio.support;

import qio.Qio;
import org.h2.tools.RunScript;
import qio.model.Element;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.util.Set;

import static qio.Qio.DATASOURCE;
import static qio.Qio.command;

public class DbMediator {

    Qio qio;

    public DbMediator(Qio qio){
        this.qio = qio;
    }

    public Boolean createDb() throws Exception {

        String artifactPath = Qio.getResourceUri(qio.servletContext);
        File createFile = new File(artifactPath + File.separator + qio.getDbScript());

        DataSource datasource = (DataSource) qio.getElement(DATASOURCE);

        if(datasource == null){
            command("\n");
            throw new Exception("\n\n           " +
                    "You have qio.dev set to true in qio.props.\n           " +
                    "In addition you need to configure a datasource. \n           " +
                    "Feel free to use qio.jdbc.BasicDataSource to " +
                    "get started.\n" +
                    "           " +
                    "You can also checkout HikariCP, it is pretty good!" +
                    "\n\n" +
                    "           https://github.com/brettwooldridge/HikariCP\n\n\n");
        }
        Connection conn = datasource.getConnection();
        RunScript.execute(conn, new StringReader("drop all objects;"));
        RunScript.execute(conn, new FileReader(createFile));
        conn.commit();
        conn.close();

        return true;
    }

    public Boolean dropDb() {
        command("\n\n        //| " + Qio.BLUE + " Q" +
                Qio.BLACK + "io  cleaning dev env...\n");

        try {

            DataSource datasource = (DataSource) qio.getElement(DATASOURCE);
            Connection conn = datasource.getConnection();

            RunScript.execute(conn, new StringReader("drop all objects;"));
            conn.commit();
            conn.close();

        } catch (Exception e) {}

        return true;
    }

}

