package demo;

import java.util.*;

public class TestPojo2 {

    public Map<String, SubPojo2> mapField = new HashMap<>();


    public Map<String, Map<String, SubPojo2>> mapField2 = new HashMap<>();

    public TestPojo2() {
        mapField.put("f1", new SubPojo2());
        mapField2.put("mapField2", mapField);
    }

}
