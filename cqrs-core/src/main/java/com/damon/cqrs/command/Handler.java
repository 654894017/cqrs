package com.damon.cqrs.command;

import com.damon.cqrs.domain.Command;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public interface Handler {
     CompletableFuture invoke(final Command command, final Function function);
}
