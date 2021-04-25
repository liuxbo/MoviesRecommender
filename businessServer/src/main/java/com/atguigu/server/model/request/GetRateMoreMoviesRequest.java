package com.atguigu.server.model.request;

public class GetRateMoreMoviesRequest {

    private int num;

    public GetRateMoreMoviesRequest(int num) {
        this.num = num;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
