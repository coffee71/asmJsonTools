package com.asm.tools.handler;

import com.asm.tools.classloader.HotspotClassLoader;
import com.asm.tools.constants.ToStringHandlerConstants;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Field;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.IFNULL;

public interface ToStringHandler {

    /**
     * @param cw
     * @param ga
     * @param propertyName
     * @param clazz
     * @param field
     * @param classLoader     复杂对象嵌套时需要递归热加载类，因此需要传递classLoader
     * @param updateClassFile
     */
    void toJsonString(ClassWriter cw, GeneratorAdapter ga, String propertyName, Class clazz, Field field,
                      HotspotClassLoader classLoader, boolean updateClassFile);

    /**
     * 为非空属性生成json key-value
     *
     * @param cw
     * @param ga
     * @param propertyName
     * @param clazz
     * @param field
     * @param classLoader     复杂对象嵌套时需要递归热加载类，因此需要传递classLoader
     * @param updateClassFile
     */
    default void toJsonStringIfNotNull(ClassWriter cw, GeneratorAdapter ga, String propertyName, Class clazz,
                                       Field field, HotspotClassLoader classLoader, boolean updateClassFile) {
        Class propertyClazz = field.getType();
        Label nullJudgeLabel = new Label();
        if (!propertyClazz.isPrimitive()) {
            //读取属性值
            ga.visitVarInsn(ALOAD, ToStringHandlerConstants.LOCAL_THIS);
            ga.getField(Type.getType(clazz), propertyName, Type.getType(propertyClazz));
            ga.visitJumpInsn(IFNULL, nullJudgeLabel);
        }
        toJsonString(cw, ga, propertyName, clazz, field, classLoader, updateClassFile);
        if (!propertyClazz.isPrimitive()) {
            //如果属性为null则跳过不调用toString
            ga.visitLabel(nullJudgeLabel);
        }
    }

    /**
     * 将属性value append到StringBuffer
     *
     * @param cw
     * @param ga
     * @param propertyName
     * @param clazz
     * @param field
     */
    void appendValue(ClassWriter cw, GeneratorAdapter ga, String propertyName, Class clazz, Field field, HotspotClassLoader classLoader, boolean updateClassFile);

    void appendKey(GeneratorAdapter ga, String fieldName);
}
