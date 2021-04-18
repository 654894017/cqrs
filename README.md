### cqrs
cqrs,ddd,event-sourcing,in-memory,dubbo,spring cloud


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

Goods goods2 = service.process(command, goods -> goods.addStock(1, name)).join();
