### cqrs
cqrs,ddd,event-sourcing,in-memory,dubbo,spring cloud, group-commit,event-mailbox


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

### 测试报告

CPU:I7-3740QM（4核8线程）   24G内存   mysql 5.7   ssd(早期固态硬盘)  jdk1.8

性能数据：

商品添加：6.5K TPS/s

单个商品库存添加：1.4W TPS/S

三个商品库存添加：3w TPS/S     mysql cpu：18%  mysql内存占用：300M ， jvm cpu: 20% jvm 内存占用：1.8G 






