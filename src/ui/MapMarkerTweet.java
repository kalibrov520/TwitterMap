package ui;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.Layer;
import org.openstreetmap.gui.jmapviewer.MapMarkerCircle;
import twitter4j.Status;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

public class MapMarkerTweet extends MapMarkerCircle {

    private final BufferedImage tweetAvatar;
    private final int IMAGE_SIZE = 24;
    private final Color borderColor;
    private final Status tweet;

    public MapMarkerTweet(Layer layer, Coordinate coordinate, BufferedImage image, Color color, Status tweet) {
        super(layer, coordinate, MapMarkerSimple.defaultMarkerSize);
        if (image == null) {
            throw new IllegalArgumentException("Image cannot be null");
        }
        tweetAvatar = image;
        borderColor = color;
        this.tweet = tweet;
    }

    @Override
    public void paint(Graphics g, Point position, int rad) {
        if (tweetAvatar == null) {
            throw new IllegalStateException("Image cannot be null");
        }
        final ImageObserver nullImageObserver = null;
        borderedImage((Graphics2D) g, position, nullImageObserver);
    }

    private void borderedImage(Graphics2D g, Point position, ImageObserver imageObserver) {
        applyQualityRenderingHints(g);
        int border = 4;
        int offset = IMAGE_SIZE / 2;
        Color oldColor = g.getColor();
        g.setColor(borderColor);
        g.fillRect(position.x - offset - border, position.y - offset - border,
                IMAGE_SIZE + 2 * border, IMAGE_SIZE + 2 * border);
        g.setColor(oldColor);
        g.drawImage(tweetAvatar,
                position.x - offset, position.y - offset,
                IMAGE_SIZE, IMAGE_SIZE,
                imageObserver);
    }

    private void applyQualityRenderingHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
    }


    public String getTwitterTip() {
        String html = String.format("<html><p style=\"word-wrap: break-word;  width: 300px;\">" +
                        "<img src=\"%s\" alt=\"\" width=\"40\" height=\"40\" />%s</p>" +
                        "</html>",
                tweet.getUser().getOriginalProfileImageURL(),
                tweet.getText());

        return html;
    }
}
