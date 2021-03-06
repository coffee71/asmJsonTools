package com.asm.tools.handler.impl;

import com.asm.tools.model.JsonContext;
import com.asm.tools.utils.ClassUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Field;

import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

/**
 * 基础属性生成json key-Value的处理类
 */
public class SingleElementToStringHandler extends BaseToStringHandler {

    /**
     * 将属性value append到StringBuffer
     *
     * @param context
     * @param clazz
     * @param field
     */
    @Override
    public void appendValue(JsonContext context, Class clazz, Field field) {
        GeneratorAdapter ga = context.getGa();
        Class fieldClazz = field.getType();

        loadFieldValue(ga, clazz, field.getName(), fieldClazz);
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
