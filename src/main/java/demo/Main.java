package demo;

import com.asm.tools.AsmJson;
import com.asm.tools.constants.ToStringHandlerConstants;
import com.asm.tools.utils.ClassUtils;

import java.lang.reflect.ParameterizedType;

public class Main {

    public static void main(String[] args) throws Throwable {
        testMapValue();
//        TestPojo2 pojo = new TestPojo2();
//        System.out.println(ClassUtils.getGenericTypeInfo((ParameterizedType)pojo.getClass().getField("mapField2").getGenericType()));
    }

    public static void testObjectValue() throws Throwable {
        TestPojo pojo = new TestPojo();
        System.out.println("复写前：" + pojo.toString());

        Class updatedClass = AsmJson.overwriteToJsonString(TestPojo.class, true);
        java.lang.reflect.Method method = updatedClass.getMethod(ToStringHandlerConstants.TO_JSON_METHOD_NAME);
        System.out.println("复写后：" + method.invoke(updatedClass.getConstructor().newInstance()));
    }

    public static void testMapValue() throws Throwable {
        TestPojo2 pojo = new TestPojo2();
        System.out.println("复写前：" + pojo.toString());

        Class updatedClass = AsmJson.overwriteToJsonString(TestPojo2.class, true);
        java.lang.reflect.Method method = updatedClass.getMethod(ToStringHandlerConstants.TO_JSON_METHOD_NAME);
        System.out.println("复写后：" + method.invoke(updatedClass.getConstructor().newInstance()));
    }
}
