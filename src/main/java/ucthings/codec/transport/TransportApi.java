package ucthings.codec.transport;


import ucthings.codec.message.RequestMessage;
import ucthings.codec.message.ResponseMessage;
import ucthings.codec.transport.queue.QueueSubscriber;

import java.util.concurrent.CompletableFuture;

/**
 * 传输层API
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/11/14 09:50
 */
public interface TransportApi {

	/**
	 * 发送消息
	 *
	 * @param message 消息
	 */
	void sendMessage(RequestMessage message);

	/**
	 * 发起请求
	 *
	 * @param request 请求
	 */
	CompletableFuture<ResponseMessage> request(RequestMessage request);

	/**
	 * 响应
	 *
	 * @param response 响应
	 */
	void response(ResponseMessage response);

	/**
	 * 获取订阅器
	 *
	 * @return 订阅器
	 */
	QueueSubscriber getSubscriber();


}
