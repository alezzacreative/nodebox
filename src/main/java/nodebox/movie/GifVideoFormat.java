package nodebox.movie;

import java.util.ArrayList;

public class GifVideoFormat extends AbstractVideoFormat {
    public static final GifVideoFormat HighQualityGif = new GifVideoFormat("Animated GIF (High Quality)", true);
    public static final GifVideoFormat StandardGif = new GifVideoFormat("Animated GIF", false);

    private boolean highQuality;

    private GifVideoFormat(String displayName, boolean highQuality) {
        super(displayName, "gif");
        this.highQuality = highQuality;
    }

    public ArrayList<String> getArgumentList(Movie movie) {
        ArrayList<String> argumentList = new ArrayList<String>();
        if (highQuality) {
            // High-quality palette generation for crisp, banding-free animated GIFs
            argumentList.add("-vf");
            argumentList.add("split[s0][s1];[s0]palettegen=stats_mode=diff[p];[s1][p]paletteuse=dither=bayer:bayer_scale=3");
        }
        argumentList.add("-loop");
        argumentList.add("0");
        argumentList.add("-f");
        argumentList.add("gif");
        return argumentList;
    }
}
