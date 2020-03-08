package com.asm.tools.handler.impl;

import com.asm.tools.classloader.HotspotClassLoader;
import com.asm.tools.constants.ToStringHandlerConstants;
import com.asm.tools.handler.ToStringHandler;
import com.asm.tools.utils.ClassUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.objectweb.asm.Opcodes.*;

/**
 * 基础属性生成json key-Value的处理类
 */
public abstract class BaseToStringHandler implements ToStringHandler {
    @Override
    public void toJsonString(ClassWriter cw, GeneratorAdapter ga, Class clazz, Field field,
                             HotspotClassLoader classLoader, boolean updateClassFile) {
        ga.visitVarInsn(ALOAD, ToStringHandlerConstants.LOCAL_MAIN_STRING_BUFFER);
        appendKey(ga, field.getName());
        appendValue(cw, ga, clazz, field, classLoader, updateClassFile);
        ga.visitInsn(POP);
    }

    /**
     * 如果属性存在get方法则get方法读取值，否则如果是私有
     *
     * @param ga
     * @param clazz
     * @param fieldName
     * @param fieldClazz
     */
    protected void loadFieldValue(GeneratorAdapter ga, Class clazz, String fieldName, Class fieldClazz) {
        ga.visitVarInsn(ALOAD, ToStringHandlerConstants.LOCAL_THIS);
        Method getMethod = ClassUtils.getGetMethod(clazz, fieldName, fieldClazz);
        if (getMethod == null) {
            //没有get方法或get方法不是public，直接访问属性值
            ga.getField(Type.getType(clazz), fieldName, Type.getType(fieldClazz));
        } else {
            //调用get方法访问属性值
            ga.visitMethodInsn(INVOKEVIRTUAL, ClassUtils.getClazzName(clazz), getMethod.getName(), "()" + Type.getType(fieldClazz).getDescriptor());
        }
    }

    /**
     * 将属性名字append到StringBuffer
     *
     * @param ga
     * @param fieldName
     */
    @Override
    public void appendKey(GeneratorAdapter ga, String fieldName) {
        ga.visitLdcInsn("\"" + fieldName + "\":");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    }
}
