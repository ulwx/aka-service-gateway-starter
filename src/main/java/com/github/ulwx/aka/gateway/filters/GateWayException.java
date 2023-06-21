package com.github.ulwx.aka.gateway.filters;

import com.ulwx.tool.StringUtils;

public class GateWayException extends RuntimeException{
    private ResponseCode responseCode;

    public GateWayException(Exception exception) {
        super(ResponseCode.UNKNOWN_ERROR.getMessage()+"["+ exception+"]");
        responseCode=ResponseCode.UNKNOWN_ERROR;


    }
    public GateWayException(ResponseCode responseCode,String msg) {
        super(responseCode.getMessage()+ (StringUtils.hasText(msg)?"[" +msg+"]":""));
        this.responseCode = responseCode;
    }
    public GateWayException(ResponseCode responseCode) {
        super(responseCode.getMessage());
        this.responseCode = responseCode;
    }
    public ResponseCode getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(ResponseCode responseCode) {
        this.responseCode = responseCode;
    }
}
