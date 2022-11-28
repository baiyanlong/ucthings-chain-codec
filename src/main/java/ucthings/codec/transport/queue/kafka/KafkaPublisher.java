package ucthings.codec.transport.queue.kafka;


import ucthings.codec.transport.queue.QueuePublisher;

/**
 * kafka 发布
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2021/12/28 21:11
 */
public interface KafkaPublisher extends QueuePublisher {

	/**
	 * 发布消息
	 *
	 * @param topic 主题
	 * @param data  消息
	 */
	void send(String topic, String data);

}