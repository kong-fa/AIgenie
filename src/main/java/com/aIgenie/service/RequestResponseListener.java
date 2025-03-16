package com.aIgenie.service;

/**
 * 请求和响应监听器接口
 * 用于监听AI请求和响应的回调
 */
public interface RequestResponseListener {
    /**
     * 当有新的请求和响应时被调用
     * 
     * @param request 发送到AI的请求内容（JSON格式）
     * @param response 从AI收到的响应内容（JSON格式）
     */
    void onRequestResponse(String request, String response);
} 