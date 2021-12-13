package qio.web;

import qio.Qio;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;

import java.io.*;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class StaticResource {

    Qio qio;
    String uri;
    ServletContext context;
    HttpServletResponse resp;

    final String WEBAPP_PREFIX = "src/main/webapp";

    public StaticResource(String uri, Qio qio, ServletContext context, HttpServletResponse resp){
        this.uri = uri;
        this.qio = qio;
        this.context = context;
        this.resp = resp;
    }

    public void serve(){

        try {

            InputStream in = null;

            if(qio.isJar()){

                JarFile jarFile = qio.getJarFile();
                JarEntry jarEntry = jarFile.getJarEntry(WEBAPP_PREFIX + uri);

                in = jarFile.getInputStream(jarEntry);
                resp.setContentLength((int) jarEntry.getSize());

                String mimeType = URLConnection.guessContentTypeFromName(jarEntry.getName());
                resp.setContentType(mimeType);

            } else {

                String filename = context.getRealPath(uri);
                String mimeType = context.getMimeType(filename);
                if (mimeType == null) {
                    resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    return;
                }

                resp.setContentType(mimeType);
                File file = new File(filename);
                resp.setContentLength((int) file.length());
                in = new FileInputStream(file);

            }

            OutputStream out = resp.getOutputStream();

            byte[] buf = new byte[1024];
            int count = 0;
            while ((count = in.read(buf)) >= 0) {
                out.write(buf, 0, count);
            }

            out.close();
            in.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected static Boolean isResource(String uri, List<String> resources){
        if(resources == null) return false;

        String[] parts = uri.split("/");
        if(parts.length > 1) {
            String asset = parts[1];
            if (resources.contains(asset)) return true;
        }
        return false;
    }
}
