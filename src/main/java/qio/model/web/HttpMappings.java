package qio.model.web;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class HttpMappings {

    ConcurrentMap<String, HttpMapping> mappings;

    public HttpMappings(){
        this.mappings = new ConcurrentHashMap<>();
    }

    public void add(String key, HttpMapping httpMapping){
        this.mappings.put(key, httpMapping);
    }

    public HttpMapping get(String key){
        if(this.mappings.containsKey(key)){
            return this.mappings.get(key);
        }
        return null;
    }

    public boolean contains(String key){
        return this.mappings.containsKey(key);
    }

    public ConcurrentMap<String, HttpMapping> getMappings() {
        return mappings;
    }

    public void setMappings(ConcurrentMap<String, HttpMapping> m) {
        this.mappings = mappings;
    }
}
