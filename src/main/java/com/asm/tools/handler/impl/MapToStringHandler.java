package com.asm.tools.handler.impl;

import com.asm.tools.classloader.HotspotClassLoader;
import com.asm.tools.constants.ToStringHandlerConstants;
import com.asm.tools.model.MapGeneric;
import com.asm.tools.utils.ClassUtils;
import com.asm.tools.utils.LocalIndexUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.objectweb.asm.Opcodes.*;

/**
 * Map属性toString
 * 重要：为了简化实现，和保证性能（避免运行时递归动态修改类）目前要求Map必须带有泛型
 */
public class MapToStringHandler extends ArrayCollectionToStringHandler {
    @Override
    public void appendValue(ClassWriter cw, GeneratorAdapter ga, String fieldName, Class clazz, Field field, HotspotClassLoader classLoader, boolean updateClassFile) {
        //获取map上的泛型

        MapGeneric mapGeneric = ClassUtils.getGenericTypeInfo((ParameterizedType) field.getGenericType());
        List<Class> genericTypeList = ClassUtils.getGenericTypeList(field);
        Class keyClazz = genericTypeList.get(0);
        Class valueClazz = genericTypeList.get(1);

        ga.visitLdcInsn("{");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        ga.visitInsn(POP);
        super.loadFieldValue(ga, clazz, field.getName(), field.getType());
        //实际声明类型可能是接口也可能是具体类型，因此这里统一用接口处理
        ga.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "entrySet", "()Ljava/util/Set;");
        ga.visitMethodInsn(INVOKEINTERFACE, "java/util/Set", "iterator", "()Ljava/util/Iterator;");

        //用iterator遍历entitySet
        int iteratorIndex = LocalIndexUtil.applyLocalIndex();
        ga.visitVarInsn(ASTORE, iteratorIndex);
        Label iteratorJudgeLabel = new Label();
        ga.visitLabel(iteratorJudgeLabel);
        ga.visitVarInsn(ALOAD, iteratorIndex);
        ga.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z");
        Label judgeEndLabel = new Label();
        ga.visitJumpInsn(IFEQ, judgeEndLabel);
        ga.visitVarInsn(ALOAD, iteratorIndex);
        ga.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "next", "()Ljava/lang/Object;");
        ga.visitTypeInsn(CHECKCAST, "java/util/Map$Entry");

        int entityIndex = appendMapKey(ga, keyClazz);
        ga.visitVarInsn(ALOAD, entityIndex);
        appendMapValue(cw, ga, clazz, field, classLoader, updateClassFile, valueClazz);

        ga.visitVarInsn(ALOAD, iteratorIndex);
        //如果不是最后一个元素则拼接","

        ga.visitMethodInsn(INVOKEINTERFACE, "java/util/Iterator", "hasNext", "()Z");
        Label lastOneLabel = new Label();
        ga.visitJumpInsn(IFEQ, lastOneLabel);
        ga.visitLdcInsn(",");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        ga.visitLabel(lastOneLabel);

        ga.visitInsn(POP);
        ga.visitJumpInsn(GOTO, iteratorJudgeLabel);
        ga.visitLabel(judgeEndLabel);
        ga.visitVarInsn(ALOAD, ToStringHandlerConstants.LOCAL_MAIN_STRING_BUFFER);

        ga.visitLdcInsn("}");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        ga.visitLdcInsn(",");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    }

    @Override
    protected Class getElementClass(Field field) {
        return null;
    }

    private void appendMapValue(ClassWriter cw, GeneratorAdapter ga, Class clazz, Field field,
                                HotspotClassLoader classLoader, boolean updateClassFile, Class valueClazz) {

        ga.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;");
        ga.visitTypeInsn(CHECKCAST, ClassUtils.getClazzName(valueClazz));
        boolean isMapValue = Map.class.isAssignableFrom(valueClazz);

        if(isMapValue) {
//            appendValue(cw, ga, field, clazz, valueClazz, classLoader, updateClassFile);
        } else {
            super.appendElementValue(cw, ga, field, clazz, valueClazz, classLoader, updateClassFile);
        }


    }

    /**
     * 生成
     * @param keyClazz
     * @return
     */
    private int appendMapKey(GeneratorAdapter ga, Class keyClazz) {
        int entityIndex = LocalIndexUtil.applyLocalIndex();
        ga.visitVarInsn(ASTORE, entityIndex);
        if (Number.class.isAssignableFrom(keyClazz)) {
            //基础类型
            ga.visitVarInsn(ALOAD, ToStringHandlerConstants.LOCAL_MAIN_STRING_BUFFER);
            ga.visitVarInsn(ALOAD, entityIndex);
            ga.visitMethodInsn(INVOKEINTERFACE, ClassUtils.getClazzName(Map.Entry.class), "getKey",
                    "()Ljava/lang/Object;");
            ga.visitTypeInsn(CHECKCAST, ClassUtils.getClazzName(keyClazz));
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StingBuffer", "append", String.format("(%s)Ljava/lang" +
                    "/StringBuffer;", Type.getType(keyClazz).getDescriptor()));
        }  else if (CharSequence.class.isAssignableFrom(keyClazz) || Character.class.isAssignableFrom(keyClazz)) {
            ga.visitVarInsn(ALOAD, ToStringHandlerConstants.LOCAL_MAIN_STRING_BUFFER);
            ga.visitLdcInsn("\"");
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
            ga.visitVarInsn(ALOAD, entityIndex);

            ga.visitMethodInsn(INVOKEINTERFACE, ClassUtils.getClazzName(Map.Entry.class), "getKey",
                    "()Ljava/lang/Object;");
            ga.visitTypeInsn(CHECKCAST, ClassUtils.getClazzName(keyClazz));
            ga.visitMethodInsn(INVOKEVIRTUAL, ClassUtils.getClazzName(keyClazz), "toString", "()Ljava/lang/String;");

            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
            ga.visitLdcInsn("\"");
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");

        } else {
            ga.visitLdcInsn("\"");
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
            ga.visitLdcInsn(keyClazz.getName());
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
            ga.visitLdcInsn("\"");
            ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        }
        ga.visitLdcInsn(":");
        ga.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append", "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
        return entityIndex;
    }
}
