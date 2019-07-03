package jp.ac.titech.itpro.sdl.vrmap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class Octahedron implements Obj{
    private final static float[] VERTICES = {
            // left
            0, 1, 1,
            0, 0, 2,
            1, 0, 1,
            // right
            1, 0, 1,
            0, 0, 2,
            0, -1, 1,
            // back
            0, -1, 1,
            0, 0, 2,
            -1, 0, 1,
            // front
            -1, 0, 1,
            0, 0, 2,
            0, 1, 1,
            // left
            0, 1, 1,
            0, 0, 0,
            1, 0, 1,
            // right
            1, 0, 1,
            0, 0, 0,
            0, -1, 1,
            // back
            0, -1, 1,
            0, 0, 0,
            -1, 0, 1,
            // front
            -1, 0, 1,
            0, 0, 0,
            0, 1, 1
    };

    private final FloatBuffer vbuf;

    Octahedron() {
        vbuf = ByteBuffer
                .allocateDirect(VERTICES.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vbuf.put(VERTICES);
        vbuf.position(0);
    }

    @Override
    public void draw(GL10 gl) {
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, vbuf);

        // left
        gl.glNormal3f(1, 1, 1);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 0, 3);

        // right
        gl.glNormal3f(1, -1, 1);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 3, 3);

        // back
        gl.glNormal3f(-1, -1, 1);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 6, 3);

        // front
        gl.glNormal3f(-1, 1, 1);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 9, 3);

        // left
        gl.glNormal3f(1, 1, -1);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 12, 3);

        // right
        gl.glNormal3f(1, -1, -1);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 15, 3);

        // back
        gl.glNormal3f(-1, -1, -1);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 18, 3);

        // front
        gl.glNormal3f(-1, 1, -1);
        gl.glDrawArrays(GL10.GL_TRIANGLES, 21, 3);
    }
}
