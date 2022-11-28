package ucthings.codec.message;

import java.io.Serializable;
import java.util.Optional;

/**
 * 传输消息
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/11/14 09:50
 */
public interface TransportMessage<T> extends Serializable {

	/**
	 * 请求/响应代码
	 *
	 * @return code
	 */
	String code();

	/**
	 * 消息ID
	 *
	 * @return id
	 */
	String id();

	/**
	 * 数据
	 *
	 * @return T data
	 */
	Optional<T> data();

	/**
	 * 主题
	 *
	 * @return 主题
	 */
	String topic();

	/**
	 * 请求状态
	 * true 为请求
	 * false 为响应
	 *
	 * @return true
	 */
	boolean state();

}
