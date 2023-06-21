package com.github.ulwx.aka.gateway.filters;

public class ResponseResult<T> {
    private Integer status= Status.SUC;
    private int error = 0;
    private String message;
    private String src="gateway";
    private T data;

    public ResponseResult(int status ,int errorcode, String msg) {
        this.error = errorcode;
        this.message = msg;
        this.status=status;
    }

    public ResponseResult(int status,int errorcode, String msg, T data) {
        this.error = errorcode;
        this.message = msg;
        this.data = data;
        this.status=status;
    }

    public static ResponseResult error(Integer errorCode,String msg){
        ResponseResult result=new ResponseResult(Status.ERR,errorCode,msg,null);
        return result;
    }
    public static ResponseResult error(ResponseCode responseCode,Object data){
        ResponseResult result=new ResponseResult(Status.ERR,responseCode.getCode()
                ,responseCode.getMessage(),data);
        return result;
    }
    public static ResponseResult error(Integer errorCode,String msg,Object data){
        ResponseResult result=new ResponseResult(Status.ERR,errorCode,msg,data);
        return result;
    }
    public static <T> ResponseResult<T> success(String msg, T data){
        ResponseResult result=new ResponseResult(Status.SUC,0,msg,data);
        return result;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
