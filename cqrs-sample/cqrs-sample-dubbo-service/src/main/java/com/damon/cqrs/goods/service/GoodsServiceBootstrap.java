package com.damon.cqrs.goods.service;

import com.damon.cqrs.goods.api.IGoodsCommandService;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.apache.dubbo.config.ServiceConfig;
import org.apache.dubbo.config.bootstrap.DubboBootstrap;

public class GoodsServiceBootstrap {

    public static void main(String[] args) {
        // 定义具体的服务
        ServiceConfig<IGoodsCommandService> service = new ServiceConfig<>();
        service.setInterface(IGoodsCommandService.class);
        service.setRef(new GoodsCommandService(TestConfig.init()));
        service.setLoadbalance("consistenthash");
        service.setRetries(0);
        service.setTimeout(5000);

        ProtocolConfig protocolConfig = new ProtocolConfig("dubbo", 20880);
        protocolConfig.setSerialization("kryo");
        // 启动 Dubbo
        DubboBootstrap.getInstance()
                .application("first-dubbo-provider")
                .registry(new RegistryConfig("zookeeper://127.0.0.1:2181"))
                .protocol(protocolConfig)
                .service(service)
                .start()
                .await();
    }


}