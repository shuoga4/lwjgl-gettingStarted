import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class HelloWorld {

    // The window handle
    private long window;

    public void run() {
        System.out.println("Hello LWJGL " + Version.getVersion() + "!");

        init();
        loop();

        //ここから違う。なにかウィンドウが消されたあとの処理を書いている.

        // Free the window callbacks and destroy the window
        // このFreeCallbacksはコールバック領域を開放するが、windowについてないコールバックである
        // SetErrorCallbackは個別に開放する必要がある。
        // destroyWindowがコールされると、イベントがwindowに送られなくなる。
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        // これが、すべてのwindowの破壊 + glfw用に割り当てられたスペースを削除する
        glfwTerminate();
        //この下のは多分コールバックの開放だけど、わからない。
        glfwSetErrorCallback(null).free();
        // 最初に作ったエラー用コールバックの個別の開放だった.
    }

    private void init() {
        // すべてのイベントはコールバックを通してwindowから渡される。キー入力、windowの位置や大きさ、エラーなど。
        // Cの特徴である、引数を使って結果を返す仕様を用いてCallbackしている。
        // glfwにおいて、初期化の前にする重要な関数であるエラーのコールバックの設定。
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if ( !glfwInit() )
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
        // ここで、glfwのversionを指定することで特定の初期化エラーを解決できるかも。

        // Create the window
        window = glfwCreateWindow(500, 800, "Hello World!", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");

        //-------------------------------------
        // 作成手順としては、
        // まずエラーのコールバックの設定、glfwInit()の成功確認(失敗のException作成)、
        // WindowHintの設定(基本default設定なのでDefaultWindowHintsはいらない)、
        // windowの作成、windowに紐づくコールバックの作成
        // となる。
        //ここまで同じ
        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        // この関数で、windowが受けたinputなどたくさんのコールバックを受け取れる。
        // window毎に変えられる。最初の引数がwindowで、次の引数群は関数内で使える変数。

        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE ) {
                System.out.println("hi!");
                glfwSetWindowShouldClose(window, true);
            }
                // We will detect this in the rendering loop // ループの中にコールバックが入っていなくても、inputやeventが起こるとすぐ帰って来るから // コールバックと呼ぶのかも。だから初期化フェーズとループフェーズを分離しているのか?

        });

        // Get the thread stack and push a new frame
        try ( MemoryStack stack = stackPush() ) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
            System.out.println("Resolution: " + vidmode.width() + "x" + vidmode.height());
            System.out.println("Window size: " + pWidth.get(0) + "x" + pHeight.get(0));
        } // the stack frame is popped automatically

        // ---------------------------
        //ここまで変、あと同じ
        // これは、OpenGLのAPIを使うときに必要で、contextを指定する必要がある。
        // 次のcontextを指定するか、現在のcontextを持つwindowが破壊されるまで持続。
        // Make the OpenGL context current
        glfwMakeContextCurrent(window);
        // Enable v-sync
        // SwapのIntervalとは、バックバッファとフロントバッファの入れ替え頻度のこと。
        // 1だと、1frameに1回切り替える。垂直同期と呼ばれる。0だと、バックバッファの描画が終わった瞬間フロントバッファに切り替える。
        // tearingが発生したり、たくさんCPUとGPUを使う。
        // 逆に1frame以上にすると入力と結果の描画に遅延が発生する。この関数は現在のcontextがwindowに指定されている限り持続する。

        glfwSwapInterval(1);

        // Make the window visible
        glfwShowWindow(window);
    }

    private void loop() {
        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();
        // 以降OpenGL機能解禁

        // Set the clear color
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        // 同じ
        while ( !glfwWindowShouldClose(window) ) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

            // GLFWのwindowは、デフォルトでダブルバファリングを使っている。フロントとバックバッファ。
            // バックバッファに描画が終了したら、フロントバッファをバックバッファに、バックバッファをフロントバッファに切り替える。
            // それを切り替えるのがこのSwapBuffers。
            glfwSwapBuffers(window); // swap the color buffers

            glfwPollEvents();
            //これのおかげでeventをコールバックできるっぽい。　
            //
        }
    }

    public static void main(String[] args) {
        new HelloWorld().run();
    }
}