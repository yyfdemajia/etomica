package etomica.graphics;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import etomica.units.Pixel;

public interface DisplayCanvasInterface {

    //Boundary Constants
    static final int DRAW_BOUNDARY_NONE = 0;
    static final int DRAW_BOUNDARY_OUTLINE = 1;
    static final int DRAW_BOUNDARY_SHELL = 2;
    static final int DRAW_BOUNDARY_ALL = 3;
    static final int DRAW_BOUNDARY_MAX = 4;
    
    public void createOffScreen();
    public void createOffScreen(int p);
    public void createOffScreen(int w, int h);
    public void doPaint(Graphics g);
    public void update(Graphics g);
    public void paint(Graphics g);
    public void setMovable(boolean b);
    public boolean isMovable();
    public void setResizable(boolean b);
    public boolean isResizable();
    public void setWriteScale(boolean s);
    public boolean getWriteScale();
    public void setDrawBoundary(int b);
    public int getDrawBoundary();
    //public boolean getDrawOverflow();
    //public void setDrawOverflow(boolean b);
    //public double getScale();
    //public void setScale(double s);
    //public int getImageShells();
    //public void setImageShells(int n);
    public void addMouseListener(MouseListener listener);
    public void addMouseMotionListener(MouseMotionListener listener);
    public void addKeyListener(KeyListener listener);
    public void setSize(int width, int height);
    public void reshape(int width, int height);
    public void setMinimumSize(Dimension temp);
    public void setMaximumSize(Dimension temp);
    public void setPreferredSize(Dimension temp);
    public void setBounds(int x, int y, int width, int height);
    public void setPixelUnit(Pixel pixel);
    public Pixel getPixelUnit();
    public java.awt.Dimension getSize();
    public void repaint();
    public void requestFocus();
    public void transferFocus();
    public void setShiftX(float x);
    public void setShiftY(float y);
    public void setPrevX(float x);
    public void setPrevY(float y);
    public void setXRot(float x);
    public void setYRot(float y);
    public void setZoom(float z);
    public float getShiftX();
    public float getShiftY();
    public float getPrevX();
    public float getPrevY();
    public float getXRot();
    public float getYRot();
    public float getZoom();
    public void startRotate(float x, float y);
    public void stopRotate();
}

