package com.damon.cqrs.command;

import com.damon.cqrs.domain.Command;
import lombok.Data;
import lombok.ToString;

import java.util.concurrent.CompletableFuture;
@Data
@ToString
public class CommandContext<R> {
    private CompletableFuture<R> future;
    private Command command;
    private Object callback;
    public CommandContext(CompletableFuture<R> future) {
        this.future = future;
    }
}
