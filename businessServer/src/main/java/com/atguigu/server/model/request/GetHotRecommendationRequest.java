package com.atguigu.server.model.request;

public class GetHotRecommendationRequest {

    private int sum;

    public GetHotRecommendationRequest(int sum) {
        this.sum = sum;
    }

    public int getSum() {
        return sum;
    }

    public void setSum(int sum) {
        this.sum = sum;
    }
}
