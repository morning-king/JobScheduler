package com.cn.dmd.config;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * @author morningking
 * @date 2017/7/13 14:03
 * @contact 243717042@qq.com
 */
@Slf4j
@AllArgsConstructor
public class ApplicationStatus {
    private final String LOCAL_MACHINE_IP_ADDRESS;
    private final int LOCAL_EMBED_SERVLET_CONTAINER_LISTEN_PORT;
    private final String LOCAL_EMBED_SERVLET_CONTAINER_SOCKET_INFO;

    /**
     * @return 获取本机ip信息
     */
    public String getLocalIp() {
        return LOCAL_MACHINE_IP_ADDRESS;
    }

    /**
     * @return 获取servlet容器监听的端口
     */
    public int getServletContainerListenPort() {
        return LOCAL_EMBED_SERVLET_CONTAINER_LISTEN_PORT;
    }

    /**
     * @return 获取servlet容器socket信息
     */
    public String getServletContainerSocketInfo() {
        return LOCAL_EMBED_SERVLET_CONTAINER_SOCKET_INFO;
    }
}
