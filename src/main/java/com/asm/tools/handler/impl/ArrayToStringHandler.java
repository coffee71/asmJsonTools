package com.asm.tools.handler.impl;

import com.asm.tools.classloader.HotspotClassLoader;
import com.asm.tools.constants.ToStringHandlerConstants;
import com.asm.tools.exception.AsmBusinessException;
import com.asm.tools.utils.LocalIndexUtil;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import java.lang.reflect.Field;

import static org.objectweb.asm.Opcodes.*;

/**
 * 数组类型属性转换toString
 */
public class ArrayToStringHandler extends ArrayCollectionToStringHandler {

    @Override
    protected Class getElementClass(Field field) {
        Class fieldClazz = field.getType();
        //获取数组元素类型
        Class elementClass = fieldClazz.getComponentType();
        return elementClass;
    }
}
