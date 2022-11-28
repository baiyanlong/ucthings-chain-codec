package ucthings.codec.log;

/**
 * 队列消息日志
 *
 * @author xigexb
 * @version 1.0.0
 * @since 2022/10/29 14:05
 */
public class QueueMessageLog extends UcKvLog {

	public QueueMessageLog(String group, String type, String level, int kvSize, String msg) {
		super(group, type, level, kvSize, msg);
	}
}
