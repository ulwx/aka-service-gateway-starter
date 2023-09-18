package com.github.ulwx.aka.gateway.filters.model;

import java.util.HashMap;
import java.util.Map;

public class ErrorCode {
    public static final int NO_ERROR = 0;
    public static final int COMMON_ERROR = 999;
    public static final int VIEW_ERROR = 998;
    public static final int GW_BLOCK_EXCEPTION = 990;
    public static Map<Integer,String> errors=new HashMap<Integer,String>();

    static{
        errors.put(COMMON_ERROR, "返回视图出错！");
        errors.put(VIEW_ERROR, "");
    }

    public Map<Integer, String> getError() {
        // TODO Auto-generated method stub
        return errors;
    }

    public static void  put(Map<Integer,String> map){
        errors.putAll(map);
    }
}
