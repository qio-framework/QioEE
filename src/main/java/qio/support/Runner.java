package qio.support;

import qio.Qio;

import java.util.logging.Logger;

public class Runner {

    static Logger log = Logger.getLogger("Runner");

    public static void main(String[] args) {

        log.info("Runner ...");
        try {
            Qio.Injector.badge();
        }catch(Exception ex){
            ex.printStackTrace();
        }
    }

}