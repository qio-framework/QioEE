package qio.storage;

import qio.model.Element;

import java.util.HashMap;
import java.util.Map;

public class ElementStorage {

    Map<String, Element> beans;

    public ElementStorage(){
        this.beans = new HashMap<>();
    }

    public Map<String, Element> getBeans(){
        return this.beans;
    }

}
