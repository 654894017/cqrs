### cqrs
支持集群扩容、缩容聚合冲突事件回溯、聚合根在内存、聚合根快照、可集成dubbo、spring cloud、事件组提交。

### 架构概述
      Event Store   Projections
        +----+        +----+
        |    |        |    |
        | DB |        | DB |
        +--+-+        +-+--+
          ^             ^
          |             |
    +------------+------------+
    |     |      |      |     |
    |     |    Events   |     |
    |     +------+----+ |     |
    |     |      |    | |     |
    |     +      |    v +     |
    |   Domain   |   Read     |
    |   Model    |   Model    |
    |            |            |
    +------------+------------+
    |                         |
    |           API           |
    |                         |
    +-------------------------+ 


### 使用示例：

//1.初始化商品库存管理服务

GoodsStockService service = new GoodsStockService(committingService);

//2.初始化商品

GoodsAddCommand command = new GoodsAddCommand(IdWorker.getId(), 2, "iphone 6 plus", 1000);

Goods goods1 = service.process(command, () -> new Goods(2, command.getName(), command.getCount())).join();

//3.库存+1

GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 5);

Goods goods2 = service.process(command, goods -> goods.addStock(1)).join();


### 注意事项

1.如果使用dubbo、spring cloud负载均衡策略请选择一致性hash，这样可以减少在集群扩容、缩容聚合根回溯的成本。
2.关闭dubbo、spring cloud的失败重试。
3.Dubbo服务抛出该异常AggregateEventConflictException，可以发起请求。(出现此异常的原因是当前聚合根在多个实例中存在（集群扩容时），可以捕获此异常然后重新在client发起调用，当前的请求会负载到新的实例上。)

### 测试报告

CPU:I7-3740QM（4核8线程）   24G内存   mysql 5.7   ssd(早期固态硬盘)  jdk1.8

性能数据：

商品添加：6.5K TPS/s

单个商品库存添加：1.4W TPS/S

三个商品库存添加：3w TPS/S     mysql cpu：18%  mysql内存占用：300M ， jvm cpu: 20% jvm 内存占用：1.8G 






