package ucthings.codec.message;

import lombok.Getter;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 响应消息
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/11/14 09:50
 */
@Getter
public class ResponseMessage implements TransportMessage<Map<String, Object>> {

	/**
	 * ID
	 */
	private final String id;

	/**
	 * code
	 */
	private final String code;

	/**
	 * topic
	 */
	private final String topic;

	private final AtomicBoolean state = new AtomicBoolean(false);

	/**
	 * 数据
	 */
	private final Map<String, Object> data;

	public ResponseMessage(String id, String code, String topic, Map<String, Object> data) {
		this.id = id;
		this.code = code;
		this.topic = topic;
		this.data = Collections.unmodifiableMap(Objects.requireNonNullElseGet(data, HashMap::new));
	}

	/**
	 * 请求/响应代码
	 *
	 * @return code
	 */
	@Override
	public String code() {
		return code;
	}

	/**
	 * 消息ID
	 *
	 * @return id
	 */
	@Override
	public String id() {
		return id;
	}

	/**
	 * 数据
	 *
	 * @return T data
	 */
	@Override
	public Optional<Map<String, Object>> data() {
		return Optional.ofNullable(data);
	}

	/**
	 * 主题
	 *
	 * @return 主题
	 */
	@Override
	public String topic() {
		return this.topic;
	}

	/**
	 * 请求状态
	 * true 为请求
	 * false 为响应
	 *
	 * @return true
	 */
	@Override
	public boolean state() {
		return state.get();
	}

}
