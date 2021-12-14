package qio.support;

import qio.Qio;
import org.h2.tools.RunScript;
import qio.model.Element;

import javax.sql.DataSource;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static qio.Qio.DATASOURCE;
import static qio.Qio.command;

public class DbMediator {

    Qio qio;

    final String CREATEDB_URI = "src/main/resources/create-db.sql";

    public DbMediator(Qio qio){
        this.qio = qio;
    }

    public Boolean createDb() throws Exception {

        String artifactPath = Qio.getResourceUri(qio.servletContext);

        if(!qio.isBasic &&
                qio.createDb) {

            StringBuilder createSql;
            if (qio.isJar()) {
                JarFile jarFile = qio.getJarFile();
                JarEntry jarEntry = jarFile.getJarEntry(CREATEDB_URI);
                InputStream in = jarFile.getInputStream(jarEntry);
                createSql = qio.convert(in);
            } else {
                File createFile = new File(artifactPath + File.separator + qio.getDbScript());
                InputStream in = new FileInputStream(createFile);
                createSql = qio.convert(in);
            }

            DataSource datasource = (DataSource) qio.getElement(DATASOURCE);

            if (datasource == null) {
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

            if(qio.dropDb) {
                RunScript.execute(conn, new StringReader("drop all objects;"));
            }

            RunScript.execute(conn, new StringReader(createSql.toString()));
            conn.commit();
            conn.close();
        }

        return true;
    }

    public Boolean dropDb() {

        if(!qio.isBasic &&
                qio.dropDb) {

            command("\n\n        //| " + Qio.BLUE + " Q" +
                    Qio.BLACK + "io  cleaning dev env...\n");

            try {

                DataSource datasource = (DataSource) qio.getElement(DATASOURCE);
                Connection conn = datasource.getConnection();

                RunScript.execute(conn, new StringReader("drop all objects;"));
                conn.commit();
                conn.close();

            } catch (Exception e) {
            }

        }

        return true;
    }


}

