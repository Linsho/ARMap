package jp.ac.titech.itpro.sdl.vrmap;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class FieldRenderer implements GLSurfaceView.Renderer {
    private final static String TAG = FieldRenderer.class.getSimpleName();
    private static final float ALPHA = 0.9f;

    private Obj obj;
    private Obj field;
    private Obj point;

    private float azimuth;
    private float pitch;
    private float roll;
    protected final static float RAD2DEG = (float) (180/Math.PI);
    private float centerX;
    private float centerY;
    private float centerZ;
    private float upX;
    private float upY;
    private float upZ;
    private float relativeX = 0;
    private float relativeY = 0;
    private static final float[] blue = {0.0f, 0.0f, 1.0f, 1.0f};
    private static final float[] gray = {0.9f, 0.9f, 0.9f, 1.0f};


    FieldRenderer (){
        field = new Field();
        point = new Octahedron();
    }


    public FieldRenderer setObj(Obj obj){
        this.obj = obj;
        return this;
    }

    public FieldRenderer setAttitude(float[] attitude, float[] attitude2){


// Get the pitch, yaw and roll from the sensor.

//        float yaw = attitude[2];//z
//        float pitch = (float) (attitude[1]+Math.PI/2);//x
//        float roll = -attitude[0];//y
//        Log.i(TAG, "setAttitude: " + yaw*180/Math.PI + " " + pitch*180/Math.PI + " " + roll*180/Math.PI);
// Convert pitch, yaw and roll to a vector

//        float x = (float)(Math.cos(pitch) * Math.sin(roll));
//        float y = (float)(- Math.sin(pitch));
//        float z = (float)(Math.cos(pitch)*Math.cos(roll));
//        centerX = x;
//        centerY = y;
//        centerZ = z;
//        upX = (float)(-Math.sin(yaw)*Math.cos(roll) + Math.sin(roll)*Math.cos(yaw)*Math.sin(pitch));
//        upY = (float)(Math.cos(yaw)*Math.cos(pitch));
//        upZ = (float)(Math.sin(yaw)*Math.sin(roll)+Math.cos(roll)*Math.cos(yaw)*Math.sin(pitch));

//        centerX = (float)(Math.sin(roll)*Math.cos(yaw)+Math.cos(roll)*Math.sin(pitch)*Math.sin(yaw));
//        centerY = (float)(Math.sin(roll)*Math.sin(yaw)-Math.cos(roll)*Math.sin(pitch)*Math.cos(yaw));
//        centerZ = (float)(Math.cos(roll)*Math.cos(pitch));
//        upX = (float)(-Math.cos(pitch)*Math.sin(yaw));
//        upY = (float)(Math.cos(pitch)*Math.cos(yaw));
//        upZ = (float)(Math.sin(pitch));

        centerX = centerX*ALPHA + (1-ALPHA)*attitude[2];
        centerY = centerY*ALPHA + (1-ALPHA)*attitude[6];
        centerZ = centerZ*ALPHA + (1-ALPHA)*attitude[10];
        upX = upX*ALPHA + (1-ALPHA)*attitude[1];
        upY = upY*ALPHA + (1-ALPHA)*attitude[5];
        upZ = upZ*ALPHA + (1-ALPHA)*attitude[9];

        return this;
    }
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);
        gl.glEnable(GL10.GL_LIGHTING);
        gl.glEnable(GL10.GL_LIGHT0);
        float lightpos[] = {0.0f, 0.0f, 1.0f, 0.0f};
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_POSITION, lightpos, 0);
        float lightDiffuse[] = {1.0f, 1.0f, 1.0f};
        gl.glLightfv(GL10.GL_LIGHT0, GL10.GL_AMBIENT_AND_DIFFUSE, lightDiffuse, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluPerspective(gl, 30, ((float) width) / height, 1, 20000);

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        gl.glClearColor(137f/255f, 189f/255f, 222f/255f, 0.0f);
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
//        gl.glPushMatrix();
        GLU.gluLookAt(gl, 0, 0, 1.7f, -centerX, -centerY, -centerZ+1.7f, upX, upY, upZ);
//        Log.i(TAG, "onDrawFrame: " + centerX + " " + centerY + " " + centerZ);
        gl.glPushMatrix();
        gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT_AND_DIFFUSE, gray, 0);
        field.draw(gl);
        gl.glPopMatrix();
//        gl.glPopMatrix();
        if (relativeX != 0 && relativeY != 0){
            gl.glPushMatrix();
            gl.glTranslatef(relativeX, relativeY, 0);
            gl.glScalef(10, 10, 100);
            gl.glMaterialfv(GL10.GL_FRONT_AND_BACK, GL10.GL_AMBIENT_AND_DIFFUSE, blue, 0);
            point.draw(gl);
            gl.glPopMatrix();
        }


    }

    public FieldRenderer setRelativePosition(float x, float y){
        this.relativeX = x;
        this.relativeY = y;
        return this;
    }
}
