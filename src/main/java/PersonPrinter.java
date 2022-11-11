import com.eclipsesource.v8.JavaVoidCallback;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;

/**
 * @author byl
 * @since 2022/11/11 16:32
 */
public class  PersonPrinter implements JavaVoidCallback {

	@Override
	public void invoke(V8Object receiver, V8Array parameters) {
		if (parameters.length() > 0) {
			Object arg1 = parameters.get(0);
			System.out.println(arg1);
			if (arg1 instanceof Releasable) {
				((Releasable) arg1).release();
			}
		}
	}
}



