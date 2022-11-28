package ucthings.codec.transport.impl;


import lombok.Getter;
import ucthings.codec.message.MessagePool;
import ucthings.codec.message.RequestMessage;
import ucthings.codec.message.ResponseMessage;
import ucthings.codec.message.TransportMessage;
import ucthings.codec.transport.TransportApi;
import ucthings.codec.transport.queue.QueuePublisher;
import ucthings.codec.transport.queue.QueueSubscriber;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 公共传输服务
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/8/27 22:48
 */
public class CommonTransportService implements TransportApi {

	/**
	 * 消息池
	 */
	private final MessagePool<TransportMessage<Map<String, Object>>, ResponseMessage> pool;

	/**
	 * 队列发布
	 */
	private final QueuePublisher publisher;

	@Getter
	private final QueueSubscriber subscriber;

	public CommonTransportService(MessagePool<TransportMessage<Map<String, Object>>, ResponseMessage> pool,
								  QueuePublisher publisher, QueueSubscriber subscriber) {
		this.pool = pool;
		this.subscriber = subscriber;
		this.publisher = publisher;
	}


	/**
	 * 发送消息
	 *
	 * @param message 消息
	 */
	@Override
	public void sendMessage(RequestMessage message) {
		publisher.send(message.topic(), message);
	}

	/**
	 * 发起请求
	 *
	 * @param request 请求
	 */
	@Override
	public CompletableFuture<ResponseMessage> request(RequestMessage request) {
		int size = pool.size();
		if (size > pool.maxSize()) {
			throw new RuntimeException("消息池已满" + size);
		}
		//发送消息
		publisher.send(request.topic(), request);
		return pool.put(request, null, null);
	}


	/**
	 * 响应
	 *
	 * @param response 响应
	 */
	@Override
	public void response(ResponseMessage response) {
		pool.response(response);
	}
}
