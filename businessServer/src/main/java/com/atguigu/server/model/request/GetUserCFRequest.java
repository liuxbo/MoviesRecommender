package com.atguigu.server.model.request;

//获取ALS算法下的用户推荐矩阵
public class GetUserCFRequest {

    private int uid;
    private int sum;

    public GetUserCFRequest(int uid, int sum) {
        this.uid = uid;
        this.sum = sum;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }
}
