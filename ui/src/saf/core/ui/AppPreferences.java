package saf.core.ui;

import saf.core.ui.dock.Perspective;

import java.awt.*;
import java.util.List;
import java.util.StringTokenizer;
import java.util.prefs.Preferences;

/**
 * @author Nick Collier
 * @version $Revision: 1.1 $ $Date: 2006/06/01 16:30:52 $
 */
public class AppPreferences {

  public static final String SAVE_PATH_KEY = "SAVE_PATH_KEY";
  private static final String APP_BOUNDS = "APP_BOUNDS";

  private String nodeName;
  private Rectangle rect;
  private List<Perspective> perspectives;

  public AppPreferences(String nodeName, java.util.List<Perspective> perspectives) {
    this.nodeName = nodeName;
    Preferences prefs = Preferences.userRoot().node(nodeName);
    String rectData = prefs.get(APP_BOUNDS, "-1, -1, -1, -1");
    StringTokenizer tok = new StringTokenizer(rectData, ",");
    int x = Integer.parseInt(tok.nextToken().trim());
    if (x != -1) {
      int y = Integer.parseInt(tok.nextToken().trim());
      int width = Integer.parseInt(tok.nextToken().trim());
      int height = Integer.parseInt(tok.nextToken().trim());
      rect = new Rectangle(x, y, width, height);
    }

    this.perspectives = perspectives;
  }

  /**
   * Gets the last rectangle stored as the app bounds preference. If no bounds
   * have been saved, yet then the default values will be used.
   * 
   * @param defaultX
   *          the default x location
   * @param defaultY
   *          the default y location
   * @param defaultWidth
   *          the default width
   * @param defaultHeight
   *          the default height
   * 
   * @return the last rectangle stored as the app bounds preference.
   */
  public Rectangle getApplicationBounds(int defaultX, int defaultY, int defaultWidth,
      int defaultHeight) {
    if (rect == null) {
      rect = new Rectangle(defaultX, defaultY, defaultWidth, defaultHeight);
    }
    return rect;
  }

  public void useSavedViewLayout(String path) {
    Preferences prefs = Preferences.userRoot().node(nodeName);
    prefs.put(SAVE_PATH_KEY, path);
    for (Perspective perspective : perspectives) {
      perspective.loadLayout(prefs);
    }
  }
  
  public boolean usingSavedViewLayout() {
    return !Preferences.userRoot().node(nodeName).get(SAVE_PATH_KEY, "").equals("");
  }

  /**
   * Saves the current view layout (fill percentages) to the preference store.
   */
  public void saveViewLayout() {
    Preferences prefs = Preferences.userRoot().node(nodeName);
    for (Perspective perspective : perspectives) {
      perspective.saveLayout(prefs);
    }
  }

  /**
   * Saves the specified rectangle as the application bounds.
   * 
   * @param rect
   *          the current application bounds
   */
  public void saveApplicationBounds(Rectangle rect) {
    Preferences prefs = Preferences.userRoot().node(nodeName);
    prefs.put(APP_BOUNDS, rect.x + ", " + rect.y + ", " + rect.width + ", " + rect.height);
  }
}
