package ucthings.codec.message;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * 消息池
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/3/1 10:59
 */
public interface MessagePool<Req extends TransportMessage<Map<String, Object>>, Resp extends ResponseMessage> {


	/**
	 * 放入消息
	 *
	 * @param message 消息
	 */
	CompletableFuture<ResponseMessage> put(Req message, Consumer<Req> timeoutAction, MessageRemoveHandler handler);


	/**
	 * 获取消息
	 *
	 * @param id 消息ID
	 */
	TransportMessage<Map<String, Object>> getRequest(String id);


	/**
	 * 移除消息
	 *
	 * @param id 消息ID
	 */
	TransportMessage<Map<String, Object>> removeRequest(String id);


	/**
	 * 获取响应结果
	 *
	 * @param id 消息ID
	 */
	CompletableFuture<ResponseMessage> getResponseFuture(String id);


	/**
	 * 删除响应结果
	 *
	 * @param id 消息ID
	 */
	CompletableFuture<ResponseMessage> removeResponseFuture(String id);


	/**
	 * 清除
	 */
	void clear();


	/**
	 * 大小
	 */
	int size();

	/**
	 * 最大大小
	 */
	int maxSize();

	void response(Resp response);
}
