package ucthings.codec.transport.queue;


import ucthings.codec.message.TransportMessage;

import java.util.Map;

/**
 * 队列发布
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/4/3 17:17
 */
public interface QueuePublisher {

	/**
	 * 发布消息
	 *
	 * @param topic   主题
	 * @param message 消息
	 */
	void send(String topic, TransportMessage<Map<String, Object>> message);


}
