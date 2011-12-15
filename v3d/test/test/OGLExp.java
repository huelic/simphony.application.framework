package test;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.sun.opengl.util.BufferUtil;

public class OGLExp {

  private static final int LINE_COUNT = 20000;
  private GLCanvas canvas;
  private List<Line> lines = new ArrayList<Line>();
  private JButton button = new JButton("Go");

  public OGLExp() {
    final JFrame frame = new JFrame();
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    GLCapabilities caps = new GLCapabilities();
    caps.setSampleBuffers(true);
    caps.setNumSamples(4);
    canvas = new GLCanvas(caps);
    canvas.addGLEventListener(new ExpEvtListener());
    canvas.setAutoSwapBufferMode(false);

    frame.setLayout(new BorderLayout());
    frame.add(canvas, BorderLayout.CENTER);
    frame.setSize(new Dimension(800, 800));

    button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        run();
      }
    });

    frame.add(button, BorderLayout.NORTH);

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        frame.setVisible(true);
      }
    });
  }

  private void run() {
    lines.clear();
    for (int i = 0; i < LINE_COUNT; i++) {
      lines.add(new Line(canvas.getWidth(), canvas.getHeight()));
    }

    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        long start = new Date().getTime();
        for (int i = 0; i < 100; i++) {
          for (Line line : lines) {
            line.update();
          }
          canvas.display();
        }

        long duration = new Date().getTime() - start;
        System.out.printf("Duration: %f%n", duration / 1000f);
      }
    });
  }

  public static void main(String[] args) {
    new OGLExp();
  }

  class ExpEvtListener implements GLEventListener {

    int vboIndex, colorIndex;

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.media.opengl.GLEventListener#display(javax.media.opengl.GLAutoDrawable
     * )
     */
    @Override
    public void display(GLAutoDrawable drawable) {
      GL gl = drawable.getGL();
      gl.glClear(GL.GL_COLOR_BUFFER_BIT);

      gl.glLoadIdentity();
      gl.glColor3f(1, 0, 0);

      /*
       * for (int i = 0; i < lines.size(); i++) { lines.get(i).draw(gl); }
       */

      if (lines.size() > 0) {
      
        Map<Float, List<Line>> map = new HashMap<Float, List<Line>>();
        List<Line> ones = new ArrayList<Line>();
        for (Line line : lines) {
          float val = line.getWidth();
          if (val == 1.0) ones.add(line);
          else {
            List<Line> list = map.get(val);
            if (list == null) {
              list = new ArrayList<Line>();
              map.put(val, list);
            }
            list.add(line);
          }
        }
        map.put(1f, ones);
        
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboIndex);

        ByteBuffer buf = gl.glMapBuffer(GL.GL_ARRAY_BUFFER, GL.GL_WRITE_ONLY);
        buf.position(0);
        
        for (List<Line> list : map.values()) {
          for (Line line : list) {
            line.vboDraw(gl, buf);
          }
        }
       
        /*
        for (int i = 0; i < lines.size(); i++) {
          lines.get(i).vboDraw(gl, buf);
        }
        */
        gl.glUnmapBuffer(GL.GL_ARRAY_BUFFER);

        // draw the buffer in one go // vertices are separated by a color,
        // stride is the offset from the BEGINNING of one
        // vertex to the beginning of the next --
        // so its 2 (the vertex coords) + 3 (the color components)
        gl.glVertexPointer(2, GL.GL_FLOAT, 5 * BufferUtil.SIZEOF_FLOAT, 0);
        gl.glColorPointer(3, GL.GL_FLOAT, 5 * BufferUtil.SIZEOF_FLOAT, 2 * BufferUtil.SIZEOF_FLOAT);
        gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL.GL_COLOR_ARRAY);
        
        int start = 0;
        for (Map.Entry<Float, List<Line>> vals : map.entrySet()) {
          gl.glLineWidth(1f); //vals.getKey());
          int end =  vals.getValue().size() * 2;
          gl.glDrawArrays(GL.GL_LINES, start, end);
          start = end;
        }
        
        gl.glDisableClientState(GL.GL_COLOR_ARRAY);
        gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
        gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
      }

      drawable.swapBuffers();
    }

    /*
     * (non-Javadoc)
     * 
     * @seejavax.media.opengl.GLEventListener#displayChanged(javax.media.opengl.
     * GLAutoDrawable, boolean, boolean)
     */
    @Override
    public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.media.opengl.GLEventListener#init(javax.media.opengl.GLAutoDrawable
     * )
     */
    @Override
    public void init(GLAutoDrawable drawable) {
      System.out.println("init");
      GL gl = drawable.getGL();
      // turn off vsyncs
      gl.setSwapInterval(0);
      gl.glClearColor(1, 1, 1, 0);
      gl.glDisable(GL.GL_DEPTH_TEST);

      int[] indices = new int[1];
      gl.glGenBuffers(1, indices, 0);
      vboIndex = indices[0];
      gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboIndex);
      // buffer big enough for all the lines and color values
      // each line has 4 floats for vertices and 6 floats for color
      gl.glBufferData(GL.GL_ARRAY_BUFFER, LINE_COUNT * 10 * BufferUtil.SIZEOF_FLOAT, null,
          GL.GL_STREAM_DRAW);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.media.opengl.GLEventListener#reshape(javax.media.opengl.GLAutoDrawable
     * , int, int, int, int)
     */
    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
      System.out.println("reshape");
      GL gl = drawable.getGL();
      gl.glMatrixMode(GL.GL_PROJECTION);
      gl.glLoadIdentity();
      gl.glOrtho(0, width, 0, height, -1, 1);
      gl.glMatrixMode(GL.GL_MODELVIEW);
      gl.glPushMatrix();
      gl.glLoadIdentity();
      // for exact pixelization
      // see
      // http://www.opengl.org/resources/faq/technical/transformations.htm#tran0030
      gl.glTranslatef(0.375f, 0.375f, 0);

      // Make sure depth testing and lighting are disabled for 2D rendering
      gl.glDisable(GL.GL_DEPTH_TEST);
      gl.glDisable(GL.GL_LIGHTING);

    }

  }
}