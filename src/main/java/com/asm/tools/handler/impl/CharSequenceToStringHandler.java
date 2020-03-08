package com.asm.tools.handler.impl;

import com.asm.tools.classloader.HotspotClassLoader;
import com.asm.tools.constants.ToStringHandlerConstants;
import com.asm.tools.utils.ClassUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

public class CharSequenceToStringHandler extends SingleElementToStringHandler {
    /**
     * 将属性valueappend到StringBuffer
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
        ga.visitLdcInsn("\"");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        ga.visitVarInsn(ALOAD, ToStringHandlerConstants.LOCAL_THIS);

        Method getMethod = ClassUtils.getGetMethod(clazz, fieldName, fieldClazz);
        if (getMethod == null) {
            //没有get方法或get方法不是public，直接访问内部属性
            ga.getField(Type.getType(clazz), fieldName, Type.getType(fieldClazz));
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", String.format("(%s)Ljava/lang/StringBuffer;", Type.getType(fieldClazz).getDescriptor()));
        } else {
            //调用get方法访问属性值
            ga.visitMethodInsn(INVOKEVIRTUAL, ClassUtils.getClazzName(clazz), getMethod.getName(), "()" + Type.getType(fieldClazz).getDescriptor());
            if (!fieldClazz.isPrimitive() && !fieldClazz.isArray()) {
                ga.visitMethodInsn(INVOKEVIRTUAL, ClassUtils.getClazzName(fieldClazz), "toString", "()Ljava/lang/String;");
                ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
            } else {
                ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", String.format("(%s)Ljava/lang/StringBuffer;", Type.getType(fieldClazz).getDescriptor()));
            }
        }

        ga.visitLdcInsn("\"");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");

        ga.visitLdcInsn(",");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    }
}
