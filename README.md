# cqrs
cqrs,event sourcing,in memory,dubbo,spring clound


使用示例：

//1.初始化商品库存管理服务
GoodsStockService goodsStockService = new GoodsStockService(committingService);

//2.初始化商品
GoodsAddCommand command = new GoodsAddCommand(IdWorker.getId(), 2, "iphone 6 plus", 1000);
goodsStockService.process(command, () -> new Goods(2, command.getName(), command.getCount()), 5).join();

//3.库存+1
GoodsStockAddCommand command = new GoodsStockAddCommand(IdWorker.getId(), 5);
Goods goods = goodsStockService.process(command, goods -> goods.addStock(1, name)).join();
