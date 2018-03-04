package ui;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.Layer;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import query.Query;
import twitter.LiveTwitterSource;
import twitter.TwitterSource;
import util.SphericalGeometry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.Timer;

/**
 * The Twitter viewer application
 * Derived from a JMapViewer demo program written by Jan Peter Stotz
 */
public class Application extends JFrame {
    // The content panel, which contains the entire UI
    private final ContentPanel contentPanel;
    // The provider of the tiles for the getMap, we use the Bing source
    private BingAerialTileSource bing;
    // All of the active queries
    private List<Query> queries = new ArrayList<>();
    // The source of tweets, a TwitterSource, either live or playback
    private TwitterSource twitterSource;

    private void initialize() {
        // To use the live twitter stream, use the following line
        // twitterSource = new LiveTwitterSource();

        // To use the recorded twitter stream, use the following line
        // The number passed to the constructor is a speedup value:
        //  1.0 - play back at the recorded speed
        //  2.0 - play back twice as fast
        twitterSource = new LiveTwitterSource();

        queries = new ArrayList<>();
    }

    /**
     * A new query has been entered via the User Interface
     *
     * @param query The new query object
     */
    public void addQuery(Query query) {
        queries.add(query);
        Set<String> allTerms = getQueryTerms();
        twitterSource.setFilterTerms(allTerms);
        contentPanel.addQuery(query);
        twitterSource.addObserver(query);
    }

    /**
     * return a list of all terms mentioned in all queries. The live twitter source uses this
     * to request matching tweets from the Twitter API.
     *
     * @return
     */
    private Set<String> getQueryTerms() {
        Set<String> ans = new HashSet<>();
        for (Query q : queries) {
            ans.addAll(q.getFilter().terms());
        }
        return ans;
    }

    /**
     * Constructs the {@code Application}.
     */
    public Application() {
        super("Twitter content viewer");
        setSize(300, 300);
        initialize();

        bing = new BingAerialTileSource();

        // Do UI initialization
        contentPanel = new ContentPanel(this);
        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);

        // Always have getMap markers showing.
        getMap().setMapMarkerVisible(true);
        // Always have zoom controls showing,
        // and allow scrolling of the getMap around the edge of the world.
        getMap().setZoomContolsVisible(true);
        getMap().setScrollWrapEnabled(true);

        // Use the Bing tile provider
        getMap().setTileSource(bing);

        //NOTE This is so that the getMap eventually loads the tiles once Bing attribution is ready.
        Coordinate coord = new Coordinate(0, 0);

        Timer bingTimer = new Timer();
        TimerTask bingAttributionCheck = new TimerTask() {
            @Override
            public void run() {
                // This is the best method we've found to determine when the Bing data has been loaded.
                // We use this to trigger zooming the getMap so that the entire world is visible.
                if (!bing.getAttributionText(0, coord, coord).equals("Error loading Bing attribution data")) {
                    getMap().setZoom(2);
                    bingTimer.cancel();
                }
            }
        };
        bingTimer.schedule(bingAttributionCheck, 100, 200);

        // Set up a motion listener to create a tooltip showing the tweets at the pointer position
        getMap().addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                ICoordinate pos = getMap().getPosition(p);
                getMarkersCovering(pos, pixelWidth(p))
                        .stream()
                        .filter(MapMarkerTweet.class::isInstance)
                        .map(MapMarkerTweet.class::cast)
                        .forEach(tweet -> getMap().setToolTipText(tweet.getTwitterTip()));
            }
        });
    }

    // How big is a single pixel on the getMap?  We use this to compute which tweet markers
    // are at the current most position.
    private double pixelWidth(Point p) {
        ICoordinate center = getMap().getPosition(p);
        ICoordinate edge = getMap().getPosition(new Point(p.x + 1, p.y));
        return SphericalGeometry.distanceBetween(center, edge);
    }

    // Get those layers (of tweet markers) that are visible because their corresponding query is enabled
    private Set<Layer> getVisibleLayers() {
        Set<Layer> ans = new HashSet<>();
        for (Query q : queries) {
            if (q.getVisible()) {
                ans.add(q.getLayer());
            }
        }
        return ans;
    }

    // Get all the markers at the given getMap position, at the current getMap zoom setting
    private List<MapMarker> getMarkersCovering(ICoordinate pos, double pixelWidth) {
        List<MapMarker> ans = new ArrayList<>();
        Set<Layer> visibleLayers = getVisibleLayers();
        for (MapMarker m : getMap().getMapMarkerList()) {
            if (!visibleLayers.contains(m.getLayer())) continue;
            double distance = SphericalGeometry.distanceBetween(m.getCoordinate(), pos);
            if (distance < m.getRadius() * pixelWidth) {
                ans.add(m);
            }
        }
        return ans;
    }

    public JMapViewer getMap() {
        return contentPanel.getViewer();
    }

    /**
     * @param args Application program arguments (which are ignored)
     */
    public static void main(String[] args) {
        new Application().setVisible(true);
    }

    // Update which queries are visible after any checkBox has been changed
    public void updateVisibility() {
        SwingUtilities.invokeLater(() -> {
            System.out.println("Recomputing visible queries");
            for (Query q : queries) {
                JCheckBox box = q.getCheckBox();
                Boolean state = box.isSelected();
                q.setVisible(state);
            }
            getMap().repaint();
        });
    }

    // A query has been deleted, remove all traces of it
    public void terminateQuery(Query query) {
        queries.remove(query);
        Set<String> allTerms = getQueryTerms();
        twitterSource.setFilterTerms(allTerms);
        twitterSource.deleteObserver(query);
    }

}
