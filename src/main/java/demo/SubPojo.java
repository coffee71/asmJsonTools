package demo;

import java.util.ArrayList;
import java.util.List;

public class SubPojo {
    private String simpleField = "simpleField";
    public NullSubPojo nullSubPojo = new NullSubPojo();
    private Integer[] arrayIntegerValues = new Integer[] {1, 2, 3};
    public String[] arrayStringValues = new String[] {"s1", "s2"};

    public Integer[] getArrayIntegerValues() {
        return arrayIntegerValues;
    }


    public String getSimpleField() {
        return simpleField;
    }

    public void setSimpleField(String simpleField) {
        this.simpleField = simpleField;
    }
public String toJsonString() {
    StringBuffer var1 = new StringBuffer();
    var1.append("{");
    String var10000;
    if (var1.length() > 1) {
        String var2 = var1.substring(0, var1.length() - 1);
        var10000 = var2 + "}";
    } else {
        var10000 = var1.append("}").toString();
    }

    return var10000;
}
}
