package com.github.ulwx.aka.gateway.filters;

import com.github.ulwx.aka.gateway.filters.model.CbResult;
import com.github.ulwx.aka.gateway.filters.model.Status;

public class ResponseResult<T> {

    public static CbResult error(Integer errorCode, String msg) {
        CbResult result = new CbResult();
        result.setStatus(Status.ERR);
        result.setError(errorCode);
        result.setMessage(msg);
        return result;
    }

    public static CbResult error(ResponseCode responseCode, Object data) {
        CbResult result = new CbResult();
        result.setStatus(Status.ERR);
        result.setError(responseCode.getCode());
        result.setMessage(responseCode.getMessage());
        result.setData(data);
        return result;
    }

    public static CbResult error(Integer errorCode, String msg, Object data) {
        CbResult result = new CbResult();
        result.setStatus(Status.ERR);
        result.setError(errorCode);
        result.setMessage(msg);
        result.setData(data);
        return result;
    }

    public static <T> CbResult<T> success(String msg, T data) {
        CbResult result = new CbResult();
        result.setStatus(Status.SUC);
        result.setError(0);
        result.setMessage(msg);
        result.setData(data);
        return result;
    }

}
