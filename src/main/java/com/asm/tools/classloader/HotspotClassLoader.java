package com.asm.tools.classloader;

import com.asm.tools.exception.AsmBusinessException;

import java.util.HashSet;
import java.util.Set;

/**
 * 用于热加载复写的class
 */
public class HotspotClassLoader extends ClassLoader {
    Set<String> loadedClass = new HashSet<>();

    @SuppressWarnings("unchecked")
    public Class defineClassByName(String name, byte[] b, int off, int len) {
        if(loadedClass.add(name)) {
            Class clazz = super.defineClass(name, b, off, len);
            return clazz;
        } else {
            try {
                return Class.forName(name);
            } catch (ClassNotFoundException e) {
                throw new AsmBusinessException(e.getMessage(), e);
            }
        }
    }
}
