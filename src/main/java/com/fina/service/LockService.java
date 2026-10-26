package com.fina.service;

import java.util.Collection;
import java.util.function.Supplier;

public interface LockService {

    <T> T withLock(String key, Supplier<T> action);

    <T> T withLocks(Collection<String> keys, Supplier<T> action);
}
