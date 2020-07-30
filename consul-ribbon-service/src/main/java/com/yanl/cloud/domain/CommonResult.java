package com.yanl.cloud.domain;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommonResult<T> {
    private T data;
    private String msg;
    private Integer code;

    public CommonResult(){}

    public CommonResult(T data, String msg, Integer code){
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    public CommonResult(String msg, Integer code){
        this(null, msg, code);
    }

    public CommonResult(T data){
        this(data, "操作成功", 200);
    }
}
