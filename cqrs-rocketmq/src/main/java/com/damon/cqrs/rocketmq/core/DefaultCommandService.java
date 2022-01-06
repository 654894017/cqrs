package com.damon.cqrs.rocketmq.core;

import com.alipay.remoting.rpc.RpcServer;
import com.damon.cqrs.domain.Command;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DefaultCommandService implements ICommandService {

    private RpcServer server;

    private CommandResponseProcessorAsync processor = new CommandResponseProcessorAsync();

    private IMessageSendService sendMessageService;

    public DefaultCommandService() {
        /**
         * 创建 RpcServer 实例，指定监听 port
         */
        server = new RpcServer(8888);
        /**
         * 注册业务逻辑处理器 UserProcessor
         */
        server.registerUserProcessor(processor);
        /**
         * 启动服务端：先做 netty 配置初始化操作，再做 bind 操作
         * 配置 netty 参数两种方式：[SOFABolt 源码分析11 - Config 配置管理的设计](https://www.jianshu.com/p/76b0be893745)
         */
        server.startup();
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {
        final CompletableFuture<String> future = new CompletableFuture<String>();

        new Thread(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {

            }
            future.complete("asdfa");
        }).start();

        String result = future.get(5, TimeUnit.SECONDS);
        System.out.println(result);

    }

    /**
     * 执行command命令
     */
    public CompletableFuture<CommandResult> executeAsync(Command command, CommandReturnType returnType) {
        CompletableFuture<CommandResult> future = new CompletableFuture<>();
        // 注册回调
        processor.addCommandResultCallback(command.getCommandId(), future);
        // 发送消息到消息队列
        return sendMessageService.sendMessage(command).thenCompose(result -> future);
    }

}
