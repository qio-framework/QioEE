package qio.model.web;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EndpointMappings {

    ConcurrentMap<String, EndpointMapping> mappings;

    public EndpointMappings(){
        this.mappings = new ConcurrentHashMap<>();
    }

    public void add(String key, EndpointMapping endpointMapping){
        this.mappings.put(key, endpointMapping);
    }

    public EndpointMapping get(String key){
        if(this.mappings.containsKey(key)){
            return this.mappings.get(key);
        }
        return null;
    }

    public boolean contains(String key){
        return this.mappings.containsKey(key);
    }

    public ConcurrentMap<String, EndpointMapping> getMappings() {
        return mappings;
    }

    public void setMappings(ConcurrentMap<String, EndpointMapping> m) {
        this.mappings = mappings;
    }
}
