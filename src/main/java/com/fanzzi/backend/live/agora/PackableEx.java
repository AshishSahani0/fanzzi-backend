package com.fanzzi.backend.live.agora;

public interface PackableEx {

    ByteBuf marshal(ByteBuf out);

    void unmarshal(ByteBuf in);

}