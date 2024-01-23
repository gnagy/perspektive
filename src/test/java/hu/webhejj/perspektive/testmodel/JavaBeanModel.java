package hu.webhejj.perspektive.testmodel;

import java.util.Objects;

public class JavaBeanModel {

    private String field;
    private Boolean bool;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Boolean isBool() {
        return bool;
    }

    public void setBool(Boolean bool) {
        this.bool = bool;
    }

    public String method(String arg) {
        return arg;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JavaBeanModel that = (JavaBeanModel) o;
        return Objects.equals(field, that.field) && Objects.equals(bool, that.bool);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, bool);
    }
}
