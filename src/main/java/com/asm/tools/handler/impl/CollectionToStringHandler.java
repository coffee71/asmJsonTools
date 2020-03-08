package com.asm.tools.handler.impl;

import com.asm.tools.constants.ToStringHandlerConstants;
import com.asm.tools.exception.AsmBusinessException;
import com.asm.tools.utils.ClassUtils;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;

import static org.objectweb.asm.Opcodes.*;

/**
 * 集合属性toString
 * 重要：为了简化实现，和保证性能（避免运行时递归动态修改类）目前要求Collection必须带有泛型
 */
public class CollectionToStringHandler extends ArrayCollectionToStringHandler {
    @Override
    protected void loadFieldValue(GeneratorAdapter ga, Class clazz, String fieldName, Class fieldClazz) {
        super.loadFieldValue(ga, clazz, fieldName, fieldClazz);
        //实际声明类型可能是接口也可能是具体类型，因此这里统一用接口处理
        ga.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "toArray", "()[Ljava/lang/Object;");
    }

    @Override
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
}
