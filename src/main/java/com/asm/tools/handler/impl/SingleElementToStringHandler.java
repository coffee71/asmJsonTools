package com.asm.tools.handler.impl;

import com.asm.tools.classloader.HotspotClassLoader;
import com.asm.tools.constants.ToStringHandlerConstants;
import com.asm.tools.handler.ToStringHandler;
import com.asm.tools.utils.ClassUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Field;

import static org.objectweb.asm.Opcodes.*;

/**
 * 基础属性生成json key-Value的处理类
 */
public class SingleElementToStringHandler extends BaseToStringHandler {

    /**
     * 将属性value append到StringBuffer
     *
     * @param cw
     * @param ga
     * @param fieldName
     * @param clazz
     * @param field
     * @param classLoader
     * @param updateClassFile
     */
    @Override
    public void appendValue(ClassWriter cw, GeneratorAdapter ga, String fieldName, Class clazz, Field field,
                            HotspotClassLoader classLoader, boolean updateClassFile) {
        Class fieldClazz = field.getType();

        loadFieldValue(ga, clazz, fieldName, fieldClazz);
        if (!fieldClazz.isPrimitive() && !fieldClazz.isArray()) {
            //为了方便调用append先toString转成String类型，否则Integer等包装类需要对append的方法签名做特殊判断
            ga.visitMethodInsn(INVOKEVIRTUAL, ClassUtils.getClazzName(fieldClazz), "toString", "()Ljava/lang/String;");
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        } else {
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", String.format("(%s)Ljava/lang/StringBuffer;", Type.getType(fieldClazz).getDescriptor()));
        }

        ga.visitLdcInsn(",");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    }
}
