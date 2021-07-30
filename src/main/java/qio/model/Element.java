package qio.model;

public class Element {

    Object element;
    Class<?> elementClass;

    public Object getElement() {
        return element;
    }

    public void setElement(Object element) {
        this.element = element;
    }

    public Class<?> getElementClass() {
        return elementClass;
    }

    public void setElementClass(Class<?> elementClass) {
        this.elementClass = elementClass;
    }
}
