package qio.model.web;

import qio.model.support.ObjectDetails;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class HttpMapping {

    String path;
    String regexedPath;
    String verb;

    Method method;

    List<TypeFeature> typeDetails;
    List<String> typeNames;
    List<Integer> variablePositions;

    ObjectDetails objectDetails;

    public HttpMapping(){
        this.variablePositions = new ArrayList<>();
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getRegexedPath() {
        return regexedPath;
    }

    public void setRegexedPath(String regexedPath) {
        this.regexedPath = regexedPath;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public List<TypeFeature> getTypeDetails() {
        return typeDetails;
    }

    public void setTypeDetails(List<TypeFeature> typeDetails) {
        this.typeDetails = typeDetails;
    }

    public List<String> getTypeNames() {
        return typeNames;
    }

    public void setTypeNames(List<String> typeNames) {
        this.typeNames = typeNames;
    }

    public List<Integer> getVariablePositions() {
        return variablePositions;
    }

    public void setVariablePositions(List<Integer> variablePositions) {
        this.variablePositions = variablePositions;
    }

    public ObjectDetails getClassDetails() {
        return objectDetails;
    }

    public void setClassDetails(ObjectDetails objectDetails) {
        this.objectDetails = objectDetails;
    }

}
