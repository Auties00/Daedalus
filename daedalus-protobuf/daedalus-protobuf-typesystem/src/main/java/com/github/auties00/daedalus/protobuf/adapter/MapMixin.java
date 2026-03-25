package com.github.auties00.daedalus.protobuf.adapter;

import com.github.auties00.daedalus.protobuf.annotation.ProtobufMessage;
import com.github.auties00.daedalus.typesystem.annotation.TypeMixin;

import java.util.Map;

@TypeMixin
public final class MapMixin {
    private MapMixin() {
        throw new UnsupportedOperationException("MapMixin is a mixin and cannot be instantiated");
    }

    @ProtobufMessage.UnknownFields.Setter
    public static void addUnknownField(Map<Long, Object> map, long index, Object value) {
        map.put(index, value);
    }
}
