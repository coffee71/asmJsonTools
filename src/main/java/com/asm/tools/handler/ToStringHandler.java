package com.asm.tools.handler;

import com.asm.tools.classloader.HotspotClassLoader;
import com.asm.tools.constants.ToStringHandlerConstants;
import com.asm.tools.model.JsonContext;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Field;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.IFNULL;

public interface ToStringHandler {

    /**
     * @param context
     * @param clazz
     * @param field
     */
    void toJsonString(JsonContext context, Class clazz, Field field);

    /**
     * 为非空属性生成json key-value
     *
     * @param context
     * @param clazz
     * @param field
     */
    default void toJsonStringIfNotNull(JsonContext context, Class clazz, Field field) {
        GeneratorAdapter ga = context.getGa();
        Class propertyClazz = field.getType();
        Label nullJudgeLabel = new Label();
        if (!propertyClazz.isPrimitive()) {
            //读取属性值
            ga.visitVarInsn(ALOAD, ToStringHandlerConstants.LOCAL_THIS);
            ga.getField(Type.getType(clazz), field.getName(), Type.getType(propertyClazz));
            ga.visitJumpInsn(IFNULL, nullJudgeLabel);
        }
        toJsonString(context, clazz, field);
        if (!propertyClazz.isPrimitive()) {
            //如果属性为null则跳过不调用toString
            ga.visitLabel(nullJudgeLabel);
        }
    }

    /**
     * 将属性value append到StringBuffer
     *
     * @param context
     * @param clazz
     * @param field
     */
    void appendValue(JsonContext context, Class clazz, Field field);

    void appendKey(GeneratorAdapter ga, String fieldName);
}
