package com.damon.cqrs.disruptor;

import com.damon.cqrs.domain.Command;

public  class GoodsCreateCmd extends Command {
        private Long goodsId;

        public GoodsCreateCmd(Long commandId, Long aggregateId) {
            super(commandId, aggregateId);
            this.goodsId = aggregateId;
        }
    }