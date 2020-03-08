package demo;

import com.asm.tools.exception.AsmBusinessException;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author 陈昕
 * @date 2019/2/14.
 * 文件功能：
 * 主要逻辑：
 */
public class Test3 {
    private Map<String, Object> map = new HashMap<>();

    public void test() {
        for(Map.Entry<String, Object> entry : map.entrySet()) {
            System.out.println(entry.getKey());
            System.out.println(entry.getValue());
        }
    }

    protected Class getElementClass(Field field) {
        try {
            ParameterizedType listGenericType = (ParameterizedType) field.getGenericType();
            java.lang.reflect.Type[] listActualTypeArguments = listGenericType.getActualTypeArguments();
            Class elementClass = Class.forName(listActualTypeArguments[0].getTypeName());

            return elementClass;
        } catch (ClassNotFoundException e) {
            throw new AsmBusinessException(e.getMessage(), e);
        }
    }

    public static void main(String[] args) throws NoSuchFieldException {
        Test3 test3 = new Test3();
        test3.getElementClass(test3.getClass().getDeclaredField("map"));
    }
}
