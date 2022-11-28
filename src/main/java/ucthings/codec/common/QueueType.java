package ucthings.codec.common;

/**
 * 队列类型
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2021/12/10 22:56
 */
public enum QueueType {

	RABBITMQ(1, "RABBITMQ", "RABBITMQ队列"),


	KAFKA(2, "KAFKA", "KAFKA队列"),


	PULSAR(3, "PULSAR", "PULSAR队列"),


	ROCKETMQ(4, "ROCKETMQ", "ROCKETMQ队列"),


	ACTIVEMQ(5, "ACTIVEMQ", "ACTIVEMQ队列"),


	ZEROMQ(6, "ZEROMQ", "ZEROMQ队列"),


	MEMORY(7, "MEMORY", "内存队列"),


	FILE(8, "FILE", "FILE队列");

	/**
	 * code
	 */
	private final int code;

	/**
	 * 消息类型
	 */
	private final String type;

	/**
	 * 描述
	 */
	private final String desc;

	QueueType(int code, String type, String desc) {
		this.code = code;
		this.type = type;
		this.desc = desc;
	}

	public int getCode() {
		return code;
	}

	public String getType() {
		return type;
	}

	public String getDesc() {
		return desc;
	}

}
