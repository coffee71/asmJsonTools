package demo;

import com.asm.tools.utils.ClassUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestPojo {
    public String publicField = "publicValue";

    public String stringField = "StringValue";

    public int intValue = 0;

    public Integer integerValue = 1;

    public Double doubleValue = 1D;

    public SubPojo subPojo = new SubPojo();

    private SubPojo2[] subPojoArray = new SubPojo2[2];

    public String[][] dyadicArray = new String[][] {{"s1", "s2"}, {"s3", "s4"}};

    private List<SubPojo> subPojoList = new ArrayList<>();


    public Map<String, Map<String, SubPojo2>> mapField = new HashMap<>();

    public TestPojo() {
        subPojoArray[0] = new SubPojo2();
        subPojoArray[1] = new SubPojo2();
        subPojoList.add(new SubPojo());
        Map<String, SubPojo2> mapField2= new HashMap<>();
        mapField2.put("f1", new SubPojo2());
        mapField.put("map嵌套", mapField2);
    }

    public SubPojo2[] getSubPojoArray() {
        return subPojoArray;
    }

    public List<SubPojo> getSubPojoList() {
        return subPojoList;
    }
}
