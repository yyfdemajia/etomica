package etomica.virial.cluster2.ui;

import java.awt.Dimension;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.WindowConstants;

import com.jgoodies.looks.LookUtils;

public abstract class FrameView extends JFrame implements View {

  private static final long serialVersionUID = -2713944570258412752L;
  // Frame's Preferred Dimensions
  public static final Dimension FP_DIM1 = new Dimension(560, 420);
  public static final Dimension FP_DIM2 = new Dimension(640, 480);
  public static final Dimension FP_DIM3 = new Dimension(720, 540);
  public static final Dimension FP_DIM4 = new Dimension(800, 600);

  protected FrameView(final String frameTitle) {

    super(frameTitle);
  }

  // Creates the client area of the frame.
  protected abstract JComponent buildContentPane();

  // Creates the main menu of the frame.
  protected JMenuBar buildMainMenu() {

    return null;
  }

  public Map<String, Object> getData() {

    return null;
  }

  // Returns the resource name of this frame's icon
  protected String getIconName() {

    return null;
  }

  // Determines this frame's preferred size in high resolution
  protected Dimension getPreferredHighResSize() {

    return FP_DIM3;
  }

  // Determines this frame's preferred size in low resolution
  protected Dimension getPreferredLowResSize() {

    return FP_DIM2;
  }

  // Determines if this frame should display maximized
  protected boolean isMaximized() {

    return false;
  }

  public void activate() {

    setContentPane(buildContentPane());
    JMenuBar menu = buildMainMenu();
    if (menu != null) {
      setJMenuBar(menu);
    }
    configure();
    setVisible(true);
    if (isMaximized()) {
      setExtendedState(JFrame.MAXIMIZED_BOTH);
    }
  }

  public void configure() {

    if (getIconName() != null) {
      setIconImage(ApplicationUI.readImageIcon(getIconName()).getImage());
    }
    setSize(getPreferredDefaultSize());
    setPreferredSize(getPreferredDefaultSize());
    Dimension paneSize = getSize();
    Dimension screenSize = getToolkit().getScreenSize();
    setLocation((screenSize.width - paneSize.width) / 2,
        (screenSize.height - paneSize.height) / 2);
    setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
  }

  protected Dimension getPreferredDefaultSize() {

    return LookUtils.IS_LOW_RESOLUTION ? getPreferredLowResSize()
        : getPreferredHighResSize();
  }
}