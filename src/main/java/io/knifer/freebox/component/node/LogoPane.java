package io.knifer.freebox.component.node;

import io.knifer.freebox.util.ValidationUtil;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 带默认占位、自动加载图片链接的LOGO展示布局
 *
 * @author Knifer
 */
public class LogoPane extends StackPane {

    private final Label placeholderLabel;
    private final ImageView logoImageView;
    private final static List<Pair<Paint, Paint>> BACKGROUND_TEXT_PAINT_PAIRS = List.of(
            Pair.of(Color.rgb(70, 130, 180), Color.WHITE),
            Pair.of(Color.rgb(255, 127, 80), Color.WHITE),
            Pair.of(Color.rgb(39, 139, 34), Color.WHITE),
            Pair.of(Color.rgb(200, 160, 0), Color.WHITE),
            Pair.of(Color.rgb(85, 107, 47), Color.WHITE),
            Pair.of(Color.rgb(255, 140, 0), Color.WHITE),
            Pair.of(Color.rgb(188, 10, 40), Color.WHITE),
            Pair.of(Pair.of(Color.rgb(138, 43, 226), Color.WHITE)),
            Pair.of(Color.rgb(65, 105, 225), Color.WHITE),
            Pair.of(Color.rgb(255, 105, 180), Color.WHITE),
            Pair.of(Color.rgb(210, 105, 30), Color.WHITE),
            Pair.of(Color.rgb(0, 139, 139), Color.WHITE)
    );
    /**
     * LOGO 图片缓存
     * key=logoUrl，value=Image
     */
    private static final Map<String, Image> IMAGE_CACHE_MAP = new HashMap<>();

    public LogoPane(double size, double placeHolderFontSize) {
        this(size, size, placeHolderFontSize);
    }

    public LogoPane(double imageSize, double placeHolderSize, double placeHolderFontSize) {
        super();
        int colorPairIdx = RandomUtils.nextInt(0, BACKGROUND_TEXT_PAINT_PAIRS.size() - 1);
        Pair<Paint, Paint> colorPair = BACKGROUND_TEXT_PAINT_PAIRS.get(colorPairIdx);
        Rectangle logoImageViewClip = new Rectangle(imageSize, imageSize);
        double paneSize = Math.max(imageSize, placeHolderSize);
        Rectangle placeholderBackground = new Rectangle(placeHolderSize, placeHolderSize);

        this.setPrefSize(paneSize, paneSize);
        this.setMinSize(paneSize, paneSize);
        this.setMaxSize(paneSize, paneSize);
        placeholderLabel = new Label();
        placeholderLabel.setAlignment(Pos.CENTER);
        placeholderLabel.setFont(Font.font(null, FontWeight.BOLD, placeHolderFontSize));
        placeholderLabel.setTextFill(colorPair.getRight());
        placeholderBackground.setFill(colorPair.getLeft());
        placeholderBackground.setArcWidth(20);
        placeholderBackground.setArcHeight(20);
        logoImageView = new ImageView();
        logoImageView.setClip(logoImageViewClip);
        logoImageView.setFitWidth(imageSize);
        logoImageView.setFitHeight(imageSize);
        logoImageView.setPreserveRatio(true);
        placeholderBackground.visibleProperty().bind(logoImageView.visibleProperty().not());
        placeholderLabel.visibleProperty().bind(logoImageView.visibleProperty().not());
        this.getChildren().addAll(placeholderBackground, placeholderLabel, logoImageView);
    }

    public void setTitleAndLogoUrl(String title, String logoUrl) {
        setPlaceholderText(title);
        setLogoUrl(logoUrl);
    }

    private void setPlaceholderText(String text) {
        placeholderLabel.setText(StringUtils.substring(text, 0, 1));
    }

    private void setLogoUrl(String logoUrl) {
        Image logoImageCached;
        Image logoImage;

        if (ValidationUtil.isURL(logoUrl)) {
            logoImageCached = IMAGE_CACHE_MAP.get(logoUrl);
            if (logoImageCached != null) {
                logoImageView.setImage(logoImageCached);
            } else {
                showPlaceholder(true);
                logoImage = new Image(logoUrl, true);
                logoImage.progressProperty()
                        .addListener((ob, oldVal, newVal) -> {
                            if (newVal.doubleValue() >= 1.0 && !logoImage.isError()) {
                                IMAGE_CACHE_MAP.put(logoUrl, logoImage);
                                logoImageView.setImage(logoImage);
                                showPlaceholder(false);
                            }
                        });
            }
        } else {
            showPlaceholder(true);
        }
    }

    private void showPlaceholder(boolean show) {
        logoImageView.setVisible(!show);
    }
}
