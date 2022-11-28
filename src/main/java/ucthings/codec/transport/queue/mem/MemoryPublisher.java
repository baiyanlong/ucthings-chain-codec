package ucthings.codec.transport.queue.mem;


import ucthings.codec.transport.queue.QueuePublisher;

/**
 * 内存队列发布
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2021/12/28 21:11
 */
public interface MemoryPublisher extends QueuePublisher {

	/**
	 * 发布消息
	 *
	 * @param name    队列
	 * @param message 消息
	 */
	void send(String name, Object message);
}
