package qio.model.web;

import java.util.HashMap;
import java.util.Map;

public class ResponseData {

    Map<String, Object> data;

    public ResponseData(){
        this.data = new HashMap<>();
    }

    public void set(String key, Object value){
        this.data.put(key, value);
    }
    public void put(String key, Object value){
        this.data.put(key, value);
    }

    public Object get(String key){
        if(this.data.containsKey(key)){
            return this.data.get(key);
        }
        return null;
    }

    public Map<String, Object> getData(){
        return this.data;
    }

}
