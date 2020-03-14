# asmJsonTools
通过字节码生成方式为类生成json序列化方法（参考lombok 生成的toString）旨在解决json序列化、反序列化性能问题

限制：
1.当前仍不支持反序列化
2.要求Map、Collection必须带有泛型，编译期间无法探知实际类型。
