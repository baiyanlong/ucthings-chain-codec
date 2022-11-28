package ucthings.codec.message.memory;


import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.Scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucthings.codec.common.executor.SystemExecutorService;
import ucthings.codec.message.MessagePool;
import ucthings.codec.message.MessageRemoveHandler;
import ucthings.codec.message.ResponseMessage;
import ucthings.codec.message.TransportMessage;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 内存消息池
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/3/2 22:15
 */
public class MemoryMessagePool implements MessagePool<TransportMessage<Map<String, Object>>, ResponseMessage> {

	/**
	 * 日志
	 */
	private final Logger logger;

	private final Cache<String, TransportMessage<Map<String, Object>>> cache;

	private final Map<String, CompletableFuture<ResponseMessage>> responses;

	private final Map<String, MessageRemoveHandler> removeHandlers;

	private final Map<String, Consumer<TransportMessage<Map<String, Object>>>> timeoutActions;

	private final MessageRemoveHandler defRemoveHandler;

	private final AtomicInteger size = new AtomicInteger(0);
	private final Integer maxSize;

	public MemoryMessagePool(int timeoutMs, int maxSize, ThreadPoolExecutor executor, MessageRemoveHandler handler) {
		this.logger = LoggerFactory.getLogger(MemoryMessagePool.class);
		this.removeHandlers = new ConcurrentHashMap<>(8);
		this.timeoutActions = new ConcurrentHashMap<>(8);
		this.defRemoveHandler = handler;
		this.responses = new ConcurrentHashMap<>(10000);
		this.maxSize = maxSize;
		this.cache = Caffeine.newBuilder().expireAfterWrite(timeoutMs, TimeUnit.MILLISECONDS)
				.scheduler(Scheduler.forScheduledExecutorService(SystemExecutorService.getSchedule())).maximumSize(maxSize).executor(executor).removalListener((id, message, cause) -> {
					// 明确移除和替换的不参与移除监听
					if (cause == RemovalCause.EXPLICIT || cause == RemovalCause.REPLACED) {
						return;
					}
					if (id != null && message instanceof TransportMessage) {
						@SuppressWarnings("unchecked") TransportMessage<Map<String, Object>> transportMessage = (TransportMessage<Map<String, Object>>) message;
						String messageId = (String) id;
						Consumer<TransportMessage<Map<String, Object>>> consumer = timeoutActions.remove(messageId);
						if (consumer != null) {
							consumer.accept(transportMessage);
						}
						CompletableFuture<ResponseMessage> future = responses.remove(messageId);
						MessageRemoveHandler removeHandler = removeHandlers.get((String) id);
						if (removeHandler != null) {
							removeHandler.process(messageId, transportMessage, future, cause.toString());
						} else if (defRemoveHandler != null) {
							defRemoveHandler.process(messageId, transportMessage, future, cause.toString());
						}
					}
				}).build();
	}


	/**
	 * 放入消息
	 *
	 * @param message       消息
	 * @param timeoutAction
	 * @param handler
	 * @return
	 */
	@Override
	public CompletableFuture<ResponseMessage> put(TransportMessage<Map<String, Object>> message, Consumer<TransportMessage<Map<String, Object>>> timeoutAction, MessageRemoveHandler handler) {
		String id = message.id();
		if (timeoutAction != null) {
			timeoutActions.put(id, timeoutAction);
		}
		if (handler != null) {
			removeHandlers.put(id, handler);
		}
		cache.put(id, message);
		size.incrementAndGet();
		CompletableFuture<ResponseMessage> future = new CompletableFuture<>();
		responses.put(id, future);
		return future;
	}

	/**
	 * 获取消息
	 *
	 * @param id 消息ID
	 */
	@Override
	public TransportMessage<Map<String, Object>> getRequest(String id) {
		return cache.getIfPresent(id);
	}


	/**
	 * 移除消息
	 *
	 * @param id 消息ID
	 */
	@Override
	public TransportMessage<Map<String, Object>> removeRequest(String id) {
		TransportMessage<Map<String, Object>> message = cache.getIfPresent(id);
		cache.invalidate(id);
		timeoutActions.remove(id);
		removeHandlers.remove(id);
		size.decrementAndGet();
		return message;
	}


	/**
	 * 获取响应结果
	 *
	 * @param id 消息ID
	 */
	@Override
	public CompletableFuture<ResponseMessage> getResponseFuture(String id) {
		return responses.get(id);
	}


	/**
	 * 删除响应结果
	 *
	 * @param id 消息ID
	 */
	@Override
	public CompletableFuture<ResponseMessage> removeResponseFuture(String id) {
		return responses.remove(id);
	}

	/**
	 * 清除
	 */
	@Override
	public void clear() {
		timeoutActions.clear();
		removeHandlers.clear();
		cache.invalidateAll();
		size.set(0);
		removeHandlers.clear();
	}

	/**
	 * 大小
	 */
	@Override
	public int size() {
		return size.get();
	}

	public int maxSize() {
		return maxSize;
	}

	@Override
	public void response(ResponseMessage response) {
		String id = response.id();
		CompletableFuture<ResponseMessage> future = getResponseFuture(id);
		if (future != null) {
			future.complete(response);
		}
		removeRequest(id);
		removeResponseFuture(id);
	}
}
