package ucthings.codec.message;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 消息remove handler
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/8/26 23:50
 */
@FunctionalInterface
public interface MessageRemoveHandler {


	void process(String id, TransportMessage<Map<String, Object>> message, CompletableFuture<ResponseMessage> result, String cause);

}
