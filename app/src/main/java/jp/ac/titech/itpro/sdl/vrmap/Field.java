package jp.ac.titech.itpro.sdl.vrmap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class Field implements Obj {
    private final static float[] VERTICES = {
        -10000, -10000, 0,
        -10000, 10000, 0,
        10000, 10000, 0,
        10000, -10000, 0,

    };

    private final FloatBuffer vbuf;

    Field (){
        vbuf = ByteBuffer
                .allocateDirect(VERTICES.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vbuf.put(VERTICES);
        vbuf.position(0);    }

    @Override
    public void draw(GL10 gl) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vbuf);

        gl.glNormal3f(0, 0, 1);
        gl.glDrawArrays(GL10.GL_TRIANGLE_FAN, 0, 4);
    }
}
