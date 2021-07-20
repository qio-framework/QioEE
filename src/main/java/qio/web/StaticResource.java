package qio.web;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Arrays;
import java.util.List;

public class StaticResource {

    String uri;
    ServletContext context;
    HttpServletResponse resp;

    public StaticResource(String uri, ServletContext context, HttpServletResponse resp){
        this.uri = uri;
        this.context = context;
        this.resp = resp;
    }

    public void serve(){

        try {

            String filename = context.getRealPath(uri);
            String mime = context.getMimeType(filename);
            if (mime == null) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                return;
            }

            resp.setContentType(mime);
            File file = new File(filename);
            resp.setContentLength((int)file.length());

            FileInputStream in = new FileInputStream(file);
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

    protected static Boolean isResource(String uri, String[] resources){
        if(resources == null) return false;

        String[] parts = uri.split("/");
        List<String> resourceList = Arrays.asList(resources);
        if(parts.length > 0) {
            String asset = parts[1];
            if (resourceList.contains(asset)) return true;
        }
        return false;
    }
}
