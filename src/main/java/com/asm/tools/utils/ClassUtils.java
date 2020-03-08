package com.asm.tools.utils;

import com.asm.tools.exception.AsmBusinessException;
import com.asm.tools.model.MapGeneric;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ClassUtils {

    /**
     * java类文件魔数固定地址
     */
    private static final int JAVA_CLASS_MAGIC = 0xCAFEBABE;
    /**
     * 公有方法标识位
     */
    private static final int PUBLIC_FLAG = 1;

    /**
     * 将修改后的类重新写到类文件
     *
     * @param classWriter
     * @return
     * @throws Throwable
     */
    public static String saveClassFile(ClassWriter classWriter, Class clazz) {
        String filePath = getClassFilePath(clazz);
        File file = new File(getClassFilePath(clazz));
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(classWriter.toByteArray());
            fos.flush();
        } catch (Throwable e) {
            throw new AsmBusinessException(e.getMessage(), e);
        }
        return filePath;
    }

    /**
     * 通过classloader找到类文件路径
     *
     * @param clazz
     * @return
     */
    private static String getClassFilePath(Class clazz) {
        String folderPath = clazz.getClassLoader().getResource("").getFile();
        String classPath = folderPath + clazz.getCanonicalName().replace(".", File.separator) + ".class";
        return new File(classPath).getPath();
    }

    /**
     * 通过类文件魔数获取类文件编译版本号
     *
     * @param clazz
     * @return
     */
    public static int getClassVersion(Class clazz) throws Throwable {
        try (DataInputStream dataInputStream = new DataInputStream(clazz.getResourceAsStream(getClassFilePath(clazz)))) {
            // 前面8个字节CA FE BA BE 是固定的，之后4个字节是次版本号，次版本号后面的4个字节是jdk的版本号
            int magic = dataInputStream.readInt();
            if (magic == JAVA_CLASS_MAGIC) {
                // int minorVersion =
                dataInputStream.readUnsignedShort();
                int majorVersion = dataInputStream.readUnsignedShort();
                // Java 1.2 >> 46
                // Java 1.3 >> 47
                // Java 1.4 >> 48
                // Java 5 >> 49
                // Java 6 >> 50
                // Java 7 >> 51
                // Java 8 >> 52
                return majorVersion;
            }
            throw new UnsupportedClassVersionError("无法解析类文件版本号");
        }
    }

    /**
     * 返回全类名并且用"/"替换"."
     *
     * @param clazz
     * @return
     */
    public static String getClazzName(Class clazz) {
        return clazz.getName().replace(".", "/");
    }

    /**
     * 判断当前是否可访问：
     *
     * @param clazz
     * @param field
     * @return
     */
    public static boolean isAccessAble(Class clazz, Field field) {
        //1、优先判断是否存在get方法，并且方法可访问
        Method readMethod = getGetMethod(clazz, field.getName(), field.getType());
        if (readMethod != null) {
            return true;
        }
        //2、如果get方法不存在则判断是否公有属性
        if ((field.getModifiers() & PUBLIC_FLAG) == PUBLIC_FLAG) {
            return true;
        } else {
            return false;
        }
    }

    public static List<Class> getGenericTypeList(Field field) {
        try {
            ParameterizedType listGenericType = (ParameterizedType) field.getGenericType();
            java.lang.reflect.Type[] listActualTypeArguments = listGenericType.getActualTypeArguments();
            List<Class> genericTypeList = new ArrayList<>();
            for(java.lang.reflect.Type type : listActualTypeArguments) {
                String className = type.getTypeName();
                if(type instanceof ParameterizedType) {
                    className = ((ParameterizedType) type).getRawType().getTypeName();
                }
                genericTypeList.add(Class.forName(className));
            }

            return genericTypeList;
        } catch (ClassNotFoundException e) {
            throw new AsmBusinessException(e.getMessage(), e);
        }
    }

    public static MapGeneric getGenericTypeInfo(ParameterizedType parameterizedType) {
        java.lang.reflect.Type[] listActualTypeArguments = parameterizedType.getActualTypeArguments();

        if(listActualTypeArguments.length != 2) {
            final StringBuilder typeNames = new StringBuilder();
            Arrays.stream(listActualTypeArguments).forEach(t -> {
                typeNames.append(t.getTypeName()).append(" ");
            });
            throw new IllegalArgumentException(String.format("Map泛型参数不正确%s", typeNames.toString()));
        }

        MapGeneric mapGeneric = new MapGeneric(listActualTypeArguments[0].getTypeName(), listActualTypeArguments[1].getTypeName());
        analysisMapGenericRecursion(mapGeneric);
        return mapGeneric;
    }

    /**
     * 递归解析Map的泛型
     * @param mapGeneric
     */
    private static void analysisMapGenericRecursion(MapGeneric mapGeneric) {
        if(Map.class.isAssignableFrom(mapGeneric.getKeyClass())) {
            //key 存在map递归嵌套
            //fixme 为了实现简单，Map必须声明泛型
            MapGeneric keySubMapGeneric = analysisMapGenericByName(mapGeneric.getKeyClassName());
            mapGeneric.setKeyMapInfo(keySubMapGeneric);
        }

        if(Map.class.isAssignableFrom(mapGeneric.getValueClass())) {
            //value 存在map递归嵌套
            //fixme 为了实现简单，Map必须声明泛型
            MapGeneric valueSubMapGeneric = analysisMapGenericByName(mapGeneric.getValueClassName());
            mapGeneric.setValueMapInfo(valueSubMapGeneric);
        }
    }

    /**
     * 分析Map泛型key 和 value类型
     * @param className
     * @return
     */
    private static MapGeneric analysisMapGenericByName(String className) {
        className = className.substring(className.indexOf("<") + 1);
        className = className.substring(0, className.indexOf(">"));
        String[] keyAndValueClassNames = className.split(",");
        return new MapGeneric(keyAndValueClassNames[0].trim(), keyAndValueClassNames[1].trim());
    }

    /**
     * 根据属性名字和类型获取get方法
     * 因为考虑到外部访问类来自不同的包，因此只有public的get方法才会被返回，找不到方法则返回null
     * get方法需要按照java标准习惯命名，boolean值方法为isXxx()
     *
     * @param clazz
     * @param fieldName
     * @param typeClazz
     * @return
     */
    public static Method getGetMethod(Class clazz, String fieldName, Class typeClazz) {
        String getMethodName;
        if (Boolean.class.isAssignableFrom(typeClazz) ||
                Type.getType(typeClazz).getSort() == Type.BOOLEAN) {
            getMethodName = "is" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        } else {
            getMethodName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
        }

        try {
            Method getMethod = clazz.getMethod(getMethodName);
            //PUBLIC: 1 （二进制 0000 0001）
            //PRIVATE: 2 （二进制 0000 0010）
            //PROTECTED: 4 （二进制 0000 0100）
            //STATIC: 8 （二进制 0000 1000）
            //FINAL: 16 （二进制 0001 0000）
            //SYNCHRONIZED: 32 （二进制 0010 0000）
            //VOLATILE: 64 （二进制 0100 0000）
            //TRANSIENT: 128 （二进制 1000 0000）
            //NATIVE: 256 （二进制 0001 0000 0000）
            //INTERFACE: 512 （二进制 0010 0000 0000）
            //ABSTRACT: 1024 （二进制 0100 0000 0000）
            //STRICT: 2048 （二进制 1000 0000 0000）
            if ((getMethod.getModifiers() & PUBLIC_FLAG) == PUBLIC_FLAG) {
                return getMethod;
            } else {
                return null;
            }
        } catch (Exception e) {
            //TODO 日志 这里不需要报错，只需要直接输出没有get方法的警告
            return null;
        }
    }
}
