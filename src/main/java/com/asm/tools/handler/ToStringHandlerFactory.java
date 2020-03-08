package com.asm.tools.handler;

import com.asm.tools.handler.impl.*;

import java.util.Collection;
import java.util.Map;

public class ToStringHandlerFactory {

    private ArrayToStringHandler arrayToStringHandler;

    private SingleElementToStringHandler singleElementToStringHandler;

    private CharSequenceToStringHandler charSequenceToStringHandler;

    private CollectionToStringHandler collectionToStringHandler;

    private MapToStringHandler mapToStringHandler;

    private ObjectToStringHandler objectToStringHandler;

    private ToStringHandlerFactory() {
        arrayToStringHandler = new ArrayToStringHandler();
        singleElementToStringHandler = new SingleElementToStringHandler();
        charSequenceToStringHandler = new CharSequenceToStringHandler();
        collectionToStringHandler = new CollectionToStringHandler();
        mapToStringHandler = new MapToStringHandler();
        objectToStringHandler = new ObjectToStringHandler();
    }

    public static ToStringHandlerFactory getInstance() {
        return SINGLETON.INSTANCE;
    }

    /**
     * 根据参数类型适配处理器，先用最原始的if-else分发
     *
     * @param fieldClazz
     * @return
     */
    public ToStringHandler getToStringHandler(Class fieldClazz) {
        if (fieldClazz.isPrimitive()) {
            //基础类型
            return singleElementToStringHandler;
        }

        if (fieldClazz.isArray()) {
            //数组类型
            return arrayToStringHandler;
        }

        if (Number.class.isAssignableFrom(fieldClazz)) {
            //数字类型
            return singleElementToStringHandler;
        }

        if (CharSequence.class.isAssignableFrom(fieldClazz) || Character.class.isAssignableFrom(fieldClazz)) {
            //字符(串)
            return charSequenceToStringHandler;
        }

        if (Collection.class.isAssignableFrom(fieldClazz)) {
            //集合类型
            return collectionToStringHandler;
        }

        if (Map.class.isAssignableFrom(fieldClazz)) {
            //Map类型
            return mapToStringHandler;
        }
        //匹配不到其它类型时默认当做Object处理
        return objectToStringHandler;
    }

    static class SINGLETON {
        public static final ToStringHandlerFactory INSTANCE = new ToStringHandlerFactory();
    }
}
