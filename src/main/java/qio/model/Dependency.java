package qio.model;

import java.util.Map;

public class Dependency {

    String name;
    String pojo;
    Map<String, String> props;
    String constructor;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPojo() {
        return pojo;
    }

    public void setPojo(String pojo) {
        this.pojo = pojo;
    }

    public Map<String, String> getProps() {
        return props;
    }

    public void setProps(Map<String, String> props) {
        this.props = props;
    }

    public String getConstructor() {
        return constructor;
    }

    public void setConstructor(String constructor) {
        this.constructor = constructor;
    }
}
