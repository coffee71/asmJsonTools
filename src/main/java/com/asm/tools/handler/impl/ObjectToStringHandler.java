package com.asm.tools.handler.impl;

import com.asm.tools.AsmJson;
import com.asm.tools.constants.ToStringHandlerConstants;
import com.asm.tools.exception.AsmBusinessException;
import com.asm.tools.model.JsonContext;
import com.asm.tools.utils.ClassUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;

/**
 * 复杂属性toString
 */
public class ObjectToStringHandler extends SingleElementToStringHandler {

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
        try {
            Class fieldClazz = field.getType();
            //生成toJsonString方法，然后调用toJsonString 将value压入StringBuffer，因为与主类同一个classLoader所以能实现热加载
            AsmJson.overwriteToJsonStringInner(fieldClazz, context.getClassLoader(), context.isUpdateClassFile());

            ga.visitVarInsn(ALOAD, ToStringHandlerConstants.LOCAL_THIS);

            Method getMethod = ClassUtils.getGetMethod(clazz, field.getName(), fieldClazz);
            if (getMethod == null) {
                //没有get方法或get方法不是public，直接访问内部属性
                ga.getField(Type.getType(clazz), field.getName(), Type.getType(fieldClazz));
                if (!fieldClazz.isPrimitive() && !fieldClazz.isArray()) {
                    //为了方便调用append先toString转成String类型，否则Integer等包装类需要对append的方法签名做特殊判断
                    ga.visitMethodInsn(INVOKEVIRTUAL, ClassUtils.getClazzName(fieldClazz), ToStringHandlerConstants.TO_JSON_METHOD_NAME, "()Ljava/lang/String;");
                    ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
                } else {
                    ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", String.format("(%s)Ljava/lang/StringBuffer;", Type.getType(fieldClazz).getDescriptor()));
                }
            } else {
                //调用get方法访问属性值
                ga.visitMethodInsn(INVOKEVIRTUAL, ClassUtils.getClazzName(clazz), getMethod.getName(), "()" + Type.getType(fieldClazz).getDescriptor());
                ga.visitMethodInsn(INVOKEVIRTUAL, ClassUtils.getClazzName(fieldClazz), ToStringHandlerConstants.TO_JSON_METHOD_NAME, "()Ljava/lang/String;");
                ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
            }

            ga.visitLdcInsn(",");
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        } catch (Throwable e) {
            throw new AsmBusinessException(e.getMessage(), e);
        }
    }
}
