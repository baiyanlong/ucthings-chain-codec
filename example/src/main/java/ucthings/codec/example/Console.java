package ucthings.codec.example;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Object;

/**
 * @author byl
 * @since 2022/11/11 16:03
 */
public class Console {


		public static  Console  newInstance() {
			return new Console();
		}
		public void log(final Object message) {
			System.out.println("[INFO] " + message);
		}

		public void err(final Object message) {
			System.out.println("[ERROR] " + message);
		}



	public V8Object registerConsoleLog(V8 runtime) {
		//反射注入
		Console console = new Console();
		V8Object v8Console = new V8Object(runtime);
		runtime.add("console", v8Console);
		v8Console.registerJavaMethod(console, "log", "log", new Class<?>[]{Object.class});
		v8Console.registerJavaMethod(console, "err", "err", new Class<?>[]{Object.class});
		return v8Console;
	}

}
