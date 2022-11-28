package ucthings.codec.transport;

/**
 * 队列消息处理器
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2021/12/28 17:28
 */
public interface QueueMessageHandler {

	/**
	 * handler id
	 *
	 * @return id
	 */
	String id();

	/**
	 * 初始化
	 */
	void init(String topic);

	/**
	 * 消息处理
	 *
	 * @param topic   队列名称
	 * @param message 消息
	 * @return object
	 */
	Object onMessage(String topic, Object message) throws InterruptedException;

	/**
	 * 错误情况
	 *
	 * @param message   消息
	 * @param handler   handler
	 * @param throwable 错误情况
	 */
	void onExceptionCaught(Throwable throwable, QueueMessageHandler handler, Object message);

	/**
	 * 下一个handler
	 *
	 * @return MessageHandler
	 */
	QueueMessageHandler next();

	/**
	 * destroy
	 */
	void destroy();

	/**
	 * 运行状态
	 */
	boolean runState();
}
